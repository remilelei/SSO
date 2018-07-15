
#include "remile.h"

namespace ssodev {

    // 加解密相关
    std::string encrypt(std::string str_content, std::string str_public_key);
    std::string decrypt(std::string str_content, std::string str_private_key);
    std::string read_private_key_from_public_key(std::string str_public_key);

    std::shared_ptr<SsoDev> SsoDev::create(const std::string & address, const std::string & ca) {
        return std::make_shared<SsoClient>(address, ca);
    }

    SsoClient::SsoClient(const std::string & address, const std::string & ca) {
        grpc::SslCredentialsOptions opts;
        opts.pem_root_certs = ca;
        auto channelCredentials = grpc::SslCredentials(opts);
        auto channel = grpc::CreateChannel(address, channelCredentials);
        stub_ = SsoProcessor::NewStub(channel);
    }

    // 拉取公钥
    std::string SsoClient::get_public_key() {
        // 构造请求
        ReqGetRegKey request;
        RspGetRegKey response;

        // 发起请求
        ClientContext context;
        Status status = stub_->generateKey(&context, request, &response);

        // 处理结果
        if(status.ok()) {
            return response.key();
        } else {
            LOGI("getPublicKey errorCode=%d", static_cast<grpc_status_code>(status.error_code()));
            LOGI("getPublicKey errorMsg=%s", status.error_message().c_str());
            LOGI("getPublicKey errorDetail=%s", status.error_details().c_str());
            return "error";
        }
    }

    // 注册
    int32_t SsoClient::do_register(const std::string & username,
                                   const std::string & password, const std::string & mac,
                                   const std::string & public_key) {
        /*
         * 拼接json并加密
         * 举例：{"userId":0,"userName":"remile","passWord":"123456","lastLoginTime":0}
         */
        std::string json = "{\"userId\":0,\"userName\":\"";
        json.append(username)
                .append("\",\"passWord\":\"")
                .append(password)
                .append("\",\"loginMac\":\"")
                .append(mac)
                .append("\"}");
        std::string str_json_encrypted = encrypt(json, public_key);

        // 构造请求
        RegUserInfo request;
        RegResult response;
        request.set_encrypteduserinfo(str_json_encrypted);

        // 发起请求
        ClientContext context;
        Status status = stub_->doRegister(&context, request, &response);

        // 处理结果
        if(status.ok()) {
            return response.regresult();
        } else {
            LOGI("doRegister errorCode=%d", static_cast<grpc_status_code>(status.error_code()));
            LOGI("doRegister errorMsg=%s", status.error_message().c_str());
            LOGI("doRegister errorDetail=%s", status.error_details().c_str());
            return -1;
        }
    }

    // 登录
    std::string SsoClient::do_login(const std::string & username,
                                    const std::string & password, const std::string & mac,
                                    const std::string & public_key) {

        /*
         * 拼接json并加密
         * 举例：{"userId":0,"userName":"remile","passWord":"123456","lastLoginTime":0}
         */
        std::string json = "{\"userId\":0,\"userName\":\"";
        json.append(username)
                .append("\",\"passWord\":\"")
                .append(password)
                .append("\",\"loginMac\":\"")
                .append(mac)
                .append("\"}");
        std::string str_json_encrypted = encrypt(json, public_key);

        // 构造请求
        ReqLogin request;
        RspLogin response;
        request.set_encrypteduserinfo(str_json_encrypted);

        // 发起请求
        ClientContext context;
        Status status = stub_->doLogin(&context, request, &response);

        // 处理结果
        if(status.ok()) {
            if(response.loginresult() == 0) {
                return *response.release_encryptedticket();
            } else {
                return "accountError";
            }
        } else return "serverError";
    }

    // 创建长连接
    int32_t SsoClient::do_keep_touch(const std::string & username,
                                  const std::string & mac, const std::string & ticket) {
        if(stream_ == nullptr) { // 当前尚未创建连接，开始创建
            // 创建双向流
            ClientContext context;
            std::shared_ptr<::grpc::ClientReaderWriter<::sso::CommonMessage, ::sso::CommonMessage>> stream(
                    stub_->keepTouch(&context)
            );
            stream_ = stream;

            // 创建发起连接的请求并发送
            CommonMessage msg;
            msg.set_msgcode(0);
            msg.set_sender(username);
            msg.set_sendermac(mac);
            msg.set_msgcontent(ticket);
            stream->Write(msg);

            // 服务端发来的消息在这里处理
            int res = 0;
            while(stream->Read(&msg)) {
                LOGD("msgcode=%d", msg.msgcode());
                int msg_code = msg.msgcode();
                if(msg_code == 0) {
                    LOGI("link success");
                } else if(msg_code == 1) {
                    // 收到"下线包"，提示上层被踢下线。
                    LOGI("kicked...");
                    CommonMessage c;
                    c.set_msgcode(1);
                    stream->Write(c);
                    stream->WritesDone();
                    res = 1;
                    break;
                } else if(msg_code == 2) {
                    // 后台批准下线
                    LOGI("leave now");
                    stream->WritesDone();
                    res = 2;
                    break;
                }
            }

            // 连接中断
            LOGI("read finished");
            stream->Finish();
            stream_ = nullptr;
            LOGI("link finished");
            return res;
        } else { // 已有连接，忽略
            return -1;
        }
    }

    // 下线
    int32_t SsoClient::do_logout(const std::string & username, const std::string & ticket) {
        if(stream_ != nullptr) {
            stream_->WritesDone();
        }
        return 0;
    }

    // 测试
    std::string SsoClient::test(const std::string & t) {
        return t + " recvd";
    }

    // 从公钥中获取私钥
    std::string read_private_key_from_public_key(std::string str_public_key)
    {
        std::string ret;
        for(int i = 0; i < str_public_key.length(); ++ i) {
            if(i % 2 == 0) {
                ret += str_public_key[i] ^ str_public_key.length();
            }
        }
        return ret;
    }

    // 使用公钥加密
    std::string encrypt(std::string str_content, std::string str_public_key)
    {
        std::string privateKey = read_private_key_from_public_key(str_public_key);

        int lenContent = str_content.length();
        int lenResult = lenContent * 2;
        char encryptedText[lenResult + 1];
        for(int i = 0; i < lenContent; i ++) {
            encryptedText[lenResult - i * 2 - 1] = str_content[i];
            if(lenResult - 2 - i * 2>= 0) {
                encryptedText[lenResult - 2 - i * 2] = privateKey[i % privateKey.length()];
            }
            // 这个算法有坑，字符串里会出现 '\0'
//        encryptedText[str_content.length() - 1 - i]
//        = (str_content[i] ^ privateKey[i % privateKey.length()]);
//        std::cout << str_content.length() - 1 - i << "=" << (int)encryptedText[str_content.length() - 1 - i] << ' ';
        }
        encryptedText[lenResult] = '\0';
        return encryptedText;
    }

    // 使用私钥解密
    std::string decrypt(std::string str_content, std::string str_private_key)
    {
        int lenRet = str_content.length() / 2;
        int lenArg = str_content.length();
        char ret[lenRet + 1];

        for(int i = 0; i < lenRet; ++ i) {
            ret[i] = str_content[lenArg - 1 - 2 * i];
        }
        ret[str_content.length()] = '\0';
        return ret;
    }
}

# SSO系统设计文档

---

## 1 系统功能

系统命题如下：
>具备注册登录功能
 一个⽤用户只能在⼀一个设备上登录，切换终端登录时，其他已登录的终端会被踢出 
 后台可以根据管理理策略略将已登录设备踢出

总结一下：注册，登录，登录状态的维持与销毁。

## 2 程序设计
这里描述一下程序的思路，并且概括性的说一下个人实现的思想与实现方法。
### 2.1 流程设计
#### 2.1.1 注册
![此处输入图片的描述][1]
这里采用不对称加密的方式，加强了传输本身与敏感信息的安全。流程开始时从后台取到RSA算法的公钥，并保存在本地。后续用户提交用于注册的信息（用户名，密码）时，使用该公钥加密（公钥只能用来加密，无法解密）并把加密的内容提交至后台服务。后台服务得到数据后使用自己的私钥来解密，并把解密的数据存入数据库处理，并对其Mysql账户管理的模式把密码转为MD5值。存储结果经由后台服务转交至客户端。
#### 2.1.2 登录
基本连接方式与注册流程基本相似。作为SSO单点登录系统需要提一下的是,`登录对于客户端有两种方式:  1、用户输入账号密码登录。 2、用户近期输入过账号密码，使用之前的票据登录`。无票据时，用户输入的账号密码信息与设备信息加密提供给后台。后台利用数据库验证账号信息是否正确，如果信息正确的话，结合客户端提供的账号信息、s设备信息以及后台当前系统时间（用来后期计算时候过期），按照一定算法生成票据并反馈给客户端。 后续客户端有票据时，经历需要身份验证的场景只需要提供该票据至后台，后台根据逆向算法从票据中取出账号信息、设备信息与票据生成时间，后台校验利用这三个内容来决定该票据是否有效以及身份验证是否通过。如果不通过，客户端重新进行手动身份验证。
#### 2.1.3 登录状态的变化控制
![此处输入图片的描述][2]
客户端的在线状态本身维护于长连接中，根据长连接建立的请求在服务端抽象维护一个会话层，会话层中每一个会话都保存着用户的唯一索引和设备地址。当服务端收到一个长连接建立请求，并且会话层用有一个会话的用户信息与本次请求的用户信息一致但是设备地址不一致则打断会话中原本的长连接，再根据本次请求来建立新的长连接。
### 2.2 涉及工具
1.后台：据要求，使用RGPC。
2.数据存储：mysql。

## 3 具体实现
### 3.1 后台实现
#### 3.1.1 RSA公私钥
![此处输入图片的描述][3]
#### 3.1.2 票据生成
![此处输入图片的描述][4]
#### 3.1.3 关于长连接
真正实现长连接比较麻烦，包括心跳轮训等开发代价比较大。时间原因，这里先不具体实现了，所幸GRPC提供的双向流通信具有长连接的性质，本次长连接就是用的双向流来做的。
#### 3.1.4 会话管理
![此处输入图片的描述][5]
简单的说：使用Hash表维护一个会话层，针对该表中会话执行我们的状态转化工作。
### 3.2 客户端实现
执行开发客户端的时候不巧时间比较紧，为了不让完成时间拖太久，免去配置环境流程就直接在官方提供的example上修改了哈^o^~
单点登录的执行流程在文档的服务端部分已经介绍了这里就不赘述了。客户端相对于后台的逻辑较为简单：流程启动时从服务端拉取公钥，后续发送给服务端的信息都使用该公钥加密，传达到服务端后服务端再使用本地的私钥解密。

#### 3.2.1 Djinni接口
这里接口规范按要求使用Djinni生成，Djinni规范文件内容如下：
```
sso_dev = interface +c {
	static create(address:string, ca:string):sso_dev;

	// 拉取公钥
	get_public_key(): string;

	// 注册
	do_register(username:string, password:string, mac:string, public_key:string): i32;

	// 登录
	do_login(username:string, password:string, mac:string, public_key:string): string;

	// 请求长连接
	do_keep_touch(username:string, mac:string, ticket:string): i32;

	// 登出
	do_logout(username:string, ticket:string): i32;
	test(t:string):string;

} 
```

#### 3.2.2 客户端下层逻辑实现
对于上述接口的实现如下：
``` c++
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
```
### 3.3 协议
直接码上看：
```
service SsoProcessor {
    // 拉公钥
    rpc generateKey(ReqGetRegKey) returns (RspGetRegKey) {}

    // 注册
    rpc doRegister(RegUserInfo) returns (RegResult) {}

    // 登录
    rpc doLogin(ReqLogin) returns (RspLogin) {}
    rpc doLoginWithTicket(ReqLoginWithTicket) returns (RspLoginWithTicket) {}

    // 在线
    rpc keepTouch(stream CommonMessage) returns (stream CommonMessage) {}
}

message ReqGetRegKey { // 获取公钥请求

}

message RspGetRegKey { // 返回给客户端生成的key
    bytes key = 1;
}

message RegUserInfo { // 加密过的用户信息，服务端解密后执行注册逻辑
    string encryptedUserInfo = 1;
}

message RegResult { // 返回给客户端用户信息
    int32 regResult = 1;  // 0=成功 1=用户名被占用 2=不明原因
}

message ReqLogin {
    string encryptedUserInfo = 1;
}

message RspLogin {
    int32 loginResult = 1; // 0=成功 1=账号密码失败 2=RSA证书失效
    string encryptedTicket = 2;
}

message ReqLoginWithTicket {
    string encryptedTicket = 1;
}

message RspLoginWithTicket {
    int32 loginResult = 1; // 0=成功 1=账号密码失败 2=RSA证书失效
}

message CommonMessage {
    int32 msgCode = 1; // 0=建立链接 1=下线指令
    string msgContent = 2;
    string sender = 3;
    string senderMac = 4;
}
```
可以发现请求包体中很多请求都只有一个参数，是一个加密的json。目前的设计和流程可能存在一定的瑕疵，为了方便后续的完善和拓展，所以这里设计为一个加密的json方便我们的拓展。这样如果有修改，能降低对前后端接口的影响。
### 3.4 数据库
```
+---------------+------------------+------+-----+---------+----------------+
| Field         | Type             | Null | Key | Default | Extra          |
+---------------+------------------+------+-----+---------+----------------+
| UserId        | int(11) unsigned | NO   | PRI | NULL    | auto_increment |
| UserName      | char(16)         | NO   | UNI |         |                |
| password      | char(64)         | NO   |     |         |                |
| loginMac      | char(64)         | NO   |     |         |                |
| lastLoginTime | timestamp        | YES  |     | NULL    |                |
+---------------+------------------+------+-----+---------+----------------+
```
存储这里设计的比较简单，只在ID和账号上有索引约束。如果有对应要求后面，这里可以针对要求去做修改。

## 4 小结
简单的实现了一个单点登录系统，后端为Grpc-java，前端为Android + Grpc-c++。
单点登录的基本原理：

 1. 完成账号密码登录后下发票据，使用该票据可以直接通过身份验证，避免多次输入账号密码。
 2. 使用一个HashMap抽象出会话层，把已经创建了的长连接维护在会话层，并且使用用户信息作为key。
 3. 下线实际上是后台发送一条“下线指令消息”告诉客户端有其他客户端登录了这个账号，之后后台就关闭了和他的长连接。从这个意义上完成“被踢下线”的工作。
 4. 根据策略下线用户，题中没有给出是何种策略，但是任何一种策略都会筛选出一个ID集合，这里策略下线的逻辑是使用策略生成的ID集合，遍历集合找到会话层中对应的会话，向客户端发送“下线指令消息”。
 
  [1]: http://chuantu.biz/t6/337/1530450701x-1566657759.png
  [2]: https://image.ibb.co/d6ZB48/image.png
  [3]: https://image.ibb.co/jo8hBo/image.png
  [4]: http://chuantu.biz/t6/337/1530453872x-1566657759.png
  [5]: https://image.ibb.co/mawVxT/image.png
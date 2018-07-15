
#ifndef HELLOWORLD_REMILE_H
#define HELLOWORLD_REMILE_H

#include <grpc++/grpc++.h>
#include <jni.h>
#include <thread>
#include <android/log.h>
#include <google/protobuf/compiler/plugin.pb.h>
#include "sso.grpc.pb.h"
#include "sso_dev.hpp"

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "remileNative", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG ,  "remileNative", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO  ,  "remileNative", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN  ,  "remileNative", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "remileNative", __VA_ARGS__)

// GRPC 框架接口对象
using grpc::Channel;
using grpc::ClientContext;
using grpc::Server;
using grpc::ServerBuilder;
using grpc::ServerContext;
using grpc::Status;
using grpc::SslCredentials;

// 请求实现
using sso::SsoProcessor;

// 拉取加密公钥
using sso::ReqGetRegKey;
using sso::RspGetRegKey;

// 注册
using sso::RegUserInfo;
using sso::RegResult;

// 登录
using sso::ReqLogin;
using sso::RspLogin;
using sso::ReqLoginWithTicket;
using sso::RspLoginWithTicket;

// 普通消息
using sso::CommonMessage;

namespace ssodev {
    class SsoClient : public ssodev::SsoDev {
    public:

        SsoClient(const std::string & address, const std::string & ca);

        // 拉取公钥
        std::string get_public_key();

        // 注册
        int32_t do_register(const std::string & username, const std::string & password, const std::string & mac, const std::string & public_key);

        // 登录
        std::string do_login(const std::string & username, const std::string & password, const std::string & mac, const std::string & public_key);

        // 创建长连接
        int32_t do_keep_touch(const std::string & username, const std::string & mac, const std::string & ticket);

        // 下线
        int32_t do_logout(const std::string & username, const std::string & ticket);

        // 测试
        std::string test(const std::string & t);


    private:
        std::unique_ptr<SsoProcessor::Stub> stub_;
        std::shared_ptr<::grpc::ClientReaderWriter<::sso::CommonMessage, ::sso::CommonMessage>> stream_;
    };
}


#endif //HELLOWORLD_REMILE_H

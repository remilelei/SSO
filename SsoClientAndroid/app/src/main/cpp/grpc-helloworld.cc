#include <atomic>

#include <grpc++/grpc++.h>
#include <jni.h>
#include <thread>
#include <android/log.h>
#include <google/protobuf/compiler/plugin.pb.h>
#include "sso.grpc.pb.h"
#include "helloworld.grpc.pb.h"
#include "djinni/remile.h"

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "remileNative", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG ,  "remileNative", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO  ,  "remileNative", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN  ,  "remileNative", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "remileNative", __VA_ARGS__)


using ssodev::SsoClient;
using ssodev::SsoDev;

static std::shared_ptr<SsoDev> client;

// 注册逻辑
extern "C"
JNIEXPORT jint JNICALL
Java_io_grpc_helloworldexample_cpp_HelloworldActivity_doRegister(JNIEnv *env, jobject obj_this,
                                                                 jstring username_,
                                                                 jstring password_, jstring mac_,
                                                                 jbyteArray publicKey_) {
    const char *username = env->GetStringUTFChars(username_, 0);
    const char *password = env->GetStringUTFChars(password_, 0);
    const char *mac = env->GetStringUTFChars(mac_, 0);
    jbyte *publicKey = env->GetByteArrayElements(publicKey_, NULL);

    // 转化公钥
    char * c_publicKey = nullptr; // 公钥
    int key_len = env->GetArrayLength(publicKey_);
    c_publicKey = new char[key_len];
    memset(c_publicKey, 0, key_len);
    memcpy(c_publicKey, publicKey, key_len);


    // 发起请求，得到注册结果
    int regResult = 0;
    if(client == nullptr) {
        regResult =  -1;
    } else {
        regResult = client->do_register(username, password, mac, c_publicKey);
    }

    // 资源释放
    env->ReleaseStringUTFChars(username_, username);
    env->ReleaseStringUTFChars(password_, password);
    env->ReleaseStringUTFChars(mac_, mac);
    env->ReleaseByteArrayElements(publicKey_, publicKey, 0);
    return regResult;
}

// 登录逻辑---账号密码登录
extern "C"
JNIEXPORT jstring JNICALL
Java_io_grpc_helloworldexample_cpp_HelloworldActivity_login(JNIEnv *env, jobject this_obj,
                                                            jstring username_,
                                                            jstring password_,
                                                            jstring mac_,
                                                            jbyteArray publicKey_) {
    const char *username = env->GetStringUTFChars(username_, 0);
    const char *password = env->GetStringUTFChars(password_, 0);
    const char *mac = env->GetStringUTFChars(mac_, 0);
    jbyte *publicKey = env->GetByteArrayElements(publicKey_, NULL);

    // 转化公钥
    char * c_publicKey = nullptr; // 公钥
    int key_len = env->GetArrayLength(publicKey_);
    c_publicKey = new char[key_len];
    memset(c_publicKey, 0, key_len);
    memcpy(c_publicKey, publicKey, key_len);


    // 发起请求，得到注册结果
    std::string str_login_res;
    if(client == nullptr) {
        str_login_res = "clientError";
    } else {
        str_login_res = client->do_login(username, password, mac, c_publicKey);
    }

    // 资源释放
    env->ReleaseStringUTFChars(username_, username);
    env->ReleaseStringUTFChars(password_, password);
    env->ReleaseStringUTFChars(mac_, mac);
    env->ReleaseByteArrayElements(publicKey_, publicKey, 0);

    return env->NewStringUTF(str_login_res.c_str());
}

// 登录逻辑---票据登录
extern "C"
JNIEXPORT jint JNICALL
Java_io_grpc_helloworldexample_cpp_HelloworldActivity_loginWithTicket(JNIEnv *env, jobject instance,
                                                                      jstring host_, jint port,
                                                                      jstring ticket_,
                                                                      jbyteArray publicKey_) {
    const char *host = env->GetStringUTFChars(host_, 0);
    const char *ticket = env->GetStringUTFChars(ticket_, 0);
    jbyte *publicKey = env->GetByteArrayElements(publicKey_, NULL);

    // 类型转换
    int iPort = static_cast<int>(port);
    std::string str_host(host, env->GetStringUTFLength(host_));
    std::string str_ticket(ticket, env->GetStringLength(ticket_));

    // 转化公钥
    char * c_publicKey = nullptr; // 公钥
    int key_len = env->GetArrayLength(publicKey_);
    c_publicKey = new char[key_len];
    memset(c_publicKey, 0, key_len);
    memcpy(c_publicKey, publicKey, key_len);

    // 拼接域名（IP）与端口
    const int buf_size = 1024;
    char host_port[buf_size]; // 域名(IP):端口
    snprintf(host_port, buf_size, "%s:%d", str_host.c_str(), iPort);

    LOGI("loginWithTicket ticket=%s", str_ticket.c_str());

    // 建立连接并发起请求，得到注册结果
    auto channelCredentials = grpc::InsecureChannelCredentials();
    auto channel = grpc::CreateChannel(host_port, channelCredentials);
//    SsoClient client(channel);
//    int login_res = client.doLoginWithTicket(str_ticket);


    // 资源释放
    env->ReleaseStringUTFChars(host_, host);
    env->ReleaseStringUTFChars(ticket_, ticket);
    env->ReleaseByteArrayElements(publicKey_, publicKey, 0);

    return 0;
}

// 测试
extern "C"
JNIEXPORT jstring JNICALL
Java_io_grpc_helloworldexample_cpp_HelloworldActivity_testJni__(JNIEnv *env, jobject instance) {

    // TODO
    std::string res = "JNI---success";

//    jclass cls = env->GetObjectClass(instance);
//    jmethodID is_cancelled_mid =
//            env->GetMethodID(cls, "isRunServerTaskCancelled", "()Z");
//    env->CallBooleanMethod(instance, is_cancelled_mid);

//    if(client == nullptr) {
//        client = SsoClient::create();
//    }
//
//    res = client->test(res);
//    LOGI("test res=%s", res.c_str());

    return env->NewStringUTF(res.c_str());

}

// 登出操作
extern "C"
JNIEXPORT jint JNICALL
Java_io_grpc_helloworldexample_cpp_HelloworldActivity_logout(JNIEnv *env, jobject instance,
                                                             jstring username_, jstring ticket_) {
    const char *username = env->GetStringUTFChars(username_, 0);
    const char *ticket = env->GetStringUTFChars(ticket_, 0);

    if(client != nullptr) {
        client->do_logout(username, ticket);
    }

    env->ReleaseStringUTFChars(username_, username);
    env->ReleaseStringUTFChars(ticket_, ticket);
    return 0;
}

// 建立长连接
extern "C"
JNIEXPORT void JNICALL
Java_io_grpc_helloworldexample_cpp_HelloworldActivity_keepTouch(JNIEnv *env, jobject instance,
                                                                jstring username_, jstring mac_,
                                                                jstring ticket_) {
    const char *username = env->GetStringUTFChars(username_, 0);
    const char *mac = env->GetStringUTFChars(mac_, 0);
    const char *ticket = env->GetStringUTFChars(ticket_, 0);

    // 获取java回调方法
    jclass cls = env->GetObjectClass(instance);
    jmethodID handleServerMessage =
            env->GetMethodID(cls, "handleServerMessage", "(I)V");

    // 创建长连接
    int res = -1;
    if(client != nullptr) {
        res = client->do_keep_touch(username, mac, ticket);
    }
    env->CallVoidMethod(instance, handleServerMessage, res);

    env->ReleaseStringUTFChars(username_, username);
    env->ReleaseStringUTFChars(mac_, mac);
    env->ReleaseStringUTFChars(ticket_, ticket);
}

// 拉取公钥提供给上层
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_io_grpc_helloworldexample_cpp_HelloworldActivity_getPublicKey__Ljava_lang_String_2ILjava_lang_String_2(
        JNIEnv *env, jobject instance, jstring host_, jint port, jstring ca_) {
    // 类型转换
    const char *host = env->GetStringUTFChars(host_, 0);
    const char *ca = env->GetStringUTFChars(ca_, 0);
    int iPort = static_cast<int>(port);

    // 拼接地址
    const int buf_size = 1024;
    char host_port[buf_size];
    snprintf(host_port, buf_size, "%s:%d", host, iPort);
    LOGI("create channel by host=%s", host_port);

    // 初始化Client，发起请求
    if(client == nullptr) {
        client = SsoClient::create(host_port, ca);
    }
    std::string publicKey = client->get_public_key();
    LOGI("get key=%s len=%d", publicKey.c_str(), publicKey.length());

    // 转化公钥，返回给上层
    jbyte jByteBuf[buf_size];
    memset(&jByteBuf, 0, buf_size);
    memcpy(&jByteBuf, publicKey.c_str(), publicKey.length());
    for(int i = 0; i < publicKey.length(); ++ i) {
        jByteBuf[i] = publicKey.at(i);
    }
    jbyteArray  jArrByte = env->NewByteArray(publicKey.length());
    env->SetByteArrayRegion(jArrByte, 0, publicKey.length(), jByteBuf);

    env->ReleaseStringUTFChars(host_, host);
    env->ReleaseStringUTFChars(ca_, ca);

    return jArrByte;
}
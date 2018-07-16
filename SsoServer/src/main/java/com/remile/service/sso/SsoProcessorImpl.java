package com.remile.service.sso;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.remile.dao.UserInfoDAO;
import com.remile.datamodel.Session;
import com.remile.datamodel.UserInfo;
import com.remile.grpc.sso.*;
import com.remile.service.session.UserOnlineStatusManager;
import com.remile.util.KeyPairGenUtil;
import io.grpc.stub.StreamObserver;
import com.remile.util.Log;
import com.remile.util.RSAUtil;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentHashMap;

public class SsoProcessorImpl extends SsoProcessorGrpc.SsoProcessorImplBase {
    final public static String TAG = "RegisterImpl";

    private final static int REG_ERR_SUCCESS = 0;
    private final static int REG_ERR_ACCOUNT_EXIST = 1;
    private final static int REG_ERR_BAD_RSA = 2;

    private final static int LOGIN_ERR_SUCCESS = 0;
    private final static int LOGIN_ERR_PWD = 1;
    private final static int LOGIN_ERR_BAD_RSA = 2;

    public final static int MESSAGE_CODE_LINK = 0;
    public final static int MESSAGE_CODE_KICK = 1;
    public final static int MESSAGE_CODE_LOGOUT = 2;
    public final static int MESSAGE_TICKET_INVALID = 3;

    public final static int TICKET_INVALID_DURATION = 60 * 60 * 1000;

    /**
     * 这里抽象一个简单的会话层，用来管理用户在线状态。
     */
    UserOnlineStatusManager onlineManager = new UserOnlineStatusManager();

    @Override
    /**
     * 客户端获取注册流程用来加密的公钥
     */
    public void generateKey(ReqGetRegKey request, StreamObserver<RspGetRegKey> responseObserver) {
        byte[] publicKey = /*RSAUtil.getKey(RSAUtil.PUBLIC_KEY_FILE).getEncoded();*/
                KeyPairGenUtil.readKey("PublicKey.remile");
        Log.info(TAG, "generateKey len=" + publicKey.length);
        System.out.print("publicKey");
        for(byte b : publicKey) {
            System.out.print(b + " ");
        }
        System.out.println();
        RspGetRegKey reply = null;
        reply = RspGetRegKey.newBuilder().setKey(
                        ByteString.copyFrom(publicKey))
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    /**
     * 执行注册操作
     * 客户端用公钥加密了提交的账号及密码，这里用私钥来解密。
     * 之后读写数据库
     */
    public void doRegister(RegUserInfo request, StreamObserver<RegResult> responseObserver) {
        // 1，解密客户端发来的文本
        String encryptedUserInfo = request.getEncryptedUserInfo();
        String decryptedUserInfo = null;
        try {

            byte[] privateKey = KeyPairGenUtil.readKey("PrivateKey.remile");
            decryptedUserInfo = KeyPairGenUtil.decrypt(encryptedUserInfo, privateKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(decryptedUserInfo != null) { // 解密成功
            Log.info(TAG, "doRegister decryptedUserInfo=" + decryptedUserInfo);

            // 2，结构化文本内容
            Gson gson = new Gson();
            UserInfo userInfo = gson.fromJson(decryptedUserInfo, UserInfo.class);

            // 3，持久化。可能会因为账号已存在而失败
            UserInfoDAO userInfoDAO = UserInfoDAO.getInstance();
            int errCode = -1;
            try {
                int res = userInfoDAO.saveUserInfo(userInfo);
                if(res > 0) errCode = REG_ERR_SUCCESS;
                else errCode = REG_ERR_ACCOUNT_EXIST; // 账号已存在
            } catch (Exception e) {
                errCode = REG_ERR_BAD_RSA;
                e.printStackTrace();
            }
            Log.info(TAG, "doRegister errCode=" + errCode);
            RegResult reply = RegResult.newBuilder().setRegResult(errCode).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } else { //解密不成功，客户端和服务端证书没有对齐，告知客户端
            RegResult reply = RegResult.newBuilder().setRegResult(REG_ERR_BAD_RSA).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

    @Override
    /**
     * 一般登录，使用账号密码
     */
    public void doLogin(ReqLogin request, StreamObserver<RspLogin> responseObserver) {
        // 1，解密客户端数据，结构化登录信息
        String encryptedUserInfo = request.getEncryptedUserInfo();
        if(encryptedUserInfo != null && !"".equals(encryptedUserInfo)) {
            String decryptedUserInfo = null;
            try {
                byte[] privateKey = KeyPairGenUtil.readKey("PrivateKey.remile");
                decryptedUserInfo = KeyPairGenUtil.decrypt(encryptedUserInfo, privateKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(decryptedUserInfo != null) { // 成功解密
                Log.info(TAG, "doLogin decryptedUserInfo=" + decryptedUserInfo);
                Gson gson = new Gson();
                UserInfo userInfo = gson.fromJson(decryptedUserInfo, UserInfo.class);

                // 2，查询数据库
                UserInfoDAO dao = UserInfoDAO.getInstance();
                boolean loginSuccess = dao.login(userInfo);

                // 3，返回登录结果，如果成功了返回票据
                RspLogin reply = null;
                Log.info(TAG, "doLogin loginSuccess=" + loginSuccess);
                if (loginSuccess) { // 3-1，登录成功，返回登录结果和票据
                    String encryptTicket = RSAUtil.generatTicket(userInfo);
                    Log.info(TAG, "doLogin encryptTicket=" + encryptTicket);
                    reply = RspLogin.newBuilder().setLoginResult(LOGIN_ERR_SUCCESS)
                            .setEncryptedTicket(encryptTicket).build();
                } else { // 3-2，登录失败，只返回结果
                    reply = RspLogin.newBuilder().setLoginResult(LOGIN_ERR_PWD).build();
                }
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } else { //解密不成功，客户端和服务端证书没有对齐，告知客户端
                RspLogin reply = RspLogin.newBuilder().setLoginResult(LOGIN_ERR_BAD_RSA).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            }
        } else {
            Log.warning(TAG, "doLogin encryptedUserInfo is null");
        }
    }

    @Override
    /**
     * 使用票据登录，票据也可以用来其他场景身份验证
     */
    public void doLoginWithTicket(ReqLoginWithTicket request, StreamObserver<RspLoginWithTicket> responseObserver) {
        String encryptedTicket = request.getEncryptedTicket();
        Log.info(TAG, "doLoginWithTicket encryptTicket=" + encryptedTicket);
        if(encryptedTicket != null && !"".equals(encryptedTicket)) {
            // 1，先从票据里面读出用户信息
            UserInfo userInfo = null;
            try {
                userInfo = RSAUtil.getUserInfoFromTicket(encryptedTicket);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(userInfo != null) {
                Log.info(TAG, "doLoginWithTicket userInfo=" + userInfo);
                // 2，查询数据库
                UserInfoDAO dao = UserInfoDAO.getInstance();
                boolean res = dao.loginByTicket(userInfo);
                Log.info(TAG, "doLoginWithTicket res=" + res);

                // 3，回复客户端
                RspLoginWithTicket reply = RspLoginWithTicket.newBuilder()
                        .setLoginResult(res? LOGIN_ERR_SUCCESS: LOGIN_ERR_PWD)
                        .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } else { // 没有解析到用户信息，客户端证书没有和服务端对其，告知客户端
                Log.warning(TAG, "doLoginWithTicket userInfo can not be parsed, encryptedTicket=" + encryptedTicket);
                RspLoginWithTicket reply = RspLoginWithTicket.newBuilder()
                        .setLoginResult(LOGIN_ERR_BAD_RSA)
                        .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            }
        } else {
            Log.warning(TAG, "doLoginWithTicket encryptedTicket is null");
        }
    }

    @Override
    /**
     * 维系一个长链接，允许用户被强制中断链接
     */
    public StreamObserver<CommonMessage> keepTouch(StreamObserver<CommonMessage> responseObserver) {
        return new StreamObserver<CommonMessage>() {
            String senderName;
            String senderMac;

            @Override
            public void onNext(CommonMessage reqMsg) {
                Log.info(TAG, "keepTouch recv msg code=" + reqMsg.getMsgCode()
                        + " sender=" + reqMsg.getSender() + " mac=" + reqMsg.getSenderMac()
                        + " content=" + reqMsg.getMsgContent());
                senderName = reqMsg.getSender();
                senderMac = reqMsg.getMsgContent();

                onlineManager.processCommonMessage(reqMsg, responseObserver);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                Log.info(TAG, "keepTouch onComplete");
                responseObserver.onCompleted();
                onlineManager.handleStreamComplete(senderName);
            }
        };
    }
}

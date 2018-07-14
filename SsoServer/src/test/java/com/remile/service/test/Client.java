package com.remile.service.test;


import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.remile.datamodel.UserInfo;
import com.remile.grpc.sso.*;
import com.remile.util.RSAUtil;
import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import sun.misc.BASE64Encoder;
import com.remile.util.Log;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Client {
    public static final String TAG = "Client";

    private final ManagedChannel channel;
    private final SsoProcessorGrpc.SsoProcessorBlockingStub registerBlockingStub;

    private volatile boolean isFinish = false;

    public Client(String host, int port) throws SSLException {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(host, port);
//        channelBuilder.sslContext(
//                SslContextBuilder.forClient()
//                        .sslProvider(
//                        OpenSsl.isAlpnSupported()? SslProvider.OPENSSL: SslProvider.JDK)
//                        .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
//                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
//                        .applicationProtocolConfig(new ApplicationProtocolConfig(
//                                ApplicationProtocolConfig.Protocol.ALPN,
//                                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
//                                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
//                                ApplicationProtocolNames.HTTP_2,ApplicationProtocolNames.HTTP_1_1
//                        )).build()
//                GrpcSslContexts.forClient().trustManager(new File("ca.crt")).build()
//        );

        channel = channelBuilder.usePlaintext().build();
        registerBlockingStub = SsoProcessorGrpc.newBlockingStub(channel);
    }

    public static void main(String[] args) {
        Client client = null;
        try {
            client = new Client("192.168.43.172", 50051);
//            client.getPublicKey();
//            client.doRegister();
//            client.doLogin();
//            client.doLoginWithTicket();
            client.createLink();

            client.shutdown();
        } catch (SSLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void getPublicKey() {
        ReqGetRegKey request = ReqGetRegKey.newBuilder().build();
        Log.info(TAG, "getPublicKey...");
        RspGetRegKey response = registerBlockingStub.generateKey(request);
        ByteString byteString = response.getKey();
        byte[] keyBytes = byteString.toByteArray();
        String publicKey = new BASE64Encoder().encode(keyBytes);
        Log.info(TAG, "getPublicKey=" + publicKey);
    }

    private void doRegister() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName("remile");
        userInfo.setPassWord("123456");
        Gson gson = new Gson();
        String jsonUserInfo = gson.toJson(userInfo);
        Log.info(TAG, "doRegister jsonUserInfo=" + jsonUserInfo);
        String encryptUserInfo = RSAUtil.encrypt(jsonUserInfo);
        RegUserInfo regUserInfo = RegUserInfo.newBuilder()
                .setEncryptedUserInfo(encryptUserInfo)
                .build();
        RegResult result = registerBlockingStub.doRegister(regUserInfo);
        Log.info(TAG, "doRegister result=" + result.getRegResult());

    }

    private void doLogin() {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName("remile");
        userInfo.setPassWord("123456");
        Gson gson = new Gson();
        String jsonUserInfo = gson.toJson(userInfo);
        Log.info(TAG, "doLogin jsonUserInfo=" + jsonUserInfo);
        String encryptUserInfo = RSAUtil.encrypt(jsonUserInfo);
        ReqLogin reqLogin = ReqLogin.newBuilder().setEncryptedUserInfo(encryptUserInfo).build();
        RspLogin result = registerBlockingStub.doLogin(reqLogin);
        int res = result.getLoginResult();
        String encryptedTicket = result.getEncryptedTicket();
        String plainTicket = null;
        if(encryptedTicket != null) {
            Log.info(TAG, "doLogin encryptedTicket=" + encryptedTicket);
            String decryptedTicket = null;
            try {
                decryptedTicket = RSAUtil.decrypt(encryptedTicket);
            } catch (Exception e) {
                e.printStackTrace();
            }
            byte[] plainTextTicketBytes = decryptedTicket.getBytes();
            int len = plainTextTicketBytes.length;
            byte[] tempBytes = new byte[len];
            for(int i = 0; i < len; ++ i) {
                tempBytes[i] = plainTextTicketBytes[len - i - 1];
            }
            plainTicket = new String(tempBytes);
        }
        Log.info(TAG, "doLogin result=" + result.getLoginResult()
         + " ticket=" + plainTicket);
    }

    private void doLoginWithTicket() {
        // 这是一段加了密的票据
        String ticket = "HPOJ/zMdmlINzy9u7XLNziMxB7DCyH4n8jqjgcFRHj/yY0Ejb2jKK9u0aoO+WRk4e85bWLNbfGh1\n" +
                "ELacQPkjIyTs3A/Ye0yAhWCVdIGd8QtkKC+yE7tfiSIqXvbg0tEU2QfM1z1IUfXZOExALmemarl3\n" +
                "HdXhXC7047HjxaRmKe8=";
        ReqLoginWithTicket req = ReqLoginWithTicket.newBuilder().setEncryptedTicket(ticket).build();
        RspLoginWithTicket reply = registerBlockingStub.doLoginWithTicket(req);
        int res = reply.getLoginResult();
        Log.info(TAG, "doLoginWithTicket res=" + res);
    }

    private void createLink() {
        String mac = "mac-" + new Random().nextInt();
        Log.info(TAG, "createLink mac=" + mac);
        CommonMessage msg = CommonMessage.newBuilder().setMsgCode(0)
                .setSender("remile").setSenderMac(mac).build();
        SsoProcessorGrpc.SsoProcessorStub stub = SsoProcessorGrpc.newStub(channel);
        StreamObserver<CommonMessage> responseObserver = new StreamObserver<CommonMessage>() {
            StreamObserver<CommonMessage> requestObserver = stub.keepTouch(this);

            @Override
            public void onNext(CommonMessage response) {
                // 处理服务端的回包
                int msgCode = response.getMsgCode();
                Log.info(TAG, "recv msg code=" + response.getMsgCode()
                        + " sender=" + response.getSender() + " mac=" + response.getSenderMac()
                        + " content=" + response.getMsgContent());
                switch (msgCode) {
                    case 0: { // 创建连接的回包
                        // link success...
                        Log.info(TAG, "createLink success");
                        break;
                    }
                    case 1: { // 收到服务端的下线指令
                        // logout
                        Log.info(TAG, "createLink kicked");
                        CommonMessage msg = CommonMessage.newBuilder().setMsgCode(1)
                                .setSender("remile").setMsgContent("ok,recv kick").setSenderMac(mac).build();
                        requestObserver.onNext(msg);
                        break;
                    }
                    default:break;
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                Log.info("TAG", "onCompleted");
                isFinish = true;
            }
        };

        // 发送创建连接的请求
        StreamObserver<CommonMessage> requestObserver = stub.keepTouch(responseObserver);
        requestObserver.onNext(msg);

        while(!isFinish) {

        }
    }


    private void shutdown() throws InterruptedException {
        System.out.println("***** client shutdown");
        channel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
    }

}

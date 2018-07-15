package com.remile.service;

import com.remile.dao.UserInfoDAO;
import com.remile.service.sso.SsoProcessorImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import com.remile.util.KeyPairGenUtil;
import com.remile.util.Log;

import java.io.File;

import static com.remile.util.RSAUtil.PRIVATE_KEY_FILE;
import static com.remile.util.RSAUtil.PUBLIC_KEY_FILE;
import static com.remile.util.RSAUtil.encrypt;

public class Bootstrap {

    final public static String CERTIFICATE = "server.crt";
    final public static String PRIVTEKEY = "server.pem";
    final public static int SERVER_PORT = 50051;
    private static String TAG = "Bootstrap";

    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();
        try {
            bootstrap.start();
            bootstrap.blockUntilShutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Server mServer;

    private void start() throws Exception {
        // 1.生成加密用的公钥和私钥, 如果文件被删除才重新生成
        File publicKey = new File(PUBLIC_KEY_FILE);
        File privateKey = new File(PRIVATE_KEY_FILE);
        if(!publicKey.exists() || !privateKey.exists()) {
            KeyPairGenUtil.generateKeyPair();
        }

        // 2.开启服务器并绑定各服务
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        Log.info(TAG, "***** server is started, port=" + SERVER_PORT);
        mServer = ServerBuilder.forPort(SERVER_PORT)
                .useTransportSecurity(ssc.certificate(), ssc.privateKey())
//                .useTransportSecurity(new File(CERTIFICATE), new File(PRIVTEKEY))
                .addService(new SsoProcessorImpl())
                .build().start();

        // 3.JVM关闭安全关闭服务器
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                Log.info(TAG, "***** JVM is shutting down, so grpc server shutting down");
                Bootstrap.this.stop();
                Log.info(TAG, "***** server shut down now, bye");
            }
        });
    }

    private void blockUntilShutdown() throws InterruptedException {
        if(mServer != null) {
            mServer.awaitTermination();
        }
    }

    private void stop() {
        UserInfoDAO.close();
        if(mServer != null) {
            mServer.shutdown();
        }
    }


}

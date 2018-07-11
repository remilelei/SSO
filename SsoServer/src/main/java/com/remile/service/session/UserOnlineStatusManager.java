package com.remile.service.session;

import com.remile.datamodel.Session;
import com.remile.grpc.sso.CommonMessage;
import com.remile.service.sso.SsoProcessorImpl;
import com.remile.util.Log;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 这个类顾名思义用于管理用户在线状态
 * 原理是内部维护一个哈希表来当作会话层，会话层中保存用户的在线状态
 */
public class UserOnlineStatusManager {
    public final static String TAG = "UserOnlineStatusManager";

    // Key=会话Owner的用户名  Value=会话对象
    private ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 处理一个想要建立连接的请求
     * @param req
     */
    public void handleLinkStartRequest(CommonMessage req, StreamObserver<CommonMessage> streamObserver) {
        String sender = req.getSender();
        Session session = sessionMap.get(sender);

        if(session == null) { // 一个新的连接，会话层中还没有，这里添加一下
            Log.info(TAG, "handleLinkStartRequest create session.");
            session = Session.createFromCommonMessage(req);
            session.setResponseObserver(streamObserver);
            sessionMap.put(sender, session);

            CommonMessage linkReply = CommonMessage.newBuilder()
                    .setMsgCode(SsoProcessorImpl.MESSAGE_CODE_LINK).setSender("Server")
                    .setMsgContent("link started success").build();
            streamObserver.onNext(linkReply);
        } else {
            /**
             * 会话层中已经有了这个链接。
             * 如果请求和会话层存根中的mac地址是一样的，那么代表客户端请求的连接已经创建，本次请求无效，所以忽略它。
             * 如果存根和请求中的mac地址不同，则代表当前用户设备更换，关闭原先的连接，转而维护新的连接。
             */
            String macFromRequest = req.getSenderMac();
            String macFromSession = session.getUniqueFlag();
            if(macFromRequest.equals(macFromSession)) { // 两mac地址相同，本次请求无效
                Log.info(TAG, "handleLinkStartRequest ignore request because same mac address.");
                CommonMessage linkReply = CommonMessage.newBuilder()
                        .setMsgCode(SsoProcessorImpl.MESSAGE_CODE_LINK).setSender("Server")
                        .setMsgContent("link started ignore").build();
                streamObserver.onNext(linkReply);
                streamObserver.onCompleted();
            } else { // 两个mac不同，关闭session中的连接，并维护最新的
                Log.info(TAG, "handleLinkStartRequest mac:" + macFromSession
                        +  " kicked by mac:" + macFromRequest);
                CommonMessage kickReply = CommonMessage.newBuilder()
                        .setMsgCode(SsoProcessorImpl.MESSAGE_CODE_KICK).setSender("Server").build();
                StreamObserver<CommonMessage> responseObserver = session.getResponseObserver();
                responseObserver.onNext(kickReply); // 发送这个消息，让收到的客户端关连接下线。
                session.setUniqueFlag(macFromRequest);
                session.setResponseObserver(streamObserver);

                CommonMessage linkReply = CommonMessage.newBuilder()
                        .setMsgCode(SsoProcessorImpl.MESSAGE_CODE_LINK).setSender("Server")
                        .setMsgContent("link started success," + macFromSession + " has been kicked.").build();
                streamObserver.onNext(linkReply); // 告诉新的会话连接成功了。
            }
        }
    }
}

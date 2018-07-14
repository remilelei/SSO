package com.remile.service.session;

import com.remile.dao.UserInfoDAO;
import com.remile.datamodel.Session;
import com.remile.datamodel.UserInfo;
import com.remile.grpc.sso.CommonMessage;
import com.remile.service.sso.SsoProcessorImpl;
import com.remile.util.Log;
import com.remile.util.RSAUtil;
import io.grpc.stub.StreamObserver;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.remile.service.sso.SsoProcessorImpl.MESSAGE_CODE_KICK;
import static com.remile.service.sso.SsoProcessorImpl.MESSAGE_CODE_LINK;

/**
 * 这个类顾名思义用于管理用户在线状态
 * 原理是内部维护一个哈希表来当作会话层，会话层中保存用户的在线状态
 */
public class UserOnlineStatusManager {
    public final static String TAG = "UserOnlineStatusManager";

    // Key=会话Owner的用户名  Value=会话对象
    private ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    //在此处理CommonMessage带来的事务
    public void processCommonMessage(CommonMessage message, StreamObserver<CommonMessage> responseObserver) {
        int msgCode = message.getMsgCode();
        switch (msgCode) {
            case MESSAGE_CODE_LINK: {
                // 收到客户端建立链接的请求, 先检查票据，如果通过则创建长连接。

                // 1，从票据中读取用户信息
                String strTicket = message.getMsgContent();
                UserInfo userInfo = null;
                try {
                    userInfo = RSAUtil.getUserInfoFromTicket(strTicket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                UserInfoDAO dao = UserInfoDAO.getInstance();

                // 2，验证读取出来的用户信息，如果成功则创建长连接
                if(userInfo != null && dao.checkTicket(userInfo)) {
                    handleLinkStartRequest(message, responseObserver);
                } else {
                    Log.warning(TAG, "keepTouch keep touch failed, userInfo=" + userInfo);
                    responseObserver.onCompleted();
                }
                break;
            }
            case MESSAGE_CODE_KICK: {
                // 踢下线的回包
                responseObserver.onCompleted();
                break;
            }
            default:
                break;
        }
    }

    /**
     * 处理一个想要建立连接的请求
     * @param req
     */
    public void handleLinkStartRequest(CommonMessage req, StreamObserver<CommonMessage> streamObserver) {
        String sender = req.getSender();
        Session session = sessionMap.get(sender);

        if(session == null) { // 一个新的连接，会话层中还没有，这里添加一下
            Log.info(TAG, "handleLinkStartRequest create session. userName=" + sender
                    + " mac=" + req.getSenderMac());
            session = Session.createFromCommonMessage(req);
            session.setResponseObserver(streamObserver);
            sessionMap.put(sender, session);

            CommonMessage linkReply = CommonMessage.newBuilder()
                    .setMsgCode(MESSAGE_CODE_LINK).setSender("Server")
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
                        .setMsgCode(MESSAGE_CODE_LINK).setSender("Server")
                        .setMsgContent("link started ignore").build();
                streamObserver.onNext(linkReply);
                streamObserver.onCompleted();
            } else { // 两个mac不同，关闭session中的连接，并维护最新的
                Log.info(TAG, "handleLinkStartRequest mac:" + macFromSession
                        +  " kicked by mac:" + macFromRequest);
                CommonMessage kickReply = CommonMessage.newBuilder()
                        .setMsgCode(MESSAGE_CODE_KICK).setSender("Server").build();
                StreamObserver<CommonMessage> responseObserver = session.getResponseObserver();
                responseObserver.onNext(kickReply); // 发送这个消息，让收到的客户端关连接下线。
                responseObserver.onCompleted();

                // 更新session信息
                session.setUniqueFlag(macFromRequest);
                session.setResponseObserver(streamObserver);

                CommonMessage linkReply = CommonMessage.newBuilder()
                        .setMsgCode(MESSAGE_CODE_LINK).setSender("Server")
                        .setMsgContent("link started success," + macFromSession + " has been kicked.").build();
                streamObserver.onNext(linkReply); // 告诉新的会话连接成功了。
            }
        }
    }

    // 连接关闭，用户下线，从会话层中除去该用户的会话
    public boolean handleStreamComplete(String userName) {
        Log.info(TAG, "handleStreamComplete userName:" + userName);
        if(sessionMap.containsKey(userName)) {
            sessionMap.remove(userName);
            return true;
        } else return false;
    }

    // 批量使用户下线，外部可以根据策略使用
    public void kickUsersLogout(List<UserInfo> userInfos) {
        Iterator<UserInfo> iter = userInfos.iterator();
        while(iter.hasNext()) {
            UserInfo userInfo = iter.next();
            String userName = userInfo.getUserName();
            if(sessionMap.containsKey(userName)) {
                // 读取出用户当前的会话
                Session session = sessionMap.get(userName);
                // 构造下线指令
                CommonMessage kickReply = CommonMessage.newBuilder()
                        .setMsgCode(MESSAGE_CODE_KICK).setSender("Server").build();
                // 让他下线。
                StreamObserver<CommonMessage> responseObserver = session.getResponseObserver();
                responseObserver.onNext(kickReply); // 发送这个消息，让收到的客户端关连接下线。
                responseObserver.onCompleted();
                // 从会话层中移出此人的会话
                sessionMap.remove(userName);
            }
        }
    }

}

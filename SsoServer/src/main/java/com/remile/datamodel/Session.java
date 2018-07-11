package com.remile.datamodel;

import com.remile.grpc.sso.CommonMessage;
import io.grpc.stub.StreamObserver;

public class Session {
    private String userId;
    private String uniqueFlag;
    private String kickingUniqueFlag;
    private StreamObserver<CommonMessage> responseObserver;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUniqueFlag() {
        return uniqueFlag;
    }

    public void setUniqueFlag(String uniqueFlag) {
        this.uniqueFlag = uniqueFlag;
    }

    public String getKickingUniqueFlag() {
        return kickingUniqueFlag;
    }

    public void setKickingUniqueFlag(String kickingUniqueFlag) {
        this.kickingUniqueFlag = kickingUniqueFlag;
    }

    public StreamObserver<CommonMessage> getResponseObserver() {
        return responseObserver;
    }

    public void setResponseObserver(StreamObserver<CommonMessage> responseObserver) {
        this.responseObserver = responseObserver;
    }

    public static Session createFromCommonMessage(CommonMessage message) {
        Session session = new Session();
        session.userId = message.getSender();
        session.uniqueFlag = message.getSenderMac();
        return session;
    }
}

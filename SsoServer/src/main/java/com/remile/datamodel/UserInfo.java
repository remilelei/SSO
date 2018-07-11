package com.remile.datamodel;

import java.util.Date;

public class UserInfo {
    private int userId;
    private String userName;
    private String passWord;
    private String loginMac;
    private long lastLoginTime;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getLoginMac() {
        return loginMac;
    }

    public void setLoginMac(String loginMac) {
        this.loginMac = loginMac;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("UserInfo { ")
                .append(",userId=" + userId)
                .append(",userName=" + userName)
                .append(",password=" + passWord)
                .append(",loginMac=" + loginMac)
                .append(",lastLoginTime=" + new Date(lastLoginTime))
                .append(" }");
        return sb.toString();
    }
}

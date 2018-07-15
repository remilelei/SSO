package com.remile.service.test;

import com.remile.dao.UserInfoDAO;
import com.remile.datamodel.UserInfo;
import com.remile.util.RSAUtil;
import org.junit.Test;

import java.sql.*;

public class TestDB {

    public static void main(String[] args) {
//        String ticket = "UxWa7KzECPUk+W+i5nbt0UuQ6gmdou1j3Jer4LhYaqmt0qHb0Syr0J9cXShL8A3VRhs9z1I28FES\n" +
//                "6L5TW/9utZVoOpotI6RXqtPouGw367U9ob3A3BHL6b4dPc3HkTbQ1/kVGIjCEpPP4TQzGcsfxiMU\n" +
//                "weEKBDmPBDP4F5XPuWY=";
//        UserInfoDAO dao = UserInfoDAO.getInstance();
//        UserInfo userInfo = null;
//        try {
//            userInfo = RSAUtil.getUserInfoFromTicket(ticket);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
////        boolean res = dao.loginByTicket(userInfo);
//        boolean res = dao.checkTicket(userInfo);
//        System.out.println(res);
    }

    @Test
    public void testRegister() {
        UserInfoDAO dao = UserInfoDAO.getInstance();
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName("qwerty");
        userInfo.setPassWord("940829");
        userInfo.setLoginMac("macTest");
        int res = dao.saveUserInfo(userInfo);
        System.out.println("testRegister res="+res);
    }

    @Test
    public void testLogin() {
        UserInfoDAO dao = UserInfoDAO.getInstance();
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName("qwerty");
        userInfo.setPassWord("940829");
        userInfo.setLoginMac("macTest");
        boolean res = dao.login(userInfo);
        System.out.println("testRegister res="+res);
    }
}

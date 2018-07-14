package com.remile.service.test;

import com.remile.dao.UserInfoDAO;
import com.remile.datamodel.UserInfo;
import com.remile.util.RSAUtil;

import java.sql.*;

public class TestDB {

    public static void main(String[] args) {
        String ticket = "UxWa7KzECPUk+W+i5nbt0UuQ6gmdou1j3Jer4LhYaqmt0qHb0Syr0J9cXShL8A3VRhs9z1I28FES\n" +
                "6L5TW/9utZVoOpotI6RXqtPouGw367U9ob3A3BHL6b4dPc3HkTbQ1/kVGIjCEpPP4TQzGcsfxiMU\n" +
                "weEKBDmPBDP4F5XPuWY=";
        UserInfoDAO dao = UserInfoDAO.getInstance();
        UserInfo userInfo = null;
        try {
            userInfo = RSAUtil.getUserInfoFromTicket(ticket);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        boolean res = dao.loginByTicket(userInfo);
        boolean res = dao.checkTicket(userInfo);
        System.out.println(res);
    }
}

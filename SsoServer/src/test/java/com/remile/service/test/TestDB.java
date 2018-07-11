package com.remile.service.test;

import com.remile.dao.UserInfoDAO;
import com.remile.datamodel.UserInfo;
import com.remile.util.RSAUtil;

import java.sql.*;

public class TestDB {

    public static void main(String[] args) {
        String ticket = "PxflFh3KdPxVoBdXzd+q5q8di5gEa1X6upkDTnx/z8XfbCAXFWhC5XGwA1AEnc64rgUT9tzRFB08\n" +
                "s8x/TUEMoOi83QTbCt8o2xl8jz4cMKf5TFJuvUqxGwAWe13VeFeb61rSWeaDJuPgCGAgwWuMU6K/\n" +
                "Jh7U0I4AsjHWv8vkYJg=";
        UserInfoDAO dao = UserInfoDAO.getInstance();
        UserInfo userInfo = null;
        try {
            userInfo = RSAUtil.getUserInfoFromTicket(ticket);
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean res = dao.loginByTicket(userInfo);
        System.out.println(res);
    }
}

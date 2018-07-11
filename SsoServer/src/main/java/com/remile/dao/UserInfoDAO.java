package com.remile.dao;

import com.alibaba.druid.pool.DruidDataSource;
import com.remile.datamodel.UserInfo;

import java.sql.*;

import static com.remile.service.sso.SsoProcessorImpl.TICKET_INVALID_DURATION;

public class UserInfoDAO {

    private static final String CLASS_NAME = "org.mariadb.jdbc.Driver";
    private static final String URL = "jdbc:mysql://localhost:3306/sso_system";
    private static final String USER_NAME = "root";
    private static final String PASS_WORD = "940829Hg*";

    private static final String SQL_SAVE = "insert into UserInfo(user_name, pass_word, login_mac) " +
            "values(?, md5(?), ?);";
    private static final String SQL_REGISTER = "{call register_user(?, ?, ?)}";
    private static final String SQL_LOGIN = "{call login(?, ?, ?)}";
    private static final String SQL_LOGIN_BY_TICKET = "{call loginByTicket(?, ?, ?, ?)}";

    private DruidDataSource mDataSource;
    public static UserInfoDAO sInstance;

    private UserInfoDAO() {
        mDataSource = new DruidDataSource();

        mDataSource.setDriverClassName(CLASS_NAME);
        mDataSource.setUrl(URL);
        mDataSource.setUsername(USER_NAME);
        mDataSource.setPassword(PASS_WORD);
    }

    public synchronized static UserInfoDAO getInstance() {
        if(sInstance == null) {
            sInstance = new UserInfoDAO();
        }
        return sInstance;
    }

    public synchronized static void close() {
        if(sInstance != null && sInstance.mDataSource != null) {
            sInstance.mDataSource.close();
        }
    }

    /**
     * 注册
     * @param userInfo 要保存的用户信息
     * @return 是否成功添加（失败是因为账号有唯一性约束）
     */
    public int saveUserInfo(UserInfo userInfo) {
        Connection conn = null;
        PreparedStatement ps = null;
        int ret = 0;
        try {
            conn = mDataSource.getConnection();
            ps = conn.prepareStatement(SQL_SAVE);
            ps.setString(1, userInfo.getUserName());
            ps.setString(2, userInfo.getPassWord());
            ps.setString(3, userInfo.getLoginMac());

            ret = ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 普通登录，用户手动输入账号和密码
     * @param userInfo
     * @return
     */
    public boolean login(UserInfo userInfo) {
        Connection conn = null;
        CallableStatement statement = null;
        boolean res = false;
        try {
            conn = mDataSource.getConnection();
            statement = conn.prepareCall(SQL_LOGIN);
            statement.setString(1, userInfo.getUserName());
            statement.setString(2, userInfo.getPassWord());
            statement.registerOutParameter(3, Types.BOOLEAN);
            statement.execute();
            res = statement.getBoolean(3);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 隐式登录，根据用户票据验证身份。票据有可能会过期。
     * @param userInfo
     * @return
     */
    public boolean loginByTicket(UserInfo userInfo) {
        Connection conn = null;
        CallableStatement statement = null;
        boolean res = false;
        try {
            conn = mDataSource.getConnection();
            statement = conn.prepareCall(SQL_LOGIN_BY_TICKET);
            statement.setString(1, userInfo.getUserName());
            statement.setString(2, userInfo.getPassWord());
            statement.registerOutParameter(3, Types.BOOLEAN);
            statement.registerOutParameter(4, Types.TIMESTAMP);
            statement.execute();
            res = statement.getBoolean(3);
            if(res) { // 账号和密码正确
                long time = System.currentTimeMillis();
                System.out.println("ticketTime=" + new java.util.Date(userInfo.getLastLoginTime())
                        + " curTime=" + new java.util.Date(time) );
                if(time - userInfo.getLastLoginTime() < TICKET_INVALID_DURATION) { // 票据仍在有效期
                    res = true;
                } else { // 票据已过期，通知客户端重新登录，进行身份校验
                    res = false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }
}

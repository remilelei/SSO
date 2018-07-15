package com.remile.dao;

import com.alibaba.druid.pool.DruidDataSource;
import com.remile.datamodel.UserInfo;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

import static com.remile.service.sso.SsoProcessorImpl.TICKET_INVALID_DURATION;

public class UserInfoDAO {

    private static final String CLASS_NAME = "org.mariadb.jdbc.Driver";
    private static final String URL = "jdbc:mysql://localhost:3306/sso_system";
    private static final String USER_NAME = "root";
    private static final String PASS_WORD = "940829Hg*";
    private static final String SQL_FIND_PWD_BY_USERNAME = "select password,lastLoginTime from UserInfo where UserName=?";
    private static final String SQL_ADD_USERINFO = "insert into UserInfo(UserName, password, loginMac) " +
            "values(?, ?, ?);";
    private static final String SQL_UPDATE_LOGIN_TIME = "update UserInfo set lastLoginTime=? where userName=?";

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
            // 密码使用BCrypt加密一下再入库
            String bcryptPwd = BCrypt.hashpw(userInfo.getPassWord(), BCrypt.gensalt());
            ps = conn.prepareStatement(SQL_ADD_USERINFO);
            ps.setString(1, userInfo.getUserName());
            ps.setString(2, bcryptPwd);
            ps.setString(3, userInfo.getLoginMac());

            ret = ps.executeUpdate();
            ps.close();
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
        PreparedStatement statement1 = null; // 查询当前用户的密码
        PreparedStatement statement2 = null; // 更新用户的登录时间

        boolean res = false;
        try {
            conn = mDataSource.getConnection();
            conn.setAutoCommit(false);
            statement1 = conn.prepareStatement(SQL_FIND_PWD_BY_USERNAME);
            statement2 = conn.prepareStatement(SQL_UPDATE_LOGIN_TIME);

            statement1.setString(1, userInfo.getUserName());
            statement2.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            statement2.setString(2, userInfo.getUserName());
            ResultSet rs1 = statement1.executeQuery();
            if(rs1.next()) { // 有匹配用户
                String pwdFromDB = rs1.getString(1);
                if(BCrypt.checkpw(userInfo.getPassWord(), pwdFromDB)) { // 密码正确
                    res = statement2.executeUpdate() > 0;
                }
            }
            conn.commit();
            statement1.close();
            statement2.close();
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
        PreparedStatement statement1 = null; // 查询当前用户的密码
        PreparedStatement statement2 = null; // 更新用户的登录时间
        boolean res = false;
        try {
            conn = mDataSource.getConnection();
            statement1 = conn.prepareStatement(SQL_FIND_PWD_BY_USERNAME);
            statement2 = conn.prepareStatement(SQL_UPDATE_LOGIN_TIME);
            statement1.setString(1, userInfo.getUserName());
            statement2.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            statement2.setString(2, userInfo.getUserName());
            ResultSet rs1 = statement1.executeQuery();
            if(rs1.next()) { // 有匹配用户
                String pwdFromDB = rs1.getString(1);
                Timestamp lastLoginTime = rs1.getTimestamp(2);
                if(System.currentTimeMillis() - lastLoginTime.getTime() <= TICKET_INVALID_DURATION) { // 检验票据是否过期
                    if(BCrypt.checkpw(userInfo.getPassWord(), pwdFromDB)) { // 检验密码是否正确
                        res = statement2.executeUpdate() > 0;
                    }
                }
            }
            statement1.close();
            statement2.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    public boolean checkTicket(UserInfo userInfo) {
        Connection conn = null;
        PreparedStatement statement = null; // 查询当前用户的密码
        boolean res = false;
        try {
            statement = conn.prepareStatement(SQL_FIND_PWD_BY_USERNAME);
            statement.setString(1, userInfo.getUserName());
            ResultSet rs1 = statement.executeQuery();
            if(rs1.next()) { // 有匹配用户
                String pwdFromDB = rs1.getString(1);
                Timestamp lastLoginTime = rs1.getTimestamp(2);
                if(System.currentTimeMillis() - lastLoginTime.getTime() <= TICKET_INVALID_DURATION) { // 检验票据是否过期
                    if(BCrypt.checkpw(userInfo.getPassWord(), pwdFromDB)) { // 检验密码是否正确
                        res = true;
                    }
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }
}

package com.remile.util;

import com.remile.datamodel.UserInfo;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class RSAUtil {
    /** 指定加密算法为RSA */
    private static final String ALGORITHM = "RSA";
    /** 指定公钥存放文件 */
    public static String PUBLIC_KEY_FILE = "PublicKey";
    /** 指定私钥存放文件 */
    public static String PRIVATE_KEY_FILE = "PrivateKey";

    public static void main(String[] args) throws Exception {

        String source = "remiletest";// 要加密的字符串
        System.out.println("准备用公钥加密的字符串为：" + source);

        String cryptograph = encrypt(source);// 生成的密文
        System.out.print("用公钥加密后的结果为:" + cryptograph);
        System.out.println();

        String target = decrypt(cryptograph);// 解密密文
        System.out.println("用私钥解密后的字符串为：" + target);
        System.out.println();
    }

    /**
     * 加密方法
     * @param source 源数据
     * @return
     * @throws Exception
     */
    public static String encrypt(String source) {

        Key publicKey = getKey(PUBLIC_KEY_FILE);
        String ret = null;

        /** 得到Cipher对象来实现对源数据的RSA加密 */
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] b = source.getBytes();
            /** 执行加密操作 */
            byte[] b1 = cipher.doFinal(b);
            BASE64Encoder encoder = new BASE64Encoder();
            ret = encoder.encode(b1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 解密算法
     * @param cryptograph    密文
     * @return
     * @throws Exception
     */
    public static String decrypt(String cryptograph) throws Exception {

        Key privateKey = getKey(PRIVATE_KEY_FILE);
        String ret = null;
        /** 得到Cipher对象对已用公钥加密的数据进行RSA解密 */
        Cipher cipher = null;
        cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] b1 = decoder.decodeBuffer(cryptograph);

        /** 执行解密操作 */
        byte[] b = cipher.doFinal(b1);
        ret = new String(b);
        return ret;
    }

    public static Key getKey(String fileName) {
        Key key;
        ObjectInputStream ois = null;
        try {
            /** 将文件中的私钥对象读出 */
            ois = new ObjectInputStream(new FileInputStream(fileName));
            key = (Key) ois.readObject();
        } catch (Exception e) {
            return null;
        } finally {
            try {
                ois.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return key;
    }

    /**
     * 生成提供给客户端的票据，只有登录后才下发
     * 利用用户的（账号+密码的md5+mac地址+当前时间）格式化加密
     * 加密：反转字符串+抑或+RSA公钥
     */
    public static String generatTicket(UserInfo userInfo) {
        // 1.生成明文票据
        StringBuffer sb = new StringBuffer();
        sb.append('[')
                .append(userInfo.getUserName()).append(',')
                .append(userInfo.getPassWord()).append(',')
                .append(userInfo.getLoginMac()).append(',')
                .append(System.currentTimeMillis()).append(']');
        String plainTextTicket = sb.toString();

        // 2.加密
        byte[] plainTextTicketBytes = plainTextTicket.getBytes();
        int len = plainTextTicketBytes.length;
        byte[] tempBytes = new byte[len];
        for(int i = 0; i < len; ++ i) {
            tempBytes[i] = plainTextTicketBytes[len - i - 1];
        }
        String tempTicket = new String(tempBytes);
        String encryptedTicket = encrypt(tempTicket);
        return encryptedTicket;
    }

    public static UserInfo getUserInfoFromTicket(String encryptedTicket) throws Exception {
        String decryptedTicket = decrypt(encryptedTicket);
        byte[] plainTextTicketBytes = decryptedTicket.getBytes();
        int len = plainTextTicketBytes.length;
        byte[] tempBytes = new byte[len];
        for(int i = 0; i < len; ++ i) {
            tempBytes[i] = plainTextTicketBytes[len - i - 1];
        }
        String plainTicket = new String(tempBytes);
        String[] userInfosArr = plainTicket.substring(1, plainTicket.length() - 1).split(",");
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName(userInfosArr[0]);
        userInfo.setPassWord(userInfosArr[1]);
        userInfo.setLoginMac(userInfosArr[2]);
        long lastLoginTime = Long.valueOf(userInfosArr[3]);
        userInfo.setLastLoginTime(lastLoginTime);

        return userInfo;
    }
}

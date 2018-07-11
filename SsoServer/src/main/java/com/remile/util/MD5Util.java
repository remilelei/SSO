package com.remile.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

public class MD5Util {
    public static char hexDigits[] = { // 用来将字节转换成 16 进制表示的字符
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * 计算源字符串的MD5散列值
     *
     * @param source
     *            统一 UTF-8 编码
     * @return md5 hash value (128bit, 16byte) or null if failed
     */
    public static byte[] toMD5Byte(String source) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        byte[] byteArray;
        try {
            byteArray = source.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        return md5.digest(byteArray);
    }

    /**
     * 计算源字符串的MD5
     *
     * @param source
     *            统一 UTF-8 编码
     * @return 32位大写MD5串 or "" if failed
     */
    public static String toMD5(String source) {
        return bytesToHexString(toMD5Byte(source));
    }

    /**
     * 将 md5 hash value 转换成 32位大写MD5串
     *
     * @param md5Bytes
     *            md5 hash value (128bit, 16byte)
     * @return 32位大写MD5串 or "" if md5Bytes is wrong
     */
    public static String bytesToHexString(byte md5Bytes[]) {
        if (md5Bytes != null && md5Bytes.length == 16) {
            char str[] = new char[16 * 2]; // 每个字节用 16 进制表示的话，使用两个字符，所以表示成 16
            // 进制需要 32 个字符
            int k = 0; // 表示转换结果中对应的字符位置
            for (int i = 0; i < 16; i++) { // 从第一个字节开始，对 MD5 的每一个字节，转换成 16
                // 进制字符的转换
                byte byte0 = md5Bytes[i]; // 取第 i个字节
                str[k++] = hexDigits[byte0 >>> 4 & 0xf]; // 取字节中高 4 位的数字转换， >>>
                // 为逻辑右移，将符号位一起右移
                str[k++] = hexDigits[byte0 & 0xf]; // 取字节中低 4 位的数字转换
            }
            return new String(str); // 换后的结果转换为字符串
        } else {
            return "";
        }
    }

    public static String encode2HexStr(byte[] bytes) {
        return bytesToHexString(encode(bytes));
    }

    /**
     * 用MD5算法加密字节数组
     *
     * @param bytes
     *            要加密的字节
     * @return byte[] 加密后的字节数组，若加密失败，则返回null
     */
    public static byte[] encode(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(bytes);
            byte[] digesta = digest.digest();
            return digesta;
        } catch (Exception e) {
            return null;
        }
    }
}

package com.tencent.remile.util;

public class StringUtil {

    /***
     * byte转为String
     * @param bytes
     * @return
     */
    public static String bytesToString(byte[] bytes){
        if (bytes == null || bytes.length == 0) {
            return null ;
        }
        StringBuilder buf = new StringBuilder();
        for (byte b : bytes) {
            buf.append(String.format("%02X:", b));
        }
        if (buf.length() > 0) {
            buf.deleteCharAt(buf.length() - 1);
        }
        return buf.toString();
    }
}

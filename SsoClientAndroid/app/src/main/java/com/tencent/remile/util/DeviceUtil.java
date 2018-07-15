package com.tencent.remile.util;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class DeviceUtil {

    /** 根据版本获取手机Mac地址 */
    public static String getMac(Context context) {
        String mac = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mac = getMachineHardwareAddress();
        } else {
            mac = getLocalMacAddress(context);
        }
        return mac;
    }

    /** 获取手机mac地址 （6.0以下的手机） */
    private static String getLocalMacAddress(Context context) {
        try {
            WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi == null) {
                return "null";
            }

            WifiInfo info = wifi.getConnectionInfo();
            if (info == null) {
                return "null";
            }
            return info.getMacAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "null";
    }

    /**
     * 获取设备的mac地址和IP地址（android6.0以上专用）
     */
    private static String getMachineHardwareAddress(){
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        String hardWareAddress = null;
        NetworkInterface iF = null;
        while (interfaces.hasMoreElements()) {
            iF = interfaces.nextElement();
            try {
                hardWareAddress = StringUtil.bytesToString(iF.getHardwareAddress());
                if(hardWareAddress == null) continue;
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        if(iF != null && iF.getName().equals("wlan0")){
            hardWareAddress = hardWareAddress.replace(":","");
        }
        return hardWareAddress ;
    }
}

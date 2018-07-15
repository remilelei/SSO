package com.tencent.remile.util;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.grpc.helloworldexample.cpp.R;

public class FileUtil {

    public static boolean isExist(String filePath) {
        return new File(filePath).exists();
    }

    public static void copy(InputStream is, String target) {
        try {
            File targetFile = new File(target);
            File targetParent = targetFile.getParentFile();
            targetParent.mkdirs();
            targetFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(target);
            byte[] buffer = new byte[1024];
            int bytePos = 0;
            while((bytePos = is.read(buffer)) > 0) {
                fos.write(buffer, 0, bytePos);
            }
            fos.flush();
            is.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取证书
     * @return 证书内容
     */
    public static String readCa(Context ctx) {
        InputStream is = null;
        String res = null;
        try {
            is = ctx.getResources().openRawResource(R.raw.ca);
            byte[] buf = new byte[is.available()];
            is.read(buf);
            res = new String(buf);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return res;
    }
}

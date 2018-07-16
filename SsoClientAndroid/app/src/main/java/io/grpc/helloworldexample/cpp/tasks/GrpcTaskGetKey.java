package io.grpc.helloworldexample.cpp.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.tencent.remile.util.FileUtil;

import java.lang.ref.WeakReference;

import io.grpc.helloworldexample.cpp.HelloworldActivity;

// 从后台拉取公钥的任务
public class GrpcTaskGetKey extends AsyncTask<String, Void, byte[]> {
    private final WeakReference<HelloworldActivity> activityReference;

    public GrpcTaskGetKey(HelloworldActivity activity) {
        this.activityReference = new WeakReference<HelloworldActivity>(activity);
    }

    @Override
    protected byte[] doInBackground(String... params) {
        String host = params[0];
        String portStr = params[1];
        int port = TextUtils.isEmpty(portStr) ? 50051 : Integer.valueOf(portStr);
        HelloworldActivity activity = activityReference.get();
        if (activity == null || isCancelled()) {
            return null;
        }
        return activity.getPublicKey(host, port, FileUtil.readCa(activity));
    }

    @Override
    protected void onPostExecute(byte[] result) {
        HelloworldActivity activity = activityReference.get();
        if (activity == null || isCancelled()) {
            return;
        }
        if("error".equals(new String(result))) {
            Toast.makeText(activity, "服务器连接失败", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuffer sb = new StringBuffer();
        for(byte b : result) {
            sb.append(b + ",");
        }
        String sPublicKey = new String(result);
        SharedPreferences sp = activity.getSharedPreferences("remile", Context.MODE_PRIVATE);
        sp.edit().putString("publicKey", sPublicKey).apply();
        activity.cachePublicKey = result;
        Log.i("remile", "public Key=" + activity.cachePublicKey.length + ":" + sb.toString());

        activity.back2Login();
    }
}

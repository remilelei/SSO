package io.grpc.helloworldexample.cpp.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import io.grpc.helloworldexample.cpp.HelloworldActivity;
import io.grpc.helloworldexample.cpp.R;

public class GrpcTaskLogin extends AsyncTask<String, Void, String> {
    private final WeakReference<HelloworldActivity> activityReference;
    private String inputUsername;


    public GrpcTaskLogin(HelloworldActivity activity) {
        this.activityReference = new WeakReference<HelloworldActivity>(activity);
    }

    @Override
    protected String doInBackground(String... params) {
        String username = params[0];
        String password = params[1];
        String mac = params[2];
        inputUsername = username;
        HelloworldActivity activity = activityReference.get();
        if (activity == null || isCancelled()) {
            return null;
        }
        return activity.login(username, password, mac, activity.cachePublicKey);
    }

    @Override
    protected void onPostExecute(String result) {
        HelloworldActivity activity = activityReference.get();
        if (activity == null || isCancelled()) {
            return;
        }
        if("serverError".equals(result)) {
            // 服务端错误
            Toast.makeText(activity, "服务异常，稍后重试", Toast.LENGTH_SHORT).show();
        } else if("accountError".equals(result)) {
            // 账号密码错误
            Toast.makeText(activity, "账号信息错误", Toast.LENGTH_SHORT).show();
        } else {
            // 登录成功，保存票据，跳转
            activity.curUsername = inputUsername;
            SharedPreferences sp = activity.getSharedPreferences("remile", Context.MODE_PRIVATE);
            sp.edit().putString("loginTicket", result).apply();
            activity.cacheTicket = result;
            activity.jump2AfterLogin();
            // 启动长连接
            activity.doKeepTouch();
        }
        Log.i("remile", "login result=" + result);
    }
}

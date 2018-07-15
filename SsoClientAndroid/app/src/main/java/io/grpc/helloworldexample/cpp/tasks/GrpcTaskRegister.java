package io.grpc.helloworldexample.cpp.tasks;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import io.grpc.helloworldexample.cpp.HelloworldActivity;

public class GrpcTaskRegister extends AsyncTask<String, Void, Integer> {
    private final WeakReference<HelloworldActivity> activityReference;

    String username;
    String password;
    String mac;

    public GrpcTaskRegister(HelloworldActivity activity) {
        this.activityReference = new WeakReference<HelloworldActivity>(activity);
    }

    @Override
    protected Integer doInBackground(String... params) {
        username = params[0];
        password = params[1];
        mac = params[2];
        HelloworldActivity activity = activityReference.get();
        if (activity == null || isCancelled()) {
            return -1;
        }
        return activity.doRegister(username, password, mac, activity.cachePublicKey);
    }

    @Override
    protected void onPostExecute(Integer result) {
        HelloworldActivity activity = activityReference.get();
        if (activity == null || isCancelled()) {
            return;
        }
        Log.i("remile", "register result=" + result);
        if(result == 0) { // 注册成功，接着登录
            activity.doLogin(username, password, mac);
        } else if(result == 1) { // 账号已存在
            Toast.makeText(activity, "账号被占用", Toast.LENGTH_SHORT).show();
        } else { // 公钥失效
            Toast.makeText(activity, "公钥失效，请重新连接服务端", Toast.LENGTH_SHORT).show();
        }
    }
}

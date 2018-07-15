package io.grpc.helloworldexample.cpp.tasks;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;

import io.grpc.helloworldexample.cpp.HelloworldActivity;

public class GrpcTaskLoginWithTicket extends AsyncTask<String, Void, Integer> {
    private final WeakReference<HelloworldActivity> activityReference;

    public GrpcTaskLoginWithTicket(HelloworldActivity activity) {
        this.activityReference = new WeakReference<HelloworldActivity>(activity);
    }

    @Override
    protected Integer doInBackground(String... params) {
        String host = params[0];
        String portStr = params[1];

        HelloworldActivity activity = activityReference.get();
        if (activity == null || isCancelled()) {
            return -1;
        }

        int port = TextUtils.isEmpty(portStr) ? 50051 : Integer.valueOf(portStr);
        return activity.loginWithTicket(host, port, activity.cacheTicket, activity.cachePublicKey);
    }

    @Override
    protected void onPostExecute(Integer result) {
        HelloworldActivity activity = activityReference.get();
        if (activity == null || isCancelled()) {
            return;
        }
        Log.i("remile", "login result=" + result);
    }
}

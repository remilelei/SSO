package io.grpc.helloworldexample.cpp.tasks;

import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;

import io.grpc.helloworldexample.cpp.HelloworldActivity;

public class GrpcTaskLogout extends AsyncTask<String, Void, Integer> {

    private final WeakReference<HelloworldActivity> activityReference;
    private String username;
    private String ticket;

    public GrpcTaskLogout(HelloworldActivity activity) {
        this.activityReference = new WeakReference<HelloworldActivity>(activity);
    }
    @Override
    protected Integer doInBackground(String... strings) {
        username = strings[0];
        ticket = strings[1];
        HelloworldActivity activity = activityReference.get();
        if (activity == null || isCancelled()) {
            return -1;
        }
        return activity.logout(username, ticket);
    }
    @Override
    protected void onPostExecute(Integer result) {
        Log.i("remile", "logout result=" + result);
        HelloworldActivity activity = activityReference.get();
        if (activity != null) {
            activity.back2Login();
        }
    }
}
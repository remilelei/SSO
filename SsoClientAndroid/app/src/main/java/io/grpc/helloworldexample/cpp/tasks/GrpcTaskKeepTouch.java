package io.grpc.helloworldexample.cpp.tasks;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.remile.util.DeviceUtil;

import java.lang.ref.WeakReference;

import io.grpc.helloworldexample.cpp.HelloworldActivity;

public class GrpcTaskKeepTouch extends AsyncTask<String, Void, Void> {
    private final WeakReference<HelloworldActivity> activityReference;

    public GrpcTaskKeepTouch(HelloworldActivity activity) {
        this.activityReference = new WeakReference<HelloworldActivity>(activity);
    }

    @Override
    protected Void doInBackground(String... params) {
        HelloworldActivity activity = activityReference.get();
        if (!(activity == null || isCancelled()) && !TextUtils.isEmpty(activity.curUsername)) {
            activity.keepTouch(activity.curUsername, DeviceUtil.getMac(activity), activity.cacheTicket);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.i("remile", "link end. bye");
    }
}

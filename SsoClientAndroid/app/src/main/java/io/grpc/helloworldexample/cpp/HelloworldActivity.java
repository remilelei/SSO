package io.grpc.helloworldexample.cpp;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import io.grpc.helloworldexample.cpp.fragments.AfterLoginFragment;
import io.grpc.helloworldexample.cpp.fragments.EditHostFragment;
import io.grpc.helloworldexample.cpp.fragments.LoginFragment;
import io.grpc.helloworldexample.cpp.fragments.RegisterFragment;
import io.grpc.helloworldexample.cpp.tasks.GrpcTaskGetKey;
import io.grpc.helloworldexample.cpp.tasks.GrpcTaskKeepTouch;
import io.grpc.helloworldexample.cpp.tasks.GrpcTaskLogin;
import io.grpc.helloworldexample.cpp.tasks.GrpcTaskLogout;
import io.grpc.helloworldexample.cpp.tasks.GrpcTaskRegister;

public class HelloworldActivity extends AppCompatActivity {

    static {
        System.loadLibrary("grpc-helloworld");
    }
    private static final int MSG_KICKED = 1001;
    private static final int MSG_TICKET_ERROR = 1002;


    public byte[] cachePublicKey = null;
    public String cacheTicket = null;
    GrpcTaskKeepTouch mServerTask;
    String mHost = null;
    int mPort = 0;
    public String curUsername;

    EditHostFragment mEditHostFragment;
    LoginFragment mLoginFragment;
    AfterLoginFragment mAfterLoginFragment;
    RegisterFragment mRegisterFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helloworld);


        initFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!TextUtils.isEmpty(curUsername) && !TextUtils.isEmpty(cacheTicket)) {
            logout(curUsername, cacheTicket);
        }
    }

    private void initFragment() {
        mEditHostFragment = EditHostFragment.newInstance();
        mLoginFragment = LoginFragment.newInstance();
        mAfterLoginFragment = AfterLoginFragment.newInstance();
        mRegisterFragment = RegisterFragment.newInstance();

        getFragmentManager().beginTransaction()
                .add(R.id.fl_fragment_container, mEditHostFragment)
                .commit();
    }

    /**
     * 与服务端的交互逻辑在这里
     */
    // 从服务器拉取公钥
    public void getPublicKey(String host, String port) {
        mHost = host;
        try {
            mPort = Integer.valueOf(port);
        } catch (Exception e) {
            mPort = 50051;
        }
        GrpcTaskGetKey grpcTaskGetKey = new GrpcTaskGetKey(this);
        grpcTaskGetKey.executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR,
                mHost,port);
    }
    // 注册逻辑
    public void doRegister(String username, String password, String mac) {
        GrpcTaskRegister task = new GrpcTaskRegister(this);
        Log.i("remile", String.format("doRegister username=%s password=%s mac=%s", username, password, mac));
        task.executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR,
                username, password, mac);
    }
    // 登录逻辑
    public void doLogin(String username, String password, String mac) {
        GrpcTaskLogin task = new GrpcTaskLogin(this);
        task.executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR,
                username,
                password,
                mac);
    }
    // 申请发起长连接（登录后）
    public void doKeepTouch() {
        mServerTask = new GrpcTaskKeepTouch(this);
        mServerTask.executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);

    }
    // 退出登录
    public void doLogout() {
        if(!TextUtils.isEmpty(curUsername) && !TextUtils.isEmpty(cacheTicket)) {
            GrpcTaskLogout task = new GrpcTaskLogout(this);
            task.executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR,
                    curUsername,
                    cacheTicket);
        }
    }


    /**
     * fragment之间的替换跳转在这里
     */
    public void jump2Register() {
        getFragmentManager().beginTransaction()
                .replace(R.id.fl_fragment_container, mRegisterFragment)
                .commit();
    }
    public void back2Login() {
        getFragmentManager().beginTransaction()
                .replace(R.id.fl_fragment_container, mLoginFragment)
                .commit();
    }
    public void jump2AfterLogin() {
        getFragmentManager().beginTransaction()
                .replace(R.id.fl_fragment_container, mAfterLoginFragment)
                .commit();
    }

    /**
     * 还可以直接使用之前留下的票据进行登录，免于输入账号密码。为便于自测就不放开了。
     */
//    public void doLoginWithTicket(View view) {
//        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
//                .hideSoftInputFromWindow(hostEdit.getWindowToken(), 0);
//        GrpcTaskLoginWithTicket task = new GrpcTaskLoginWithTicket(this);
//        task.executeOnExecutor(
//                AsyncTask.THREAD_POOL_EXECUTOR,
//                hostEdit.getText().toString(),
//                portEdit.getText().toString());
//    }




    /**
     * 处理native传来的消息
     * @param msg native层传来的消息
     */
    public void handleServerMessage(int msg) {
        if(msg == 1) {
            Log.i("remile", "isRunServerTaskCancelled");
            if (mServerTask != null) {
                mMsgHandler.sendEmptyMessage(MSG_KICKED);
            }
        } else if(msg == 2) {
            Log.i("remile", "isRunServerTaskCancelled");
            mMsgHandler.sendEmptyMessage(MSG_TICKET_ERROR);
        }
    }

    Handler mMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_KICKED:
                    AlertDialog.Builder builder = new AlertDialog.Builder(HelloworldActivity.this);
                    builder.setTitle("from Server");
                    builder.setMessage("同名账号登录，被踢下线");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            back2Login();
                        }
                    });
                    builder.setCancelable(false);
                    builder.show();
                    break;
                case MSG_TICKET_ERROR:
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(HelloworldActivity.this);
                    builder2.setTitle("from Server");
                    builder2.setMessage("票据失效，重新登录");
                    builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            back2Login();
                        }
                    });
                    builder2.setCancelable(false);
                    builder2.show();
                    break;
                    default:
                        break;
            }
        }
    };

    public native byte[] getPublicKey(String host, int port, String ca);
    public native int doRegister(String username, String password, String mac, byte[] publicKey);
    public native String login(String username, String password, String mac, byte[] publicKey);
    public native int loginWithTicket(String host, int port, String ticket, byte[] publicKey);
    public native void keepTouch(String username, String mac, String ticket);
    public native int logout(String username, String ticket);
    public native String testJni();
}

package io.grpc.helloworldexample.cpp.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tencent.remile.util.DeviceUtil;

import io.grpc.helloworldexample.cpp.HelloworldActivity;
import io.grpc.helloworldexample.cpp.R;

public class RegisterFragment extends Fragment implements View.OnClickListener {

    private EditText mUsername;
    private EditText mPassword;
    private Button mRegister;
    private Button mBack;

    public RegisterFragment() {
        // Required empty public constructor
    }

    public static RegisterFragment newInstance() {
        RegisterFragment fragment = new RegisterFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_register, container, false);
        mUsername = (EditText) rootView.findViewById(R.id.et_username);
        mPassword = (EditText) rootView.findViewById(R.id.et_password);
        mBack = (Button) rootView.findViewById(R.id.btn_back);
        mRegister = (Button) rootView.findViewById(R.id.btn_register);

        mBack.setOnClickListener(this);
        mRegister.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.btn_register) {
            // 点击提交用户信息，进行注册
            Activity act = getActivity();
            if(act != null && act instanceof HelloworldActivity) {
                String username = mUsername.getText().toString();
                String password = mPassword.getText().toString();
                String mac = DeviceUtil.getMac(act);
                if(!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(mac)) {
                    ((HelloworldActivity)act).doRegister(username, password, mac);
                } else if (TextUtils.isEmpty(mac)) {
                    Toast.makeText(act, "mac地址获取失败！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(act, "请保证信息完整", Toast.LENGTH_SHORT).show();
                }
            }
        } else if(id == R.id.btn_back) {
            // 转到登录界面
            Activity act = getActivity();
            if(act != null && act instanceof HelloworldActivity) {
                ((HelloworldActivity)act).back2Login();
            }
        }
    }
}

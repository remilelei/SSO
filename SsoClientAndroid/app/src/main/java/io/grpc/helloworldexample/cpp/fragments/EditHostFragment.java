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

import io.grpc.helloworldexample.cpp.HelloworldActivity;
import io.grpc.helloworldexample.cpp.R;


public class EditHostFragment extends Fragment implements View.OnClickListener {

    private EditText mHost;
    private EditText mPort;
    private Button mCommit;

    public EditHostFragment() {
    }

    public static EditHostFragment newInstance() {
        EditHostFragment fragment = new EditHostFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_edit_host, container, false);
        mHost = (EditText) rootView.findViewById(R.id.et_host);
        mPort = (EditText) rootView.findViewById(R.id.et_ip);
        mCommit = (Button) rootView.findViewById(R.id.btn_cmt);
        mCommit.setOnClickListener(this);

        mHost.setText("192.168.43.172");
        mPort.setText("50051");

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
        Activity act = getActivity();
        if(act != null && act instanceof HelloworldActivity) {
            String host = mHost.getText().toString();
            String port = mPort.getText().toString();
            if(!TextUtils.isEmpty(host) && !TextUtils.isEmpty(port)) {
                ((HelloworldActivity)act).getPublicKey(host, port);
            }
        } else {
            Toast.makeText(getActivity(), "请填写服务器信息", Toast.LENGTH_SHORT).show();
        }
    }
}

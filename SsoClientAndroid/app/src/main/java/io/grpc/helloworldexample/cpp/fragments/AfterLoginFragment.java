package io.grpc.helloworldexample.cpp.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import io.grpc.helloworldexample.cpp.HelloworldActivity;
import io.grpc.helloworldexample.cpp.R;


public class AfterLoginFragment extends Fragment implements View.OnClickListener {
    private Button btnLogout;

    public AfterLoginFragment() {
        // Required empty public constructor
    }

    public static AfterLoginFragment newInstance() {
        AfterLoginFragment fragment = new AfterLoginFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_after_login, container, false);
        btnLogout = (Button) rootView.findViewById(R.id.btn_logout);

        btnLogout.setOnClickListener(this);
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
            ((HelloworldActivity) act).doLogout();
        }
    }
}

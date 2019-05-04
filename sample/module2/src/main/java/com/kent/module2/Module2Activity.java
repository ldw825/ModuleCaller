package com.kent.module2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kent.modulecaller.ModuleCaller;

public class Module2Activity extends AppCompatActivity {

    private Button mButton;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module2);
        mButton = findViewById(R.id.btn);
        mTextView = findViewById(R.id.textview);
    }

    public void onBtnClick(View view) {
        mTextView.setText("");
        mButton.setEnabled(false);
        ModuleCaller.getInstance().action("module1.getValueAsync").callback(new ModuleCaller.Callback() {
            @Override
            public void onCallSuccess(String action, Object result) {
                mTextView.setText("返回结果：" + result);
                mButton.setEnabled(true);
            }

            @Override
            public void onCallFailed(String action, String message) {
                mTextView.setText("返回结果：" + message);
                mButton.setEnabled(true);
            }
        }).call();
    }

}

package com.cazaea.scancode;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ScanActivity extends AppCompatActivity {

    @BindView(R.id.default_start)
    Button defaultStart;
    @BindView(R.id.wechat_start)
    Button wechatStart;
    @BindView(R.id.alipay_start)
    Button alipayStart;
    @BindView(R.id.create_code)
    Button createCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.default_start)
    public void onDefaultClicked() {
        startActivity(new Intent(this,DefaultCaptureActivity.class));
    }

    @OnClick(R.id.wechat_start)
    public void onWechatClicked() {
        startActivity(new Intent(this,WeChatCaptureActivity.class));
    }

    @OnClick(R.id.alipay_start)
    public void onAlipayClicked() {
        startActivity(new Intent(this,AliCaptureActivity.class));
    }

    @OnClick(R.id.create_code)
    public void onViewClicked() {
        startActivity(new Intent(this, ShareActivity.class));
    }
}

package com.cazaea.scancode;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.client.android.AnimViewCallback;
import com.google.zxing.client.android.BaseCaptureActivity;
import com.google.zxing.client.android.FlowLineView;

public class AliCaptureActivity extends BaseCaptureActivity {

    private static final String TAG = "AliCaptureActivity";
    private SurfaceView surfaceView;
    private FlowLineView flowLineView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ali_capture);
        surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        flowLineView = (FlowLineView) findViewById(R.id.auto_scanner_view);
    }

    @Override
    public SurfaceView getSurfaceView() {
        return (surfaceView == null) ? (SurfaceView) findViewById(R.id.preview_view) : surfaceView;
    }

    @Override
    public AnimViewCallback getViewfinderHolder() {
        return (flowLineView == null) ? (FlowLineView) findViewById(R.id.viewfinder_view) : flowLineView;
    }

    @Override
    public void dealDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        Log.i(TAG, "dealDecode ~~~~~ " + rawResult.getText() + " " + barcode + " " + scaleFactor);
        playBeepSoundAndVibrate(true, false);
        Toast.makeText(this, rawResult.getText(), Toast.LENGTH_SHORT).show();
//        对此次扫描结果不满意可以调用
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                reScan();
            }
        },1500);

    }
}

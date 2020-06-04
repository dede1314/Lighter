package com.life.lighter.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;

import com.life.lighter.R;
import com.life.lighter.ui.baseInfo.BaseInfoActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Handler handler=new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                goNext();
            }
        },1000L);

    }
    private void goNext(){
        // TODO 需要根据是否填过信息进行跳转的判断，先去填写信息页面
        startActivity(new Intent(this, BaseInfoActivity.class));
    }


}
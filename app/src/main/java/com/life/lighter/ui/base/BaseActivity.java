package com.life.lighter.ui.base;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public abstract class BaseActivity extends AppCompatActivity {


    protected View mContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupContentView();
    }


    private void setupContentView() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        mContentView = View.inflate(this, layoutID(), null);
//        root_layout.addView(mContentView, layoutParams);
    }

    public abstract int layoutID();
}
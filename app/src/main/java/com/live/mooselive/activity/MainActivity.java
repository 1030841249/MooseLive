package com.live.mooselive.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.live.mooselive.R;
import com.live.mooselive.base.BaseActivity;
import com.live.mooselive.utils.PermissionUtil;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    @BindView(R.id.btn_anchor)
    Button btnAnchor;
    @BindView(R.id.btn_audience)
    Button btnAudience;

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void init() {

        PermissionUtil.requestWRPermissions(this);
    }

    @OnClick({R.id.btn_anchor, R.id.btn_audience})
    void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_anchor:
                startActivity(new Intent(this,AnchorActivity.class));
                break;
            case R.id.btn_audience:
                startActivity(new Intent(this,AudienceActivity.class));
                break;
        }
    }
}

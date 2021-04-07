package com.live.mooselive.utils;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Toast;

import com.live.mooselive.App;

public class ToastUtil {
    static Handler handler = new Handler(Looper.getMainLooper());
    static Toast mToast;
    static {
        mToast = Toast.makeText(App.getInstance(), "", Toast.LENGTH_SHORT);
    }
    public static void showShortToast(String msg) {
            handler.post(()->{
                mToast.setText(msg);
                mToast.setGravity(Gravity.CENTER, 0, 0);
                mToast.show();
            });
    }
}

package com.live.mooselive.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.live.mooselive.App;

public class ToastUtil {
    static Handler handler = new Handler(Looper.getMainLooper());
    public static void showShortToast(String msg) {
            handler.post(()->{
                Toast.makeText(App.getInstance(), msg, Toast.LENGTH_SHORT);
            });
    }
}

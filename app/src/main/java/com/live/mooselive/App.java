package com.live.mooselive;

import android.app.Application;
import android.content.Context;

/**
 * author: ZDH
 * Date: 2021/2/14
 * Description:
 */
public class App extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getInstance() {
        return mContext;
    }
}

package com.example.screens;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

public class FloatWindowValue {
    private static FloatWindowValue floatWindowValue;
    public  int mScreenWidth;
    public  int mScreenHeight;
    public  int mScreenDensity;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;


    public WindowManager.LayoutParams getmLayoutParams() {
        if(mLayoutParams ==null) {
            mLayoutParams = new WindowManager.LayoutParams();
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            mLayoutParams.format = PixelFormat.RGBA_8888;
            // 设置Window flag
            mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
            mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                mLayoutParams.x = floatWindowValue.mScreenWidth;
                mLayoutParams.y = 100;
        }
        return mLayoutParams;
    }

    public void setmWindowManager(WindowManager mWindowManager) {
        if (mWindowManager == null) {
            this.mWindowManager = mWindowManager;
        }
    }

    public WindowManager getmWindowManager() {
        return mWindowManager;
    }

    public static FloatWindowValue getInstance(){
        if (floatWindowValue == null){
            Log.d("FloatWindowValue", "getInstance: ");
            floatWindowValue = new FloatWindowValue();
        }
        return floatWindowValue;
    }
}

package com.example.screens.preview;

import android.app.Application;
import android.graphics.Bitmap;

/**別人製作的預覽圖片類別
 * Created by Ryze on 2016-7-20.
 */
public class ScreenCaptureApplication extends Application {


  private Bitmap mScreenCaptureBitmap;

  @Override
  public void onCreate() {
    super.onCreate();
  }


  public Bitmap getmScreenCaptureBitmap() {
    return mScreenCaptureBitmap;
  }

  public void setmScreenCaptureBitmap(Bitmap mScreenCaptureBitmap) {
    this.mScreenCaptureBitmap = mScreenCaptureBitmap;
  }
}

package com.example.screens;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by branch on 2016-5-25.
 *
 * 启动悬浮窗界面
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class FloatWindowsService extends Service {
  private static final String TAG = "FloatWindowsService";

  private static FloatWindowsService floatWindowsService;

  public static FloatWindowsService getInstance(){
    floatWindowsService = new FloatWindowsService();
    return floatWindowsService;
  }

  public static Intent newIntent(Context context, Intent mResultData) {

    Intent intent = new Intent(context, FloatWindowsService.class);

    if (mResultData != null) {
      intent.putExtras(mResultData);
    }
    return intent;
  }

  private MediaProjection mMediaProjection;
  private VirtualDisplay mVirtualDisplay;

  private static Intent mResultData = null;


  private ImageReader mImageReader;
  private WindowManager mWindowManager;
  private WindowManager.LayoutParams mLayoutParams;
  private GestureDetector mGestureDetector;

  private ImageView mFloatView;

  private int mScreenWidth;
  private int mScreenHeight;
  private int mScreenDensity;


  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "onCreate: ");
    createFloatView();
    createImageReader();
  }

  public static Intent getResultData() {
    return mResultData;
  }

  public static void setResultData(Intent mResultData) {
    Log.d(TAG, "setResultData: ");
    FloatWindowsService.mResultData = mResultData;
  }


  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void createFloatView() {
    Log.d(TAG, "createFloatView: ");
    //創建手勢監聽物件
    mGestureDetector = new GestureDetector(getApplicationContext(), new FloatGestrueTouchListener());
    mLayoutParams = new WindowManager.LayoutParams();
    mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

    DisplayMetrics metrics = new DisplayMetrics();
    mWindowManager.getDefaultDisplay().getMetrics(metrics);
    mScreenDensity = metrics.densityDpi;
    mScreenWidth = metrics.widthPixels;
    mScreenHeight = metrics.heightPixels;
    // 需請求權限<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>，以及宣告 ACTION_MANAGE_OVERLAY_PERMISSION
    mLayoutParams.type =  WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    mLayoutParams.format = PixelFormat.RGBA_8888;
    // 设置Window flag
    mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
    mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
    mLayoutParams.x = mScreenWidth;
    mLayoutParams.y = 100;
    mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
    mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;


    mFloatView = new ImageView(getApplicationContext());
    mFloatView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_imagetool_crop));
    mWindowManager.addView(mFloatView, mLayoutParams);


    mFloatView.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
      }
    });

  }


  private class FloatGestrueTouchListener implements GestureDetector.OnGestureListener {

    int lastX, lastY;
    int paramX, paramY;

    @Override
    public boolean onDown(MotionEvent event) {
      lastX = (int) event.getRawX();
      lastY = (int) event.getRawY();
      paramX = mLayoutParams.x;
      paramY = mLayoutParams.y;
      return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
      //按下後沒有鬆開手，短時間就會觸發
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
      //單擊
      startScreenShot();
      return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
      int dx = (int) e2.getRawX() - lastX;
      int dy = (int) e2.getRawY() - lastY;
      mLayoutParams.x = paramX + dx;
      mLayoutParams.y = paramY + dy;
      // 更新悬浮窗位置
      mWindowManager.updateViewLayout(mFloatView, mLayoutParams);
      return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
      //跟onShowPress類似，其實就是長按事件
      Log.d(TAG, "onLongPress: ");
      AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());

      AlertDialog alertDialog =
      builder.setMessage("是否關閉彈窗?")
              .setPositiveButton("關閉彈窗", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  Intent intent = new Intent(getApplicationContext(),FloatWindowsService.class);
                  stopService(intent);
                }
              })
              .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
              })
              .setTitle("提示")
              .setIcon(R.mipmap.ic_imagetool_cancel)
              .create();
      alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
      alertDialog.show();
    }


    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      return false;
    }
  }


  private void startScreenShot() {
    Log.d(TAG, "startScreenShot: ");
    mFloatView.setVisibility(View.GONE);

    Handler handler1 = new Handler();
    handler1.postDelayed(new Runnable() {
      public void run() {
        //start virtual
        startVirtual();
      }
    }, 5);

    handler1.postDelayed(new Runnable() {
      public void run() {
        //capture the screen
        startCapture();

      }
    }, 30);
  }


  private void createImageReader() {
    Log.d(TAG, "createImageReader: ");
    mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1);

  }

  public void startVirtual() {
    Log.d(TAG, "startVirtual: ");
    if (mMediaProjection != null) {
      virtualDisplay();
    } else {
      setUpMediaProjection(); //设定MediaProjectionManager
      virtualDisplay();
    }
  }

  public void setUpMediaProjection() {
    Log.d(TAG, "setUpMediaProjection: ");
    if (mResultData == null) {
      Intent intent = new Intent(Intent.ACTION_MAIN);
      intent.addCategory(Intent.CATEGORY_LAUNCHER);
      startActivity(intent);
    } else {
      mMediaProjection = getMediaProjectionManager().getMediaProjection(Activity.RESULT_OK, mResultData);
    }
  }

  private MediaProjectionManager getMediaProjectionManager() {
    Log.d(TAG, "getMediaProjectionManager: ");
    return (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
  }

  private void virtualDisplay() {
    Log.d(TAG, "virtualDisplay: ");
    mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
        mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        mImageReader.getSurface(), null, null);
  }

  private void startCapture() {
    Log.d(TAG, "startCapture: ");
    Image image = mImageReader.acquireLatestImage();

    if (image == null) {
      startScreenShot();
    } else {
      SaveTask mSaveTask = new SaveTask();
      mSaveTask.execute(image);
//      AsyncTaskCompat.executeParallel(mSaveTask, image);  在新API中已經移除這個類別
    }
  }


  public class SaveTask extends AsyncTask<Image, Void, Bitmap> {

    @Override
    protected Bitmap doInBackground(Image... params) {

      if (params == null || params.length < 1 || params[0] == null) {

        return null;
      }

      Image image = params[0];

      int width = image.getWidth();
      int height = image.getHeight();
      final Image.Plane[] planes = image.getPlanes();
      final ByteBuffer buffer = planes[0].getBuffer();
      //每个像素的间距
      int pixelStride = planes[0].getPixelStride();
      //总的间距
      int rowStride = planes[0].getRowStride();
      int rowPadding = rowStride - pixelStride * width;
      Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
      bitmap.copyPixelsFromBuffer(buffer);
      bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
      image.close();
      File fileImage = null;
      if (bitmap != null) {

//        fileImage = saveShopImage(FloatWindowsService.this,bitmap);

        try {
          fileImage = new File(FileUtil.getScreenShotsName(getApplicationContext()));
          Log.d(TAG, "doInBackground: "+fileImage);
          if (!fileImage.exists()) {
            fileImage.createNewFile();
          }
          FileOutputStream out = new FileOutputStream(fileImage);
          if (out != null) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(fileImage);
            media.setData(contentUri);
            sendBroadcast(media);
          }
        } catch (FileNotFoundException e) {
          e.printStackTrace();
          fileImage = null;
        } catch (IOException e) {
          e.printStackTrace();
          fileImage = null;
        }
      }

      if (fileImage != null) {
        return bitmap;
      }
      return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
      super.onPostExecute(bitmap);
      //预览图片
      if (bitmap != null) {

        ((ScreenCaptureApplication) getApplication()).setmScreenCaptureBitmap(bitmap);
        Log.d(TAG, "onPostExecute: 获取图片成功");
        startActivity(PreviewPictureActivity.newIntent(getApplicationContext()));
      }

      mFloatView.setVisibility(View.VISIBLE);

    }

    private File saveShopImage(Service mService , Bitmap bmp) {
      //檔名設置
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd");
      NumberFormat nf = new DecimalFormat("000");
      int count = 1;
      String finename = "Screen_" + sdf.format(new Date());
      // 圖片檔案路徑
      File file = new File(mService.getExternalFilesDir("SCREEN"), finename + "_001.png");
      while ((file.exists())) {
        file = new File(mService.getExternalFilesDir("SCREEN"), finename + "_" + nf.format(count) + ".png");
        count++;
      }
      try {
        FileOutputStream os = new FileOutputStream(file);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.flush();
        os.close();
        Log.d("MainActivity", "screenshot: 创建:" + file);
      } catch (Exception e) {
        Log.e("MainActivity", "screenshot: " + e.toString());
      }

      return file;
    }

  }


  private void tearDownMediaProjection() {
    Log.d(TAG, "tearDownMediaProjection: ");
    if (mMediaProjection != null) {
      mMediaProjection.stop();
      mMediaProjection = null;
    }
  }

  private void stopVirtual() {
    Log.d(TAG, "stopVirtual: ");
    if (mVirtualDisplay == null) {
      return;
    }
    mVirtualDisplay.release();
    mVirtualDisplay = null;
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy: ");
    // to remove mFloatLayout from windowManager
    super.onDestroy();
    if (mFloatView != null) {
      mWindowManager.removeView(mFloatView);
    }
    stopVirtual();
    tearDownMediaProjection();
  }

}

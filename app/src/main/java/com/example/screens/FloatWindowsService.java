package com.example.screens;

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
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * Created by branch on 2016-5-25.
 * <p>
 * 启动悬浮窗界面
 */

public class FloatWindowsService extends Service {
    private FloatWindowValue floatWindowValue = FloatWindowValue.getInstance();
    private static final String TAG = "FloatWindowsService";
    private Intent mintent;

//    public static Intent newIntent(Context context, Intent mResultData) {
//
//        Intent intent = new Intent(context, FloatWindowsService.class);
//
//        if (mResultData != null) {
//            intent.putExtras(mResultData);
//        }
//        return intent;
//    }

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;

    private static Intent mResultData = null;


    private ImageReader mImageReader;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private GestureDetector mGestureDetector;
    private ImageView mFloatView;


    @Override
    public void onCreate() {
        mintent = new Intent(getApplicationContext(), FloatWindowsService.class);
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        createFloatView();
        createImageReader();
    }

//    public static Intent getResultData() {
//        return mResultData;
//    }

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
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();

        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        floatWindowValue.mScreenDensity = metrics.densityDpi;
        floatWindowValue.mScreenWidth = metrics.widthPixels;
        floatWindowValue.mScreenHeight = metrics.heightPixels;

        mLayoutParams = floatWindowValue.getmLayoutParams();
        mFloatView = new ImageView(getApplicationContext());
        mFloatView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_imagetool_crop));
        //創建手勢監聽物件
        mGestureDetector = new GestureDetector(getApplicationContext(), new FloatGestrueTouchListener());
        mFloatView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });
        mWindowManager.addView(mFloatView, mLayoutParams);
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
            // 更新位置
            mWindowManager.updateViewLayout(mFloatView, mLayoutParams);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            //跟onShowPress類似，其實就是長按事件
            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
            AlertDialog alertDialog =
                    builder.setMessage("是否關閉彈窗?")
                            .setPositiveButton("關閉彈窗", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    stopService(mintent);
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
        //截圖時隱藏按鈕
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
        mImageReader =
                ImageReader.newInstance(floatWindowValue.mScreenWidth,
                        floatWindowValue.mScreenHeight,
                        PixelFormat.RGBA_8888,
                        1);
        // 1.前兩個參數用來指定生成圖像的寬和高。
        // 2.第三個參數format是圖像的格式，
        //   必須是ImageFormat或PixelFormat中的一種，這裡使用PixelFormat.RGBA_8888(並非所有格式ImageReader都支援，例如ImageFormat.NV21)。
        // 3.第四個參數是maxImages，這個參數是你想同時在ImageReader裡獲取到的Image對象的個數，ImageReader是一個類似數組的東西，
        //   然後我們可以通過acquireLatestImage()或acquireNextImage()方法來得到裡面的Image對象。
        //   這個值應該設置的越小越好，但是得大於0。
    }

    public void startVirtual() {
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
            Intent intent = new Intent(this, MainActivity.class);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(intent);
        } else {
            mMediaProjection =
                    ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE))
                            .getMediaProjection(Activity.RESULT_OK, mResultData);
        }
    }

    private void virtualDisplay() {
        Log.d(TAG, "virtualDisplay: ");
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                floatWindowValue.mScreenWidth,
                floatWindowValue.mScreenHeight,
                floatWindowValue.mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
    }

    private void startCapture() {
        Log.d(TAG, "startCapture: ");
        Image image = mImageReader.acquireLatestImage();

        if (image == null) {
            startScreenShot();
        } else {
            SaveTask mSaveTask = new SaveTask(this);
            mSaveTask.execute(image);
        }
    }


    public class SaveTask extends AsyncTask<Image, Void, Bitmap> {

        private WeakReference<Service> mWeakReference;

        public SaveTask(Service mService) {
            mWeakReference = new WeakReference<>(mService);
        }

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
            //到這邊已經取得了Bitmap

            File fileImage = null;
            if (bitmap != null) {
//                fileImage = Util.getInstance().saveImage(FloatWindowsService.this, bitmap);
//                fileImage = Util.getInstance().saveImageMatchBroadcast(FloatWindowsService.this, bitmap);
                fileImage = Util.getInstance().saveImageMatchMediaStore(FloatWindowsService.this, bitmap);
            }
            if (fileImage != null) {
                return bitmap;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            Service mService = mWeakReference.get();
            if (isCancelled() || mService == null) {
                return;
            }
            //預覽圖片
            if (bitmap != null) {
                /*********************************************************/
            ImageView lookImage = new ImageView(getApplicationContext());
            lookImage.setImageBitmap(bitmap);
            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
            AlertDialog alertDialog =
                    builder.setPositiveButton("關閉", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            })
                            .setTitle("預覽")
                            .setIcon(R.mipmap.ic_imagetool_cancel)
                            .setView(lookImage)
                            .create();
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            alertDialog.show();
                /*********************************************************/
//                ((ScreenCaptureApplication) getApplication()).setmScreenCaptureBitmap(bitmap);
                Log.d(TAG, "onPostExecute: 獲取圖片成功");
//                startActivity(PreviewPictureActivity.newIntent(getApplicationContext()));
            }
            //結束時再顯示按鈕
            mFloatView.setVisibility(View.VISIBLE);
            mMediaProjection.stop();
            mMediaProjection = null;
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

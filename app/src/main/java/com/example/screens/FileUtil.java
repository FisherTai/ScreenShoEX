package com.example.screens;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ryze on 2016-5-26.
 */
public class FileUtil {

    //系统保存截图的路径
    public static final String SCREENCAPTURE_PATH = "ScreenCapture" + File.separator + "Screenshots" + File.separator;
//  public static final String SCREENCAPTURE_PATH = "ZAKER" + File.separator + "Screenshots" + File.separator;

    public static final String SCREENSHOT_NAME = "Screenshot";

    public static String getAppPath(Context context) {

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {


            return Environment.getExternalStorageDirectory().toString();

        } else {

            return context.getFilesDir().toString();
        }

    }


    public static String getScreenShots(Context context) {

        StringBuffer stringBuffer = new StringBuffer(getAppPath(context));
        stringBuffer.append(File.separator);

        stringBuffer.append(SCREENCAPTURE_PATH);

        File file = new File(stringBuffer.toString());

        if (!file.exists()) {
            file.mkdirs();
        }

        return stringBuffer.toString();

    }

    public static String getScreenShotsName(Context context) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");

        String date = simpleDateFormat.format(new Date());

        StringBuffer stringBuffer = new StringBuffer(getScreenShots(context));
        stringBuffer.append(SCREENSHOT_NAME);
        stringBuffer.append("_");
        stringBuffer.append(date);
        stringBuffer.append(".png");

        return stringBuffer.toString();

    }


    public static File saveImage(Service mService, Bitmap bmp) {
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
            Log.d("saveShopImage", "screenshot: 创建:" + file);
        } catch (Exception e) {
            Log.e("saveShopImage", "screenshot: " + e.toString());
        }
        return file;
    }

    public static File saveImageMatchBroadcast(Service mService, Bitmap bmp) {
        File fileImage = null;
        try {
            fileImage = new File(FileUtil.getScreenShotsName(mService));
            if (!fileImage.exists()) {
                fileImage.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(fileImage);
            if (out != null) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
                Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(fileImage);
                media.setData(contentUri);
                mService.sendBroadcast(media);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileImage;
    }

    public static File saveImageMatchMediaStore(Service mService, Bitmap bmp) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DESCRIPTION, "This is an image");
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "Image.png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.TITLE, "Image.png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/test");
        Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver resolver = mService.getContentResolver();

        Uri insertUri = resolver.insert(external, values);
        Log.d("saveImageMatchMediaStore", "doInBackground: Test " + insertUri);

        OutputStream os = null;
        try {
            if (insertUri != null) {
                os = resolver.openOutputStream(insertUri);
            }
            if (os != null) {
              bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
        } catch (IOException e) {
            Log.e("saveImageMatchMediaStore", "doInBackground: " + e.toString());
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                Log.e("fail in close: ", e.toString());
            }
        }
        return null;
    }


}

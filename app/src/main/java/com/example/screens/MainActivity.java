package com.example.screens;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends FragmentActivity {
    private static final String TAG = "MainActivity";
    public static final int ACTION_MANAGE_OVERLAY_PERMISSION = 0;
    public static final int WRITE_EXTERNAL_STORAGE = 1;
    public static final int REQUEST_MEDIA_PROJECTION = 18;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission();

        findViewById(R.id.btn_on).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(new Intent(getApplicationContext(), FloatWindowsService.class));
            }
        });
    }


    public void requestPermission() {

        int permission = ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE);
        }

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION);
        }

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_MEDIA_PROJECTION:
                if (resultCode == RESULT_OK && data != null) {
                    FloatWindowsService.setResultData(data);
                    Log.d(TAG, "onActivityResult: 獲得擷取螢幕權限");
                }
                break;
            case ACTION_MANAGE_OVERLAY_PERMISSION:
                Log.d(TAG, "onActivityResult: 獲得顯示在其他APP上的權限");
                break;
            case WRITE_EXTERNAL_STORAGE:
                Log.d(TAG, "onActivityResult: 獲得存儲手機資料的權限");
                break;
        }

    }


}

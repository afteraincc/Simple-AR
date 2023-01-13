package com.example.simple_ar_android;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private boolean capturing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        findViewById(R.id.switchOverlayPermisson).setOnClickListener(
                view -> enableOverlayPermission()
        );
        findViewById(R.id.btnStart).setOnClickListener(
                view -> startClick()
        );

        capturing = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        Switch s = findViewById(R.id.switchOverlayPermisson);
        s.setClickable(!hasOverlayPermission());
        s.setChecked(hasOverlayPermission());

        s = findViewById(R.id.switchWifiStatus);
        s.setClickable(false);
        s.setChecked(isWifiApEnabled());

        findViewById(R.id.btnStart).setEnabled(hasOverlayPermission() && !capturing);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (isCalledFromOverlayService(intent)) {
            capturing = false;
            findViewById(R.id.btnStart).setEnabled(true);
        }
        setIntent(intent);
        super.onNewIntent(intent);
    }

    private boolean isCalledFromOverlayService(Intent intent) {
        if (intent != null) {
            return intent.getBooleanExtra("CallFromOverlayService", false);
        } else {
            return false;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    private void enableOverlayPermission() {
        startActivityForResult(
                new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())),
                0);
    }

    private boolean hasOverlayPermission() {
        return Settings.canDrawOverlays(this);
    }

    private boolean isWifiApEnabled() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApState");
            int status = (Integer) method.invoke(wifiManager);
            // WIFI_AP_STATE_ENABLED = 13
            return 13 == status;
        } catch (Exception e) {
            Log.e(TAG, "isWifiApEnabled: Cannot get WiFi AP state", e);
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (hasOverlayPermission()) {
                Toast.makeText(this, "悬浮窗口已授权", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "悬浮窗口未授权", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "录屏已授权", Toast.LENGTH_SHORT).show();
                startCapture(resultCode, data);
            } else {
                Toast.makeText(this, "录屏未授权", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startClick() {
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), 1);

        findViewById(R.id.btnStart).setEnabled(false);
        capturing = true;
    }

    private void startCapture(int resultCode, Intent data) {
        moveTaskToBack(true);
        Intent intent = new Intent(MainActivity.this, OverlayService.class);
        intent.putExtra("code", resultCode);
        intent.putExtra("data", data);

        TextView textView = (TextView) findViewById(R.id.textIP);
        intent.putExtra("IP", textView.getText().toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
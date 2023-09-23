package com.example.simple_ar_android;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class OverlayService extends IntentService {

    private static final String TAG = "OverlayService";
    private String ip;

    private boolean exitFlag;
    private int startId;

    private WindowManager winManager;
    private Button overlayButton;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    public OverlayService() {
        super("OverlayService");
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + startId);
        this.startId = startId;
        ip = intent.getStringExtra("IP");

        initNotification();
        startView();
        startCapture(intent.getIntExtra("code", -1),
                intent.getParcelableExtra("data"));

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent");

        exitFlag = false;
        FrameSender frameSender = new FrameSender();
        frameSender.start(ip);

        while (!exitFlag) {
            Image image = imageReader.acquireLatestImage();
            if (image == null) {
                //Log.d(TAG, "onHandleIntent: image is null");
                SystemClock.sleep(10);
                continue;
            }
            if (frameSender.connected()) {
                frameSender.sendFrame(image);
                frameSender.receiveAck();
            } else {
                frameSender.stop();
                frameSender.start(ip);
                SystemClock.sleep(10);
            }
            image.close();
        }

        frameSender.stop();
    }

    private void initNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());

        builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setContentTitle("OverlayCapture");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("OverlayCaptureChannelId");

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                    "OverlayCaptureChannelId", "OverlayCapture",
                    NotificationManager.IMPORTANCE_NONE);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build();
        startForeground(1, notification);
    }

    private void startView() {
        winManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);

        LayoutParams wmParams = new LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wmParams.type = LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            wmParams.type = LayoutParams.TYPE_PHONE;
        }
        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCH_MODAL |
                LayoutParams.FLAG_LAYOUT_IN_SCREEN | LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        wmParams.gravity = Gravity.BOTTOM | Gravity.END;
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.width = 256;
        wmParams.height = 128;
        wmParams.format = PixelFormat.RGBA_8888;

        overlayButton = new Button(getApplicationContext());
        overlayButton.setText("停止");
        winManager.addView(overlayButton, wmParams);

        overlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCapture();
                stopView();

                exitFlag = true;
                stopForeground(true);
                stopSelf(startId);

                startActivity(new Intent(OverlayService.this, MainActivity.class)
                        .putExtra("CallFromOverlayService", true)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        });
    }

    private void stopView() {
        winManager.removeView(overlayButton);
    }

    public void startCapture(int resultCode, Intent data) {
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        this.mediaProjection = manager.getMediaProjection(resultCode, data);

        imageReader = ImageReader.newInstance(winManager.getDefaultDisplay().getWidth(),
                winManager.getDefaultDisplay().getHeight(), PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay("OverlayCapture",
                winManager.getDefaultDisplay().getWidth(),
                winManager.getDefaultDisplay().getHeight(),
                1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);
    }

    private void stopCapture() {
        virtualDisplay.release();
        mediaProjection.stop();
    }
}
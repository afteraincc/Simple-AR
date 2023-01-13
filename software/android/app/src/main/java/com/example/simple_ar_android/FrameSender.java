package com.example.simple_ar_android;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class FrameSender {
    private static final String TAG = "FrameSender";
    Socket socket;
    boolean connected;

    public boolean start(String ip) {
        socket = new Socket();
        connected = false;
        try {
            socket.connect(new InetSocketAddress(ip, 12345), 1000);
            connected = true;
        } catch (IOException e) {
            Log.e(TAG, "start: connect exception", e);
        }
        return connected;
    }

    public void stop() {
        if (connected()) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "stop: close exception", e);
            }
            connected = false;
        }
    }

    public boolean connected() {
        return connected;
    }


    public void sendFrame(Image image) {
        if (!connected()) {
            return;
        }

        Bitmap screenBmp = Bitmap.createBitmap(
                image.getPlanes()[0].getRowStride() / image.getPlanes()[0].getPixelStride(),
                image.getHeight(), Bitmap.Config.ARGB_8888);
        screenBmp.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());

        int w = screenBmp.getWidth();
        int h = screenBmp.getHeight();
        Bitmap originBmp = Bitmap.createBitmap(screenBmp, 0, 0, w, w);

        Matrix matrix = new Matrix();
        matrix.postScale(1.0F * 240 / originBmp.getWidth(), 1.0F * 240 / originBmp.getHeight());
        Bitmap scaleBmp = Bitmap.createBitmap(originBmp, 0, 0, originBmp.getWidth(), originBmp.getHeight(), matrix, false);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        scaleBmp.compress(Bitmap.CompressFormat.JPEG, 80, out);

        try {
            Log.d(TAG, "sendFrame: " + out.size());
            socket.getOutputStream().write(out.toByteArray(), 0, out.size());
        } catch (IOException e) {
            Log.e(TAG, "sendFrame: write exception", e);
            connected = false;
        }
    }

    public void receiveAck() {
        if (!connected()) { return; }

        byte[] ack =  new byte[3];
        try {
            int off = 0;
            while (off < 3) {
                int size = socket.getInputStream().read(ack, off, 3-off);
                if (size > 0) {
                    off += size;
                }
            }
            if (ack[0] != 'A' && ack[1] != 'C' & ack[2] !='K') {
                connected = false;
            }
            Log.d(TAG, "receiveAck: " + connected);
        } catch (IOException e) {
            Log.e(TAG, "receiveAck: read exception", e);
            connected = false;
        }
    }
}

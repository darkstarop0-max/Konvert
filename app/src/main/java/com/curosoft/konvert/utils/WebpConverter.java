package com.curosoft.konvert.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class WebpConverter {
    private static final String TAG = "WebpConverter";

    public static String convertWebpToJpgOrPng(Context context, Uri webpUri, String targetFormat) {
        Bitmap bitmap = null;
        InputStream inputStream = null;
        OutputStream outStream = null;
        File outFile = null;
        try {
            inputStream = context.getContentResolver().openInputStream(webpUri);
            bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode WEBP");
                showToast(context, "WEBP conversion failed");
                return null;
            }
            String ext = targetFormat.toLowerCase();
            String baseName = getBaseName(context, webpUri);
            String outFileName = baseName + "_converted." + ext;
            File outDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Konvert/Converted");
            if (!outDir.exists()) outDir.mkdirs();
            outFile = new File(outDir, outFileName);
            outStream = new FileOutputStream(outFile);
            boolean result = false;
            if (ext.equals("jpg")) {
                result = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
            } else if (ext.equals("png")) {
                result = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            } else {
                Log.e(TAG, "Unsupported target format: " + ext);
                showToast(context, "WEBP conversion failed");
                return null;
            }
            outStream.flush();
            outStream.close();
            if (result && outFile.length() > 0) {
                addToMediaStore(context, outFile, ext);
                return outFile.getAbsolutePath();
            } else {
                if (outFile.exists()) outFile.delete();
                showToast(context, "WEBP conversion failed");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "WEBP conversion failed", e);
            if (outFile != null && outFile.exists()) outFile.delete();
            showToast(context, "WEBP conversion failed");
            return null;
        } finally {
            if (bitmap != null) bitmap.recycle();
            try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
            try { if (outStream != null) outStream.close(); } catch (Exception ignored) {}
        }
    }

    private static void showToast(Context context, String msg) {
        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }

    private static String getBaseName(Context context, Uri uri) {
        String name = "image";
        try {
            String[] proj = {MediaStore.MediaColumns.DISPLAY_NAME};
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    name = cursor.getString(0);
                    if (name.contains(".")) name = name.substring(0, name.lastIndexOf('.'));
                }
            }
        } catch (Exception ignored) {}
        return name;
    }

    private static void addToMediaStore(Context context, File file, String ext) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
            values.put(MediaStore.MediaColumns.MIME_TYPE, ext.equals("png") ? "image/png" : "image/jpeg");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Konvert/Converted");
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add to MediaStore", e);
        }
    }
}

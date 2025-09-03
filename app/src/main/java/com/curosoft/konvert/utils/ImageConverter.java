package com.curosoft.konvert.utils;

import android.database.Cursor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageConverter {
    private static final String TAG = "ImageConverter";

    public static boolean convertImage(Context context, Uri inputUri, String targetFormat) {
        Bitmap bitmap = null;
        InputStream inputStream = null;
        OutputStream outStream = null;
        File outFile = null;
        boolean result = false;
        try {
            // Downsample large images
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            inputStream = context.getContentResolver().openInputStream(inputUri);
            BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            int maxDim = 2048;
            int scale = 1;
            while (options.outWidth / scale > maxDim || options.outHeight / scale > maxDim) {
                scale *= 2;
            }
            options.inSampleSize = scale;
            options.inJustDecodeBounds = false;

            inputStream = context.getContentResolver().openInputStream(inputUri);
            bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            if (bitmap == null) {
                Log.e(TAG, "Bitmap decode failed: Bitmap is null");
                showToast(context, "Invalid PNG file.");
                return false;
            }

            // Prepare output file
            String ext = targetFormat.toLowerCase();
            String baseName = getBaseName(context, inputUri);
            String outFileName = baseName + "_converted." + ext;
            File outDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Konvert/Converted");
            if (!outDir.exists()) outDir.mkdirs();
            outFile = new File(outDir, outFileName);
            outStream = new FileOutputStream(outFile);

            switch (ext) {
                case "png":
                    result = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                    break;
                case "webp":
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        result = bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 90, outStream);
                    } else {
                        result = bitmap.compress(Bitmap.CompressFormat.WEBP, 90, outStream);
                    }
                    break;
                case "jpeg":
                case "jpg":
                    result = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
                    break;
                default:
                    result = false;
            }
            if (outStream != null) {
                outStream.flush();
                outStream.close();
            }
            if (result && outFile != null && outFile.length() > 0) {
                addToMediaStore(context, outFile, ext);
            } else {
                if (outFile != null && outFile.exists()) outFile.delete();
                showToast(context, "Conversion failed.");
            }
            return result && outFile != null && outFile.length() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Image conversion failed", e);
            if (outFile != null && outFile.exists()) outFile.delete();
            showToast(context, "Conversion failed.");
            return false;
        } finally {
            if (bitmap != null) bitmap.recycle();
            try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
            try { if (outStream != null) outStream.close(); } catch (Exception ignored) {}
        }
    }

    private static String getBaseName(Context context, Uri uri) {
        String name = "image";
        try {
            String[] proj = {MediaStore.MediaColumns.DISPLAY_NAME};
            try (Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null)) {
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
            values.put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(ext));
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Konvert/Converted/Images");
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add to MediaStore", e);
        }
    }

    private static String getMimeType(String ext) {
        switch (ext) {
            case "png": return "image/png";
            case "webp": return "image/webp";
            case "heic": return "image/heic";
            case "bmp": return "image/bmp";
            case "gif": return "image/gif";
            default: return "image/jpeg";
        }
    }

    // Minimal BMP encoder
    private static boolean saveBmp(Bitmap bmp, OutputStream out) {
        try {
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            int[] pixels = new int[width * height];
            bmp.getPixels(pixels, 0, width, 0, 0, width, height);
            // BMP header
            out.write(new byte[]{0x42, 0x4D}); // BM
            int fileSize = 54 + width * height * 3;
            out.write(intToBytesLE(fileSize));
            out.write(new byte[4]); // reserved
            out.write(intToBytesLE(54)); // offset
            out.write(intToBytesLE(40)); // header size
            out.write(intToBytesLE(width));
            out.write(intToBytesLE(height));
            out.write(new byte[]{1, 0}); // planes
            out.write(new byte[]{24, 0}); // bpp
            out.write(new byte[24]); // rest zero
            // Pixel data (BGR)
            for (int y = height - 1; y >= 0; y--) {
                for (int x = 0; x < width; x++) {
                    int c = pixels[y * width + x];
                    out.write((c) & 0xFF); // B
                    out.write((c >> 8) & 0xFF); // G
                    out.write((c >> 16) & 0xFF); // R
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static byte[] intToBytesLE(int v) {
        return new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24)};
    }

    // Minimal static GIF encoder (single frame, no animation)
    private static boolean saveGif(Bitmap bmp, OutputStream out) {
        // Android does not provide a native GIF encoder, so this is a stub.
        // For now, save as PNG and rename to .gif (not a real GIF, but placeholder for demo).
        try {
            return bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            return false;
        }
    }

    private static void showToast(Context context, String msg) {
        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }
}

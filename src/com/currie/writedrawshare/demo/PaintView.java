package com.currie.writedrawshare.demo;

import java.io.File;
import java.io.FileOutputStream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.os.Environment;
import android.text.format.Time;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class PaintView extends View {
    public static File mediaStorageDir = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "WriteDrawShare");
    private boolean orientationSet = false;
    private SharedPreferences preferences;
    private Bitmap bitmap;
    private int penColor;
    private Canvas canvas;
    private Path path;
    private Paint paint;
    private float x;
    private float y;
    private static final float MIN_MOVE = 4;
    private final int OFF_WHITE = Color.argb(255, 255, 250, 250);
    private Display display;
    private Activity mainAcitivity;
    private int displayWidth;
    private int displayHeight;

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public PaintView(Context context) {
        super(context);
        this.mainAcitivity = (Activity) context;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        display = wm.getDefaultDisplay();
        Point size = new Point();
        try {
            display.getSize(size);
            displayWidth = size.x;
            displayHeight = size.y;
        } catch (java.lang.NoSuchMethodError ignore) {
            size.x = display.getWidth();
            size.y = display.getHeight();
        }
        preferences = context.getSharedPreferences("preferences", 0);
        penColor = preferences.getInt("penColor", Color.BLACK);

        bitmap = Bitmap.createBitmap(displayWidth, displayHeight, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        path = new Path();
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(10);
        paint.setColor(penColor);
        paint.setDither(true);
    }

    public void changePenColor(int penColor) {
        this.penColor = penColor;
        paint.setColor(this.penColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(OFF_WHITE);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        canvas.drawPath(path, paint);
        super.onDraw(canvas);
    }

    private void touch_start(float x, float y) {
        if (!orientationSet) {
            mainAcitivity
                    .setRequestedOrientation(mainAcitivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            orientationSet = true;
        }
        path.reset();
        path.moveTo(x, y);
        this.x = x;
        this.y = y;
    }

    private void touch_move(float x, float y) {
        float distanceX = Math.abs(this.x - x);
        float distanceY = Math.abs(this.y - y);

        if (distanceX >= MIN_MOVE || distanceY >= MIN_MOVE) {
            path.quadTo(this.x, this.y, (x + this.x) / 2, (y + this.y) / 2);
            this.x = x;
            this.y = y;
        }
    }

    private void touch_up() {
        path.lineTo(this.x, this.y);
        canvas.drawPath(path, paint);
        path.reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }

    public void clear() {
        mainAcitivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        orientationSet = false;
        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
        canvas.drawPaint(paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC));
        invalidate();
    }

    public int getPenColor() {
        return this.penColor;
    }

    public void setPenColor(int penColor) {
        this.penColor = penColor;
    }

    /**
     * Create a File for saving image
     */
    private File getOutputMediaFile() {
        // creating directories if needed
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        // Creating file
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        String dateTime = today.format2445();
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "write_draw_share" + dateTime + ".jpg");

        return mediaFile;
    }

    /**
     * saves the image on canvas as a jpeg and text vile
     * 
     * @return
     */
    public File savePicture() {
        setDrawingCacheEnabled(true);
        setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        File file = getOutputMediaFile();
        FileOutputStream ostream;
        Matrix rotation = new Matrix();
        rotation.preRotate(display.getRotation() / 90 * 90);
        Bitmap bitmap = Bitmap.createBitmap(getDrawingCache());
        destroyDrawingCache();
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotation, true);

        try {
            // saves the bitmap
            ostream = new FileOutputStream(file);
            rotatedBitmap.compress(CompressFormat.JPEG, 80, ostream);
            ostream.flush();
            ostream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    public void addImageToCanvas(Bitmap bitmap) {
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int imageWidth = bitmap.getWidth();
        int imageHeight = bitmap.getHeight();
        float ratio = Math.min((float)canvasWidth / (float)imageWidth, (float)canvasHeight/(float)imageHeight);
        int scaledWidth = Math.round(ratio * imageWidth);
        int scaledHeight = Math.round(ratio * imageHeight);
        int floatLeft = (canvasWidth - scaledWidth) / 2;
        int floatTop = (canvasHeight - scaledHeight) / 2;
        canvas.drawBitmap(Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false), floatLeft, floatTop, paint);
    }

}

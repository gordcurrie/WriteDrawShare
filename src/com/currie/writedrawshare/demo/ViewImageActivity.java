package com.currie.writedrawshare.demo;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class ViewImageActivity extends Activity implements OnGestureListener {
    private ImageView imageView;
    private int currentImage = 0;
    private GestureDetector detector;
    private File[] images;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);
        imageView = (ImageView) findViewById(R.id.imageView);
        // gets all the files in the AsciiView jpg directory
        images = PaintView.mediaStorageDir.listFiles();
        // exits if there isn't any
        if (images == null || images.length <= 0) {
            this.finish();
            return;
        }
        if (images.length % 3 == 0) {
            showUpgradeDialog();
        }
        // sets the image to the last picture taken
        bitmap = BitmapFactory.decodeFile(images[images.length - 1].getAbsolutePath());
        imageView.setImageBitmap(bitmap);
        // listener for fling to new image
        detector = new GestureDetector(this, this);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        // TODO Auto-generated method stub
        return false;
    }

    // listener for fling to new image
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1.getRawY() < e2.getRawY()) {
            increaseCurrentImage();
        } else {
            decreaseCurrentImage();
        }
        bitmap = BitmapFactory.decodeFile(images[currentImage].getAbsolutePath());
        imageView.setImageBitmap(bitmap);
        imageView.invalidate();
        return false;
    }

    /**
     * tracks pointer for current image in images array goes it loops from end
     * of array to start
     */
    private void increaseCurrentImage() {
        if (currentImage >= images.length - 1) {
            currentImage = 0;
        } else {
            currentImage++;
        }
    }

    /**
     * tracks pointer for current image in images array goes it loops from start
     * of array to end
     */
    private void decreaseCurrentImage() {
        if (currentImage < 1) {
            currentImage = images.length - 1;
        } else {
            currentImage--;
        }
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    /**
     * launches share intent with current image
     */
    public void shareImage(View view) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/jpeg");
        share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(images[currentImage]));
        startActivity(Intent.createChooser(share, "Share Image"));
    }

    /**
     * deletes current image from directory
     */
    public void deleteImage(View view) {
        new AlertDialog.Builder(this).setTitle("Are you sure you wish to delete this image?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        images[currentImage].delete();
                        increaseCurrentImage();
                        bitmap = BitmapFactory.decodeFile(images[currentImage].getAbsolutePath());
                        imageView.setImageBitmap(bitmap);
                        imageView.invalidate();
                    }
                }).setNegativeButton(android.R.string.no, null).show();
    }

    /**
     * AlertDialog to ask people to upgrade
     */
    public void showUpgradeDialog() {
        AlertDialog.Builder builder = new Builder(this);
        builder.setTitle("Thank you for choosing Write Draw Share.")
        .setMessage("If you find this app of value would please consider upgrading " 
                + "to Write Draw Share GS.\nThe features are the same but you would help me to "
                + "create more apps like this one.\nWould you like to upgrade now?")
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // try to open in play if installed
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri
                            .parse("market://details?id=com.currie.writedrawshare")));
                // falls back to web browser if play doesn't work.
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri
                            .parse("http://play.google.com/store/apps/details?id=com.currie.writedrawshare")));
                }
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        }).show();
    }
}

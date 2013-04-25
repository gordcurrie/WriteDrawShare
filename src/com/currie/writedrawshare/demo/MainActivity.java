package com.currie.writedrawshare.demo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE = 1;
    private static final int TAKE_PICTURE = 0;
    private PaintView paintView;
    private SharedPreferences preferences;
    private Bitmap bitmap;
    private String tempFilePath = PaintView.mediaStorageDir + "/temp.jpg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("preferences", 0);
        paintView = new PaintView(this);
        setContentView(paintView);
        if (!preferences.getBoolean("dontShow", false)) {
            displaySplash();
        }
    }

    private void displaySplash() {
        View checkBoxView = View.inflate(this, R.layout.start_dialog, null);
        final CheckBox dontDisplayCheckBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        dontDisplayCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("dontShow", dontDisplayCheckBox.isChecked());
                editor.commit();
            };
        });
        dontDisplayCheckBox.setText("Don't show again.");
        AlertDialog.Builder builder = new Builder(this);
        builder.setTitle("Thank you for choosing Write Draw Share.")
                .setView(checkBoxView)
                .setMessage(
                        "Start Drawing and have fun.\n\nPress the menu button for more options. \n\nWould you like to rate this app?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri
                                    .parse("market://details?id=com.currie.writedrawshare")));
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

    @Override
    protected void onStop() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("penColor", paintView.getPenColor());
        editor.commit();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.clear) {
            clearCanvas(paintView);
        } else if (itemId == R.id.color_picker_pen) {
            int initialColor = paintView.getPenColor();
            // initialColor is the initially-selected color to be shown in the
            // rectangle on the left of the arrow.
            // for example, 0xff000000 is black, 0xff0000ff is blue. Please be
            // aware of the initial 0xff which is the alpha.
            AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, initialColor, new OnAmbilWarnaListener() {
                @Override
                public void onOk(AmbilWarnaDialog dialog, int color) {
                    paintView.changePenColor(color);
                }

                @Override
                public void onCancel(AmbilWarnaDialog dialog) {
                    // cancel was selected by the user
                }
            });
            dialog.show();
        } else if (itemId == R.id.share) {
            paintView.savePicture();
            if (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).listFiles().length > 0) {
                startActivity(new Intent(this, ViewImageActivity.class));
            }
        } else if (itemId == R.id.save) {
            File image = paintView.savePicture();
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"
                    + image.getAbsolutePath())));
        } else if (itemId == R.id.caputre) {
            if (isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)) {
                File imageFile = new File(tempFilePath);
                Uri imageFileUri = Uri.fromFile(imageFile);
                Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageFileUri);
                startActivityForResult(i, TAKE_PICTURE);
            }
        } else if (itemId == R.id.upload) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_CODE);
        }
        return super.onOptionsItemSelected(item);
    }

    public void clearCanvas(View view) {
        paintView.clear();
    }

    public void shareImage(View view) {
        File image = paintView.savePicture();
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + image.getAbsolutePath())));
        if (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).listFiles().length > 0) {
            startActivity(new Intent(this, ViewImageActivity.class));
        }
    }

    private static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                InputStream stream = getContentResolver().openInputStream(data.getData());
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                paintView.addImageToCanvas(bitmap);
                stream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            if (resultCode == Activity.RESULT_OK) {
              if (bitmap != null) {
                bitmap.recycle();
              }
              Bitmap bitmap = BitmapFactory.decodeFile(tempFilePath);
              paintView.addImageToCanvas(bitmap);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}

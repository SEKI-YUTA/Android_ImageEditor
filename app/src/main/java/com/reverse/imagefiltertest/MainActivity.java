package com.reverse.imagefiltertest;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.reverse.imagefiltertest.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
//    https://www.simplifiedcoding.net/android-save-bitmap-to-gallery/
    private ActivityMainBinding binding;
    private boolean hasWritePermission = false;
    private MainActivity instance = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        instance = this;

        Log.d("current warm", binding.imgFilterView.getWarmth() + "");
        Log.d("current bright", binding.imgFilterView.getWarmth() + "");

        binding.slBri.setMax(200);
        binding.slBri.setMin(0);
        binding.slRotate.setMax(180);
        binding.slRotate.setMin(-180);
        binding.slWarm.setMax(200);
        binding.slWarm.setMin(0);

        binding.slWarm.setProgress(100);
        binding.slBri.setProgress(100);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            hasWritePermission = true;
        } else {
            requestPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        binding.slBri.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b) {
                    float newBri = (float) (i / 100.0);
                    binding.imgFilterView.setBrightness(newBri);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.slWarm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    float newWarm = (float) (i / 100.0);
                    binding.imgFilterView.setWarmth(newWarm);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.slRotate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b) {
                    binding.imgFilterView.setRotation(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.btnWrite.setOnClickListener((view) -> {
            Bitmap bitmap = getBitmap(binding.imgFilterView);
            bitmap = trim(bitmap);
            binding.imgPreview.setImageBitmap(bitmap);
            saveBitmap(bitmap);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private Context getContext() {
        return instance.getApplicationContext();
    }

    private Bitmap getBitmap(View view) {
        int wid = view.getWidth();
        int hei = view.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(wid, hei, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private void saveBitmap(Bitmap bitmap) {
        String filename = System.currentTimeMillis() + ".jpg";
        final Context ctx = getContext();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                OutputStream ous = null;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // MediaStore経由で保存
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

                    Uri imageUri = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                    try {
                        ous = ctx.getContentResolver().openOutputStream(imageUri);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // 直接パスを取得して保存
                    File pictureDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    File imageFile = new File(pictureDir, filename);
                    try {
                        ous = new FileOutputStream(imageFile);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

                if(ous != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ous);
                    Log.d("output", "write complete");
                    Toast.makeText(ctx, "画像を保存しました", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // https://stackoverflow.com/questions/16895945/is-it-possible-to-remove-transparent-pixels-from-bitmap-in-android
    static Bitmap trim(Bitmap source) {
        int firstX = 0, firstY = 0;
        int lastX = source.getWidth();
        int lastY = source.getHeight();
        int[] pixels = new int[source.getWidth() * source.getHeight()];
        source.getPixels(pixels, 0, source.getWidth(), 0, 0, source.getWidth(), source.getHeight());
        loop:
        for (int x = 0; x < source.getWidth(); x++) {
            for (int y = 0; y < source.getHeight(); y++) {
                if (pixels[x + (y * source.getWidth())] != Color.TRANSPARENT) {
                    firstX = x;
                    break loop;
                }
            }
        }
        loop:
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = firstX; x < source.getWidth(); x++) {
                if (pixels[x + (y * source.getWidth())] != Color.TRANSPARENT) {
                    firstY = y;
                    break loop;
                }
            }
        }
        loop:
        for (int x = source.getWidth() - 1; x >= firstX; x--) {
            for (int y = source.getHeight() - 1; y >= firstY; y--) {
                if (pixels[x + (y * source.getWidth())] != Color.TRANSPARENT) {
                    lastX = x;
                    break loop;
                }
            }
        }
        loop:
        for (int y = source.getHeight() - 1; y >= firstY; y--) {
            for (int x = source.getWidth() - 1; x >= firstX; x--) {
                if (pixels[x + (y * source.getWidth())] != Color.TRANSPARENT) {
                    lastY = y;
                    break loop;
                }
            }
        }
        return Bitmap.createBitmap(source, firstX, firstY, lastX - firstX, lastY - firstY);
    }

    ActivityResultLauncher<String> requestPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            hasWritePermission = true;
        }
    });
}
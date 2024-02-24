package com.example.takoyaki;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.checkerframework.checker.guieffect.qual.UI;
import org.checkerframework.checker.units.qual.C;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    ImageButton capture, toggleFlash, flipCamera;
    Button rankingButton;
    EditText xEdit, yEdit;
    File file;
    private PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;

    int numOfRow;
    int numOfColumn;
    ProcessCameraProvider cameraProvider;

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (result) {
                startCamera(cameraFacing);
            }
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.cameraPreview);
        capture = findViewById(R.id.capture);
        toggleFlash = findViewById(R.id.toggleFlash);
        flipCamera = findViewById(R.id.flipCamera);
        rankingButton = findViewById(R.id.rankingButton);
        xEdit = findViewById(R.id.x_edit);
        yEdit = findViewById(R.id.y_edit);

        numOfRow = getIntent().getIntExtra("ROW", 1);
        numOfColumn = getIntent().getIntExtra("COLUMN", 1);

        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startCamera(cameraFacing);
        }
    }

    public void startCamera(int cameraFacing) {
        int aspectRatio = aspectRatio(previewView.getWidth(), previewView.getHeight());
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);

        listenableFuture.addListener(() -> {
            try {
                cameraProvider = (ProcessCameraProvider) listenableFuture.get();
                Preview preview = new Preview.Builder().setTargetAspectRatio(aspectRatio).build();
                ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();
                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                capture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        }
                        takePicture(imageCapture);
                    }
                });

                toggleFlash.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setFlashIcon(camera);
                    }
                });
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                // PreviewView の上に円を描画するための View を作成
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                int cellSize;
                if (previewView.getWidth() / numOfRow < previewView.getHeight() / numOfColumn) {
                    cellSize = (previewView.getWidth() - 100) / numOfRow;
                } else {
                    cellSize = (previewView.getHeight() - 100) / numOfColumn;
                }
                int marginX = (previewView.getWidth() - cellSize * numOfRow) / 2;
                int marginY = (previewView.getHeight() - cellSize * numOfColumn) / 2;
                Circle circle = new Circle(this, numOfRow, numOfColumn, marginX, marginY, cellSize);

                addContentView(circle, layoutParams);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    public void takePicture(ImageCapture imageCapture) {
        file = new File(getExternalFilesDir(null), System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CameraActivity.this, "Image saved at: " + file.getPath(), Toast.LENGTH_SHORT).show();
                    }
                });
                Intent intent = new Intent(CameraActivity.this, PreviewActivity.class);
                intent.putExtra("FILE", file.getPath());
                intent.putExtra("ROW", numOfRow);
                intent.putExtra("COLUMN", numOfColumn);
                startActivity(intent);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CameraActivity.this, "Failed to save: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void setFlashIcon(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            if (camera.getCameraInfo().getTorchState().getValue() == 0) {
                camera.getCameraControl().enableTorch(true);
                toggleFlash.setImageResource(R.drawable.baseline_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFlash.setImageResource(R.drawable.baseline_flash_on_24);
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CameraActivity.this, "Flash is not available currently", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * アスペクト比の設定
     *
     * @param width
     * @param height
     * @return
     */
    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    /**
     * カメラの切り替え
     *
     * @param view
     */
    public void flip(View view) {
        if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
            cameraFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            cameraFacing = CameraSelector.LENS_FACING_BACK;
        }
        startCamera(cameraFacing);
    }

    /**
     * ランキング画面へ移動
     *
     * @param view
     */
    public void moveRanking(View view) {
        Intent intent = new Intent(CameraActivity.this, RankingActivity.class);
        startActivity(intent);
    }

    public void sync(View view) {
        numOfRow = Integer.parseInt(xEdit.getText().toString());
        numOfColumn = Integer.parseInt(yEdit.getText().toString());
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("ROW", numOfRow);
        intent.putExtra("COLUMN", numOfColumn);
        startActivity(intent);
        finish();
    }
}

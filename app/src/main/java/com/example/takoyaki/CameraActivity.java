package com.example.takoyaki;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;

import android.Manifest;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.Camera;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * 撮影画面Activityクラス．
 */
public class CameraActivity extends AppCompatActivity {
    private static final int DEFAULT_MARGIN_RATIO = 10;// マージンの大きさに対する画面の大きさの比率
    private ImageButton captureButton;
    private ImageButton toggleFlashButton;
    private EditText rowInput;// 入力された行数
    private EditText columnInput;// 入力された列数
    private File capturedImageFile;
    private PreviewView previewView;
    private int currentCameraFacing = CameraSelector.LENS_FACING_BACK;// 使用するカメラ．
    private int numOfRows;// 撮影するたこ焼きの行数．
    private int numOfColumns;// 撮影するたこ焼きの列数．
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (result) {
            startCamera(currentCameraFacing);
        }
    });

    /**
     * Activity生成時の処理．
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initUIComponents();

        numOfRows = getIntent().getIntExtra("ROW", 1);
        numOfColumns = getIntent().getIntExtra("COLUMN", 1);

        if (hasCameraPermissions()) {
            startCamera(currentCameraFacing);
        } else {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * UIコンポーネントの初期化．
     */
    private void initUIComponents() {
        previewView = findViewById(R.id.cameraPreview);
        captureButton = findViewById(R.id.capture);
        toggleFlashButton = findViewById(R.id.toggleFlash);
        rowInput = findViewById(R.id.x_edit);
        columnInput = findViewById(R.id.y_edit);
    }

    /**
     * カメラとストレージのパーミッションが許可されているかを確認するメソッド．
     *
     * @return すべての必要なパーミッションが許可されている場合はtrue，
     *         いずれかのパーミッションが許可されていない場合はfalseを返す．
     */
    private boolean hasCameraPermissions() {
        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean storagePermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return cameraPermissionGranted && storagePermissionGranted;
    }

    /**
     * カメラ起動メソッド．
     *
     * @param cameraFacing 使用するカメラ．
     */
    private void startCamera(int cameraFacing) {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                if (cameraProvider != null) {
                    configureCamera(cameraFacing);

                    captureButton.setOnClickListener(view -> takePicture(imageCapture));
                    toggleFlashButton.setOnClickListener(view -> setFlashIcon(camera));

                    drawCirclesOnPreview();
                } else {
                    showToast("カメラの初期化に失敗しました。");
                }
            } catch (ExecutionException | InterruptedException e) {
                showToast("カメラの起動中にエラーが発生しました。");
                Log.e("CameraActivity", "カメラの起動エラー: ", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * カメラの設定を行うメソッド．
     *
     * @param cameraFacing 使用するカメラ．
     */
    private void configureCamera(int cameraFacing) {
        // 解像度の取得
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Size resolution = new Size(metrics.widthPixels, metrics.heightPixels);

        Preview preview = new Preview.Builder()
                .setTargetResolution(resolution)
                .build();

        imageCapture = new ImageCapture.Builder()
                .setTargetResolution(resolution)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build();

        // 既存のバインディングを解除して新しいバインディングを設定
        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
    }

    /**
     * 撮影メソッド．
     *
     * @param imageCapture 写真をキャプチャするためのImageCaptureオブジェクト．
     *                     このオブジェクトは、カメラの静止画像を撮影し，
     *                     保存するために使用する．．
     */
    private void takePicture(ImageCapture imageCapture) {
        capturedImageFile = new File(getExternalFilesDir(null), System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(capturedImageFile).build();
        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                moveToPreviewActivity();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                showToast("保存に失敗しました: " + exception.getMessage());
            }
        });
    }

    /**
     * カメラのフラッシュ状態を切り替え、フラッシュアイコンを更新するメソッド．
     *
     * @param camera 操作対象のCameraオブジェクト．
     *               このオブジェクトを使用してフラッシュのオン・オフを制御する．．
     */
    private void setFlashIcon(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            Integer torchState = camera.getCameraInfo().getTorchState().getValue();
            if (torchState != null && torchState == 0) {
                camera.getCameraControl().enableTorch(true);
                toggleFlashButton.setImageResource(R.drawable.baseline_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFlashButton.setImageResource(R.drawable.baseline_flash_on_24);
            }
        } else {
            showToast("フラッシュは現在使用できません。");
        }
    }

    /**
     * プレビュー画面に円を描画するメソッド．
     */
    private void drawCirclesOnPreview() {
        int defaultMargin = previewView.getWidth() / DEFAULT_MARGIN_RATIO;
        int cellSize = calculateCellSize(defaultMargin);
        int marginX = (previewView.getWidth() - cellSize * numOfColumns) / 2;
        int marginY = (previewView.getHeight() - cellSize * numOfRows) / 2;

        Circle circle = new Circle(this, numOfRows, numOfColumns, marginX, marginY, cellSize);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        addContentView(circle, layoutParams);
    }

    /**
     * セルサイズ計算メソッド．
     *
     * @param defaultMargin デフォルトのマージンサイズ．
     * @return 表示する枠の大きさ．
     */
    private int calculateCellSize(int defaultMargin) {
        if (previewView.getWidth() / numOfColumns < previewView.getHeight() / numOfRows) {
            return (previewView.getWidth() - defaultMargin * 2) / numOfColumns;
        } else {
            return (previewView.getHeight() - defaultMargin * 2) / numOfRows;
        }
    }

    /**
     * トーストメッセージを表示するメソッド．
     *
     * @param message 表示するメッセージ．
     */
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    /**
     * カメラの切り替えメソッド．
     *
     * @param view カメラ切替アイコン．
     */
    public void flip(View view) {
        if (currentCameraFacing == CameraSelector.LENS_FACING_BACK) {
            currentCameraFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            currentCameraFacing = CameraSelector.LENS_FACING_BACK;
        }
        startCamera(currentCameraFacing);
    }

    /**
     * 行数と列数の同期メソッド．
     * ユーザーが入力した行数と列数を更新し，
     * 新しい値を持ったCameraActivityを再起動する．
     *
     * @param view 変更ボタン．
     */
    public void sync(View view) {
        try {
            numOfRows = Integer.parseInt(rowInput.getText().toString());
            numOfColumns = Integer.parseInt(columnInput.getText().toString());
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra("ROW", numOfRows);
            intent.putExtra("COLUMN", numOfColumns);
            startActivity(intent);
            finish();
        } catch (NumberFormatException e) {
            showToast("行数と列数の入力が必要です");
        }
    }

    /**
     * プレビュー画面への遷移メソッド．
     */
    private void moveToPreviewActivity() {
        Intent intent = new Intent(this, PreviewActivity.class);
        intent.putExtra("FILE", capturedImageFile.getPath());
        intent.putExtra("ROW", numOfRows);
        intent.putExtra("COLUMN", numOfColumns);
        startActivity(intent);
    }

    /**
     * ランキング画面へ遷移メソッド．
     *
     * @param view ランキングへの移動ボタン．．
     */
    public void moveToRanking(View view) {
        Intent intent = new Intent(CameraActivity.this, RankingActivity.class);
        startActivity(intent);
    }

    /**
     * ヘルプ画面への遷移メソッド．
     *
     * @param view ヘルプボタン．
     */
    public void moveToHelp(View view) {
        Intent intent = new Intent(CameraActivity.this, HelpActivity.class);
        startActivity(intent);
    }

}

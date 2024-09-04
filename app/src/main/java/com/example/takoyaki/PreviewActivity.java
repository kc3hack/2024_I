package com.example.takoyaki;

import java.io.File;
import java.io.IOException;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.MenuItem;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import com.example.takoyaki.ml.ModelUnquant;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * 結果表示Activityクラス．
 */
public class PreviewActivity extends AppCompatActivity {
    private static final int IMAGE_SIZE = 224;// モデルで読み込める最大のサイズ
    private static final int CONFIEDENCES_SIZE = 5;// 類似度を求める分類の数
    private static final int MARGIN_RATIO = 10;// マージンの大きさに対する画面の大きさの比率
    private static final String RANKING_REF = "ranking";// Firebaseの参照パス
    private ImageView imageView;// 撮影した画像のView

    /**
     * Activity生成時の処理．
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        initUIComponents();
        // 撮影した写真のパス
        String filePath = (String) getIntent().getSerializableExtra("FILE");

        // ファイルが存在すれば得点を計算し，表示．
        if (filePath != null) {
            Bitmap bitmap = createBitmap(filePath);
            imageView.setImageBitmap(bitmap);

            double score = calculateAverageScore(bitmap);
            runOnUiThread(() -> displayScore(score));
            deleteImageFile(filePath);
        }
    }

    /**
     * 引数のパスのファイルを削除するメソッド．
     *
     * @param filePath 撮影した画像のパス．
     */
    private void deleteImageFile(String filePath) {
        File file = new File(filePath);
        if (!file.delete()) {
            Log.e("PreviewActivity", "Failed to delete the file");
        }
    }

    /**
     * UIの設定メソッド．
     */
    private void initUIComponents() {
        imageView = findViewById(R.id.imageView3);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Button postYBtn = findViewById(R.id.postYBtn);
        postYBtn.setOnClickListener(new ButtonClickListener());

        Button postNBtn = findViewById(R.id.postNBtn);
        postNBtn.setOnClickListener(v -> finish());
    }

    /**
     * ファイルパスの画像からbitmapを生成するメソッド．．
     * 多くのスマホのカメラは画像を横向きに保存するのでデコード後にbitmapを回転する．
     *
     * @param filePath 撮影したたこ焼きの画像パス．
     * @return 撮影した画像データをBitmapにして返す．
     */
    private Bitmap createBitmap(String filePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }

    /**
     * 撮影した写真内のたこ焼きの平均点を計算するメソッド．
     *
     * @param bitmap 撮影した画像から作成されたbitMap
     * @return 計算した得点をdouble型で返す．
     */
    private double calculateAverageScore(Bitmap bitmap) {
        int numOfRow = getIntent().getIntExtra("ROW", 1);
        int numOfColumn = getIntent().getIntExtra("COLUMN", 1);
        int defaultMargin = bitmap.getWidth() / MARGIN_RATIO;
        int cellSize = calculateCellSize(bitmap, numOfRow, numOfColumn, defaultMargin);
        int marginX = (bitmap.getWidth() - cellSize * numOfColumn) / 2;
        int marginY = (bitmap.getHeight() - cellSize * numOfRow) / 2;
        double totalScore = 0;

        for (int i = 0; i < numOfRow; i++) {
            for (int j = 0; j < numOfColumn; j++) {
                Bitmap trimmedBitmap = Bitmap.createBitmap(bitmap, marginX + cellSize * j, marginY + cellSize * i, cellSize, cellSize);
                // モデルで読み込めるようにスケール
                trimmedBitmap = Bitmap.createScaledBitmap(trimmedBitmap, IMAGE_SIZE, IMAGE_SIZE, false);
                float[] confidences = classifyImage(trimmedBitmap);
                totalScore += calculateImageScore(confidences);

                trimmedBitmap.recycle();
            }
        }

        double averageScore = totalScore / (numOfRow * numOfColumn);
        // 小数点以下第3位以降を切り捨てて返す．
        return ((double) Math.round(averageScore * 100)) / 100;
    }

    /**
     * 画面内に指定した個数の枠線を描画できる
     * たこ焼き1つの枠の大きさを設定するメソッド．
     *
     * @param bitmap        撮影した画像を加工したbitmap．
     * @param numOfRow      たこ焼きの枠の行数．
     * @param numOfColumn   たこ焼きの枠の列数．
     * @param defaultMargin デフォルトのマージンサイズ．
     * @return 一つのたこ焼きの枠の大きさ．
     */
    private int calculateCellSize(Bitmap bitmap, int numOfRow, int numOfColumn, int defaultMargin) {
        if (bitmap.getWidth() / numOfColumn < bitmap.getHeight() / numOfRow) {
            return (bitmap.getWidth() - defaultMargin * 2) / numOfColumn;
        } else {
            return (bitmap.getHeight() - defaultMargin * 2) / numOfRow;
        }
    }

    /**
     * 学習データから各分類との類似度を求めるメソッド．
     * 配列のindex0~4の値はそれぞれ
     * "raw", "hraw", "hburnt", "perfect", "burnt"を表す．
     *
     * @param image 撮影した写真(Bitmap形式).
     * @return 各分類との類似度を保持した配列．．
     */
    public float[] classifyImage(Bitmap image) {
        try {
            ModelUnquant model = ModelUnquant.newInstance(getApplicationContext());
            TensorBuffer inputFeature0 = createInputBuffer(image);
            // Runs model inference and gets result.
            ModelUnquant.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            model.close();
            return outputFeature0.getFloatArray();
        } catch (IOException e) {
            Log.e("PreviewActivity", "Error during image classification", e);
            return new float[CONFIEDENCES_SIZE];
        }
    }

    /**
     * confidenceの値をbuntは0，rawは0.5，hrawとhburntは0.7，perfectは1倍する．
     * その値の中で最も高い値を100倍した値を点数とする(満点100)．
     *
     * @param confidences 各分類との類似度を保持する配列．
     * @return たこ焼き1つの点数．
     */
    private float calculateImageScore(float[] confidences) {
        float score = Math.max(confidences[0] * 50, confidences[1] * 70);
        score = Math.max(score, confidences[2] * 70);
        score = Math.max(score, confidences[3] * 100);
        return score;
    }

    /**
     * BitmapからTensorFlow Liteモデルに入力するための
     * TensorBufferを作成するメソッド．
     *
     * @param image たこ焼き1つ分のbitmap．
     * @return bitmapから作成したTensorBuffer．
     */
    private TensorBuffer createInputBuffer(Bitmap image) {
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, IMAGE_SIZE, IMAGE_SIZE, 3}, DataType.FLOAT32);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[IMAGE_SIZE * IMAGE_SIZE];
        image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

        int pixel = 0;
        for (int i = 0; i < IMAGE_SIZE; i++) {
            for (int j = 0; j < IMAGE_SIZE; j++) {
                int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }

        inputFeature0.loadBuffer(byteBuffer);
        return inputFeature0;
    }

    /**
     * 得点表示メソッド．
     * 得点に合わせてメッセージと画像を表示する．．
     *
     * @param averageScore 撮影した写真のたこ焼きの平均点．
     */
    private void displayScore(double averageScore) {
        TextView previewText = findViewById(R.id.previewPoint);
        previewText.setText(String.valueOf(averageScore));

        TextView takoyakiText = findViewById(R.id.takoyakiComment);
        ImageView takoyakiImage = findViewById(R.id.imageView7);
        if (averageScore >= 90) {
            takoyakiText.setText("最高だぽよ〜");
            takoyakiImage.setImageResource(R.drawable._00);
        } else if (averageScore >= 70) {
            takoyakiText.setText("ええ感じぽよ〜");
            takoyakiImage.setImageResource(R.drawable._00);
        } else if (averageScore >= 30) {
            takoyakiText.setText("普通だぽよ〜");
            takoyakiImage.setImageResource(R.drawable.takoyaki);
        } else {
            takoyakiText.setText("残念だぽよ〜");
            takoyakiImage.setImageResource(R.drawable.takoyaki);
        }
    }

    /**
     * オプションメニューの項目が選択された時の処理メソッド．
     *
     * @param menuButton 選択されたメニュー項目．
     * @return homeボタンが選択されてアクティビティが終了された場合はtrueを返し、
     * それ以外の場合はスーパークラスのメソッドの結果を返す．
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem menuButton) {
        int buttonId = menuButton.getItemId();
        if (buttonId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuButton);
    }

    /**
     * ダイアログのボタンリスナークラス．
     */
    private class ButtonClickListener implements View.OnClickListener {

        /**
         * ボタンクリック時の処理．
         *
         * @param view クリックされたビュー
         */
        @Override
        public void onClick(View view) {
            showNameRegistrationDialog("ランキングに登録しよう！");
        }

        /**
         * 名前登録ダイアログを表示し、入力された名前をFirebaseに登録するメソッド．
         */
        private void showNameRegistrationDialog(String message) {
            EditText nameEditText = new EditText(PreviewActivity.this);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PreviewActivity.this);
            dialogBuilder.setTitle("名前登録").setMessage(message).setView(nameEditText).setPositiveButton("OK", (dialog, which) -> registerScore(nameEditText.getText().toString())).setNegativeButton("キャンセル", (dialog, which) -> dialog.dismiss());
            dialogBuilder.create().show();
        }

        /**
         * 入力された名前とスコアをFirebaseに登録し，ランキング画面に遷移するメソッド．
         *
         * @param playerName 登録するプレイヤーの名前．
         */
        private void registerScore(String playerName) {
            double score = getScore();
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ref = database.getReference(RANKING_REF).child(playerName);

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                /**
                 * データが変更された際に呼び出されるメソッド．
                 * 指定されたキーが存在しない場合はデータを設定し，存在する場合は警告を表示する．
                 *
                 * @param dataSnapshot The current data at the location
                 */
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        setData(ref, score);
                    } else {
                        showNameRegistrationDialog("すでに使用されている名前です");
                    }
                }

                /**
                 * データの取得がキャンセルされた際に呼び出されるメソッド．
                 * データの取得が失敗した場合のエラーハンドリングを行う．
                 *
                 * @param databaseError A description of the error that occurred
                 */
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("FirebaseData", "データの取得に失敗しました: " + databaseError.getMessage());
                }
            });

        }

        /**
         * 得点をデータベースに格納するメソッド．
         *
         * @param ref   参照するデータベースのパス．
         * @param score 得点．
         */
        private void setData(DatabaseReference ref, double score) {
            ref.setValue(score).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Intent intent = new Intent(PreviewActivity.this, RankingActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Log.e("PreviewActivity", "Failed to register score", task.getException());
                }
            });
        }

        /**
         * 現在の得点を取得するメソッド.
         *
         * @return 得点．
         */
        private double getScore() {
            TextView previewText = findViewById(R.id.previewPoint);
            String scoreString = previewText.getText().toString().replaceAll("点", "");
            return Double.parseDouble(scoreString);
        }

    }

}

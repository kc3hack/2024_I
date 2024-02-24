package com.example.takoyaki;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.takoyaki.ml.ModelUnquant;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Objects;

public class PreviewActivity extends AppCompatActivity {

    private double averageScore;// 表示される得点
    private String filePath;// 撮影した写真のパス
    private ImageView imageView;
    private int imageSize = 224;// modelで読み込める最大のサイズ

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        //「投稿しない」でmainに戻る
        findViewById(R.id.postNBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //サブ画面を終了する（メイン画面に戻る）
                finish();
            }
        });

        imageView = findViewById(R.id.imageView3);
        //アクションバーのオブジェクトを取得
        ActionBar actionBar = getSupportActionBar();
        //アクションバーに「戻るボタン」を追加
        Objects.requireNonNull(actionBar).setDisplayHomeAsUpEnabled(true);

        //ダイアログのボタンリスナ
        Button button = findViewById(R.id.postYBtn);
        button.setOnClickListener(new ButtonClickListener());

        filePath = (String) getIntent().getSerializableExtra("FILE");
        if (filePath != null) {
            System.out.println(filePath);
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);

            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap flippedBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            imageView.setImageBitmap(flippedBmp);

            int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);

            bitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false);

            int numOfRow = getIntent().getIntExtra("ROW", 1);
            int numOfColumn = getIntent().getIntExtra("COLUMN", 1);
            int marginX = flippedBmp.getWidth() / 20;
            int cellSize = (flippedBmp.getWidth() - marginX * 2) / numOfRow;
            int marginY = (flippedBmp.getHeight() - cellSize * numOfColumn) / 2;

            ArrayList<Double> cellScores = new ArrayList<>();
            for (int i = 0; i < numOfColumn; i++) {

                for (int j = 0; j < numOfRow; j++) {
                    Bitmap trimedBitmap = Bitmap.createBitmap(flippedBmp, marginX + cellSize * j, marginY + cellSize * i, cellSize, cellSize);
                    trimedBitmap = Bitmap.createScaledBitmap(trimedBitmap, imageSize, imageSize, false);// bitmapのサイズ変更
                    cellScores.add(classifyImage(trimedBitmap));

                    // 切り取りが成功しているか確認するためのView
//                    ImageView trimedImageView = findViewById(R.id.trimedImage);
//                    trimedImageView.setImageBitmap(trimedBitmap);
                }
            }
            System.out.println(cellScores);// 各セルの得点

            double totalScore = 0;
            for (double score : cellScores) {
                totalScore += score;
            }
            averageScore = totalScore / cellScores.size();
            averageScore = ((double) Math.round(averageScore * 100)) / 100;// 小数点第2位以下切り捨て

            TextView previewText = findViewById(R.id.previewPoint);
//            previewText.setText(String.valueOf(classifyImage(bitmap)) + "点");// 画像全体の得点
            previewText.setText(String.valueOf(averageScore));
        } else {
            System.out.println("NoFile");
        }

    }

    //ダイアログ
    private class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            EditText editText = new EditText(PreviewActivity.this);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PreviewActivity.this);
            dialogBuilder.setTitle("名前登録")
                    .setMessage("ランキングに登録しよう！")
                    .setView(editText)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // OKボタンがクリックされたときの処理
                            TextView previewText = findViewById(R.id.previewPoint);
                            String scoreString = previewText.getText().toString();
                            String replaceString = scoreString.replaceAll("点", "");
                            Double score = Double.valueOf(replaceString);
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference ref = database.getReference("ranking").child(editText.getText().toString());
                            ref.setValue(score);

                            Intent intent = new Intent(PreviewActivity.this, RankingActivity.class);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // キャンセルボタンがクリックされたときの処理
                        }
                    });

            AlertDialog dialog = dialogBuilder.create(); // AlertDialogのインスタンスを生成
            dialog.show();
//            //ダイアログフラグメントのオブジェクトを生成
//            InputNameDialogFragment dialogFragment = new InputNameDialogFragment();
//            //ダイアログの表示
////            dialogFragment.show(getSupportFragmentManager(), "SampleDialogFragment");

        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem menuButton) {
        //戻り値の変数を定義。trueを初期値として代入
        boolean result = true;
        //選択されたメニューボタンのIDを取得(今回は戻るボタンのみのため条件分岐不要だが、追加したときを想定し記載)
        int buttonId = menuButton.getItemId();
        //IDで条件分岐(戻るボタンが押されたときの命令以外省略可)
        switch (buttonId) {
            //戻るボタンが押されたとき
            case android.R.id.home:
                //画面を終了させる
                finish();
                break;
            //それ以外の時
            default:
                result = super.onOptionsItemSelected(menuButton);
                break;
        }
        return result;
    }

    /**
     * 学習データから点数を返すメソッド
     *
     * @param image
     */
    public double classifyImage(Bitmap image) {
        try {
            ModelUnquant model = ModelUnquant.newInstance(getApplicationContext());// モデル生成
            // bitmapの情報を格納するbuffer.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);// 各色4byte
            byteBuffer.order(ByteOrder.nativeOrder());// 実行環境に応じてソート

            // imageの224*224pixelの情報を格納する配列
            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

            // bytebufferにRGBの情報を格納
            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            ModelUnquant.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();


            float[] confidences = outputFeature0.getFloatArray();
            String[] classes = {"raw", "hraw", "hburnt", "perfect", "burnt"};
            /*
             * 得点の計算
             * confidenceの値をbuntは0，rawは0.5，hrawとhburntは0.7，perfectは1倍する．
             * その値の中で最も高い値を100倍した値を点数とする．(満点100)
             */
            float score;
            if (confidences[0] * 50 > confidences[1] * 70) {
                score = confidences[0] * 50;
            } else {
                score = confidences[1] * 70;
            }
            if (confidences[2] * 70 > score) {
                score = confidences[2] * 70;
            }
            if (confidences[3] * 100 > score) {
                score = confidences[3] * 100;
            }
            // model実行結果をログに表示
            for (float confidence : confidences) {
                System.out.println(confidence);
            }

            double roundedScore;
            roundedScore = ((double) Math.round(score * 100)) / 100;

            // Releases model resources if no longer used.
            model.close();
            return roundedScore;
        } catch (IOException e) {
            return 0;
        }
    }
}
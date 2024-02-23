package com.example.takoyaki;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.Objects;

public class preview extends AppCompatActivity {

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

        //アクションバーのオブジェクトを取得
        ActionBar actionBar = getSupportActionBar();
//アクションバーに「戻るボタン」を追加
        Objects.requireNonNull(actionBar).setDisplayHomeAsUpEnabled(true);

        //ダイアログのボタンリスナ
        Button button = findViewById(R.id.postYBtn);
        button.setOnClickListener(new preview.ButtonClickListener());



    }

    //ダイアログ
    private class ButtonClickListener implements View.OnClickListener{
        @Override
        public void onClick(View view){
            //ダイアログフラグメントのオブジェクトを生成
            inputNameDialogFragment dialogFragment = new inputNameDialogFragment();
            //ダイアログの表示
            dialogFragment.show(getSupportFragmentManager(),"SampleDialogFragment");
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem menuButton){
        //戻り値の変数を定義。trueを初期値として代入
        boolean result = true;
        //選択されたメニューボタンのIDを取得(今回は戻るボタンのみのため条件分岐不要だが、追加したときを想定し記載)
        int buttonId = menuButton.getItemId();
        //IDで条件分岐(戻るボタンが押されたときの命令以外省略可)
        switch(buttonId){
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
}
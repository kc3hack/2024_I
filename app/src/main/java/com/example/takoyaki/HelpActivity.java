package com.example.takoyaki;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

/**
 * ヘルプページActivityクラス．
 */
public class HelpActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_help_view);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
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

}

package com.example.takoyaki;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

/**
 * ランキングActivityクラス．
 */
public class RankingActivity extends AppCompatActivity {
    private static final String RANKING_REF = "ranking";// Firebaseの参照パス

    /**
     * 画面生成時メソッド．
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference(RANKING_REF);
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            /**
             * firebaseからrankingのrefを取得し、listviewで名前とスコアをランキングとして並べる．
             *
             * @param dataSnapshot The current data at the location
             */
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                HashMap<String, Double> rankingMap = new HashMap<>();
                HashMap<Double, String> reverseKeyAndValueRankingMap = new HashMap<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String key = snapshot.getKey();
                    Object value = snapshot.getValue();
                    if (key != null && value != null) {
                        rankingMap.put(key, Double.valueOf(value.toString()));
                        reverseKeyAndValueRankingMap.put(Double.valueOf(value.toString()), key);
                    }
                }

                List<Double> sortedValues = new ArrayList<>(rankingMap.values());
                sortedValues.sort(Collections.reverseOrder());

                ArrayList<Map<String, String>> listData = new ArrayList<>();
                int i = 1;
                for (Double value : sortedValues) {
                    Map<String, String> item = new HashMap<>();
                    item.put("rank", i + "位: " + reverseKeyAndValueRankingMap.get(value));
                    item.put("score", value.toString());
                    listData.add(item);
                    i++;
                }

                ListView listView = findViewById(R.id.list);
                listView.setAdapter(new SimpleAdapter(RankingActivity.this, listData, android.R.layout.simple_list_item_2, new String[]{"rank", "score"}, new int[]{android.R.id.text1, android.R.id.text2}));
            }

            /**
             *  Firebase データ取得のリスナーで、データの取得に失敗した場合に呼び出されるコールバックメソッド．
             *
             * @param databaseError A description of the error that occurred.
             */
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(RankingActivity.this, "ランキングの取得に失敗しました。", Toast.LENGTH_SHORT).show();
                Log.e("FirebaseData", "データの取得に失敗しました: " + databaseError.getMessage());
            }
        });

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

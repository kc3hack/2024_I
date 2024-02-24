package com.example.takoyaki;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;


import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RankingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);
        //rankingのrefを読み込む
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("ranking");
        // データの読み込みをリッスン
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {

            /**
             *firebaseからrankingのrefを取得し、listviewで名前とスコアをランキングとして並べる
             * @param dataSnapshot 受け取ったデータ
             */
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //データをHashMapに入れる
                HashMap<String, Double> rankingMap = new HashMap<>();
                HashMap<Double, String> reverseKeyAndValueRankingMap = new HashMap<>();

                // データを各々処理する
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String key = snapshot.getKey();
                    Object value = snapshot.getValue();
                    if (key != null && value != null) {
                        rankingMap.put(key, Double.valueOf(value.toString()));
                        reverseKeyAndValueRankingMap.put(Double.valueOf(value.toString()), key);
//                        Log.d("FirebaseData", "Key: " + key + ", Value: " + value);
                    }
                }

                //valueによってsortされたListを生成
                List<Double> sortedValues = new ArrayList<>(rankingMap.values());
                Collections.sort(sortedValues, Collections.reverseOrder());

                //listviewで表示するための準備
                ArrayList<Map<String, String>> listData = new ArrayList<>();
                int i = 1;
                for(Double value: sortedValues){
                    Map<String, String> item = new HashMap<>();
                    item.put("rank", i + "位: " + reverseKeyAndValueRankingMap.get(value));
                    item.put("score", value.toString());
                    listData.add(item);
                    i++;
                }

                //listviewで表示
                ListView listView = findViewById(R.id.list);
                listView.setAdapter(new SimpleAdapter(
                        RankingActivity.this,
                        listData,
                        android.R.layout.simple_list_item_2,
                        new String[] {"rank", "score"},
                        new int[] {android.R.id.text1, android.R.id.text2}
                ));
            }

            /**
             * データの取得に失敗したときの処理
             * @param databaseError
             */
            @Override
            public void onCancelled(DatabaseError databaseError) {
//                Log.e("FirebaseData", "データの取得に失敗しました: " + databaseError.getMessage());
            }
        });
        //アクションバーのオブジェクトを取得
        ActionBar actionBar = getSupportActionBar();
        //アクションバーに「戻るボタン」を追加
        Objects.requireNonNull(actionBar).setDisplayHomeAsUpEnabled(true);
    }
}
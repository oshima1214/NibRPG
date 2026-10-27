package com.example.nibrpg;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** 攻略失敗画面（負けた先生によって中身が変わる） */
public class GameOverActivity extends AppCompatActivity {

    // 前の画面から「何人目の先生に負けたか」を受け取るときの名前
    public static final String EXTRA_TEACHER = "teacher";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game_over);
        // 画面のふち（ステータスバーなど）に文字がかぶらないようにする
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // 何人目の先生に負けたかを受け取る（1〜4。わからないときは4＝校長）
        int teacher = getIntent().getIntExtra(EXTRA_TEACHER, 4);

        // 先生ごとに、表示する絵・文・背景の色を決める
        int image;
        int name;
        int message;
        int bg;
        switch (teacher) {
            case 1:
                image = R.drawable.s_m_sensei;
                name = R.string.over1_name;
                message = R.string.over1_msg;
                bg = R.color.over1_bg;
                break;
            case 2:
                image = R.drawable.e_sensei;
                name = R.string.over2_name;
                message = R.string.over2_msg;
                bg = R.color.over2_bg;
                break;
            case 3:
                image = R.drawable.s_s_sensei;
                name = R.string.over3_name;
                message = R.string.over3_msg;
                bg = R.color.over3_bg;
                break;
            case BattleData.BOSS_GAME_OVER: // 理事長
                image = R.drawable.boss_rijicho;
                name = R.string.over5_name;
                message = R.string.over5_msg;
                bg = R.color.over5_bg;
                break;
            default:
                image = R.drawable.koutyou_sensei;
                name = R.string.over4_name;
                message = R.string.over4_msg;
                bg = R.color.over4_bg;
                break;
        }

        // 画面に反映する
        ((ImageView) findViewById(R.id.imgTeacher)).setImageResource(image);
        ((TextView) findViewById(R.id.txtTeacher)).setText(name);
        ((TextView) findViewById(R.id.txtMessage)).setText(message);
        findViewById(R.id.main).setBackgroundResource(bg);

        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            GameState.hp = GameState.maxHp; // HPを全部回復してから再挑戦
            GameState.save(this);
            goTo(MainActivity.class);
        });
        findViewById(R.id.btnTitle).setOnClickListener(v -> {
            // 続きは、タイトルの「つづきから」で遊べる（負けた場所から、HPは全部回復）
            GameState.hp = GameState.maxHp;
            GameState.save(this);
            goTo(TitleActivity.class);
        });
    }

    // 別の画面へ移動して、今の画面は閉じる
    private void goTo(Class<?> next) {
        startActivity(new Intent(this, next));
        finish();
    }
}

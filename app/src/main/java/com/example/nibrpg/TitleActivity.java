package com.example.nibrpg;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** タイトル画面（アプリを起動すると最初に出る） */
public class TitleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_title);
        // 画面のふち（ステータスバーなど）に文字がかぶらないようにする
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        View btnStart = findViewById(R.id.btnStart);

        // 「はじめる」を点滅させる
        AlphaAnimation blink = new AlphaAnimation(1f, 0.2f);
        blink.setDuration(600);
        blink.setRepeatMode(Animation.REVERSE);
        blink.setRepeatCount(Animation.INFINITE);
        btnStart.startAnimation(blink);

        // 押したらゲーム本編へ
        btnStart.setOnClickListener(v -> {
            GameState.reset(); // 最初から
            goTo(MainActivity.class);
        });
    }

    // 別の画面へ移動して、今の画面は閉じる
    private void goTo(Class<?> next) {
        startActivity(new Intent(this, next));
        finish();
    }
}

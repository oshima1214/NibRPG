package com.example.nibrpg;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** ゲームクリア画面 */
public class ClearActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // クリアしても、理事長を倒すまではゲームが続く（エンドロールのあと、理事長室の鍵をもらう）
        GameState.save(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clear);
        Sound.jingle(this, R.raw.jg_clear);
        Achievements.checkClear(this); // クリアが条件の実績を調べる
        // 画面のふち（ステータスバーなど）に文字がかぶらないようにする
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // 「GAME CLEAR」を小さい状態からポンと大きくする
        View txtClear = findViewById(R.id.txtClear);
        ScaleAnimation pop = new ScaleAnimation(0.3f, 1f, 0.3f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        pop.setDuration(500);
        txtClear.startAnimation(pop);

        // 卒業証書は少しおくれて、ふわっと出す
        View cardCert = findViewById(R.id.cardCert);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setStartOffset(400);
        fadeIn.setDuration(700);
        cardCert.startAnimation(fadeIn);

        findViewById(R.id.btnNext).setOnClickListener(v -> {
            Sound.se(this, R.raw.se_ok);
            goTo(EndrollActivity.class);
        });
    }

    // 別の画面へ移動して、今の画面は閉じる
    private void goTo(Class<?> next) {
        startActivity(new Intent(this, next));
        finish();
    }
}

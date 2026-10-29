package com.example.nibrpg;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** エンドロール（クリア画面のあとに流れる） */
public class EndrollActivity extends AppCompatActivity {

    private static final float SPEED_DP = 1.2f;  // 1回に上へ動かす量（dp）
    private static final int FAST = 4;           // 押している間は何倍速か
    private static final long FRAME_MS = 16;     // 動かす間かく（約60回／秒）
    private static final long WAIT_MS = 3000;    // 止まってからタイトルへ戻るまで

    private final Handler handler = new Handler(Looper.getMainLooper());
    private View credits;
    private View finalBlock;
    private float speedPx;       // SPEED_DP を画面の点の数に直したもの
    private float endY;          // 止まる位置
    private boolean fast = false;     // 押している間 true
    private boolean finished = false; // タイトルへ戻ったら true

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_endroll);
        // 画面のふち（ステータスバーなど）に文字がかぶらないようにする
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        View stage = findViewById(R.id.stage);
        credits = findViewById(R.id.credits);
        finalBlock = findViewById(R.id.finalBlock);
        speedPx = SPEED_DP * getResources().getDisplayMetrics().density;

        // 押している間だけ早送り
        stage.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                fast = true;
            } else if (e.getAction() == MotionEvent.ACTION_UP
                    || e.getAction() == MotionEvent.ACTION_CANCEL) {
                fast = false;
            }
            return true;
        });

        // スキップ：すぐタイトルへ
        findViewById(R.id.btnSkip).setOnClickListener(v -> goTitle());

        // 画面の大きさが決まってから、スタート位置を決めて流し始める
        stage.post(() -> {
            int screenH = stage.getHeight();
            credits.setTranslationY(screenH);  // 画面のすぐ下からスタート
            // 最後のかたまりが、画面のまん中に来る位置で止める
            endY = screenH / 2f - (finalBlock.getTop() + finalBlock.getHeight() / 2f);
            handler.post(scroll);
        });
    }

    // 少しずつ上へ動かす処理（くり返し呼ばれる）
    private final Runnable scroll = new Runnable() {
        @Override
        public void run() {
            float y = credits.getTranslationY() - speedPx * (fast ? FAST : 1);
            if (y <= endY) {
                credits.setTranslationY(endY);              // 最後で止める
                handler.postDelayed(() -> goTitle(), WAIT_MS); // 少し待ってタイトルへ
                return;
            }
            credits.setTranslationY(y);
            handler.postDelayed(this, FRAME_MS);
        }
    };

    // タイトル画面へ戻る（2回呼ばれても1回だけ動く）
    private void goTitle() {
        if (finished) return;
        finished = true;
        handler.removeCallbacksAndMessages(null);
        if (GameState.playing) {
            // 校長に勝ったあと：理事長室の鍵をもらう。
            // 続きは1Fのはじめの場所から（5Fから下りてくるのは大変なので）
            GameState.bossKey = true;
            GameState.floor = 0;
            GameState.px = -1;  // -1 にすると、MainActivity で 1F の P の位置になる
            GameState.py = -1;
            GameState.dir = CharacterSprite.FRONT;
            GameState.save(this);
            GameState.keyNotice = true; // タイトルで知らせる
        }
        startActivity(new Intent(this, TitleActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sound.bgm(this, R.raw.bgm_ending);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Sound.pauseBgmSoon();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // 画面を閉じたら動きを止める
    }
}

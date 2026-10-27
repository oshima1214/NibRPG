package com.example.nibrpg;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * タイトル画面（アプリを起動すると最初に出る）。
 *   つづきから     ：遊んでいる途中なら出る。1Fの続きから
 *   はじめる／はじめから：最初から遊ぶ
 *   見た目を変える ：キャラクリエイト画面へ（決定・やめるで、ここにもどる）
 */
public class TitleActivity extends AppCompatActivity {

    private static final long TYPE_MS = 45; // お知らせの文字を1文字ずつ出す速さ
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_title);
        GameState.loadOnce(this); // アプリを開いたときに、セーブデータを読みこむ
        // 画面のふち（ステータスバーなど）に文字がかぶらないようにする
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        TextView btnContinue = findViewById(R.id.btnContinue);
        TextView btnStart = findViewById(R.id.btnStart);
        View btnChara = findViewById(R.id.btnChara);

        if (GameState.playing) {
            // 遊んでいる途中：「つづきから」を出して、そちらを点滅させる
            btnContinue.setVisibility(View.VISIBLE);
            btnStart.setText(R.string.title_restart);
            blink(btnContinue);
        } else {
            btnContinue.setVisibility(View.GONE);
            btnStart.setText(R.string.title_start);
            blink(btnStart);
        }

        // つづきから → 1Fの、さっきいた場所から
        btnContinue.setOnClickListener(v -> goTo(MainActivity.class));

        // はじめる → 最初から。途中なら「本当にいい？」と聞く
        btnStart.setOnClickListener(v -> {
            if (!GameState.playing) {
                startNewGame();
                return;
            }
            new AlertDialog.Builder(this)
                    .setMessage(R.string.title_restart_ask)
                    .setPositiveButton(R.string.btn_yes, (d, w) -> {
                        Achievements.resetAll(this); // 「はじめから」は実績もリセット
                        startNewGame();
                    })
                    .setNegativeButton(R.string.btn_no, null)
                    .show();
        });

        // 見た目を変える → キャラクリ（タイトルは閉じないので、終わるとここにもどる）
        btnChara.setOnClickListener(v -> startActivity(new Intent(this, CharacterCreate.class)));

        // クリア後（エンドロールのあと）：理事長室の鍵を手に入れたことを知らせる（1回だけ）
        if (GameState.keyNotice) {
            GameState.keyNotice = false;
            showKeyNotice();
        }

        // デバッグ：階をえらんで始める（GameState.DEBUG が true のときだけ出す）
        View btnDebug = findViewById(R.id.btnDebug);
        btnDebug.setVisibility(GameState.DEBUG ? View.VISIBLE : View.GONE);
        btnDebug.setOnClickListener(v -> showDebugMenu());
    }

    /** デバッグ：何階から始めるかを選ぶ */
    private void showDebugMenu() {
        // 1F〜5F ＋ 理事長戦
        String[] floors = new String[MapData.FLOORS.length + 1];
        for (int i = 0; i < MapData.FLOORS.length; i++) {
            floors[i] = getString(R.string.debug_floor, i + 1);
        }
        floors[MapData.FLOORS.length] = getString(R.string.debug_boss);
        new AlertDialog.Builder(this)
                .setTitle(R.string.debug_title)
                .setItems(floors, (d, which) -> {
                    Achievements.resetAll(this); // 実績もリセット
                    if (which < MapData.FLOORS.length) {
                        GameState.debugStart(which);
                    } else {
                        GameState.debugStartBoss(); // 理事長室の中から（鍵あり）
                    }
                    GameState.save(this);
                    goTo(MainActivity.class);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    /** ボタンを点滅させる */
    private void blink(View v) {
        AlphaAnimation blink = new AlphaAnimation(1f, 0.2f);
        blink.setDuration(600);
        blink.setRepeatMode(Animation.REVERSE);
        blink.setRepeatCount(Animation.INFINITE);
        v.startAnimation(blink);
    }

    /** 最初から遊ぶ（見た目の色はそのまま） */
    private void startNewGame() {
        GameState.reset();
        GameState.playing = true;
        GameState.save(this);
        goTo(MainActivity.class);
    }

    /**
     * 「理事長室の鍵を手に入れた！」のお知らせ（RPGのメッセージウィンドウ風）。
     *   黒い背景に白いわく、ドットの白い文字。本文は1文字ずつ出る。
     *   画面をタップ：文字が出ている途中なら全部出す。出きっていたら閉じる
     */
    private void showKeyNotice() {
        float dp = getResources().getDisplayMetrics().density;
        Typeface dot = ResourcesCompat.getFont(this, R.font.dotgothic16);
        String body = getString(R.string.boss_key_msg);

        // 画面全体をうすく暗くする幕（うしろのボタンを押せないようにする）
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);

        // メッセージウィンドウ：黒い背景に白いわく
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding((int) (22 * dp), (int) (16 * dp), (int) (22 * dp), (int) (10 * dp));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF000000);
        bg.setStroke((int) (3 * dp), 0xFFFFFFFF);
        bg.setCornerRadius(4 * dp);
        box.setBackground(bg);

        TextView title = new TextView(this);
        title.setText(R.string.boss_key_title);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        title.setTypeface(dot);
        box.addView(title);

        TextView text = new TextView(this);
        text.setTextColor(0xFFFFFFFF);
        text.setTextSize(16);
        text.setTypeface(dot);
        text.setLineSpacing(4 * dp, 1f);
        text.setPadding(0, (int) (12 * dp), 0, 0);
        text.setText(hiddenAfter(body, 0)); // 全部の文字を入れておく（まだ透明）。ウィンドウの大きさが変わらない
        box.addView(text);

        // 右下の「▼」（全部出たら点めつする）
        TextView next = new TextView(this);
        next.setText("▼");
        next.setTextColor(0xFFFFFFFF);
        next.setTextSize(14);
        next.setTypeface(dot);
        next.setGravity(Gravity.END);
        next.setVisibility(View.INVISIBLE);
        box.addView(next, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        lp.setMargins((int) (48 * dp), 0, (int) (48 * dp), 0);
        overlay.addView(box, lp);
        ViewGroup root = findViewById(android.R.id.content);
        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 本文を1文字ずつ出す
        int[] shown = { 0 };
        Runnable type = new Runnable() {
            @Override
            public void run() {
                shown[0]++;
                text.setText(hiddenAfter(body, shown[0]));
                if (shown[0] < body.length()) {
                    handler.postDelayed(this, TYPE_MS);
                } else {
                    showNextMark(next);
                }
            }
        };
        handler.postDelayed(type, 300);

        // タップ：途中なら全部出す。出きっていたら閉じる
        overlay.setOnClickListener(v -> {
            if (shown[0] < body.length()) {
                handler.removeCallbacks(type);
                shown[0] = body.length();
                text.setText(body);
                showNextMark(next);
            } else {
                root.removeView(overlay);
            }
        });
    }

    /** n 文字目より後ろを透明にした文字（1文字ずつ出すときに使う） */
    private CharSequence hiddenAfter(String s, int n) {
        SpannableString sp = new SpannableString(s);
        if (n < s.length()) {
            sp.setSpan(new ForegroundColorSpan(Color.TRANSPARENT), n, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sp;
    }

    /** 「▼」を出して点めつさせる */
    private void showNextMark(View next) {
        next.setVisibility(View.VISIBLE);
        AlphaAnimation blink = new AlphaAnimation(1f, 0f);
        blink.setDuration(500);
        blink.setRepeatMode(Animation.REVERSE);
        blink.setRepeatCount(Animation.INFINITE);
        next.startAnimation(blink);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    // 別の画面へ移動して、今の画面は閉じる
    private void goTo(Class<?> next) {
        startActivity(new Intent(this, next));
        finish();
    }
}

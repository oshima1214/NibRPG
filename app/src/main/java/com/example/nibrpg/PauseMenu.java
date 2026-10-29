package com.example.nibrpg;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import java.util.function.BooleanSupplier;

/**
 * 戦闘画面の「一時停止」ボタンと、止めている間のメニュー（つづける・リタイア）。
 * 画面のいちばん上に重ねて表示する（レイアウトの xml は変えなくてよい）。
 */
public class PauseMenu {

    private static final long CHECK_MS = 300; // ボタンを出すかどうかを調べる間かく

    private final Activity activity;
    private final BooleanSupplier canPause; // 一時停止できるときだけ true（勝ち負けが決まったら false）
    private final Runnable onPause, onResume, onRetire;
    private final TextView button;
    private final FrameLayout overlay;
    private final TextView btnRetire;
    private boolean confirm = false; // リタイアを1回押して、確認中か

    /**
     * @param canPause 一時停止できるか（勝ち負けが決まったら false を返す）
     * @param onPause  止めるときにすること（時間を止める）
     * @param onResume 再開するときにすること（時間を進める）
     * @param onRetire リタイアするときにすること（マップに戻る）
     */
    public PauseMenu(Activity a, BooleanSupplier canPause, Runnable onPause, Runnable onResume, Runnable onRetire) {
        activity = a;
        this.canPause = canPause;
        this.onPause = onPause;
        this.onResume = onResume;
        this.onRetire = onRetire;
        Typeface dot = ResourcesCompat.getFont(a, R.font.dotgothic16);
        ViewGroup root = a.findViewById(android.R.id.content);

        // 左上の「一時停止」ボタン
        button = new TextView(a);
        button.setText(R.string.pause_button);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(16);
        button.setTypeface(dot);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.rpg_window);
        button.setPadding(dp(12), dp(4), dp(12), dp(4));
        button.setOnClickListener(v -> open());
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        blp.setMargins(dp(8), dp(8), 0, 0);
        root.addView(button, blp);

        // 止めている間のメニュー（画面全体をうす暗くして、さわれないようにする）
        overlay = new FrameLayout(a);
        overlay.setBackgroundColor(0xB0000000);
        overlay.setClickable(true); // うしろのボタンを押せないように
        overlay.setVisibility(View.GONE);

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setBackgroundResource(R.drawable.rpg_window);
        box.setPadding(dp(32), dp(20), dp(32), dp(20));

        TextView title = new TextView(a);
        title.setText(R.string.pause_title);
        title.setTextColor(0xFFFFD54F);
        title.setTextSize(24);
        title.setTypeface(dot);
        box.addView(title);

        TextView btnContinue = menuButton(a, dot, R.string.pause_continue);
        btnContinue.setOnClickListener(v -> close());
        box.addView(btnContinue, buttonParams());

        btnRetire = menuButton(a, dot, R.string.pause_retire);
        btnRetire.setOnClickListener(v -> {
            if (!confirm) {
                confirm = true; // 1回目：確認する
                btnRetire.setText(R.string.pause_retire_confirm);
                return;
            }
            overlay.setVisibility(View.GONE);
            onRetire.run();
        });
        box.addView(btnRetire, buttonParams());

        TextView note = new TextView(a);
        note.setText(R.string.pause_retire_note);
        note.setTextColor(0xFFB0BEC5);
        note.setTextSize(12);
        note.setTypeface(dot);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = dp(10);
        box.addView(note, nlp);

        overlay.addView(box, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 勝ち負けが決まったら、ボタンをかくす
        button.post(new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing()) return;
                boolean can = canPause.getAsBoolean();
                button.setVisibility(can ? View.VISIBLE : View.GONE);
                if (can) button.postDelayed(this, CHECK_MS);
            }
        });
    }

    /** 止める（メニューを出す） */
    public void open() {
        if (isOpen() || !canPause.getAsBoolean()) return;
        confirm = false;
        btnRetire.setText(R.string.pause_retire);
        overlay.setVisibility(View.VISIBLE);
        onPause.run();
    }

    /** 再開する（メニューを閉じる） */
    public void close() {
        if (!isOpen()) return;
        overlay.setVisibility(View.GONE);
        onResume.run();
    }

    public boolean isOpen() {
        return overlay.getVisibility() == View.VISIBLE;
    }

    private TextView menuButton(Activity a, Typeface dot, int textId) {
        TextView b = new TextView(a);
        b.setText(textId);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(18);
        b.setTypeface(dot);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundResource(R.drawable.nib_button);
        b.setPadding(dp(32), dp(10), dp(32), dp(10));
        return b;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        return lp;
    }

    private int dp(int v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
}

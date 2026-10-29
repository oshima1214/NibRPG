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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.function.BooleanSupplier;

/**
 * 戦闘画面の「一時停止」ボタンと、止めている間のメニュー（つづける・リタイア）。
 * ボタンは戦闘画面の左の列（先生の名前の上）に、メニューは画面のいちばん上に重ねて表示する（レイアウトの xml は変えなくてよい）。
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

        // 「一時停止」ボタン
        button = new TextView(a);
        button.setText(R.string.pause_button);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(16);
        button.setTypeface(dot);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.rpg_window);
        // 指で押しやすい大きさ（48dp くらい）にする
        button.setMinWidth(dp(64));
        button.setMinHeight(dp(44));
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        button.setOnClickListener(v -> open());
        View name = a.findViewById(R.id.txtTeacherName);
        if (name != null && name.getParent() instanceof LinearLayout) {
            // 戦闘画面：左の列（先生の名前の上）に置く。
            // 列の中なら、画面のふち（ステータスバー・カメラの切り欠き）はレイアウト側でよけてあるし、
            // 先生の名前とも重ならない（名前は、ボタンの下にずれる）
            LinearLayout column = (LinearLayout) name.getParent();
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.gravity = Gravity.START;
            clp.bottomMargin = dp(4);
            column.addView(button, 0, clp);
        } else {
            // それ以外の画面：左上に重ねる。画面のふちの下にもぐらないように、その分ずらす。
            // もぐっていると、タップがスマホ側（ステータスバー）に取られて、ボタンが反応しにくい
            FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
            blp.setMargins(dp(8), dp(8), 0, 0);
            root.addView(button, blp);
            ViewCompat.setOnApplyWindowInsetsListener(button, (v, insets) -> {
                Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) v.getLayoutParams();
                p.setMargins(safe.left + dp(8), safe.top + dp(8), 0, 0);
                v.setLayoutParams(p);
                return insets;
            });
            ViewCompat.requestApplyInsets(button);
        }

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
        btnContinue.setOnClickListener(v -> {
            Sound.se(activity, R.raw.se_ok);
            close();
        });
        box.addView(btnContinue, buttonParams());

        btnRetire = menuButton(a, dot, R.string.pause_retire);
        btnRetire.setOnClickListener(v -> {
            if (!confirm) {
                confirm = true; // 1回目：確認する
                Sound.se(activity, R.raw.se_cancel);
                btnRetire.setText(R.string.pause_retire_confirm);
                return;
            }
            overlay.setVisibility(View.GONE);
            Sound.se(activity, R.raw.se_retire);
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
        Sound.se(activity, R.raw.se_pause);
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

    /** 画面の (x, y)（画面全体での位置）に、一時停止ボタンがあるか */
    public boolean isOnButton(float x, float y) {
        if (!button.isShown()) return false;
        int[] p = new int[2];
        button.getLocationOnScreen(p);
        return x >= p[0] && x < p[0] + button.getWidth() && y >= p[1] && y < p[1] + button.getHeight();
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

package com.example.nibrpg;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 戦闘画面：ことば並べバトル。
 * バラバラの文字の札をタップして並べ、制限時間内に単語を完成させる。
 *   正解    → 先生にダメージ（速く答えるとクリティカル）
 *   時間切れ → 先生から精神的ダメージ
 */
public class BattleActivity extends AppCompatActivity {

    private static final long TICK_MS = 50;         // 残り時間を更新する間かく
    private static final long INTRO_MS = 1500;      // 「あらわれた！」を見せる時間
    private static final long AFTER_HIT_MS = 1000;  // 正解のあと、次の問題までの時間
    private static final long AFTER_MISS_MS = 1600; // 時間切れのあと、次の問題までの時間

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    // ---- 先生 ----
    private int floor;
    private String teacherName;
    private int teacherHp;
    private int teacherMaxHp;

    // ---- 今の問題 ----
    private final List<BattleData.Word> deck = new ArrayList<>(); // これから出す問題
    private BattleData.Word word;
    private final List<String> tiles = new ArrayList<>();      // 札の文字（バラバラの順）
    private final List<Integer> picked = new ArrayList<>();    // タップした札の番号（順番どおり）
    private final List<TextView> tileViews = new ArrayList<>();
    private final List<TextView> slotViews = new ArrayList<>();
    private long startTime;
    private long timeLimit;
    private boolean busy = true;      // true の間は札を押せない（演出中など）
    private boolean finished = false; // 勝ち負けが決まったら true

    // ---- 画面の部品 ----
    private ImageView imgTeacher;
    private ProgressBar barTeacher;
    private ProgressBar barPlayer;
    private ProgressBar barTime;
    private TextView txtTeacherHp;
    private TextView txtPlayerHp;
    private TextView txtLine;
    private TextView txtHint;
    private TextView txtMessage;
    private LinearLayout answerRow;
    private LinearLayout tileRow;
    private LinearLayout itemRow;
    private View flash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_battle);
        // 画面のふち（ステータスバーなど）に文字がかぶらないようにする
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        imgTeacher = findViewById(R.id.imgTeacher);
        barTeacher = findViewById(R.id.barTeacher);
        barPlayer = findViewById(R.id.barPlayer);
        barTime = findViewById(R.id.barTime);
        txtTeacherHp = findViewById(R.id.txtTeacherHp);
        txtPlayerHp = findViewById(R.id.txtPlayerHp);
        txtLine = findViewById(R.id.txtLine);
        txtHint = findViewById(R.id.txtHint);
        txtMessage = findViewById(R.id.txtMessage);
        answerRow = findViewById(R.id.answerRow);
        tileRow = findViewById(R.id.tileRow);
        itemRow = findViewById(R.id.itemRow);
        flash = findViewById(R.id.flash);

        // 先生の準備（今いる階の先生と戦う）
        floor = GameState.floor;
        teacherName = getString(MapData.TEACHER_NAMES[floor]);
        teacherMaxHp = BattleData.TEACHER_HP[floor];
        teacherHp = teacherMaxHp;
        imgTeacher.setImageResource(MapData.TEACHER_IMAGES[floor]);
        ((TextView) findViewById(R.id.txtTeacherName)).setText(teacherName);
        txtLine.setText(getString(R.string.battle_appear, teacherName));

        findViewById(R.id.btnBackspace).setOnClickListener(v -> removeLast());
        findViewById(R.id.btnReset).setOnClickListener(v -> resetPicked());

        // 戦闘からは逃げられない（戻るボタンを押してもマップに戻らない）
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!finished) txtMessage.setText(R.string.battle_no_escape);
            }
        });

        updateBars();
        refreshItems();
        handler.postDelayed(this::nextWord, INTRO_MS);
    }

    // ===================== 問題 =====================

    /** 次の問題を出す */
    private void nextWord() {
        if (finished) return;
        // 問題の山がなくなったら、全部の単語を混ぜて作り直す
        if (deck.isEmpty()) {
            Collections.addAll(deck, BattleData.WORDS);
            Collections.shuffle(deck, random);
        }
        word = deck.remove(0);

        // 札：答えの文字 ＋ ハズレの文字 を混ぜる
        tiles.clear();
        for (int i = 0; i < word.answer.length(); i++) {
            tiles.add(String.valueOf(word.answer.charAt(i)));
        }
        for (int i = 0; i < BattleData.DUMMY_COUNT; i++) {
            int r = random.nextInt(BattleData.DUMMY_LETTERS.length());
            tiles.add(String.valueOf(BattleData.DUMMY_LETTERS.charAt(r)));
        }
        Collections.shuffle(tiles, random);
        picked.clear();

        // 札を並べる
        tileRow.removeAllViews();
        tileViews.clear();
        for (int i = 0; i < tiles.size(); i++) {
            TextView t = makeBox(tiles.get(i), R.drawable.tile_letter);
            int index = i; // ラムダの中で使うため
            t.setOnClickListener(v -> onTile(index));
            tileRow.addView(t);
            tileViews.add(t);
        }

        // 答えの枠を、答えの文字数だけ並べる
        answerRow.removeAllViews();
        slotViews.clear();
        for (int i = 0; i < word.answer.length(); i++) {
            TextView s = makeBox("", R.drawable.tile_slot);
            answerRow.addView(s);
            slotViews.add(s);
        }

        txtHint.setText(getString(R.string.battle_hint, word.hint));
        txtMessage.setText("");
        timeLimit = BattleData.BASE_TIME_MS + BattleData.PER_LETTER_MS * word.answer.length();
        startTime = SystemClock.uptimeMillis();
        busy = false;
        handler.post(tick);
    }

    /** 残り時間を減らしていく処理（くり返し呼ばれる） */
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (busy || finished) return;
            long elapsed = SystemClock.uptimeMillis() - startTime;
            float left = Math.max(0f, 1f - elapsed / (float) timeLimit);
            barTime.setProgress((int) (left * barTime.getMax()));
            if (elapsed >= timeLimit) {
                onTimeout();
            } else {
                handler.postDelayed(this, TICK_MS);
            }
        }
    };

    /** 札がタップされたとき */
    private void onTile(int i) {
        if (busy || picked.contains(i)) return;
        picked.add(i);
        showPicked();
        if (picked.size() == word.answer.length()) {
            check();
        }
    }

    /** 最後に入れた1文字を消す */
    private void removeLast() {
        if (busy || picked.isEmpty()) return;
        picked.remove(picked.size() - 1);
        showPicked();
    }

    /** 入れた文字を全部消す */
    private void resetPicked() {
        if (busy) return;
        picked.clear();
        showPicked();
    }

    /** 答えの枠と札の見た目を、今の picked に合わせる */
    private void showPicked() {
        for (int s = 0; s < slotViews.size(); s++) {
            if (s < picked.size()) {
                slotViews.get(s).setText(tiles.get(picked.get(s)));
                slotViews.get(s).setBackgroundResource(R.drawable.tile_letter);
            } else {
                slotViews.get(s).setText("");
                slotViews.get(s).setBackgroundResource(R.drawable.tile_slot);
            }
        }
        for (int t = 0; t < tileViews.size(); t++) {
            tileViews.get(t).setAlpha(picked.contains(t) ? 0.25f : 1f); // 使った札はうすく
        }
    }

    /** 文字が全部うまったら、答え合わせ */
    private void check() {
        StringBuilder sb = new StringBuilder();
        for (int i : picked) sb.append(tiles.get(i));
        if (sb.toString().equals(word.answer)) {
            onCorrect();
        } else {
            txtMessage.setText(R.string.battle_wrong);
            shake(answerRow);
            picked.clear();          // ちがったら、入れ直し（時間は止まらない）
            showPicked();
        }
    }

    // ===================== ダメージ =====================

    /** 正解：先生にダメージ */
    private void onCorrect() {
        busy = true;
        long elapsed = SystemClock.uptimeMillis() - startTime;
        boolean critical = elapsed <= timeLimit / 2; // 半分の時間で答えたらクリティカル
        int damage = GameState.attack() * BattleData.DAMAGE_RATE;
        if (critical) damage = damage * 3 / 2;

        teacherHp = Math.max(0, teacherHp - damage);
        updateBars();
        shake(imgTeacher);
        txtMessage.setText(getString(critical ? R.string.battle_critical : R.string.battle_correct,
                teacherName, damage));

        if (teacherHp <= 0) {
            win();
        } else {
            handler.postDelayed(this::nextWord, AFTER_HIT_MS);
        }
    }

    /** 時間切れ：先生から精神的ダメージ */
    private void onTimeout() {
        busy = true;
        int damage = BattleData.TEACHER_ATTACK[floor];
        GameState.hp = Math.max(0, GameState.hp - damage);
        updateBars();
        flashRed();

        String[] taunts = getResources().getStringArray(BattleData.TAUNTS[floor]);
        txtLine.setText(taunts[random.nextInt(taunts.length)]);
        txtMessage.setText(getString(R.string.battle_timeout, word.answer, damage));

        if (GameState.hp <= 0) {
            finished = true;
            handler.postDelayed(this::lose, AFTER_MISS_MS);
        } else {
            handler.postDelayed(this::nextWord, AFTER_MISS_MS);
        }
    }

    // ===================== 勝ち・負け =====================

    private void win() {
        finished = true;
        GameState.defeated[floor] = true;
        int reward = MapData.REWARDS[floor];
        GameState.money += reward;

        txtLine.setText(R.string.battle_teacher_lose);
        ((TextView) findViewById(R.id.txtResult)).setText(getString(R.string.battle_win, teacherName, reward));
        View box = findViewById(R.id.resultBox);
        box.setVisibility(View.VISIBLE);
        findViewById(R.id.btnResult).setOnClickListener(v -> {
            if (floor == MapData.LAST) {
                // 最上階の先生に勝ったらクリア（マップの画面も閉じる）
                Intent intent = new Intent(this, ClearActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            finish(); // マップにもどる（マップの画面は開いたままなので、続きから）
        });
    }

    private void lose() {
        // 攻略失敗画面へ（後ろに残っているマップの画面も閉じる）
        Intent intent = new Intent(this, GameOverActivity.class);
        intent.putExtra(GameOverActivity.EXTRA_TEACHER, floor);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ===================== 道具 =====================

    /** 持っている道具のボタンを並べ直す（持っていない道具は出さない） */
    private void refreshItems() {
        itemRow.removeAllViews();
        for (int i = 0; i < ShopData.ITEMS.length; i++) {
            if (GameState.itemCounts[i] <= 0) continue;
            TextView b = new TextView(this);
            b.setText(getString(R.string.battle_item, ShopData.ITEMS[i].name, GameState.itemCounts[i]));
            b.setTextColor(getColor(R.color.white));
            b.setTextSize(13);
            b.setBackgroundResource(R.drawable.rpg_window);
            b.setPadding(dp(10), dp(6), dp(10), dp(6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(6));
            b.setLayoutParams(lp);
            int index = i;
            b.setOnClickListener(v -> useItem(index));
            itemRow.addView(b);
        }
    }

    /** 道具を使う（時間は止まらない） */
    private void useItem(int i) {
        if (finished || GameState.itemCounts[i] <= 0) return;
        int before = GameState.hp;
        GameState.hp = Math.min(GameState.maxHp, GameState.hp + ShopData.ITEMS[i].power);
        GameState.itemCounts[i]--;
        txtMessage.setText(getString(R.string.battle_item_used, ShopData.ITEMS[i].name, GameState.hp - before));
        updateBars();
        refreshItems();
    }

    // ===================== 見た目 =====================

    /** HPのバーと数字を、今の値に合わせる */
    private void updateBars() {
        barTeacher.setMax(teacherMaxHp);
        barTeacher.setProgress(teacherHp);
        txtTeacherHp.setText(getString(R.string.battle_hp, teacherHp, teacherMaxHp));
        barPlayer.setMax(GameState.maxHp);
        barPlayer.setProgress(GameState.hp);
        txtPlayerHp.setText(getString(R.string.battle_hp, GameState.hp, GameState.maxHp));
    }

    /** 札・答えの枠になる四角を1つ作る */
    private TextView makeBox(String text, int background) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(24);
        t.setTextColor(getColor(R.color.nib_navy));
        t.getPaint().setFakeBoldText(true);
        t.setBackgroundResource(background);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(50), dp(50));
        lp.setMargins(dp(3), 0, dp(3), 0);
        t.setLayoutParams(lp);
        return t;
    }

    /** 左右にぶるぶるゆらす */
    private void shake(View v) {
        TranslateAnimation a = new TranslateAnimation(0, dp(8), 0, 0);
        a.setDuration(400);
        a.setInterpolator(new CycleInterpolator(4));
        v.startAnimation(a);
    }

    /** 画面を一瞬赤くする */
    private void flashRed() {
        flash.setAlpha(0.5f);
        flash.animate().alpha(0f).setDuration(500).start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}

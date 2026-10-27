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
 * 3Fの戦闘：穴うめバトル（E先生）。
 * 単語のいくつかの文字が □ になっているので、下の札から正しい文字を選んでうめる。
 * ハズレの札は「ぱ／ば」「よ／ょ」のような、見まちがえやすい文字。
 *   正解    → 先生にダメージ（速く答えるとクリティカル）
 *   時間切れ → 先生から精神的ダメージ
 * 画面のレイアウトは、2Fと同じ activity_battle.xml を使う。
 */
public class FillBattleActivity extends AppCompatActivity {

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
    private boolean boss;  // 理事長戦か
    private int stage;     // 理事長戦の何問目か（0〜3）

    // ---- 今の問題 ----
    private final List<BattleData.Word> deck = new ArrayList<>(); // これから出す問題
    private BattleData.Word word;
    private final List<Integer> blanks = new ArrayList<>();    // □にした文字の場所（左から順）
    private final List<String> choices = new ArrayList<>();    // 下の札の文字
    private final List<String> filled = new ArrayList<>();     // □に入れた文字（左から順）
    private final List<TextView> slotViews = new ArrayList<>(); // □の部品（blanks と同じ順）
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
        boss = getIntent().getBooleanExtra(BattleData.EXTRA_BOSS, false);
        stage = getIntent().getIntExtra(BattleData.EXTRA_STAGE, 0);
        if (boss) {
            // 理事長戦：HPは前の問題から引きつぐ
            teacherName = getString(R.string.boss_name);
            teacherMaxHp = BattleData.BOSS_HP;
            teacherHp = GameState.bossHp;
            imgTeacher.setImageResource(R.drawable.boss_rijicho);
        } else {
            teacherName = getString(MapData.TEACHER_NAMES[floor]);
            teacherMaxHp = BattleData.TEACHER_HP[floor];
            teacherHp = teacherMaxHp;
            imgTeacher.setImageResource(MapData.TEACHER_IMAGES[floor]);
        }
        ((TextView) findViewById(R.id.txtTeacherName)).setText(teacherName);
        long introMs = TypeWriter.show(txtLine, boss ? getResources().getStringArray(R.array.boss_stage_lines)[stage]
                : getString(R.string.battle_appear, teacherName));
        txtMessage.setText(R.string.fill_rule);

        findViewById(R.id.btnBackspace).setOnClickListener(v -> removeLast());
        findViewById(R.id.btnReset).setOnClickListener(v -> resetFilled());

        // 戦闘からは逃げられない（戻るボタンを押してもマップに戻らない）
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!finished) txtMessage.setText(R.string.battle_no_escape);
            }
        });

        updateBars();
        refreshItems();
        handler.postDelayed(this::nextWord, Math.max(INTRO_MS * 2, introMs)); // セリフを全部出して、ルールを読む時間もとる
    }

    // ===================== 問題 =====================

    /** 次の問題を出す */
    private void nextWord() {
        if (finished) return;
        // 問題の山がなくなったら、全部の単語を混ぜて作り直す
        if (deck.isEmpty()) {
            Collections.addAll(deck, boss ? BattleData.BOSS_FILL_WORDS : BattleData.FILL_WORDS);
            Collections.shuffle(deck, random);
        }
        word = deck.remove(0);
        String answer = word.answer;

        // □にする場所を決める（5文字以下なら2つ、それより長ければ3つ）
        // 見まちがえやすい文字がある場所を優先して □ にする
        int count = boss ? BattleData.BOSS_FILL_BLANKS : (answer.length() <= 5 ? 2 : 3);
        int choiceCount = boss ? BattleData.BOSS_FILL_CHOICES : BattleData.FILL_CHOICES;
        List<Integer> tricky = new ArrayList<>();
        List<Integer> others = new ArrayList<>();
        for (int i = 0; i < answer.length(); i++) {
            if (lookAlikes(answer.charAt(i)).length() > 1) tricky.add(i); else others.add(i);
        }
        Collections.shuffle(tricky, random);
        Collections.shuffle(others, random);
        tricky.addAll(others);
        blanks.clear();
        blanks.addAll(tricky.subList(0, count));
        Collections.sort(blanks);

        // 札：□に入る文字 ＋ 見まちがえやすい文字 ＋ ふつうのハズレ文字
        choices.clear();
        for (int b : blanks) addChoice(String.valueOf(answer.charAt(b)));
        for (int b : blanks) {
            String group = lookAlikes(answer.charAt(b));
            for (int k = 0; k < group.length() && choices.size() < choiceCount; k++) {
                addChoice(String.valueOf(group.charAt(k)));
            }
        }
        while (choices.size() < choiceCount) {
            int r = random.nextInt(BattleData.DUMMY_LETTERS.length());
            addChoice(String.valueOf(BattleData.DUMMY_LETTERS.charAt(r)));
        }
        Collections.shuffle(choices, random);
        filled.clear();

        // 単語を並べる（□の所は空き枠、それ以外は文字だけ）
        answerRow.removeAllViews();
        slotViews.clear();
        for (int i = 0; i < answer.length(); i++) {
            if (blanks.contains(i)) {
                TextView s = makeBox("", R.drawable.tile_slot);
                answerRow.addView(s);
                slotViews.add(s);
            } else {
                TextView t = makeBox(String.valueOf(answer.charAt(i)), 0);
                t.setTextColor(getColor(R.color.white)); // 印刷された文字のように
                answerRow.addView(t);
            }
        }

        // 下の札（同じ札を何回使ってもよい）
        tileRow.removeAllViews();
        for (String c : choices) {
            TextView t = makeBox(c, R.drawable.tile_letter);
            t.setOnClickListener(v -> onChoice(c));
            tileRow.addView(t);
        }

        txtHint.setText(getString(R.string.battle_hint, word.hint));
        txtMessage.setText("");
        timeLimit = boss
                ? BattleData.BOSS_FILL_BASE_TIME_MS + BattleData.BOSS_FILL_PER_BLANK_MS * count
                : BattleData.FILL_BASE_TIME_MS + BattleData.FILL_PER_BLANK_MS * count;
        startTime = SystemClock.uptimeMillis();
        busy = false;
        handler.post(tick);
    }

    /** その文字の「見まちがえやすいなかま」（なければその文字だけ） */
    private String lookAlikes(char c) {
        for (String group : BattleData.LOOK_ALIKES) {
            if (group.indexOf(c) >= 0) return group;
        }
        return String.valueOf(c);
    }

    /** 札の文字を追加する（同じ文字は入れない） */
    private void addChoice(String c) {
        if (!choices.contains(c)) choices.add(c);
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

    /** 札がタップされたとき：左の □ から順にうめる */
    private void onChoice(String c) {
        if (busy || filled.size() >= blanks.size()) return;
        filled.add(c);
        showFilled();
        if (filled.size() == blanks.size()) check();
    }

    /** 最後に入れた1文字を消す */
    private void removeLast() {
        if (busy || filled.isEmpty()) return;
        filled.remove(filled.size() - 1);
        showFilled();
    }

    /** 入れた文字を全部消す */
    private void resetFilled() {
        if (busy) return;
        filled.clear();
        showFilled();
    }

    /** □の見た目を、今の filled に合わせる */
    private void showFilled() {
        for (int s = 0; s < slotViews.size(); s++) {
            if (s < filled.size()) {
                slotViews.get(s).setText(filled.get(s));
                slotViews.get(s).setBackgroundResource(R.drawable.tile_letter);
            } else {
                slotViews.get(s).setText("");
                slotViews.get(s).setBackgroundResource(R.drawable.tile_slot);
            }
        }
    }

    /** □が全部うまったら、答え合わせ */
    private void check() {
        boolean ok = true;
        for (int i = 0; i < blanks.size(); i++) {
            if (!filled.get(i).equals(String.valueOf(word.answer.charAt(blanks.get(i))))) ok = false;
        }
        if (ok) {
            onCorrect();
        } else {
            txtMessage.setText(R.string.fill_wrong);
            shake(answerRow);
            filled.clear();          // ちがったら、入れ直し（時間は止まらない）
            showFilled();
        }
    }

    // ===================== ダメージ =====================

    /** 正解：先生にダメージ */
    private void onCorrect() {
        busy = true;
        long elapsed = SystemClock.uptimeMillis() - startTime;
        boolean critical = elapsed <= timeLimit / 2; // 半分の時間で答えたらクリティカル
        int damage = GameState.rollAttack(); // 少しばらつく
        if (critical) damage = damage * 3 / 2;

        teacherHp = Math.max(0, teacherHp - damage);
        updateBars();
        shake(imgTeacher);
        txtMessage.setText(getString(critical ? R.string.battle_critical : R.string.battle_correct,
                teacherName, damage));

        if (bossStageClear()) return; // 理事長戦：次の問題へ
        if (teacherHp <= 0) {
            win();
        } else {
            handler.postDelayed(this::nextWord, AFTER_HIT_MS);
        }
    }

    /** 時間切れ：先生から精神的ダメージ */
    private void onTimeout() {
        busy = true;
        int damage = teacherAttack();
        GameState.hp = Math.max(0, GameState.hp - damage);
        GameState.tookDamage = true; // 実績のため
        updateBars();
        flashRed();

        String[] taunts = getResources().getStringArray(tauntsId());
        long lineMs = TypeWriter.show(txtLine, taunts[random.nextInt(taunts.length)]);
        txtMessage.setText(getString(R.string.battle_timeout, word.answer, damage));

        if (GameState.hp <= 0) {
            finished = true;
            handler.postDelayed(this::lose, Math.max(AFTER_MISS_MS, lineMs));
        } else {
            handler.postDelayed(this::nextWord, Math.max(AFTER_MISS_MS, lineMs));
        }
    }

    // ===================== 勝ち・負け =====================

    private void win() {
        finished = true;
        Achievements.checkWin(this); // HPが残り1桁なら実績
        GameState.defeated[floor] = true;
        int reward = MapData.REWARDS[floor];
        GameState.money += reward;

        TypeWriter.show(txtLine, R.string.fill_teacher_lose);
        ((TextView) findViewById(R.id.txtResult)).setText(getString(R.string.battle_win, teacherName, reward));
        findViewById(R.id.resultBox).setVisibility(View.VISIBLE);
        findViewById(R.id.btnResult).setOnClickListener(v -> finish()); // マップにもどる（続きから）
    }

    private void lose() {
        // 攻略失敗画面へ（後ろに残っているマップの画面も閉じる）
        Intent intent = new Intent(this, GameOverActivity.class);
        intent.putExtra(GameOverActivity.EXTRA_TEACHER, boss ? BattleData.BOSS_GAME_OVER : floor);
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
        GameState.usedItem = true; // 実績のため
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

    /** 文字の四角を1つ作る（background が 0 なら、わくなし） */
    private TextView makeBox(String text, int background) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(22);
        t.setTextColor(getColor(R.color.nib_navy));
        t.getPaint().setFakeBoldText(true);
        if (background != 0) t.setBackgroundResource(background);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(46), dp(50));
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

    // ===================== 理事長戦 =====================

    /** 失敗したときのダメージ（理事長戦は BOSS_ATTACK） */
    private int teacherAttack() {
        return boss ? BattleData.BOSS_ATTACK : BattleData.TEACHER_ATTACK[floor];
    }

    /** 失敗したときのひとこと（理事長戦は taunts_boss） */
    private int tauntsId() {
        return boss ? R.array.taunts_boss : BattleData.TAUNTS[floor];
    }

    /**
     * 理事長戦：この問題（ミニゲーム）のぶんのHPを削りきったら、次の問題へ。
     * 1つの問題で削れるのは、HPの4分の1まで
     * @return 次の問題へ進むときは true
     */
    private boolean bossStageClear() {
        int stages = BattleData.BOSS_STAGES.length;
        if (!boss || stage >= stages - 1) return false;
        int border = BattleData.BOSS_HP * (stages - 1 - stage) / stages;
        if (teacherHp > border) return false;
        teacherHp = border;
        GameState.bossHp = teacherHp;
        updateBars();
        finished = true;
        long lineMs = TypeWriter.show(txtLine, R.string.boss_next_line);
        txtMessage.setText(R.string.boss_next);
        handler.postDelayed(() -> {
            Intent intent = new Intent(this, BattleData.BOSS_STAGES[stage + 1]);
            intent.putExtra(BattleData.EXTRA_BOSS, true);
            intent.putExtra(BattleData.EXTRA_STAGE, stage + 1);
            startActivity(intent);
            finish();
        }, Math.max(1800, lineMs));
        return true;
    }
}

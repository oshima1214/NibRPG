package com.example.nibrpg;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.List;
import java.util.Random;

/**
 * 4Fの戦闘：板書うつしバトル（S先生）。
 * 先生が黒板に教科の文字（国・数・英…）を書いて、すぐに消す。
 * 書かれた順番どおりに、下のボタンを押す。
 *   全部正しい → 先生にダメージ（覚えた数が多いほど大きい）。次は1文字ふえる
 *   まちがい・時間切れ → 先生から精神的ダメージ
 */
public class MemoryBattleActivity extends AppCompatActivity {

    private static final long TICK_MS = 50;         // 残り時間を更新する間かく
    private static final long INTRO_MS = 3000;      // 「あらわれた！」とルールを見せる時間
    private static final long AFTER_HIT_MS = 1200;  // 正解のあと、次の板書までの時間
    private static final long AFTER_MISS_MS = 1800; // 失敗のあと、次の板書までの時間

    private final PauseHandler handler = new PauseHandler(); // 一時停止できるタイマー
    private PauseMenu pauseMenu;                              // 一時停止ボタンとメニュー
    private final Random random = new Random();

    // ---- 先生 ----
    private int floor;
    private String teacherName;
    private int teacherHp;
    private int teacherMaxHp;
    private boolean boss;  // 理事長戦か
    private int stage;     // 理事長戦の何問目か（0〜3）

    // ---- 今の問題 ----
    private int length = BattleData.MEMORY_START;              // 覚える数（正解するとふえる）
    private final List<Integer> sequence = new ArrayList<>();  // 黒板に書かれた教科（SUBJECTS の番号）
    private int inputCount = 0;                                // 何個目まで答えたか
    private long startTime;
    private long timeLimit;
    private boolean answering = false; // true の間だけボタンを押せる
    private boolean finished = false;  // 勝ち負けが決まったら true

    // ---- 画面の部品 ----
    private ImageView imgTeacher;
    private ProgressBar barTeacher;
    private ProgressBar barPlayer;
    private ProgressBar barTime;
    private TextView txtTeacherHp;
    private TextView txtPlayerHp;
    private TextView txtLine;
    private TextView txtBoard;
    private TextView txtBoardLabel;
    private TextView txtMessage;
    private LinearLayout itemRow;
    private View flash;
    private final List<TextView> buttons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_battle_memory);
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
        txtBoard = findViewById(R.id.txtBoard);
        txtBoardLabel = findViewById(R.id.txtBoardLabel);
        txtMessage = findViewById(R.id.txtMessage);
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
        txtMessage.setText(R.string.memory_rule);
        if (boss) length = BattleData.BOSS_MEMORY_START; // 理事長戦は、はじめから多め

        // 教科のボタンを、上の段と下の段に半分ずつ並べる
        LinearLayout row1 = findViewById(R.id.buttonRow1);
        LinearLayout row2 = findViewById(R.id.buttonRow2);
        int half = (BattleData.SUBJECTS.length + 1) / 2;
        for (int i = 0; i < BattleData.SUBJECTS.length; i++) {
            TextView b = new TextView(this);
            b.setText(BattleData.SUBJECTS[i]);
            b.setGravity(Gravity.CENTER);
            b.setTextSize(20);
            b.setTextColor(getColor(R.color.nib_navy));
            b.getPaint().setFakeBoldText(true);
            b.setBackgroundResource(R.drawable.tile_letter);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.MATCH_PARENT);
            lp.setMargins(dp(4), 0, dp(4), 0);
            b.setLayoutParams(lp);
            int index = i; // ラムダの中で使うため
            b.setOnClickListener(v -> onButton(index));
            (i < half ? row1 : row2).addView(b);
            buttons.add(b);
        }
        setButtonsEnabled(false);

        // 戻るボタン：そのままマップには戻らず、一時停止メニューを出す（リタイアはメニューから）
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!finished) pauseMenu.open();
            }
        });
        // 一時停止ボタン（左上）とメニュー（つづける・リタイア）
        pauseMenu = new PauseMenu(this, () -> !finished, this::pauseBattle, this::resumeBattle, this::retire);

        updateBars();
        refreshItems();
        handler.postDelayed(this::nextRound, Math.max(INTRO_MS, introMs)); // セリフを全部出してから始める
    }

    // ===================== 板書 =====================

    /** 次の板書を始める：先生が1文字ずつ黒板に書く */
    private void nextRound() {
        if (finished) return;
        answering = false;
        setButtonsEnabled(false);
        barTime.setProgress(barTime.getMax());

        // 書く教科を決める（同じ教科が2回続かないようにする）
        sequence.clear();
        for (int i = 0; i < length; i++) {
            int s;
            do {
                s = random.nextInt(BattleData.SUBJECTS.length);
            } while (!sequence.isEmpty() && sequence.get(sequence.size() - 1) == s);
            sequence.add(s);
        }

        txtBoard.setText("");
        txtBoard.setAlpha(1f);
        txtBoardLabel.setText(getString(R.string.memory_label_watch, length));
        txtMessage.setText("");
        // 1文字ずつ書いていく
        for (int i = 0; i < length; i++) {
            int n = i + 1;
            handler.postDelayed(() -> txtBoard.setText(boardText(n)), BattleData.MEMORY_WRITE_MS * (i + 1));
        }
        // 書き終わって少ししたら、黒板消しで消す
        long erase = BattleData.MEMORY_WRITE_MS * length
                + (boss ? BattleData.BOSS_MEMORY_SHOW_MS : BattleData.MEMORY_SHOW_MS);
        handler.postDelayed(() -> txtBoard.animate().alpha(0f).setDuration(250)
                .withEndAction(this::startAnswer).start(), erase);
    }

    /** 黒板に書く文字（はじめの n 文字） */
    private String boardText(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(' ');
            sb.append(BattleData.SUBJECTS[sequence.get(i)]);
        }
        return sb.toString();
    }

    /** 消し終わったら、答える番 */
    private void startAnswer() {
        if (finished) return;
        inputCount = 0;
        txtBoard.setText(answerText());
        txtBoard.setAlpha(1f);
        txtBoardLabel.setText(R.string.memory_label_answer);
        timeLimit = BattleData.MEMORY_BASE_TIME_MS + BattleData.MEMORY_PER_LETTER_MS * length;
        startTime = handler.now();
        answering = true;
        setButtonsEnabled(true);
        handler.post(tick);
    }

    /** 答えているときの黒板：押した教科と、のこりの「？」 */
    private String answerText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(i < inputCount ? BattleData.SUBJECTS[sequence.get(i)] : "？");
        }
        return sb.toString();
    }

    /** 残り時間を減らしていく処理（くり返し呼ばれる） */
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!answering || finished) return;
            long elapsed = handler.now() - startTime;
            float left = Math.max(0f, 1f - elapsed / (float) timeLimit);
            barTime.setProgress((int) (left * barTime.getMax()));
            if (elapsed >= timeLimit) {
                onMiss(true);
            } else {
                handler.postDelayed(this, TICK_MS);
            }
        }
    };

    /** 教科のボタンが押されたとき */
    private void onButton(int s) {
        if (!answering) return;
        if (sequence.get(inputCount) != s) {
            onMiss(false); // 順番がちがう
            return;
        }
        inputCount++;
        txtBoard.setText(answerText());
        if (inputCount == length) onCorrect();
    }

    // ===================== ダメージ =====================

    /** 全部正しく写せた：先生にダメージ（覚えた数が多いほど大きい） */
    private void onCorrect() {
        answering = false;
        setButtonsEnabled(false);
        int damage = GameState.rollAttack() * length / BattleData.MEMORY_START; // 3文字で攻撃力そのまま
        if (boss) {
            // 理事長戦：素手でも BOSS_MEMORY_CLEAR 回正解すれば、この問題が終わるダメージを保証する
            int stageHp = BattleData.BOSS_HP / BattleData.BOSS_STAGES.length;
            int least = (stageHp + BattleData.BOSS_MEMORY_CLEAR - 1) / BattleData.BOSS_MEMORY_CLEAR; // 切り上げ
            damage = Math.max(damage, least);
        }
        teacherHp = Math.max(0, teacherHp - damage);
        updateBars();
        shake(imgTeacher);
        txtMessage.setText(getString(R.string.memory_correct, length, teacherName, damage));

        if (bossStageClear()) return; // 理事長戦：次の問題へ
        if (teacherHp <= 0) {
            win();
        } else {
            if (length < (boss ? BattleData.BOSS_MEMORY_MAX : BattleData.MEMORY_MAX)) length++; // 次は1文字ふやす
            handler.postDelayed(this::nextRound, AFTER_HIT_MS);
        }
    }

    /** まちがい・時間切れ：先生から精神的ダメージ */
    private void onMiss(boolean timeout) {
        answering = false;
        setButtonsEnabled(false);
        int damage = teacherAttack();
        GameState.hp = Math.max(0, GameState.hp - damage);
        GameState.tookDamage = true; // 実績のため
        updateBars();
        flashRed();
        shake(txtBoard);

        // 正しい板書を見せる
        txtBoard.setText(boardText(length));
        String[] taunts = getResources().getStringArray(tauntsId());
        long lineMs = TypeWriter.show(txtLine, taunts[random.nextInt(taunts.length)]);
        txtMessage.setText(getString(timeout ? R.string.memory_timeout : R.string.memory_wrong, damage));

        if (GameState.hp <= 0) {
            finished = true;
            handler.postDelayed(this::lose, Math.max(AFTER_MISS_MS, lineMs));
        } else {
            handler.postDelayed(this::nextRound, Math.max(AFTER_MISS_MS, lineMs));
        }
    }

    // ===================== 勝ち・負け =====================

    private void win() {
        finished = true;
        Achievements.checkWin(this); // HPが残り1桁なら実績
        GameState.defeated[floor] = true;
        int reward = MapData.REWARDS[floor];
        GameState.money += reward;

        TypeWriter.show(txtLine, R.string.memory_teacher_lose);
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

    /** 教科のボタンを押せる／押せない見た目にする */
    private void setButtonsEnabled(boolean on) {
        for (TextView b : buttons) b.setAlpha(on ? 1f : 0.4f);
    }

    /** HPのバーと数字を、今の値に合わせる */
    private void updateBars() {
        barTeacher.setMax(teacherMaxHp);
        barTeacher.setProgress(teacherHp);
        txtTeacherHp.setText(getString(R.string.battle_hp, teacherHp, teacherMaxHp));
        barPlayer.setMax(GameState.maxHp);
        barPlayer.setProgress(GameState.hp);
        txtPlayerHp.setText(getString(R.string.battle_hp, GameState.hp, GameState.maxHp));
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

    // ===================== 一時停止・リタイア =====================

    /** 一時停止：時間を止める（制限時間も減らない） */
    private void pauseBattle() {
        handler.pause();
    }

    /** 再開 */
    private void resumeBattle() {
        handler.resume();
    }

    /** リタイア：今のHPのままマップに戻る（先生のHPは元に戻る） */
    private void retire() {
        finished = true;
        handler.removeCallbacksAndMessages(null);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 画面を離れたら、一時停止する（もどってきたら、メニューの「つづける」で再開）
        if (!finished) pauseMenu.open();
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

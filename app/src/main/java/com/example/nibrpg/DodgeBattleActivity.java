package com.example.nibrpg;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
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

import java.util.Random;

/**
 * 5Fの戦闘：よけゲーム（校長先生）。
 * 四角の中のハートを指でなぞって動かし、飛んでくるチョーク・ペン・マウス・パソコンをよける。
 *   こうげきを最後までよけきる → 先生にダメージ（1回も当たらなければ1.5倍）
 *   物に当たる → HPが減る
 * 先生のHPが半分を切ると「第2形態」：2種類の物がまざって、速くなる。
 */
public class DodgeBattleActivity extends AppCompatActivity {

    private static final long TICK_MS = 50;         // 残り時間を更新する間かく
    private static final long INTRO_MS = 3500;      // 「あらわれた！」とルールを見せる時間
    private static final long BETWEEN_MS = 1800;    // こうげきとこうげきの間の時間

    // 第1形態で出す順番（1種類ずつ）。パソコンは第2形態だけ
    private static final int[] PRINCIPAL_PHASE1 = { DodgeView.P_STAMP, DodgeView.P_CHIME, DodgeView.P_CLOCK }; // 校長：第1形態
    private static final int[] PRINCIPAL_PHASE2 = { DodgeView.P_CLOCK_STAMP, DodgeView.P_PAPERS, DodgeView.P_CHIME_SPIN }; // 校長：第2形態

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
    private boolean phase2 = false; // 第2形態になったか
    private boolean shrunk = false; // 理事長戦：四角をせまくしたか

    // ---- こうげき ----
    private int wave = 0;           // 何回目のこうげきか
    private long waveStart;
    private long waveLength;
    private boolean inWave = false;
    private int keptWeapon = -1;   // 理事長に没収された装備（負けたら返す）
    private int[] keptItems = null; // 理事長に没収されたアイテムの数（負けたら返す）
    private boolean finished = false; // 勝ち負けが決まったら true

    // ---- 画面の部品 ----
    private DodgeView dodgeView;
    private ImageView imgTeacher;
    private ProgressBar barTeacher;
    private ProgressBar barPlayer;
    private ProgressBar barTime;
    private TextView txtTeacherHp;
    private TextView txtPlayerHp;
    private TextView txtLine;
    private TextView txtMessage;
    private LinearLayout itemRow;
    private View flash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_battle_dodge);
        // 画面のふち（ステータスバーなど）に文字がかぶらないようにする
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        dodgeView = findViewById(R.id.dodgeView);
        imgTeacher = findViewById(R.id.imgTeacher);
        barTeacher = findViewById(R.id.barTeacher);
        barPlayer = findViewById(R.id.barPlayer);
        barTime = findViewById(R.id.barTime);
        txtTeacherHp = findViewById(R.id.txtTeacherHp);
        txtPlayerHp = findViewById(R.id.txtPlayerHp);
        txtLine = findViewById(R.id.txtLine);
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
        txtMessage.setText(R.string.dodge_rule);
        if (boss) {
            phase2 = true; // 理事長戦は、はじめから本気
            dodgeView.setBossFrame(true);
            // デバッグで、HPが少ない所から始めたときは、はじめからわくをせまくする
            if (teacherHp <= BattleData.BOSS_SHRINK_HP) {
                shrunk = true;
                dodgeView.shrinkArena(BattleData.BOSS_SHRINK_RATE);
            }
        }

        // 当たったとき・よけきったとき
        dodgeView.setListener(new DodgeView.Listener() {
            @Override
            public void onHit() {
                onDamaged();
            }

            @Override
            public void onWaveEnd(boolean noHit) {
                onSurvived(noHit);
            }

            @Override
            public void onImpact() {
                if (boss && !finished) screenShake(5, 200); // パソコンが落ちた衝撃
            }
        });

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
        if (boss) {
            // 理事長戦（HP50から）：まず装備とアイテムを没収してから始める
            handler.postDelayed(this::confiscate, Math.max(INTRO_MS, introMs));
        } else {
            handler.postDelayed(this::nextWave, Math.max(INTRO_MS, introMs)); // セリフを全部出してから始める
        }
    }

    /** 理事長戦：装備とアイテムを没収する（負けたら lose() で返す） */
    private void confiscate() {
        if (finished) return;
        keptWeapon = GameState.weapon;
        keptItems = GameState.itemCounts.clone();
        GameState.weapon = -1; // 素手
        java.util.Arrays.fill(GameState.itemCounts, 0);
        refreshItems();
        updateBars();
        flashRed();
        screenShake(8, 400);
        long lineMs = TypeWriter.show(txtLine, R.string.boss_confiscate_line);
        txtMessage.setText(R.string.boss_confiscate);
        handler.postDelayed(this::nextWave, Math.max(BETWEEN_MS, lineMs));
    }

    // ===================== こうげき =====================

    /** 先生のこうげきを始める */
    private void nextWave() {
        if (finished) return;
        if (boss) {
            // 理事長戦：決まった攻撃パターンを、順番に1つずつ（わくがせまくなったら速くなる）
            int pat = wave % DodgeView.PATTERN_COUNT;
            wave++;
            float bossSpeed = shrunk ? BattleData.BOSS_DODGE_SPEED_SHRUNK : BattleData.BOSS_DODGE_SPEED;
            // マウスの追いかけは、わくがせまくなっても速くしない（せまいと逃げにくいため）
            if (pat == DodgeView.P_MOUSE_HOMING) bossSpeed = BattleData.BOSS_DODGE_SPEED;
            final float patSpeed = bossSpeed; // 下の「あとで動く処理」で使うため
            waveLength = BattleData.BOSS_DODGE_WAVE_MS;
            long lineMs = TypeWriter.show(txtLine, getResources().getStringArray(R.array.boss_pattern_lines)[pat]);
            txtMessage.setText(getString(R.string.dodge_wave, teacherName));
            // セリフを全部出してから、こうげき開始
            handler.postDelayed(() -> {
                if (finished) return;
                dodgeView.startPatternWave(pat, patSpeed, waveLength);
                beginWave();
            }, lineMs);
            return;
        }

        // 校長戦：決まった攻撃パターンを順番に
        //   第1形態：ハンコ連打 → チャイムの輪 → 時計の針
        //   第2形態（HP半分から・速い）：時計の針＋ハンコ → 書類の山 → チャイムの輪（すき間が回る）
        int[] set = phase2 ? PRINCIPAL_PHASE2 : PRINCIPAL_PHASE1;
        int pat = set[wave % set.length];
        wave++;
        final float sp = phase2 ? BattleData.DODGE_SPEED2 : 1f;
        waveLength = phase2 ? BattleData.DODGE_WAVE2_MS : BattleData.DODGE_WAVE_MS;
        String[] lines = getResources().getStringArray(R.array.principal_pattern_lines);
        long lineMs = TypeWriter.show(txtLine, lines[pat - DodgeView.P_STAMP]);
        txtMessage.setText(getString(R.string.dodge_wave, teacherName));

        // セリフを全部出してから、こうげき開始
        handler.postDelayed(() -> {
            if (finished) return;
            dodgeView.startPatternWave(pat, sp, waveLength);
            beginWave();
        }, lineMs);
    }

    /** こうげきの時間をはかり始める */
    private void beginWave() {
        waveStart = handler.now();
        inWave = true;
        handler.post(tick);
    }

    /** こうげきの残り時間を表示する（くり返し呼ばれる） */
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!inWave || finished) return;
            long elapsed = handler.now() - waveStart;
            float left = Math.max(0f, 1f - elapsed / (float) waveLength);
            barTime.setProgress((int) (left * barTime.getMax()));
            handler.postDelayed(this, TICK_MS);
        }
    };

    /** 物に当たった */
    private void onDamaged() {
        if (finished) return;
        int damage = BattleData.DODGE_HIT_DAMAGE;
        GameState.hp = Math.max(0, GameState.hp - damage);
        GameState.tookDamage = true; // 実績のため
        updateBars();
        flashRed();
        if (boss) screenShake(10, 350); // 当たった衝撃
        txtMessage.setText(getString(R.string.dodge_hit, damage));

        if (GameState.hp <= 0) {
            finished = true;
            inWave = false;
            dodgeView.stop();
            String[] taunts = getResources().getStringArray(tauntsId());
            long lineMs = TypeWriter.show(txtLine, taunts[random.nextInt(taunts.length)]);
            handler.postDelayed(this::lose, Math.max(1600, lineMs));
        }
    }

    /** こうげきを最後までよけきった：先生にダメージ */
    private void onSurvived(boolean noHit) {
        if (finished) return;
        inWave = false;
        barTime.setProgress(0);
        int damage = GameState.rollAttack(); // 少しばらつく
        if (noHit) damage = damage * 3 / 2;
        teacherHp = Math.max(0, teacherHp - damage);
        updateBars();
        shake(imgTeacher);
        txtMessage.setText(getString(noHit ? R.string.dodge_perfect : R.string.dodge_survived,
                teacherName, damage));

        if (bossStageClear()) return; // 理事長戦：次の問題へ
        if (teacherHp <= 0) {
            win();
            return;
        }
        // 理事長戦：HPが残りわずかになったら、四角をせまくする
        if (boss && !shrunk && teacherHp <= BattleData.BOSS_SHRINK_HP) {
            shrunk = true;
            dodgeView.shrinkArena(BattleData.BOSS_SHRINK_RATE);
            screenShake(14, 700);
            flashRed();
            long lineMs = TypeWriter.show(txtLine, R.string.boss_shrink_line);
            txtMessage.setText(R.string.boss_shrink);
            handler.postDelayed(this::nextWave, Math.max(BETWEEN_MS * 2, lineMs));
            return;
        }
        // HPが半分を切ったら、第2形態へ
        if (!phase2 && teacherHp * 2 <= teacherMaxHp) {
            phase2 = true;
            long lineMs = TypeWriter.show(txtLine, R.string.dodge_phase2_line);
            txtMessage.setText(getString(R.string.dodge_phase2, teacherName));
            shake(findViewById(R.id.main));
            handler.postDelayed(this::nextWave, Math.max(BETWEEN_MS * 2, lineMs));
            return;
        }
        handler.postDelayed(this::nextWave, BETWEEN_MS);
    }

    // ===================== 勝ち・負け =====================

    private void win() {
        finished = true;
        if (boss) {
            bossWin();
            return;
        }
        Achievements.checkWin(this); // HPが残り1桁なら実績
        dodgeView.stop();
        GameState.defeated[floor] = true;
        int reward = MapData.REWARDS[floor];
        GameState.money += reward;

        TypeWriter.show(txtLine, R.string.dodge_teacher_lose);
        ((TextView) findViewById(R.id.txtResult)).setText(getString(R.string.battle_win, teacherName, reward));
        findViewById(R.id.resultBox).setVisibility(View.VISIBLE);
        findViewById(R.id.btnResult).setOnClickListener(v -> {
            if (floor == MapData.LAST) {
                // 最上階の先生に勝ったらクリア（マップの画面も閉じる）
                Intent intent = new Intent(this, ClearActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            finish();
        });
    }

    private void lose() {
        returnItems();
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

    /** 道具を使う（こうげき中でも使える） */
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

    /** 左右にぶるぶるゆらす */
    private void shake(View v) {
        TranslateAnimation a = new TranslateAnimation(0, dp(8), 0, 0);
        a.setDuration(400);
        a.setInterpolator(new CycleInterpolator(4));
        v.startAnimation(a);
    }

    /** 画面全体をガタガタゆらす（strengthDp：ゆれの大きさ、ms：ゆれる時間） */
    private void screenShake(float strengthDp, long ms) {
        View root = findViewById(R.id.main);
        float amp = strengthDp * getResources().getDisplayMetrics().density;
        long start = SystemClock.uptimeMillis();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long t = SystemClock.uptimeMillis() - start;
                if (t >= ms) {
                    root.setTranslationX(0);
                    root.setTranslationY(0);
                    return;
                }
                float k = 1f - t / (float) ms; // だんだん小さく
                root.setTranslationX((random.nextFloat() * 2f - 1f) * amp * k);
                root.setTranslationY((random.nextFloat() * 2f - 1f) * amp * k);
                handler.postDelayed(this, 30);
            }
        });
    }

    /** 画面を一瞬赤くする */
    private void flashRed() {
        flash.setAlpha(0.4f);
        flash.animate().alpha(0f).setDuration(400).start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onPause() {
        super.onPause();
        View root = findViewById(R.id.main);
        root.setTranslationX(0); // ゆれている途中で離れても、元の位置にもどす
        root.setTranslationY(0);
        // 画面を離れたら、一時停止する（もどってきたら、メニューの「つづける」で再開）
        if (!finished) pauseMenu.open();
    }

    // ===================== 一時停止・リタイア =====================

    /** 一時停止：時間と、よける四角の動きを止める */
    private void pauseBattle() {
        handler.pause();
        dodgeView.pause();
    }

    /** 再開 */
    private void resumeBattle() {
        handler.resume();
        dodgeView.resume();
    }

    /** リタイア：今のHPのままマップに戻る（先生のHPは元に戻る） */
    private void retire() {
        finished = true;
        handler.removeCallbacksAndMessages(null);
        dodgeView.stop();
        returnItems();
        finish();
    }

    /** 理事長戦で没収した装備とアイテムを返す */
    private void returnItems() {
        if (keptItems != null) {
            GameState.weapon = keptWeapon;
            GameState.itemCounts = keptItems;
            keptItems = null;
        }
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

    /** 理事長に勝った：実績を解除して、実績以外のデータをリセットしてタイトルへ */
    private void bossWin() {
        dodgeView.stop();
        Achievements.unlock(this, Achievements.BOSS_CLEAR);
        TypeWriter.show(txtLine, R.string.boss_lose_line);
        ((TextView) findViewById(R.id.txtResult)).setText(R.string.boss_win);
        TextView btn = findViewById(R.id.btnResult);
        btn.setText(R.string.boss_to_title);
        findViewById(R.id.resultBox).setVisibility(View.VISIBLE);
        btn.setOnClickListener(v -> {
            GameState.reset();          // 実績以外は、全部リセット
            GameState.playing = false;
            GameState.save(this);
            Intent intent = new Intent(this, TitleActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}

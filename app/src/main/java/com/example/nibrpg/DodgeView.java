package com.example.nibrpg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 5Fのよけゲームの四角。
 * 指でなぞるとハートが動く（指の動いた分だけ動くので、ハートに指がかぶらない）。
 * 先生のこうげきが飛んでくるので、当たらないようによける。
 *   ・理事長：チョーク・ペン・マウス・パソコンのパターン
 *   ・校長　：ハンコ・チャイム・時計の針・マイクの音波のパターン
 */
public class DodgeView extends View {

    /** こうげきの種類 */
    public static final int CHALK = 0; // チョーク：左右から横に飛んでくる
    public static final int PEN = 1;   // ペン：上から落ちてくる
    public static final int MOUSE = 2; // マウス：かべで跳ね返りながら動き回る
    public static final int PC = 3;    // パソコン：ハートをねらって上から落ちる（赤い予告あり）
    public static final int STAMP = 4; // 校長のハンコ：赤い丸の予告のあと、ドンと押される
    public static final int BELL = 5;  // チャイムの鐘：まん中に置かれる（さわると痛い）
    public static final int RING = 6;  // チャイムの輪：鐘から広がる。向かい合わせに2か所すき間がある
    public static final int HAND = 7;  // 時計の針：まん中を軸に回る
    public static final int WAVE = 8;  // マイクの音波：上下にゆれながら横に流れる、通り道の外がわ
    public static final int PAPER = 9; // 書類：上から落ちてきて、下に積もる（積もった書類もかべになる）

    /** 理事長戦の攻撃パターン（ランダムではなく、決まった形で飛んでくる） */
    public static final int P_CHALK_WALL = 0;   // チョークの壁：1か所だけすき間がある列が、左右から交互に来る
    public static final int P_PEN_SNIPE = 1;    // ペンの狙い撃ち：わくのはしでハートに向きを合わせ、少し止まってから一直線に飛んでくる
    public static final int P_MOUSE_HOMING = 2; // マウスの追いかけ：ハートをねらって曲がりながら追いかけてくる
    public static final int P_PC_ROW = 3;       // パソコンの列：わくのはしからはしまで降る。1列だけ逃げ道がある
    public static final int PATTERN_COUNT = 4;  // 理事長のパターンの数（0〜3）

    /** 校長戦の攻撃パターン */
    public static final int P_STAMP = 4; // ハンコ連打：ハートのいる所に、予告のあとハンコが押される
    public static final int P_CHIME = 5; // チャイムの輪：まん中の鐘から輪が広がる。向かい合わせの2つのすき間のどちらかをくぐる
    public static final int P_CLOCK = 6; // 時計の針：長い針と短い針が回る。長い針はときどき逆回り
    public static final int P_MIC = 7;   // マイクの音波：ゆれながら流れる通り道の中にいる（今は使っていない）
    public static final int P_CLOCK_STAMP = 8; // 時計の針＋ハンコ：針から逃げながら、ハンコもよける
    public static final int P_PAPERS = 9;      // 書類の山：落ちてくる書類が積もって、動ける場所がだんだんせまくなる
    public static final int P_CHIME_SPIN = 10; // チャイムの輪（第2形態）：広がりながら、すき間がゆっくり回る

    /** 当たったとき・こうげきが終わったときに知らせる */
    public interface Listener {
        void onHit();
        void onWaveEnd(boolean noHit);
        /** パソコンが四角の底に落ちたとき（画面をゆらす演出に使う） */
        default void onImpact() { }
    }

    /** 飛んでくる物1つ */
    private static class Shot {
        int type;
        float x, y;     // まん中の位置
        float vx, vy;   // 1秒に進む量
        float w, h;     // 大きさ（w が長い方）
        float angle;    // 向き（度）
        float bounceLeft; // マウス：跳ね返る残り時間（秒）
        long dropAt;    // パソコン：落ち始める時刻（それまでは予告だけ）
        boolean landed; // パソコン：底に落ちたか
        long born;      // 出てきた時刻（四角の外から来る物を、すぐ消さないため）
        float homingLeft; // マウス：ハートを追いかける残り時間（秒）
        boolean snipe;    // ペンの狙い撃ち
        long lockAt;      // 狙い撃ち：この時刻から、向きを固定する（それまではハートを追って向きを変える）
        long dashAt;      // 狙い撃ち：この時刻に、一直線に飛び出す
        float dashSpeed;  // 狙い撃ち：飛び出す速さ
        float spin;       // 時計の針：1秒に回る角度（度）
        long flipAt;      // 時計の針：この時刻に逆回りになる
        long flipEvery;   // 時計の針：逆回りになる間かく
        boolean face;     // 時計の針：文字ばんもいっしょに描く
        float gapDir;     // チャイムの輪：すき間の向き（度）
        int col;          // 書類：落ちる列
    }

    private static final long HIT_GUARD_MS = 1000; // 当たったあと、無敵になる時間
    private static final long SPAWN_STOP_MS = 700; // こうげきの終わり近くは、新しい物を出さない

    private final Random random = new Random();
    private final Paint pixel = new Paint();                       // ドット絵（ぼかさない）
    private final Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);  // 四角のわく
    private final Paint fill = new Paint();
    private final Paint aim = new Paint(Paint.ANTI_ALIAS_FLAG);    // 狙い撃ちの予告線
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);   // 輪・針・波などの線
    private final Paint soft = new Paint(Paint.ANTI_ALIAS_FLAG);   // 丸などのぬりつぶし
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);   // ハンコの文字
    private final Path path = new Path();
    private final RectF arena = new RectF();  // 今の四角（せまくなることがある）
    private final RectF base = new RectF();   // 元の大きさの四角
    private final RectF dst = new RectF();
    private final float dp;

    private final Bitmap heart, chalk, pen, mouse, pc;
    private final List<Shot> shots = new ArrayList<>();

    // ---- ハート ----
    private float heartX, heartY, heartSize;
    private float lastTouchX, lastTouchY;
    private long guardUntil = 0;

    // ---- こうげき ----
    private boolean running = false;
    private int[] patterns = {};
    private float speed = 1f;
    private float density = 1f;
    private long waveStart, waveLength, lastFrame;
    private long[] nextSpawn = {};
    private boolean hitThisWave;
    private Listener listener;
    private long pcInterval = BattleData.DODGE_PC_INTERVAL_MS; // パソコンが降ってくる間かく

    // ---- 四角をせまくする演出 ----
    private static final long SHRINK_MS = 800;
    private float arenaScale = 1f, scaleFrom = 1f, scaleTo = 1f;
    private long shrinkStart = -1;
    private boolean bossFrame = false; // true なら、わくを赤くする（理事長戦）

    // ---- 攻撃パターン（理事長戦） ----
    private int pattern = -1;          // -1 はランダム（ふつうのこうげき）
    private long patternNext;          // 次に出す時刻
    private int patternStep;           // 何回出したか
    private float gapY;                // チョークの壁：すき間の位置
    private static final float MOUSE_TURN = 2.6f; // マウスが1秒に曲がれる角度（ラジアン。大きいほど、しつこく追いかける）

    // ---- 校長戦の数字 ----
    private static final long STAMP_WARN_MS = 850;   // ハンコ：予告してから押されるまで
    private static final long STAMP_HIT_MS = 250;    // ハンコ：押されてから当たる時間
    private static final float STAMP_SIZE = 0.2f;    // ハンコ：半径（わくの高さの何倍か）
    private static final float STAMP_BIG_RATE = 2.5f; // ハンコ：ハートと関係ない場所に出る、大きいハンコの倍率
    private static final long STAMP_MARK_MS = 700;   // ハンコ：押したあと、あとが残る時間（当たらない）
    private static final long STAMP_EVERY_MS = 550;  // ハンコ：次の予告まで
    private static final long BELL_WARN_MS = 900;    // チャイム：鐘が出てから、さわると痛くなるまで
    private static final long RING_EVERY_MS = 1300;  // チャイム：次の輪まで
    private static final float RING_GAP_DEG = 45f;   // チャイム：輪のすき間1つの大きさ（度）。すき間は向かい合わせに2つ
    private static final float RING_GAP_SPIN = 45f;  // チャイム（第2形態）：すき間が1秒に回る角度（度）
    private static final long HAND_WARN_MS = 800;    // 時計：針が出てから、当たるようになるまで
    private static final float HAND_SPIN = 150f;     // 時計：長い針が1秒に回る角度（度）
    private static final long HAND_FLIP_MS = 2600;   // 時計：長い針が逆回りになる間かく
    private static final long FLIP_WARN_MS = 600;    // 時計：逆回りの前に、針が点めつする時間
    private static final float COMBO_HAND_RATE = 0.6f;     // 針＋ハンコ：針の速さ（時計だけのときの何倍か）
    private static final long COMBO_STAMP_WARN_MS = 1000;  // 針＋ハンコ：ハンコの予告の長さ
    private static final long COMBO_STAMP_EVERY_MS = 1100; // 針＋ハンコ：次のハンコまで
    private static final long WAVE_OPEN_MS = 1200;   // 音波：通り道が、だんだんせまくなる時間
    private static final long PAPER_WARN_MS = 450;   // 書類：列が光ってから、落ち始めるまで
    private static final long PAPER_EVERY_MS = 400;  // 書類：次の書類まで
    private static final float PAPER_FALL = 1.1f;    // 書類：落ちる速さ（1秒に、わくの高さの何倍か）
    private static final float PAPER_THICK = 0.09f;  // 書類：1枚（1束）の厚さ（わくの高さの何倍か）
    private static final float PAPER_MAX = 0.6f;     // 書類：積もる高さの上限（わくの高さの何倍か）
    private static final float PAPER_AIM = 0.45f;    // 書類：ハートのいる列をねらう割合
    private float[] paperTop = new float[0];         // 書類：列ごとの、積もった山のてっぺん
    private int paperCols;                           // 書類：列の数

    public DodgeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        pixel.setFilterBitmap(false);
        frame.setStyle(Paint.Style.STROKE);
        frame.setStrokeWidth(3 * dp);
        frame.setColor(0xFFFFFFFF);
        aim.setStyle(Paint.Style.STROKE);
        aim.setStrokeWidth(2 * dp);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        heart = load(R.drawable.dodge_heart);
        chalk = load(R.drawable.dodge_chalk);
        pen = load(R.drawable.dodge_pen);
        mouse = load(R.drawable.dodge_mouse);
        pc = load(R.drawable.dodge_pc);
    }

    private Bitmap load(int id) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        return BitmapFactory.decodeResource(getResources(), id, o);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    /** パソコンが降ってくる間かくを変える */
    public void setPcInterval(long ms) {
        pcInterval = ms;
    }

    /** わくを赤くする（理事長戦） */
    public void setBossFrame(boolean on) {
        bossFrame = on;
        invalidate();
    }

    /** 四角を、元の大きさの rate 倍にせまくする（ギュッと縮む演出つき） */
    public void shrinkArena(float rate) {
        scaleFrom = arenaScale;
        scaleTo = rate;
        shrinkStart = SystemClock.uptimeMillis();
        invalidate();
    }

    /** arenaScale に合わせて、今の四角の大きさを決める（まん中はそのまま） */
    private void applyArena() {
        float w = base.width() * arenaScale;
        float h = base.height() * arenaScale;
        arena.set(base.centerX() - w / 2, base.centerY() - h / 2, base.centerX() + w / 2, base.centerY() + h / 2);
        float half = heartSize / 2;
        heartX = Math.max(arena.left + half, Math.min(arena.right - half, heartX));
        heartY = Math.max(arena.top + half, Math.min(arena.bottom - half, heartY));
    }

    /**
     * こうげき開始
     * @param kinds  出てくる物の種類（CHALK など。2つ入れると2種類まざる）
     * @param speedRate 速さ（1＝ふつう）
     * @param densityRate 物の多さ（1＝ふつう。2なら2倍の数が出てくる）
     * @param lengthMs  こうげきの長さ
     */
    public void startWave(int[] kinds, float speedRate, float densityRate, long lengthMs) {
        pattern = -1;
        shots.clear();
        patterns = kinds;
        speed = speedRate;
        density = densityRate;
        waveStart = SystemClock.uptimeMillis();
        lastFrame = waveStart;
        waveLength = lengthMs;
        nextSpawn = new long[kinds.length];
        for (int i = 0; i < kinds.length; i++) nextSpawn[i] = waveStart + 500;
        hitThisWave = false;
        running = true;
        postInvalidateOnAnimation();
    }

    /**
     * 理事長戦：決まった攻撃パターンで、こうげき開始
     * @param p         パターン（P_CHALK_WALL など）
     * @param speedRate 速さ（1＝ふつう）
     * @param lengthMs  こうげきの長さ
     */
    public void startPatternWave(int p, float speedRate, long lengthMs) {
        shots.clear();
        patterns = new int[0];
        pattern = p;
        speed = speedRate;
        waveStart = SystemClock.uptimeMillis();
        lastFrame = waveStart;
        waveLength = lengthMs;
        patternNext = waveStart + 600;
        patternStep = 0;
        gapY = arena.centerY();
        hitThisWave = false;
        running = true;
        postInvalidateOnAnimation();
    }

    /** パターンの物を1回分出して、次に出すまでの時間（ミリ秒）を返す */
    private long spawnPattern(long now) {
        // 大きさ・速さは「元の四角」で決める（わくがせまくなっても、物は小さくならない）
        float H = base.height();
        float W = base.width();
        // 出てくる場所は「今の四角」で決める
        float AH = arena.height();
        float AW = arena.width();
        switch (pattern) {
            case P_CHALK_WALL: {
                // たてに並んだチョークの列。すき間は前の列から少しだけずらす（読めば通れる）
                float cw = H * 0.18f;
                float ch = cw * chalk.getHeight() / chalk.getWidth();
                float gap = heartSize * 2.6f;
                float move = (random.nextFloat() * 2f - 1f) * AH * 0.35f;
                gapY = Math.max(arena.top + gap / 2 + ch, Math.min(arena.bottom - gap / 2 - ch, gapY + move));
                boolean fromLeft = (patternStep % 2 == 0); // 左右から交互に
                for (float y = arena.top + ch / 2; y < arena.bottom; y += ch * 1.5f) {
                    if (Math.abs(y - gapY) < gap / 2) continue; // ここがすき間
                    Shot s = newShot(CHALK, now);
                    s.w = cw;
                    s.h = ch;
                    s.x = fromLeft ? arena.left - cw : arena.right + cw;
                    s.y = y;
                    s.vx = (fromLeft ? 1 : -1) * W * 0.75f * speed; // 速め
                    shots.add(s);
                }
                patternStep++;
                return (long) (850 / speed); // 次の壁まで（短め）
            }
            case P_PEN_SNIPE: {
                // わくのはし（内がわ）に1本。ハートから遠い場所を選ぶ
                float pw = H * 0.26f;
                float ph = pw * pen.getHeight() / pen.getWidth();
                Shot s = newShot(PEN, now);
                s.w = pw;
                s.h = ph;
                float inset = pw * 0.6f;
                for (int tryNo = 0; tryNo < 6; tryNo++) {
                    switch (random.nextInt(4)) { // どの辺に出すか
                        case 0: s.x = arena.left + inset + random.nextFloat() * (AW - inset * 2); s.y = arena.top + inset; break;
                        case 1: s.x = arena.left + inset + random.nextFloat() * (AW - inset * 2); s.y = arena.bottom - inset; break;
                        case 2: s.x = arena.left + inset; s.y = arena.top + inset + random.nextFloat() * (AH - inset * 2); break;
                        default: s.x = arena.right - inset; s.y = arena.top + inset + random.nextFloat() * (AH - inset * 2); break;
                    }
                    if (Math.hypot(s.x - heartX, s.y - heartY) > Math.min(AW, AH) * 0.45f) break; // 近すぎたら選び直す
                }
                s.snipe = true;
                s.angle = (float) Math.toDegrees(Math.atan2(heartY - s.y, heartX - s.x));
                s.lockAt = now + (long) (550 / speed);          // ① ハートを追って向きを合わせる
                s.dashAt = s.lockAt + (long) (300 / speed);     // ② 向きを固定して、少し止まる
                s.dashSpeed = H * 3.2f * speed;                 // ③ 一直線に飛ぶ
                shots.add(s);
                return (long) (650 / speed);
            }
            case P_MOUSE_HOMING: {
                // わくの外のどこかから1匹ずつ。ハートをねらって、曲がりながら追いかける
                float mh = H * 0.16f;
                Shot s = newShot(MOUSE, now);
                s.h = mh;
                s.w = mh * mouse.getWidth() / mouse.getHeight();
                switch (random.nextInt(4)) { // 出てくる辺
                    case 0: s.x = arena.left + random.nextFloat() * AW; s.y = arena.top - mh; break;
                    case 1: s.x = arena.left + random.nextFloat() * AW; s.y = arena.bottom + mh; break;
                    case 2: s.x = arena.left - mh; s.y = arena.top + random.nextFloat() * AH; break;
                    default: s.x = arena.right + mh; s.y = arena.top + random.nextFloat() * AH; break;
                }
                double a = Math.atan2(heartY - s.y, heartX - s.x);
                float v = H * 0.75f * speed; // ギリギリ逃げられるくらい
                s.vx = (float) (Math.cos(a) * v);
                s.vy = (float) (Math.sin(a) * v);
                s.angle = (float) Math.toDegrees(a) + 90;
                s.homingLeft = 3.0f; // 3秒追いかけたら、まっすぐ飛んでいく
                shots.add(s);
                return (long) (900 / speed);
            }
            case P_STAMP: {
                // ハートのいる所（少しずらす）に予告。3回に1回は、ほかの場所に大きいハンコを押して道をふさぐ
                boolean big = patternStep % 3 == 2;
                spawnStamp(now, big, STAMP_WARN_MS);
                patternStep++;
                return (long) (STAMP_EVERY_MS / speed);
            }
            case P_CHIME:
            case P_CHIME_SPIN: {
                if (patternStep == 0) {
                    // はじめに、まん中に鐘を置く（点めつしている間は、さわっても平気）
                    Shot b = newShot(BELL, now);
                    b.w = heartSize * 1.8f;
                    b.x = arena.centerX();
                    b.y = arena.centerY();
                    b.dropAt = now + BELL_WARN_MS;
                    shots.add(b);
                    gapY = random.nextInt(360); // すき間の向き（gapY を角度に使う）
                    patternStep++;
                    return BELL_WARN_MS;
                }
                // 鐘から輪を1つ出す。すき間の向きは、前の輪から少しだけ回す（読めば通れる）
                Shot s = newShot(RING, now);
                s.x = arena.centerX();
                s.y = arena.centerY();
                s.w = heartSize * 0.9f;       // 今の半径
                s.h = heartSize * 0.55f;      // 輪の太さ
                s.vx = H * 0.32f * speed;     // 1秒に広がる量
                gapY += (random.nextBoolean() ? 1 : -1) * (40 + random.nextInt(50));
                if (pattern == P_CHIME) {
                    // 第1形態（すき間が回らない）：すき間がほぼ真上・真下を向くと、
                    // 輪がわくの上下にとどいたあと左右に逃げ場がなくなるので、ななめより横に寄せる
                    float a = ((gapY % 180) + 180) % 180; // 0〜180度
                    if (a > 60 && a < 120) gapY += (a < 90) ? (60 - a) : (120 - a);
                }
                s.gapDir = gapY;
                if (pattern == P_CHIME_SPIN) {
                    // 第2形態：広がりながら、すき間が回る（回る向きは輪ごとにランダム）
                    s.spin = (random.nextBoolean() ? 1 : -1) * RING_GAP_SPIN * speed;
                }
                shots.add(s);
                patternStep++;
                return (long) (RING_EVERY_MS / speed);
            }
            case P_CLOCK: {
                spawnClock(now, 1f);
                return waveLength; // このこうげきの間は、もう出さない
            }
            case P_CLOCK_STAMP: {
                // 時計の針（少しおそめ）＋ハートをねらうハンコ（ゆっくりめ）
                if (patternStep == 0) {
                    spawnClock(now, COMBO_HAND_RATE);
                    patternStep++;
                    return HAND_WARN_MS + 600; // 針が動き出してから、ハンコを始める
                }
                spawnStamp(now, false, COMBO_STAMP_WARN_MS);
                patternStep++;
                return (long) (COMBO_STAMP_EVERY_MS / speed);
            }
            case P_PAPERS: {
                if (patternStep == 0) {
                    // 列を決めて、山の高さを0にする
                    paperCols = Math.max(5, Math.min(9, (int) (AW / (heartSize * 1.7f))));
                    paperTop = new float[paperCols];
                    for (int c = 0; c < paperCols; c++) paperTop[c] = arena.bottom;
                }
                spawnPaper(now);
                patternStep++;
                return (long) (PAPER_EVERY_MS / speed);
            }
            case P_MIC: {
                // ゆれながら流れる通り道（外がわが音波）。はじめは広く、だんだんせまくなる
                Shot s = newShot(WAVE, now);
                s.w = AW * 0.85f;            // 波の長さ
                s.h = AH * 0.26f;            // ゆれのはば
                s.vx = AW * 0.32f * speed;   // 流れる速さ
                s.x = random.nextFloat() * s.w; // 流れた量（はじめの形をランダムに）
                s.gapDir = heartSize * 1.5f; // 通り道の半分のはば（せまくなったとき）
                shots.add(s);
                return waveLength;
            }
            default: { // P_PC_ROW
                // わくのはしからはしまで、パソコンを横一列に。ランダムな1列だけ空ける
                float bw = H * 0.32f;
                float h = bw * pc.getHeight() / pc.getWidth();
                int cols = Math.max(3, (int) (AW / bw));
                float colW = AW / cols; // 1列のはば（すき間なく並べる）
                int safe = random.nextInt(cols);
                for (int k = 0; k < cols; k++) {
                    if (k == safe) continue; // 逃げ道
                    Shot s = newShot(PC, now);
                    s.w = colW;
                    s.h = h;
                    s.x = arena.left + (k + 0.5f) * colW;
                    s.y = arena.top - h / 2;
                    s.vy = H * 2.4f * speed; // 速め
                    s.dropAt = now + (long) (550 / speed); // 赤い予告のあと、いっせいに落ちる
                    shots.add(s);
                }
                return (long) (1300 / speed); // 次の列まで（短め）
            }
        }
    }

    /** 書類を1つ落とす（ときどきハートのいる列をねらう。上限まで積もった列には落とさない） */
    private void spawnPaper(long now) {
        float H = base.height();
        float colW = arena.width() / paperCols;
        float thick = H * PAPER_THICK;
        float full = arena.bottom - arena.height() * PAPER_MAX; // ここより上には積もらない
        int heartCol = Math.max(0, Math.min(paperCols - 1, (int) ((heartX - arena.left) / colW)));
        int c = random.nextFloat() < PAPER_AIM ? heartCol : random.nextInt(paperCols);
        for (int k = 0; k < paperCols && paperTop[c] - thick < full; k++) c = (c + 1) % paperCols; // いっぱいなら となりへ
        if (paperTop[c] - thick < full) return; // 全部いっぱい
        Shot s = newShot(PAPER, now);
        s.col = c;
        s.w = colW * 0.92f;
        s.h = thick;
        s.x = arena.left + (c + 0.5f) * colW + (random.nextFloat() * 2f - 1f) * colW * 0.04f; // 少しずらして、山っぽく
        s.y = arena.top - thick / 2;
        s.vy = H * PAPER_FALL * speed;
        s.dropAt = now + (long) (PAPER_WARN_MS / speed);
        shots.add(s);
    }

    /** ハンコを1つ出す（big：ハートと関係ない場所に、大きいハンコ） */
    private void spawnStamp(long now, boolean big, long warnMs) {
        float H = base.height();
        float r = H * STAMP_SIZE * (big ? STAMP_BIG_RATE : 1f); // ハンコの半径
        Shot s = newShot(STAMP, now);
        s.w = r * 2;
        float tx = big ? arena.left + random.nextFloat() * arena.width()
                : heartX + (random.nextFloat() * 2f - 1f) * heartSize * 0.6f;
        float ty = big ? arena.top + random.nextFloat() * arena.height()
                : heartY + (random.nextFloat() * 2f - 1f) * heartSize * 0.6f;
        float mx = Math.min(r * 0.6f, arena.width() / 2);
        float my = Math.min(r * 0.6f, arena.height() / 2);
        s.x = Math.max(arena.left + mx, Math.min(arena.right - mx, tx));
        s.y = Math.max(arena.top + my, Math.min(arena.bottom - my, ty));
        s.dropAt = now + (long) (warnMs / speed); // この時刻に押される
        shots.add(s);
    }

    /** 時計の針を出す：長い針（わくのはしまで）と短い針。はじめはハートと反対を向く */
    private void spawnClock(long now, float spinRate) {
        float AW = arena.width(), AH = arena.height();
        float heartDeg = (float) Math.toDegrees(Math.atan2(heartY - arena.centerY(), heartX - arena.centerX()));
        int dir = random.nextBoolean() ? 1 : -1;
        Shot l = newShot(HAND, now);
        l.x = arena.centerX();
        l.y = arena.centerY();
        l.w = (float) Math.hypot(AW, AH) / 2 + heartSize;
        l.h = heartSize * 0.45f;
        l.angle = heartDeg + 180;
        l.spin = dir * HAND_SPIN * spinRate * speed;
        l.flipEvery = (long) (HAND_FLIP_MS / speed);
        l.flipAt = now + HAND_WARN_MS + l.flipEvery;
        l.dropAt = now + HAND_WARN_MS;
        l.face = true;
        Shot sh = newShot(HAND, now);
        sh.x = l.x;
        sh.y = l.y;
        sh.w = AH * 0.28f;
        sh.h = heartSize * 0.6f;
        sh.angle = heartDeg + 90;
        sh.spin = -dir * HAND_SPIN * 0.55f * spinRate * speed;
        sh.flipAt = Long.MAX_VALUE; // 短い針は逆回りしない
        sh.dropAt = l.dropAt;
        shots.add(l);
        shots.add(sh);
    }

    private Shot newShot(int type, long now) {
        Shot s = new Shot();
        s.type = type;
        s.born = now;
        return s;
    }

    // ---- 一時停止 ----
    private boolean paused = false;
    private long pausedAt;

    /** 一時停止：動きを止める（絵はそのまま表示しておく） */
    public void pause() {
        if (paused) return;
        paused = true;
        pausedAt = SystemClock.uptimeMillis();
    }

    /** 再開：止めていた時間の分だけ、全部の時刻を後ろにずらして、続きから動かす */
    public void resume() {
        if (!paused) return;
        paused = false;
        long d = SystemClock.uptimeMillis() - pausedAt;
        waveStart += d;
        lastFrame += d;
        patternNext += d;
        for (int i = 0; i < nextSpawn.length; i++) nextSpawn[i] += d;
        guardUntil += d;
        if (shrinkStart >= 0) shrinkStart += d;
        for (Shot s : shots) {
            s.born += d;
            s.dropAt += d;
            s.lockAt += d;
            s.dashAt += d;
            if (s.flipAt != Long.MAX_VALUE) s.flipAt += d;
        }
        postInvalidateOnAnimation();
    }

    /** こうげきを止める（勝ち負けが決まったときなど） */
    public void stop() {
        running = false;
        shots.clear();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float m = 4 * dp;
        // 横長になりすぎないように、横幅をせまくして、まん中に置く
        //   full：画面いっぱいの横幅　narrow：高さ × DODGE_MAX_ASPECT の横幅
        //   DODGE_NARROW の割合だけ narrow に近づける（0.5 なら、ちょうど中間）
        float bh = h - m * 2;
        float full = w - m * 2;
        float narrow = Math.min(full, bh * BattleData.DODGE_MAX_ASPECT);
        float bw = full - (full - narrow) * BattleData.DODGE_NARROW;
        float left = (w - bw) / 2;
        base.set(left, m, left + bw, m + bh);
        float sw = base.width() * arenaScale, sh = base.height() * arenaScale;
        arena.set(base.centerX() - sw / 2, base.centerY() - sh / 2, base.centerX() + sw / 2, base.centerY() + sh / 2);
        heartSize = base.height() * 0.09f;
        if (oldw == 0) {
            // はじめはまん中
            heartX = arena.centerX();
            heartY = arena.centerY();
        } else {
            // 大きさが変わったときは、四角の中に入るようにするだけ
            float half = heartSize / 2;
            heartX = Math.max(arena.left + half, Math.min(arena.right - half, heartX));
            heartY = Math.max(arena.top + half, Math.min(arena.bottom - half, heartY));
        }
    }

    // ===================== 動かす =====================

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 一時停止中は、止めた時刻の絵をそのまま描く（動かさない）
        long now = paused ? pausedAt : SystemClock.uptimeMillis();
        // 四角をせまくしている途中
        boolean shrinking = false;
        if (shrinkStart >= 0 && !paused) {
            float p = Math.min(1f, (now - shrinkStart) / (float) SHRINK_MS);
            float e = 1f - (1f - p) * (1f - p) * (1f - p); // はじめ速く、最後ゆっくり
            arenaScale = scaleFrom + (scaleTo - scaleFrom) * e;
            applyArena();
            shrinking = p < 1f;
            if (!shrinking) shrinkStart = -1;
        }
        if (running && !paused) {
            float dt = Math.min(50, now - lastFrame) / 1000f;
            lastFrame = now;
            update(now, dt);
        }

        // せまくなった外がわは、赤黒いかべ
        if (arena.width() < base.width() - 1) {
            fill.setColor(0xFF3A0E12);
            canvas.drawRect(base, fill);
            fill.setColor(0x33FF5252); // しましま
            float step = 12 * dp;
            for (float x = base.left - base.height(); x < base.right; x += step * 2) {
                canvas.save();
                canvas.clipRect(base);
                canvas.drawRect(x, base.top, x + step, base.bottom, fill);
                canvas.restore();
            }
        }

        // 四角（黒い地に白いわく。理事長戦は赤いわく）
        fill.setColor(0xFF000000);
        canvas.drawRect(arena, fill);

        canvas.save();
        canvas.clipRect(arena);
        for (Shot s : shots) drawShot(canvas, s, now);
        // ハート（当たった直後は点めつ）
        boolean blink = now < guardUntil && (now / 100) % 2 == 0;
        if (!blink) {
            float hw = heartSize * heart.getWidth() / heart.getHeight();
            dst.set(heartX - hw / 2, heartY - heartSize / 2, heartX + hw / 2, heartY + heartSize / 2);
            canvas.drawBitmap(heart, null, dst, pixel);
        }
        canvas.restore();
        if (shrinking) {
            frame.setColor((now / 80) % 2 == 0 ? 0xFFFF5252 : 0xFFFFFFFF); // せまくなる間は、赤と白で点めつ
        } else {
            frame.setColor(bossFrame ? 0xFFFF5252 : 0xFFFFFFFF);
        }
        canvas.drawRect(arena, frame);

        if ((running || shrinking) && !paused) postInvalidateOnAnimation();
    }

    /** 1コマ分すすめる */
    private void update(long now, float dt) {
        long elapsed = now - waveStart;
        if (elapsed >= waveLength) {
            running = false;
            shots.clear();
            boolean noHit = !hitThisWave;
            // 描いている最中に画面の文字を変えないよう、描き終わってから知らせる
            if (listener != null) post(() -> listener.onWaveEnd(noHit));
            return;
        }

        // 新しい物を出す
        if (elapsed < waveLength - SPAWN_STOP_MS && pattern >= 0) {
            if (now >= patternNext) patternNext = now + spawnPattern(now); // 理事長戦：決まったパターン
        } else if (elapsed < waveLength - SPAWN_STOP_MS) {
            for (int i = 0; i < patterns.length; i++) {
                if (now >= nextSpawn[i]) {
                    spawn(patterns[i], now);
                    if (patterns[i] == PC) {
                        nextSpawn[i] = now + pcInterval; // パソコンは決まった間かく
                    } else {
                        nextSpawn[i] = now + (long) (interval(patterns[i]) / speed / density);
                    }
                }
            }
        }

        // 動かす・外に出た物を消す
        for (int i = shots.size() - 1; i >= 0; i--) {
            Shot s = shots.get(i);
            if (s.type >= STAMP) {
                // 校長のこうげきは、それぞれの動き方
                if (updatePrincipal(s, now, dt)) shots.remove(i);
                continue;
            }
            if (s.type == PC && now < s.dropAt) continue; // 予告中は動かない
            if (s.snipe && now < s.dashAt) {
                // 狙い撃ちの準備中：はじめはハートの方を向き続け、lockAt からは向きを固定して止まる
                if (now < s.lockAt) s.angle = (float) Math.toDegrees(Math.atan2(heartY - s.y, heartX - s.x));
                continue;
            }
            if (s.snipe && s.vx == 0 && s.vy == 0) {
                // 飛び出す！
                double a = Math.toRadians(s.angle);
                s.vx = (float) (Math.cos(a) * s.dashSpeed);
                s.vy = (float) (Math.sin(a) * s.dashSpeed);
            }
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            if (s.type == PC && !s.landed && s.y + s.h / 2 >= arena.bottom) {
                s.landed = true; // ドスン！
                if (listener != null) post(() -> listener.onImpact());
            }
            if (s.type == MOUSE && s.homingLeft > 0) {
                // ハートの方へ、少しずつ向きを変える（急には曲がれないので、よけられる）
                s.homingLeft -= dt;
                float v = (float) Math.hypot(s.vx, s.vy);
                double cur = Math.atan2(s.vy, s.vx);
                double want = Math.atan2(heartY - s.y, heartX - s.x);
                double diff = Math.atan2(Math.sin(want - cur), Math.cos(want - cur)); // -π〜π
                double maxTurn = MOUSE_TURN * dt;
                cur += Math.max(-maxTurn, Math.min(maxTurn, diff));
                s.vx = (float) (Math.cos(cur) * v);
                s.vy = (float) (Math.sin(cur) * v);
                s.angle = (float) Math.toDegrees(cur) + 90;
            }
            if (s.type == MOUSE && s.bounceLeft > 0) {
                s.bounceLeft -= dt;
                if (s.x < arena.left + s.h / 2 || s.x > arena.right - s.h / 2) s.vx = -s.vx;
                if (s.y < arena.top + s.h / 2 || s.y > arena.bottom - s.h / 2) s.vy = -s.vy;
                s.angle = (float) Math.toDegrees(Math.atan2(s.vy, s.vx)) + 90; // 進む方を向く
            }
            float margin = Math.max(s.w, s.h);
            if (now - s.born < 2500) continue; // 出てすぐの物は消さない（四角の外から入ってくる物があるため）
            if (s.x < arena.left - margin * 2 || s.x > arena.right + margin * 2
                    || s.y < arena.top - margin * 2 || s.y > arena.bottom + margin * 2) {
                shots.remove(i);
            }
        }

        // 当たったか
        if (now >= guardUntil) {
            for (Shot s : shots) {
                if (s.type == PC && now < s.dropAt) continue;
                if (s.snipe && now < s.dashAt) continue; // 狙っている間は当たらない
                if (hits(s, now)) {
                    guardUntil = now + HIT_GUARD_MS;
                    hitThisWave = true;
                    if (listener != null) post(() -> listener.onHit());
                    break;
                }
            }
        }
    }

    /** 種類ごとの、出てくる間かく（ミリ秒） */
    private long interval(int type) {
        switch (type) {
            case CHALK: return 520;
            case PEN: return 380;
            case MOUSE: return 1300;
            default: return BattleData.DODGE_PC_INTERVAL_MS; // PC（BattleData で決める）
        }
    }

    /** 物を1つ出す */
    private void spawn(int type, long now) {
        // 大きさ・速さは「元の四角」で決める（わくがせまくなっても、物は小さくならない）
        float H = base.height();
        float W = base.width();
        // 出てくる場所は「今の四角」で決める
        float AH = arena.height();
        float AW = arena.width();
        Shot s = newShot(type, now);
        s.type = type;
        switch (type) {
            case CHALK: { // 左か右のかべから、横にまっすぐ
                s.w = H * 0.18f;
                s.h = s.w * chalk.getHeight() / chalk.getWidth();
                boolean fromLeft = random.nextBoolean();
                s.x = fromLeft ? arena.left - s.w : arena.right + s.w;
                s.y = arena.top + s.h + random.nextFloat() * (AH - s.h * 2);
                s.vx = (fromLeft ? 1 : -1) * W * 0.5f * speed;
                break;
            }
            case PEN: { // 上から、まっすぐ落ちる（たてに向ける）
                s.w = H * 0.26f;
                s.h = s.w * pen.getHeight() / pen.getWidth();
                s.x = arena.left + s.h + random.nextFloat() * (AW - s.h * 2);
                s.y = arena.top - s.w;
                s.vy = H * 1.5f * speed; // 1秒で四角の高さの1.5倍進む
                s.angle = 90; // 先が下
                break;
            }
            case MOUSE: { // 上のかべのどこかから、ななめに入ってきて跳ね回る
                s.h = H * 0.16f;
                s.w = s.h * mouse.getWidth() / mouse.getHeight();
                s.x = arena.left + s.h + random.nextFloat() * (AW - s.h * 2);
                s.y = arena.top + s.h / 2;
                double a = Math.toRadians(30 + random.nextInt(120)); // 下向きのどこか
                float v = H * 0.6f * speed;
                s.vx = (float) (Math.cos(a) * v);
                s.vy = (float) (Math.sin(a) * v);
                s.bounceLeft = 3f;
                s.angle = (float) Math.toDegrees(Math.atan2(s.vy, s.vx)) + 90;
                break;
            }
            default: { // PC：ハートの真上に予告を出してから、ドスンと落ちる
                s.w = H * 0.32f;
                s.h = s.w * pc.getHeight() / pc.getWidth();
                s.x = Math.max(arena.left + s.w / 2, Math.min(arena.right - s.w / 2, heartX));
                s.y = arena.top - s.h / 2;
                s.vy = H * 1.6f * speed;
                s.dropAt = now + (long) (700 / speed);
                break;
            }
        }
        shots.add(s);
    }

    /** 校長のこうげきを1コマ分動かす。消すときは true を返す */
    private boolean updatePrincipal(Shot s, long now, float dt) {
        switch (s.type) {
            case STAMP:
                if (!s.landed && now >= s.dropAt) {
                    s.landed = true; // ドン！
                    if (listener != null) post(() -> listener.onImpact());
                }
                return now > s.dropAt + STAMP_HIT_MS + STAMP_MARK_MS;
            case RING:
                s.w += s.vx * dt;       // 広がる
                s.gapDir += s.spin * dt; // すき間が回る（第1形態は spin が0なので回らない）
                return s.w > Math.hypot(arena.width(), arena.height()) / 2 + s.h;
            case HAND:
                if (now < s.dropAt) return false;
                s.angle += s.spin * dt;
                if (now >= s.flipAt) {
                    s.spin = -s.spin; // 逆回り！
                    s.flipAt += s.flipEvery;
                }
                return false;
            case WAVE:
                s.x += s.vx * dt; // 流れる
                return false;
            case PAPER:
                if (now < s.dropAt || s.landed) return false;
                s.y += s.vy * dt; // 落ちる
                float land = paperTop[s.col] - s.h / 2;
                if (s.y >= land) {
                    s.y = land;          // 山の上に乗る
                    s.landed = true;
                    paperTop[s.col] -= s.h;
                }
                return false;
            default: // BELL：動かない
                return false;
        }
    }

    /** 音波：今の通り道の半分のはば（はじめは広く、だんだんせまくなる） */
    private float waveHalf(Shot s, long now) {
        float p = Math.min(1f, (now - s.born) / (float) WAVE_OPEN_MS);
        float open = arena.height();
        return open + (s.gapDir - open) * p;
    }

    /** 音波：横の位置 x での、通り道のまん中の高さ */
    private float waveCenter(Shot s, float x) {
        return arena.centerY() + s.h * (float) Math.sin(2 * Math.PI * (x - arena.left + s.x) / s.w);
    }

    /** ハートが物に当たっているか（物の向きに合わせて調べる） */
    private boolean hits(Shot s, long now) {
        float r = heartSize * 0.3f; // ハートの当たり判定は、見た目より少し小さく
        switch (s.type) {
            case STAMP: // 押されてから少しの間だけ
                if (now < s.dropAt || now > s.dropAt + STAMP_HIT_MS) return false;
                return Math.hypot(heartX - s.x, heartY - s.y) < s.w / 2 + r;
            case BELL:
                if (now < s.dropAt) return false;
                return Math.hypot(heartX - s.x, heartY - s.y) < s.w / 2 + r;
            case RING: {
                double dist = Math.hypot(heartX - s.x, heartY - s.y);
                if (Math.abs(dist - s.w) > s.h / 2 + r) return false; // 輪の上にいない
                double deg = Math.toDegrees(Math.atan2(heartY - s.y, heartX - s.x));
                // すき間は向かい合わせに2つ（180度ちがい）なので、近い方のすき間との差（0〜90度）
                double diff = Math.abs(((deg - s.gapDir) % 180 + 270) % 180 - 90);
                double margin = Math.toDegrees(r / Math.max(1, dist)); // ハートの大きさの分
                return diff > RING_GAP_DEG / 2 - margin;
            }
            case HAND: {
                if (now < s.dropAt) return false;
                double a = Math.toRadians(s.angle);
                float dx = heartX - s.x;
                float dy = heartY - s.y;
                double along = dx * Math.cos(a) + dy * Math.sin(a);     // 針にそった長さ
                double side = -dx * Math.sin(a) + dy * Math.cos(a);     // 針からのはなれ
                return along > -s.h && along < s.w && Math.abs(side) < s.h / 2 + r;
            }
            case WAVE:
                return Math.abs(heartY - waveCenter(s, heartX)) > waveHalf(s, now) - r;
            case PAPER: // 落ちている書類も、積もった書類も当たる
                if (now < s.dropAt) return false;
                return Math.abs(heartX - s.x) < s.w / 2 + r && Math.abs(heartY - s.y) < s.h / 2 + r;
        }
        double rad = Math.toRadians(s.angle);
        float dx = heartX - s.x;
        float dy = heartY - s.y;
        float lx = (float) (dx * Math.cos(rad) + dy * Math.sin(rad));
        float ly = (float) (-dx * Math.sin(rad) + dy * Math.cos(rad));
        return Math.abs(lx) < s.w * 0.45f + r && Math.abs(ly) < s.h * 0.45f + r;
    }

    /** 物を1つ描く */
    private void drawShot(Canvas canvas, Shot s, long now) {
        if (s.type >= STAMP) {
            drawPrincipal(canvas, s, now);
            return;
        }
        if (s.snipe && now < s.dashAt) {
            // 狙い撃ちの予告線：狙っている間はうすく、向きを固定したら点めつ
            boolean locked = now >= s.lockAt;
            aim.setColor(locked ? ((now / 70) % 2 == 0 ? 0xCCFF3030 : 0x44FF3030) : 0x55FF3030);
            double a = Math.toRadians(s.angle);
            float len = base.width() + base.height();
            canvas.drawLine(s.x, s.y, s.x + (float) Math.cos(a) * len, s.y + (float) Math.sin(a) * len, aim);
        }
        if (s.type == PC && now < s.dropAt) {
            // 落ちてくる場所を、赤く点めつして予告する
            fill.setColor((now / 120) % 2 == 0 ? 0x55FF3030 : 0x22FF3030);
            canvas.drawRect(s.x - s.w / 2, arena.top, s.x + s.w / 2, arena.bottom, fill);
            return;
        }
        Bitmap b;
        switch (s.type) {
            case CHALK: b = chalk; break;
            case PEN: b = pen; break;
            case MOUSE: b = mouse; break;
            default: b = pc; break;
        }
        canvas.save();
        canvas.translate(s.x, s.y);
        canvas.rotate(s.angle);
        dst.set(-s.w / 2, -s.h / 2, s.w / 2, s.h / 2);
        canvas.drawBitmap(b, null, dst, pixel);
        canvas.restore();
    }

    /** 校長のこうげきを描く */
    private void drawPrincipal(Canvas canvas, Shot s, long now) {
        switch (s.type) {
            case STAMP: {
                float r = s.w / 2;
                if (now < s.dropAt) {
                    // 予告：赤い丸が点めつ。押される直前は、上からハンコが下りてくる
                    soft.setColor((now / 110) % 2 == 0 ? 0x55FF3030 : 0x26FF3030);
                    canvas.drawCircle(s.x, s.y, r, soft);
                    line.setStrokeWidth(2 * dp);
                    line.setColor(0xCCFF5050);
                    canvas.drawCircle(s.x, s.y, r, line);
                    long left = s.dropAt - now;
                    if (left < 220) {
                        float lift = r * 3f * left / 220f;
                        drawStampBody(canvas, s.x, s.y - lift, r);
                    }
                } else {
                    // 押したあと：赤いハンコのあと（だんだん消える）
                    float fade = 1f - Math.min(1f, (now - s.dropAt) / (float) (STAMP_HIT_MS + STAMP_MARK_MS));
                    int alpha = (int) (255 * fade);
                    line.setStrokeWidth(r * 0.14f);
                    line.setColor((alpha << 24) | 0xE53935);
                    canvas.drawCircle(s.x, s.y, r * 0.88f, line);
                    text.setColor((alpha << 24) | 0xE53935);
                    text.setTextSize(r * 0.62f);
                    canvas.drawText("校長", s.x, s.y + r * 0.22f, text);
                    if (now < s.dropAt + STAMP_HIT_MS) drawStampBody(canvas, s.x, s.y, r);
                }
                break;
            }
            case BELL: {
                // 金色の鐘。さわると痛くなるまでは点めつ
                if (now < s.dropAt && (now / 100) % 2 == 0) break;
                float r = s.w / 2;
                soft.setColor(0xFFFFC107);
                canvas.drawCircle(s.x, s.y, r, soft);
                soft.setColor(0xFFFFE082);
                canvas.drawCircle(s.x - r * 0.3f, s.y - r * 0.3f, r * 0.35f, soft);
                soft.setColor(0xFF8D6E00);
                canvas.drawCircle(s.x, s.y + r * 0.55f, r * 0.22f, soft); // 鐘の玉
                break;
            }
            case RING: {
                // 黄色い音の輪（向かい合わせの2つのすき間の所は描かない）
                line.setStrokeWidth(s.h);
                line.setColor(0xFFFFE082);
                dst.set(s.x - s.w, s.y - s.w, s.x + s.w, s.y + s.w);
                canvas.drawArc(dst, s.gapDir + RING_GAP_DEG / 2, 180 - RING_GAP_DEG, false, line);
                canvas.drawArc(dst, s.gapDir + 180 + RING_GAP_DEG / 2, 180 - RING_GAP_DEG, false, line);
                break;
            }
            case HAND: {
                if (s.face) {
                    // 時計の文字ばん（うすく。当たらない）
                    float fr = Math.min(arena.width(), arena.height()) / 2 * 0.92f;
                    line.setStrokeWidth(2 * dp);
                    line.setColor(0x44FFFFFF);
                    canvas.drawCircle(s.x, s.y, fr, line);
                    line.setStrokeWidth(3 * dp);
                    for (int k = 0; k < 12; k++) {
                        double a = Math.toRadians(k * 30);
                        canvas.drawLine(s.x + (float) Math.cos(a) * fr * 0.88f, s.y + (float) Math.sin(a) * fr * 0.88f,
                                s.x + (float) Math.cos(a) * fr, s.y + (float) Math.sin(a) * fr, line);
                    }
                }
                // 針：出てすぐはうすく点めつ。逆回りの前は赤く点めつ
                int color = 0xFFFFFFFF;
                if (now < s.dropAt) color = (now / 100) % 2 == 0 ? 0x66FFFFFF : 0x22FFFFFF;
                else if (now > s.flipAt - FLIP_WARN_MS) color = (now / 80) % 2 == 0 ? 0xFFFF5252 : 0xFFFFFFFF;
                line.setStrokeWidth(s.h);
                line.setColor(color);
                double a = Math.toRadians(s.angle);
                canvas.drawLine(s.x, s.y, s.x + (float) Math.cos(a) * s.w, s.y + (float) Math.sin(a) * s.w, line);
                soft.setColor(0xFFFFD54F);
                canvas.drawCircle(s.x, s.y, heartSize * 0.4f, soft); // まん中の軸
                break;
            }
            case PAPER: {
                if (now < s.dropAt) {
                    // 予告：落ちてくる列が、うすく点めつ
                    float colW = arena.width() / paperCols;
                    float cx = arena.left + (s.col + 0.5f) * colW;
                    soft.setColor((now / 100) % 2 == 0 ? 0x33FFFFFF : 0x14FFFFFF);
                    canvas.drawRect(cx - colW / 2, arena.top, cx + colW / 2, paperTop[s.col], soft);
                    break;
                }
                // 白い紙に、文字の線とふち（積もった紙は少し暗く）
                float l = s.x - s.w / 2, t = s.y - s.h / 2, rr = s.x + s.w / 2, b = s.y + s.h / 2;
                soft.setColor(s.landed ? 0xFFDCD8CC : 0xFFFAF8F0);
                canvas.drawRect(l, t, rr, b, soft);
                line.setStrokeWidth(Math.max(1, s.h * 0.08f));
                line.setColor(0xFF9E9A90);
                for (int k = 1; k <= 3; k++) {
                    float y = t + s.h * k / 4f;
                    canvas.drawLine(l + s.w * 0.12f, y, rr - s.w * (k == 3 ? 0.35f : 0.12f), y, line);
                }
                line.setColor(0xFF6D6A62);
                canvas.drawRect(l, t, rr, b, line);
                break;
            }
            case WAVE: {
                // 通り道の上と下を、水色の音波でぬる
                float half = waveHalf(s, now);
                float step = 4 * dp;
                soft.setColor(0x5540C4FF);
                for (int side = -1; side <= 1; side += 2) {
                    path.reset();
                    path.moveTo(arena.left, side < 0 ? arena.top : arena.bottom);
                    for (float x = arena.left; x <= arena.right + step; x += step) {
                        path.lineTo(x, waveCenter(s, x) + side * half);
                    }
                    path.lineTo(arena.right + step, side < 0 ? arena.top : arena.bottom);
                    path.close();
                    canvas.drawPath(path, soft);
                }
                // ふちの波線
                line.setStrokeWidth(3 * dp);
                line.setColor(0xFF80D8FF);
                for (int side = -1; side <= 1; side += 2) {
                    path.reset();
                    for (float x = arena.left; x <= arena.right + step; x += step) {
                        float y = waveCenter(s, x) + side * half;
                        if (x == arena.left) path.moveTo(x, y); else path.lineTo(x, y);
                    }
                    canvas.drawPath(path, line);
                }
                break;
            }
        }
    }

    /** ハンコの本体（茶色い持ち手＋赤い印面） */
    private void drawStampBody(Canvas canvas, float x, float bottom, float r) {
        soft.setColor(0xFF8D5A3B);
        canvas.drawRect(x - r * 0.45f, bottom - r * 2.2f, x + r * 0.45f, bottom - r * 0.25f, soft);
        soft.setColor(0xFFA86F4C);
        canvas.drawRect(x - r * 0.45f, bottom - r * 2.2f, x - r * 0.2f, bottom - r * 0.25f, soft);
        soft.setColor(0xFFC62828);
        canvas.drawRect(x - r * 0.6f, bottom - r * 0.3f, x + r * 0.6f, bottom, soft);
    }

    // ===================== 指でなぞる =====================

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (paused) return true; // 止めている間は、ハートを動かさない
                // 指が動いた分だけ、ハートを動かす（四角の外には出ない）
                float half = heartSize / 2;
                heartX = Math.max(arena.left + half, Math.min(arena.right - half, heartX + x - lastTouchX));
                heartY = Math.max(arena.top + half, Math.min(arena.bottom - half, heartY + y - lastTouchY));
                lastTouchX = x;
                lastTouchY = y;
                if (!running) invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                performClick(); // 画面の読み上げ機能などのため
                return true;
            case MotionEvent.ACTION_CANCEL:
                return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}

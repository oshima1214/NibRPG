package com.example.nibrpg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
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
 * 先生のこうげき（チョーク・ペン・マウス・パソコン）が飛んでくるので、当たらないようによける。
 */
public class DodgeView extends View {

    /** こうげきの種類 */
    public static final int CHALK = 0; // チョーク：左右から横に飛んでくる
    public static final int PEN = 1;   // ペン：上から落ちてくる
    public static final int MOUSE = 2; // マウス：かべで跳ね返りながら動き回る
    public static final int PC = 3;    // パソコン：ハートをねらって上から落ちる（赤い予告あり）

    /** 理事長戦の攻撃パターン（ランダムではなく、決まった形で飛んでくる） */
    public static final int P_CHALK_WALL = 0;   // チョークの壁：1か所だけすき間がある列が、左右から交互に来る
    public static final int P_PEN_SNIPE = 1;    // ペンの狙い撃ち：わくのはしでハートに向きを合わせ、少し止まってから一直線に飛んでくる
    public static final int P_MOUSE_HOMING = 2; // マウスの追いかけ：ハートをねらって曲がりながら追いかけてくる
    public static final int P_PC_ROW = 3;       // パソコンの列：わくのはしからはしまで降る。1列だけ逃げ道がある
    public static final int PATTERN_COUNT = 4;

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
    }

    private static final long HIT_GUARD_MS = 1000; // 当たったあと、無敵になる時間
    private static final long SPAWN_STOP_MS = 700; // こうげきの終わり近くは、新しい物を出さない

    private final Random random = new Random();
    private final Paint pixel = new Paint();                       // ドット絵（ぼかさない）
    private final Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);  // 四角のわく
    private final Paint fill = new Paint();
    private final Paint aim = new Paint(Paint.ANTI_ALIAS_FLAG);    // 狙い撃ちの予告線
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

    public DodgeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        pixel.setFilterBitmap(false);
        frame.setStyle(Paint.Style.STROKE);
        frame.setStrokeWidth(3 * dp);
        frame.setColor(0xFFFFFFFF);
        aim.setStyle(Paint.Style.STROKE);
        aim.setStrokeWidth(2 * dp);
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

    private Shot newShot(int type, long now) {
        Shot s = new Shot();
        s.type = type;
        s.born = now;
        return s;
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
        long now = SystemClock.uptimeMillis();
        // 四角をせまくしている途中
        boolean shrinking = false;
        if (shrinkStart >= 0) {
            float p = Math.min(1f, (now - shrinkStart) / (float) SHRINK_MS);
            float e = 1f - (1f - p) * (1f - p) * (1f - p); // はじめ速く、最後ゆっくり
            arenaScale = scaleFrom + (scaleTo - scaleFrom) * e;
            applyArena();
            shrinking = p < 1f;
            if (!shrinking) shrinkStart = -1;
        }
        if (running) {
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

        if (running || shrinking) postInvalidateOnAnimation();
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
                if (hits(s)) {
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

    /** ハートが物に当たっているか（物の向きに合わせて調べる） */
    private boolean hits(Shot s) {
        float r = heartSize * 0.3f; // ハートの当たり判定は、見た目より少し小さく
        double rad = Math.toRadians(s.angle);
        float dx = heartX - s.x;
        float dy = heartY - s.y;
        float lx = (float) (dx * Math.cos(rad) + dy * Math.sin(rad));
        float ly = (float) (-dx * Math.sin(rad) + dy * Math.cos(rad));
        return Math.abs(lx) < s.w * 0.45f + r && Math.abs(ly) < s.h * 0.45f + r;
    }

    /** 物を1つ描く */
    private void drawShot(Canvas canvas, Shot s, long now) {
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

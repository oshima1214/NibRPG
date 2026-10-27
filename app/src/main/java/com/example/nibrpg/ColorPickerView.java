package com.example.nibrpg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 色を選ぶ部品（Windowsの「色の編集」に近い形）。
 *   左の四角：横が「あざやかさ」、縦が「明るさ」
 *   右の細い棒：「色合い」（赤→黄→緑→水色→青→紫→赤）
 * 指でなぞると色が変わり、OnColorChangeListener に知らせる。
 */
public class ColorPickerView extends View {

    /** 色が変わったときに呼ばれる */
    public interface OnColorChangeListener {
        void onColorChanged(int color);
    }

    // 色を「色合い・あざやかさ・明るさ」の3つの数で持つ（HSV：Hue 色合い、Saturation あざやかさ、Value 明るさ）
    private final float[] hsv = { 0f, 0f, 0f };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF svRect = new RectF();  // 左の四角
    private final RectF hueRect = new RectF(); // 右の棒
    private final float dp;
    private int touching = 0; // 0：さわっていない、1：四角、2：棒
    private OnColorChangeListener listener;

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        ring.setStyle(Paint.Style.STROKE);
    }

    public void setOnColorChangeListener(OnColorChangeListener l) {
        listener = l;
    }

    /** 外から色をセットする（このときは listener を呼ばない） */
    public void setColor(int color) {
        float[] next = new float[3];
        Color.colorToHSV(color, next);
        // 白・黒・灰色は「色合い」がないので、今の色合いのままにしておく
        if (next[1] > 0f && next[2] > 0f) hsv[0] = next[0];
        hsv[1] = next[1];
        hsv[2] = next[2];
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(hsv);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float pad = 8 * dp;   // まわりのすき間（丸い目印がはみ出すため）
        float gap = 12 * dp;  // 四角と棒のすき間
        float bar = 24 * dp;  // 棒の太さ
        float size = Math.min(h - pad * 2, w - pad * 2 - gap - bar);
        float left = (w - size - gap - bar) / 2f;
        float top = (h - size) / 2f;
        svRect.set(left, top, left + size, top + size);
        hueRect.set(svRect.right + gap, top, svRect.right + gap + bar, top + size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (svRect.width() <= 0) return;

        // ---- 左の四角 ----
        int pure = Color.HSVToColor(new float[] { hsv[0], 1f, 1f }); // 今の色合いの一番あざやかな色
        // 横：白 → あざやかな色
        paint.setShader(new LinearGradient(svRect.left, 0, svRect.right, 0,
                0xFFFFFFFF, pure, Shader.TileMode.CLAMP));
        canvas.drawRect(svRect, paint);
        // 縦：透明 → 黒（下ほど暗くする）
        paint.setShader(new LinearGradient(0, svRect.top, 0, svRect.bottom,
                0x00000000, 0xFF000000, Shader.TileMode.CLAMP));
        canvas.drawRect(svRect, paint);

        // ---- 右の棒 ----
        paint.setShader(new LinearGradient(0, hueRect.top, 0, hueRect.bottom,
                new int[] { 0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF,
                        0xFF0000FF, 0xFFFF00FF, 0xFFFF0000 },
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(hueRect, paint);
        paint.setShader(null);

        // ---- 目印 ----
        // 四角：今の色の場所に丸
        float x = svRect.left + hsv[1] * svRect.width();
        float y = svRect.top + (1f - hsv[2]) * svRect.height();
        drawRing(canvas, x, y, 7 * dp);
        // 棒：今の色合いの場所に横線
        float hy = hueRect.top + hsv[0] / 360f * hueRect.height();
        ring.setStrokeWidth(5 * dp);
        ring.setColor(0xFF000000);
        canvas.drawRect(hueRect.left - 3 * dp, hy - 3 * dp, hueRect.right + 3 * dp, hy + 3 * dp, ring);
        ring.setStrokeWidth(2 * dp);
        ring.setColor(0xFFFFFFFF);
        canvas.drawRect(hueRect.left - 3 * dp, hy - 3 * dp, hueRect.right + 3 * dp, hy + 3 * dp, ring);
    }

    /** 白い丸（黒いふち付き：明るい色の上でも暗い色の上でも見える） */
    private void drawRing(Canvas canvas, float x, float y, float r) {
        ring.setStrokeWidth(4 * dp);
        ring.setColor(0xFF000000);
        canvas.drawCircle(x, y, r, ring);
        ring.setStrokeWidth(2 * dp);
        ring.setColor(0xFFFFFFFF);
        canvas.drawCircle(x, y, r, ring);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 棒の近くなら棒、それ以外は四角
                touching = (x > (svRect.right + hueRect.left) / 2f) ? 2 : 1;
                getParent().requestDisallowInterceptTouchEvent(true);
                update(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                update(x, y);
                return true;
            case MotionEvent.ACTION_UP:
                touching = 0;
                performClick(); // 画面の読み上げ機能などのため
                return true;
            case MotionEvent.ACTION_CANCEL:
                touching = 0;
                return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** さわった場所から色を決める */
    private void update(float x, float y) {
        if (touching == 1) {
            hsv[1] = clamp((x - svRect.left) / svRect.width());
            hsv[2] = 1f - clamp((y - svRect.top) / svRect.height());
        } else if (touching == 2) {
            hsv[0] = clamp((y - hueRect.top) / hueRect.height()) * 359.9f;
        } else {
            return;
        }
        invalidate();
        if (listener != null) listener.onColorChanged(getColor());
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}

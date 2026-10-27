package com.example.nibrpg;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

/**
 * 主人公の絵を作る係。
 * ひな形の絵（hero_〇〇.png）では、髪・目・服が「目印の色」でぬってある。
 * その目印の色を、キャラクリで選んだ色（GameState の hairColor など）に置きかえて使う。
 * 影の色や明るい色は、選んだ色から自動で作る。
 */
public class HeroSprite {

    // 向き（GameState.dir に入れる数字）
    public static final int DOWN = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int UP = 3;

    // ひな形の絵 [向き][コマ]（コマ 0＝立っている、1＝歩いている）
    private static final int[][] TEMPLATES = {
        { R.drawable.hero_down_0, R.drawable.hero_down_1 },
        { R.drawable.hero_left_0, R.drawable.hero_left_1 },
        { R.drawable.hero_right_0, R.drawable.hero_right_1 },
        { R.drawable.hero_up_0, R.drawable.hero_up_1 },
    };

    // 目印の色（ひな形の中でだけ使っている、ふつうは使わない色）
    private static final int KEY_HAIR = 0xFFFF0000;        // 髪
    private static final int KEY_HAIR_LIGHT = 0xFFFF0040;  // 髪のつや
    private static final int KEY_HAIR_DARK = 0xFFFF0080;   // 髪の影
    private static final int KEY_EYE = 0xFF00FF00;         // 目
    private static final int KEY_CLOTH = 0xFF0000FF;       // 服
    private static final int KEY_CLOTH_SHADE = 0xFF4000FF; // 服の影
    private static final int KEY_CLOTH_DARK = 0xFF8000FF;  // 服のすそ

    private static Bitmap[][] templates; // 読みこんだひな形（1回だけ読む）

    // マップ用に作った絵をとっておく（色が変わったときだけ作り直す）
    private static Bitmap[][] cache;
    private static int cachedHair, cachedEye, cachedCloth;

    /**
     * 今の色（GameState）で、マップに描く主人公の絵をもらう。
     * step は歩きのコマ：0 立つ → 1 歩く → 2 立つ → 3 反対の足で歩く → 0 …
     */
    public static Bitmap get(Resources res, int dir, int step) {
        if (cache == null || cachedHair != GameState.hairColor
                || cachedEye != GameState.eyeColor || cachedCloth != GameState.clothColor) {
            cache = makeSet(res, GameState.hairColor, GameState.eyeColor, GameState.clothColor);
            cachedHair = GameState.hairColor;
            cachedEye = GameState.eyeColor;
            cachedCloth = GameState.clothColor;
        }
        return pick(cache, dir, step);
    }

    /** 作った絵のセットから、向きと歩きのコマに合う1枚を選ぶ */
    public static Bitmap pick(Bitmap[][] set, int dir, int step) {
        if (step == 1) return set[dir][1];
        if (step == 3) return set[dir][2];
        return set[dir][0];
    }

    /**
     * 好きな色で、全部の向き・コマの絵を作る（キャラクリ画面の見本にも使う）。
     * [向き][0＝立つ, 1＝歩く, 2＝反対の足で歩く]
     */
    public static Bitmap[][] makeSet(Resources res, int hair, int eye, int cloth) {
        if (templates == null) {
            templates = new Bitmap[TEMPLATES.length][2];
            for (int d = 0; d < TEMPLATES.length; d++) {
                for (int f = 0; f < 2; f++) templates[d][f] = load(res, TEMPLATES[d][f]);
            }
        }
        Bitmap[][] set = new Bitmap[TEMPLATES.length][3];
        for (int d = 0; d < TEMPLATES.length; d++) {
            set[d][0] = recolor(templates[d][0], hair, eye, cloth);
            set[d][1] = recolor(templates[d][1], hair, eye, cloth);
            // 前向き・後ろ向きは、歩く絵を左右反転すると「反対の足」になる。
            // 横向きは反転すると向きが変わってしまうので、同じ絵を使う。
            set[d][2] = (d == DOWN || d == UP) ? flip(set[d][1]) : set[d][1];
        }
        return set;
    }

    /** ひな形を、大きさを変えずに（ぼかさずに）読みこむ */
    private static Bitmap load(Resources res, int id) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        return BitmapFactory.decodeResource(res, id, o);
    }

    /** ひな形をコピーして、目印の色を選んだ色に置きかえる */
    private static Bitmap recolor(Bitmap template, int hair, int eye, int cloth) {
        Bitmap b = template.copy(Bitmap.Config.ARGB_8888, true);
        int w = b.getWidth();
        int h = b.getHeight();
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        int hairLight = mix(hair, 0xFFFFFFFF, 0.3f);
        int hairDark = mix(hair, 0xFF000000, 0.35f);
        int clothShade = mix(cloth, 0xFF000000, 0.2f);
        int clothDark = mix(cloth, 0xFF000000, 0.35f);
        for (int i = 0; i < px.length; i++) {
            switch (px[i]) {
                case KEY_HAIR: px[i] = hair; break;
                case KEY_HAIR_LIGHT: px[i] = hairLight; break;
                case KEY_HAIR_DARK: px[i] = hairDark; break;
                case KEY_EYE: px[i] = eye; break;
                case KEY_CLOTH: px[i] = cloth; break;
                case KEY_CLOTH_SHADE: px[i] = clothShade; break;
                case KEY_CLOTH_DARK: px[i] = clothDark; break;
            }
        }
        b.setPixels(px, 0, w, 0, 0, w, h);
        return b;
    }

    /** 絵を左右反転する */
    private static Bitmap flip(Bitmap b) {
        Matrix m = new Matrix();
        m.preScale(-1, 1);
        return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, false);
    }

    /** 2つの色をまぜる（rate = 0 なら a のまま、1 なら b） */
    private static int mix(int a, int b, float rate) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - rate) + ((b >> 16) & 0xFF) * rate);
        int g = (int) (((a >> 8) & 0xFF) * (1 - rate) + ((b >> 8) & 0xFF) * rate);
        int bl = (int) ((a & 0xFF) * (1 - rate) + (b & 0xFF) * rate);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}

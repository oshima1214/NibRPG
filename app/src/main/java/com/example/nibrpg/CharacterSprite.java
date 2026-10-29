package com.example.nibrpg;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class CharacterSprite {

    public String name = "プレイヤー";

    public int hair = Color.rgb(70, 44, 35);
    public int eyes = Color.rgb(48, 80, 112);
    public int skin = Color.rgb(237, 191, 145);
    public int shirt = Color.rgb(50, 112, 185);
    public int pants = Color.rgb(49, 62, 84);
    public int shoes = Color.rgb(42, 43, 51);

    // 髪型：0 ショート、1 ボブ、2 ロング、
    // 3 センター分け、4 ポニーテール
    public int hairStyle = 0;

    // 上着：0 Tシャツ、1 パーカー、2 ジャケット
    public int shirtStyle = 0;

    // ボトムス：0 ズボン、1 スカート
    public int pantsStyle = 0;


    // アクセサリー：0は装着なし
    // 眼鏡：1=眼鏡、2=サングラス
    public int glassesStyle = 0;

    // ピアス：1=スタッド、2=リング
    public int earringsStyle = 0;

    // リボン：1=左、2=右、3=中央
    public int ribbonStyle = 0;

    // キャップ：1=前向き、2=後ろ向き
    public int capStyle = 0;

    // ネックレス：1=ペンダント、2=チェーン
    public int necklaceStyle = 0;

    // アクセサリーの色
    public int glassesColor = 0xFF31343C;
    public int earringsColor = 0xFFE4C36A;
    public int ribbonColor = 0xFFE26786;
    public int capColor = 0xFF557BA8;
    public int necklaceColor = 0xFFE4C36A;


    // 絵の大きさ（ドット）：横18・縦48。先生と同じくらいの等身
    public static final int WIDTH = 18;
    public static final int HEIGHT = 48;

    // 向き：0 正面、1 右、2 後ろ、3 左
    public static final int FRONT = 0;
    public static final int RIGHT = 1;
    public static final int BACK = 2;
    public static final int LEFT = 3;

    private final Paint paint = new Paint();


    private final Paint glassesPaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    // 眼鏡専用の細い線
    private void thinLine(
            Canvas c,
            float x1, float y1,
            float x2, float y2,
            int color) {

        glassesPaint.setColor(color);
        glassesPaint.setStrokeWidth(0.5f);
        glassesPaint.setStrokeCap(Paint.Cap.BUTT);

        c.drawLine(x1, y1, x2, y2, glassesPaint);
    }


    public CharacterSprite() {
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
    }

    // 保存
    public void save(Context context) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences(
                        "character", Context.MODE_PRIVATE
                ).edit();

        editor.putString("name", name);
        editor.putInt("hair", hair);
        editor.putInt("eyes", eyes);
        editor.putInt("skin", skin);
        editor.putInt("shirt", shirt);
        editor.putInt("pants", pants);
        editor.putInt("shoes", shoes);

        editor.putInt("style", hairStyle);
        editor.putInt("shirtStyle", shirtStyle);
        editor.putInt("pantsStyle", pantsStyle);


        editor.putInt("glassesStyle", glassesStyle);
        editor.putInt("earringsStyle", earringsStyle);
        editor.putInt("ribbonStyle", ribbonStyle);
        editor.putInt("capStyle", capStyle);
        editor.putInt("necklaceStyle", necklaceStyle);

        editor.putInt("glassesColor", glassesColor);
        editor.putInt("earringsColor", earringsColor);
        editor.putInt("ribbonColor", ribbonColor);
        editor.putInt("capColor", capColor);
        editor.putInt("necklaceColor", necklaceColor);


        editor.apply();
    }

    // 読み込み
    public static CharacterSprite load(Context context) {
        CharacterSprite sprite = new CharacterSprite();

        SharedPreferences pref =
                context.getSharedPreferences(
                        "character", Context.MODE_PRIVATE
                );

        sprite.name = pref.getString(
                "name", sprite.name);

        sprite.hair = pref.getInt(
                "hair", sprite.hair);

        sprite.eyes = pref.getInt(
                "eyes", sprite.eyes);

        sprite.skin = pref.getInt(
                "skin", sprite.skin);

        sprite.shirt = pref.getInt(
                "shirt", sprite.shirt);

        sprite.pants = pref.getInt(
                "pants", sprite.pants);

        sprite.shoes = pref.getInt(
                "shoes", sprite.shoes);

        sprite.hairStyle = pref.getInt("style", 0);
        sprite.shirtStyle = pref.getInt("shirtStyle", 0);
        sprite.pantsStyle = pref.getInt("pantsStyle", 0);

        // 髪型を0～4の5種類に対応
        sprite.hairStyle =
                Math.max(0, Math.min(4, sprite.hairStyle));

        sprite.shirtStyle =
                Math.max(0, Math.min(2, sprite.shirtStyle));

        sprite.pantsStyle =
                Math.max(0, Math.min(1, sprite.pantsStyle));


        sprite.glassesStyle =
                Math.max(0, Math.min(2,
                        pref.getInt("glassesStyle", 0)));

        sprite.earringsStyle =
                Math.max(0, Math.min(2,
                        pref.getInt("earringsStyle", 0)));

        sprite.ribbonStyle =
                Math.max(0, Math.min(3,
                        pref.getInt("ribbonStyle", 0)));

        sprite.capStyle =
                Math.max(0, Math.min(2,
                        pref.getInt("capStyle", 0)));

        sprite.necklaceStyle =
                Math.max(0, Math.min(2,
                        pref.getInt("necklaceStyle", 0)));

        sprite.glassesColor =
                pref.getInt("glassesColor", sprite.glassesColor);

        sprite.earringsColor =
                pref.getInt("earringsColor", sprite.earringsColor);

        sprite.ribbonColor =
                pref.getInt("ribbonColor", sprite.ribbonColor);

        sprite.capColor =
                pref.getInt("capColor", sprite.capColor);

        sprite.necklaceColor =
                pref.getInt("necklaceColor", sprite.necklaceColor);


        return sprite;
    }

    // 色を少し暗くする
    private int dark(int color) {
        return dark(color, 0.75f);
    }

    // 色を k 倍に暗くする
    private int dark(int color, float k) {
        return Color.rgb(
                (int) (Color.red(color) * k),
                (int) (Color.green(color) * k),
                (int) (Color.blue(color) * k)
        );
    }

    // 髪のつや用に、色を明るくする
    private int light(int color) {
        return Color.rgb(
                Math.min(255, (int) (Color.red(color) * 1.35f + 20)),
                Math.min(255, (int) (Color.green(color) * 1.35f + 20)),
                Math.min(255, (int) (Color.blue(color) * 1.35f + 20))
        );
    }

    // ドットを描く（x2・y2 はふくまない）
    private void rect(
            Canvas c,
            int x1, int y1,
            int x2, int y2,
            int color) {

        paint.setColor(color);
        c.drawRect(x1, y1, x2, y2, paint);
    }

    private static final int SHADOW = 0x66505F68;          // 地面の影
    private static final int SOLE = Color.LTGRAY;          // くつの底
    private static final int EYE_DARK = Color.rgb(35, 39, 50);
    private static final int INNER = Color.rgb(224, 227, 230); // ジャケットの中の服

    // 以前の呼び出しは静止ポーズを描画する（キャラクリもそのまま動く）
    public void draw(
            Canvas canvas,
            float x, float y,
            float scale,
            int direction) {
        draw(canvas, x, y, scale, direction, 0);
    }

    // walkFrame: 0=直立, 1=左足を出す, 2=右足を出す
    public void draw(
            Canvas canvas,
            float x, float y,
            float scale,
            int direction,
            int walkFrame) {

        int frame = (walkFrame == 1 || walkFrame == 2) ? walkFrame : 0;
        int dir = ((direction % 4) + 4) % 4;

        canvas.save();
        canvas.translate(x, y);
        canvas.scale(scale, scale);

        if (dir == RIGHT || dir == LEFT) {
            // 左向きは、右向きの絵を左右反転する
            if (dir == LEFT) {
                canvas.translate(WIDTH, 0f);
                canvas.scale(-1f, 1f);
            }
            drawSide(canvas, frame);
        } else {
            drawFrontBack(canvas, dir == BACK, frame);
        }
        canvas.restore();
    }

    // 正面・後ろ向き
    private void drawFrontBack(Canvas c, boolean back, int frame) {
        // 足を交互に1ドットずらす（腕は足と逆）
        int lf = frame == 1 ? -1 : (frame == 2 ? 1 : 0);
        int rf = -lf;

        rect(c, 3, 46, 15, 47, SHADOW);

        // ズボン・スカート
        if (pantsStyle == 0) {
            rect(c, 5, 29, 9, 44 + lf, pants);
            rect(c, 9, 29, 13, 44 + rf, dark(pants));
        } else {
            rect(c, 5, 35, 8, 44 + lf, skin);
            rect(c, 10, 35, 13, 44 + rf, skin);
            rect(c, 4, 29, 14, 35, pants);
            rect(c, 3, 32, 15, 35, pants);
            rect(c, 4, 34, 14, 35, dark(pants));
        }

        // くつ
        rect(c, 4, 42 + lf, 9, 46 + lf, shoes);
        rect(c, 9, 42 + rf, 14, 46 + rf, shoes);
        rect(c, 4, 45 + lf, 9, 46 + lf, SOLE);
        rect(c, 9, 45 + rf, 14, 46 + rf, SOLE);

        // 上着
        rect(c, 4, 15, 14, 30, shirt);
        rect(c, 11, 16, 13, 29, dark(shirt));
        if (shirtStyle == 0) {
            // Tシャツ：半そで
            rect(c, 2, 16, 4, 21, shirt);
            rect(c, 14, 16, 16, 21, shirt);
            rect(c, 2, 21, 4, 28 - lf, skin);
            rect(c, 14, 21, 16, 28 - rf, skin);
        } else {
            // パーカー・ジャケット：長そで
            rect(c, 2, 16, 4, 27 - lf, shirt);
            rect(c, 14, 16, 16, 27 - rf, shirt);
            rect(c, 2, 27 - lf, 4, 29 - lf, skin);
            rect(c, 14, 27 - rf, 16, 29 - rf, skin);
        }
        // ジャケットの前開き
        if (shirtStyle == 2 && !back) {
            rect(c, 8, 16, 10, 29, INNER);
            rect(c, 7, 16, 8, 21, dark(shirt));
            rect(c, 10, 16, 11, 21, dark(shirt));
        }
        // パーカー：前はひも、後ろはフード
        if (shirtStyle == 1) {
            if (back) {
                rect(c, 5, 15, 13, 20, dark(shirt));
                rect(c, 6, 16, 12, 19, shirt);
            } else {
                rect(c, 5, 15, 13, 16, dark(shirt));
                rect(c, 7, 16, 8, 20, Color.WHITE);
                rect(c, 10, 16, 11, 20, Color.WHITE);
            }
        }

        // 首
        rect(c, 8, 13, 10, 16, skin);

        // 頭（下の角を丸く）
        rect(c, 4, 3, 14, 13, skin);
        rect(c, 5, 13, 13, 14, skin);

        // 頭の上の髪（上の角を丸く）
        rect(c, 4, 1, 14, 3, hair);
        rect(c, 3, 3, 15, 5, hair);

        if (back) {
            if (hairStyle == 1 || hairStyle == 2) {
                // ボブ・ロング：後ろはまるく下ろす
                rect(c, 3, 5, 15, 13, hair);
                rect(c, 4, 13, 14, 14, dark(hair, 0.9f));
                if (hairStyle == 2) {
                    rect(c, 3, 13, 15, 23, hair);
                    rect(c, 4, 20, 14, 23, dark(hair, 0.9f));
                    rect(c, 4, 23, 14, 24, dark(hair, 0.9f));
                }
            } else {
                // 短い髪：えり足に向かってくびれる
                rect(c, 3, 5, 15, 9, hair);
                rect(c, 4, 9, 14, 10, hair);
                rect(c, 5, 10, 13, 11, dark(hair, 0.9f));
                rect(c, 6, 11, 12, 12, dark(hair, 0.85f));
                rect(c, 3, 8, 4, 10, skin);
                rect(c, 14, 8, 15, 10, skin);
                if (hairStyle == 4) {
                    // ポニーテール：まん中で結んだ髪
                    rect(c, 7, 9, 11, 22, hair);
                    rect(c, 7, 9, 11, 11, dark(hair, 0.7f));
                    rect(c, 8, 20, 10, 23, dark(hair, 0.9f));
                }
            }
            rect(c, 5, 2, 8, 3, light(hair));

            drawAccessoriesFrontBack(c, true);
            return; // 後ろ向きは目を描かない
        }

        // 前から見た髪
        switch (hairStyle) {
            case 0: // ショート：左寄りで分けて両側に流す
                rect(c, 3, 5, 4, 10, hair);
                rect(c, 14, 5, 15, 10, hair);
                rect(c, 4, 5, 7, 7, hair);
                rect(c, 4, 7, 5, 8, hair);
                rect(c, 8, 5, 14, 6, hair);
                rect(c, 10, 6, 14, 7, hair);
                rect(c, 13, 7, 14, 8, hair);
                break;

            case 1: // ボブ
            case 2: // ロング
                rect(c, 4, 5, 14, 7, hair);
                rect(c, 4, 7, 6, 8, hair);
                rect(c, 12, 7, 14, 8, hair);
                rect(c, 8, 7, 10, 8, hair);
                int bottom = hairStyle == 1 ? 15 : 24; // ボブはあごの下、ロングは胸まで
                rect(c, 3, 5, 5, bottom, hair);
                rect(c, 13, 5, 15, bottom, hair);
                rect(c, 3, bottom - 2, 5, bottom, dark(hair, 0.88f));
                rect(c, 13, bottom - 2, 15, bottom, dark(hair, 0.88f));
                break;

            case 3: // センター分け
                rect(c, 3, 5, 4, 10, hair);
                rect(c, 14, 5, 15, 10, hair);
                rect(c, 4, 5, 8, 7, hair);
                rect(c, 10, 5, 14, 7, hair);
                rect(c, 4, 7, 6, 8, hair);
                rect(c, 12, 7, 14, 8, hair);
                break;

            default: // ポニーテール：短い前髪＋頭の横に結んだ髪
                rect(c, 3, 5, 4, 9, hair);
                rect(c, 14, 5, 15, 9, hair);
                rect(c, 4, 5, 14, 6, hair);
                rect(c, 5, 6, 7, 7, hair);
                rect(c, 9, 6, 10, 7, hair);
                rect(c, 12, 6, 14, 7, hair);
                rect(c, 15, 6, 17, 15, hair);
                rect(c, 15, 6, 17, 7, dark(hair, 0.7f));
                rect(c, 15, 13, 17, 15, dark(hair, 0.88f));
                break;
        }
        rect(c, 5, 2, 8, 3, light(hair));

        // 目：黒目＋左上に白いハイライト
        rect(c, 6, 8, 8, 11, EYE_DARK);
        rect(c, 10, 8, 12, 11, EYE_DARK);
        rect(c, 6, 8, 7, 9, Color.WHITE);
        rect(c, 10, 8, 11, 9, Color.WHITE);
        rect(c, 7, 10, 8, 11, eyes);
        rect(c, 11, 10, 12, 11, eyes);

        drawAccessoriesFrontBack(c, false);
    }

    // 右向き（左向きはこれを反転して使う）
    private void drawSide(Canvas c, int frame) {
        rect(c, 3, 46, 15, 47, SHADOW);

        // 体の後ろの髪（ロング・ポニーテール）
        if (hairStyle == 2) {
            rect(c, 2, 12, 7, 24, hair);
            rect(c, 2, 21, 6, 24, dark(hair, 0.88f));
        }
        if (hairStyle == 4) {
            rect(c, 0, 7, 4, 18, hair);
            rect(c, 2, 6, 4, 9, dark(hair, 0.7f));
            rect(c, 0, 15, 3, 18, dark(hair, 0.88f));
        }

        // 足：歩くときは前後に1ドットずらす
        int st = frame == 1 ? 1 : (frame == 2 ? -1 : 0);
        int far = 6 - st;
        int near = 8 + st;
        if (pantsStyle == 0) {
            rect(c, far, 29, far + 4, 44, dark(pants));
            rect(c, near, 29, near + 4, 44, pants);
        } else {
            rect(c, far + 1, 35, far + 4, 44, dark(skin, 0.9f));
            rect(c, near + 1, 35, near + 4, 44, skin);
            rect(c, 5, 29, 13, 35, pants);
            rect(c, 4, 32, 14, 35, pants);
            rect(c, 4, 34, 14, 35, dark(pants));
        }

        // くつ（つま先は前に1ドット）
        if (frame == 0) {
            rect(c, 6, 42, 11, 45, dark(shoes));
            rect(c, 8, 42, 12, 45, shoes);
            rect(c, 12, 43, 13, 45, shoes);
            rect(c, 6, 45, 13, 46, SOLE);
        } else {
            rect(c, far, 42, far + 4, 45, dark(shoes));
            rect(c, near, 42, near + 4, 45, shoes);
            rect(c, near + 4, 43, near + 5, 45, shoes);
            rect(c, far, 45, far + 4, 46, SOLE);
            rect(c, near, 45, near + 5, 46, SOLE);
        }

        // 体
        rect(c, 5, 15, 13, 30, shirt);
        rect(c, 5, 16, 6, 29, dark(shirt));
        if (shirtStyle == 2) rect(c, 12, 17, 13, 29, INNER); // ジャケットの前開き
        if (shirtStyle == 1) rect(c, 5, 15, 9, 19, dark(shirt)); // パーカーのフード

        // 腕：歩くときは前後・上下に1ドットふる
        int hy = frame == 1 ? 1 : (frame == 2 ? -1 : 0);
        int hx = frame == 1 ? 1 : (frame == 2 ? -1 : 0);
        rect(c, 8, 16, 11, 21, dark(shirt, 0.88f));
        if (shirtStyle == 0) {
            rect(c, 8 + hx, 21, 11 + hx, 28 + hy, skin);
            rect(c, 10 + hx, 21, 11 + hx, 28 + hy, dark(skin, 0.88f));
        } else {
            rect(c, 8 + hx, 21, 11 + hx, 27 + hy, dark(shirt, 0.88f));
            rect(c, 8 + hx, 27 + hy, 11 + hx, 29 + hy, skin);
        }

        // 首・頭（下の角を丸く）
        rect(c, 8, 13, 11, 16, skin);
        rect(c, 4, 3, 14, 13, skin);
        rect(c, 5, 13, 13, 14, skin);
        rect(c, 14, 9, 15, 10, skin); // 鼻

        // 髪：頭の上・後頭部・おでこ
        rect(c, 4, 1, 14, 3, hair);
        rect(c, 3, 3, 15, 5, hair);
        rect(c, 3, 5, 8, 11, hair);
        rect(c, 4, 11, 8, 12, hair);
        rect(c, 3, 5, 14, 7, hair);

        switch (hairStyle) {
            case 0: // ショート：前に向かって下がる前髪
                rect(c, 9, 7, 14, 8, hair);
                rect(c, 12, 8, 14, 9, hair);
                break;
            case 1: // ボブ
            case 2: // ロング
                rect(c, 10, 7, 14, 8, hair);
                rect(c, 2, 7, 8, hairStyle == 1 ? 15 : 16, hair);
                rect(c, 2, 13, 8, 15, dark(hair, 0.88f));
                break;
            case 3: // センター分け
                rect(c, 9, 7, 12, 8, hair);
                rect(c, 13, 7, 14, 9, hair);
                break;
            default: // ポニーテール
                rect(c, 10, 7, 14, 8, hair);
                break;
        }
        rect(c, 5, 2, 8, 3, light(hair));

        // 目は1つ
        rect(c, 11, 8, 13, 11, EYE_DARK);
        rect(c, 11, 8, 12, 9, Color.WHITE);
        rect(c, 12, 10, 13, 11, eyes);
        drawAccessoriesSide(c);

    }

    // 正面・後ろ向きのアクセサリー描画
    private void drawAccessoriesFrontBack(
            Canvas c, boolean back) {

        if (!back) {

            // ネックレス
            if (necklaceStyle > 0) {
                rect(c, 6, 16, 7, 18, necklaceColor);
                rect(c, 11, 16, 12, 18, necklaceColor);
                rect(c, 7, 18, 11, 19, necklaceColor);

                if (necklaceStyle == 1) {
                    rect(c, 8, 19, 10, 22, necklaceColor);
                    rect(c, 8, 20, 9, 21,
                            light(necklaceColor));
                }
            }



            // 眼鏡・サングラス
            if (glassesStyle > 0) {

                // サングラスの薄いレンズ
                if (glassesStyle == 2) {
                    rect(c, 5, 8, 8, 11, 0x6620262E);
                    rect(c, 10, 8, 13, 11, 0x6620262E);
                }

                // 左側のフレーム
                thinLine(c, 5, 7.5f, 8.5f, 7.5f, glassesColor);
                thinLine(c, 5, 11.5f, 8.5f, 11.5f, glassesColor);
                thinLine(c, 4.7f, 8, 4.7f, 11, glassesColor);
                thinLine(c, 8.5f, 8, 8.5f, 11, glassesColor);

                // 右側のフレーム
                thinLine(c, 9.5f, 7.5f, 13, 7.5f, glassesColor);
                thinLine(c, 9.5f, 11.5f, 13, 11.5f, glassesColor);
                thinLine(c, 9.5f, 8, 9.5f, 11, glassesColor);
                thinLine(c, 13.3f, 8, 13.3f, 11, glassesColor);

                // 中央のブリッジ
                thinLine(c, 8.5f, 8.5f, 9.5f, 8.5f, glassesColor);
            }



            // ピアス（耳の位置に調整）
            if (earringsStyle == 1) {

                // スタッドピアス
                rect(c, 3, 9, 4, 10, earringsColor);
                rect(c, 14, 9, 15, 10, earringsColor);

            } else if (earringsStyle == 2) {

                // 左耳：小さなリング
                rect(c, 2, 9, 4, 10, earringsColor);
                rect(c, 2, 10, 3, 12, earringsColor);
                rect(c, 3, 11, 4, 12, earringsColor);

                // 右耳：小さなリング
                rect(c, 14, 9, 16, 10, earringsColor);
                rect(c, 15, 10, 16, 12, earringsColor);
                rect(c, 14, 11, 15, 12, earringsColor);
            }


        }

        // リボン
        if (ribbonStyle > 0) {
            int mid = ribbonStyle == 1
                    ? 3 : (ribbonStyle == 2 ? 14 : 9);

            rect(c, mid - 2, 3, mid, 6, ribbonColor);
            rect(c, mid, 4, mid + 1, 5, dark(ribbonColor));
            rect(c, mid + 1, 3, mid + 3, 6, ribbonColor);
            rect(c, mid - 1, 6, mid + 2, 7,
                    dark(ribbonColor));
        }

        // キャップ
        if (capStyle > 0) {
            rect(c, 3, 0, 15, 4, capColor);
            rect(c, 4, 0, 14, 1, light(capColor));
            rect(c, 3, 3, 15, 5, dark(capColor));

            if ((capStyle == 1 && !back)
                    || (capStyle == 2 && back)) {
                rect(c, 5, 5, 14, 6, dark(capColor));
            }

            if (capStyle == 2 && !back) {
                rect(c, 8, 1, 10, 3, light(capColor));
            }
        }
    }

    // 横向きのアクセサリー描画
    private void drawAccessoriesSide(Canvas c) {

        if (necklaceStyle > 0) {
            rect(c, 11, 16, 12, 19, necklaceColor);

            if (necklaceStyle == 1) {
                rect(c, 11, 19, 13, 21, necklaceColor);
            }
        }


        // 横向きの眼鏡・サングラス
        if (glassesStyle > 0) {

            if (glassesStyle == 2) {
                rect(c, 11, 8, 14, 11, 0x6620262E);
            }

            // 細いレンズフレーム
            thinLine(c, 11, 7.5f, 14, 7.5f, glassesColor);
            thinLine(c, 11, 11.5f, 14, 11.5f, glassesColor);
            thinLine(c, 10.7f, 8, 10.7f, 11, glassesColor);
            thinLine(c, 14.3f, 8, 14.3f, 11, glassesColor);

            // 耳にかかる部分
            thinLine(c, 7, 8.5f, 11, 8.5f, glassesColor);
        }



        // 横向きのピアス（耳の位置）
        if (earringsStyle == 1) {

            // スタッド
            rect(c, 6, 9, 7, 10, earringsColor);

        } else if (earringsStyle == 2) {

            // 小さなリング
            rect(c, 5, 9, 7, 10, earringsColor);
            rect(c, 5, 10, 6, 12, earringsColor);
            rect(c, 6, 11, 7, 12, earringsColor);
        }


        if (ribbonStyle > 0) {
            rect(c, 2, 3, 5, 5, ribbonColor);
            rect(c, 3, 5, 4, 7, dark(ribbonColor));
        }

        if (capStyle > 0) {
            rect(c, 3, 0, 15, 4, capColor);
            rect(c, 3, 3, 15, 5, dark(capColor));

            if (capStyle == 1) {
                rect(c, 13, 5, 17, 6, dark(capColor));
            } else {
                rect(c, 1, 4, 6, 5, dark(capColor));
            }
        }
    }

}
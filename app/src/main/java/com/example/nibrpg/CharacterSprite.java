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

    // 向き：0 正面、1 右、2 後ろ、3 左
    public static final int FRONT = 0;
    public static final int RIGHT = 1;
    public static final int BACK = 2;
    public static final int LEFT = 3;

    private final Paint paint = new Paint();

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

        return sprite;
    }

    // 色を少し暗くする
    private int dark(int color) {
        return Color.rgb(
                (int) (Color.red(color) * 0.75f),
                (int) (Color.green(color) * 0.75f),
                (int) (Color.blue(color) * 0.75f)
        );
    }

    // ドットを描く
    private void rect(
            Canvas c,
            int x1, int y1,
            int x2, int y2,
            int color) {

        paint.setColor(color);
        c.drawRect(x1, y1, x2, y2, paint);
    }

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
        int eyeDark = Color.rgb(35, 39, 50);

        canvas.save();
        canvas.translate(x, y);
        canvas.scale(scale, scale);


        // 左右向きは専用の全身描画を使う
        if (dir == RIGHT || dir == LEFT) {
            drawSide(canvas, dir, eyeDark, frame);
            canvas.restore();
            return;
        }


        // 地面の影
        rect(canvas, 3, 27, 17, 28, 0x66505F68);

        // 足を交互に1ドットずらす。直立(frame=0)は旧描画と完全に同じ。
        int leftFootShift = frame == 1 ? -1 : (frame == 2 ? 1 : 0);
        int rightFootShift = -leftFootShift;

        // ズボン・スカート
        if (pantsStyle == 0) {
            rect(canvas, 5, 17, 10, 25 + leftFootShift, pants);
            rect(canvas, 11, 17, 16, 25 + rightFootShift, dark(pants));

            rect(canvas, 6, 18, 9, 24 + leftFootShift, pants);
            rect(canvas, 12, 18, 15, 24 + rightFootShift, dark(pants));
        } else {
            rect(canvas, 4, 17, 17, 22, pants);
            rect(canvas, 5, 17, 16, 21, pants);

            rect(canvas, 7, 21, 10, 25 + leftFootShift, skin);
            rect(canvas, 12, 21, 15, 25 + rightFootShift, skin);
        }

        // 靴：上下差1ドットのみ。元の形と短さを維持。
        rect(canvas, 4, 24 + leftFootShift, 10, 27 + leftFootShift, shoes);
        rect(canvas, 11, 24 + rightFootShift, 17, 27 + rightFootShift, shoes);
        rect(canvas, 5, 24 + leftFootShift, 10, 26 + leftFootShift, shoes);
        rect(canvas, 12, 24 + rightFootShift, 17, 26 + rightFootShift, shoes);

        rect(canvas, 4, 26 + leftFootShift, 10, 27 + leftFootShift, Color.LTGRAY);
        rect(canvas, 11, 26 + rightFootShift, 17, 27 + rightFootShift, Color.LTGRAY);

        // 上着
        rect(canvas, 5, 11, 16, 20, shirt);
        rect(canvas, 6, 12, 15, 19, shirt);
        rect(canvas, 13, 12, 15, 19, dark(shirt));

        // 腕：足と逆のリズムで、上下に1ドットだけ振る。
        // 肩は固定なので、洋服と腕の付け根に隙間ができない。
        int leftHandShift = -leftFootShift;
        int rightHandShift = -rightFootShift;
        if (shirtStyle == 0) {
            // Tシャツ
            rect(canvas, 2, 12, 5, 16, shirt);
            rect(canvas, 16, 12, 19, 16, shirt);

            rect(canvas, 2, 16, 5, 20 + leftHandShift, skin);
            rect(canvas, 16, 16, 19, 20 + rightHandShift, skin);
        } else {
            // パーカー・ジャケットの長袖
            rect(canvas, 2, 12, 5, 19 + leftHandShift, shirt);
            rect(canvas, 16, 12, 19, 19 + rightHandShift, shirt);

            rect(canvas, 2, 19 + leftHandShift, 5, 21 + leftHandShift, skin);
            rect(canvas, 16, 19 + rightHandShift, 19, 21 + rightHandShift, skin);
        }

        // ジャケットの前開き
        if (shirtStyle == 2 && dir != BACK) {
            rect(canvas, 9, 12, 12, 19,
                    Color.rgb(224, 227, 230));
        }

        // パーカーの紐
        if (shirtStyle == 1 && dir == FRONT) {
            rect(canvas, 8, 12, 9, 16, Color.WHITE);
            rect(canvas, 12, 12, 13, 16, Color.WHITE);
        }

        // 首
        rect(canvas, 9, 10, 12, 13, skin);

        // 頭
        rect(canvas, 5, 3, 17, 12, skin);

        rect(canvas, 7, 4, 16, 12, skin);

        // 後ろ向きなら顔の部分も髪で覆う
        if (dir == BACK) {
            rect(canvas, 5, 4, 16, 12, hair);
            rect(canvas, 5, 9, 16, 12, dark(hair));
        }

        // =========================
        // 髪型の描画
        // =========================

        if (hairStyle == 3 && dir == FRONT) {

            // センター分け：中央の前髪を開ける
            rect(canvas, 4, 2, 17, 5, hair);
            rect(canvas, 4, 5, 9, 8, hair);
            rect(canvas, 12, 5, 17, 8, hair);

            // 左右の生え際
            rect(canvas, 4, 5, 6, 9, hair);
            rect(canvas, 15, 5, 17, 9, hair);

            // 分け目の影
            rect(canvas, 8, 5, 9, 6, dark(hair));
            rect(canvas, 12, 5, 13, 6, dark(hair));

        } else {

            // 共通の髪の上部分
            rect(canvas, 4, 2, 17, 6, hair);
            rect(canvas, 4, 5, 6, 9, hair);
            rect(canvas, 15, 5, 17, 9, hair);
        }

        // ボブ：顔の左右に髪を伸ばす
        if (hairStyle == 1 || hairStyle == 2) {
            rect(canvas, 4, 8, 6, 14, hair);
            rect(canvas, 15, 8, 17, 14, hair);
        }

        // ロング：肩より下まで伸ばす
        if (hairStyle == 2) {
            rect(canvas, 3, 11, 6, 20, hair);
            rect(canvas, 15, 11, 18, 20, hair);

            if (dir == BACK) {
                rect(canvas, 6, 12, 15, 18, hair);
            }
        }

        // ポニーテール：向き別に結んだ髪を描く
        if (hairStyle == 4) {

            if (dir == FRONT) {

                // 正面：頭の横から束ねた髪が見える
                rect(canvas, 16, 8, 19, 16, hair);
                rect(canvas, 17, 15, 19, 18, dark(hair));
                rect(canvas, 16, 8, 19, 10, dark(hair));

            } else if (dir == BACK) {

                // 後ろ：中央で結んだ髪
                rect(canvas, 9, 10, 13, 19, hair);
                rect(canvas, 9, 10, 13, 12, dark(hair));
                rect(canvas, 10, 18, 12, 20, dark(hair));

            } else if (dir == RIGHT) {

                // 右向き：後頭部の左側に束ねた髪
                rect(canvas, 2, 8, 6, 17, hair);
                rect(canvas, 3, 8, 6, 10, dark(hair));
                rect(canvas, 2, 16, 5, 18, dark(hair));

            } else if (dir == LEFT) {

                // 左向き：後頭部の右側に束ねた髪
                rect(canvas, 15, 8, 19, 17, hair);
                rect(canvas, 15, 8, 18, 10, dark(hair));
                rect(canvas, 16, 16, 19, 18, dark(hair));
            }
        }

        // =========================
        // 目：以前の2x2ドット＋色付き1ドットに戻す
        // =========================

        if (dir == FRONT) {

            // 正面は両目
            rect(canvas, 7, 8, 9, 10, eyeDark);
            rect(canvas, 12, 8, 14, 10, eyeDark);

            rect(canvas, 8, 8, 9, 9, eyes);
            rect(canvas, 13, 8, 14, 9, eyes);

        } else if (dir == RIGHT) {

            // 右向きは片目
            rect(canvas, 12, 8, 14, 10, eyeDark);
            rect(canvas, 13, 8, 14, 9, eyes);

        } else if (dir == LEFT) {

            // 左向きは片目
            rect(canvas, 7, 8, 9, 10, eyeDark);
            rect(canvas, 7, 8, 8, 9, eyes);
        }

        // 後ろ向きでは目を描かない

        // 後ろ向きパーカーのフード
        if (shirtStyle == 1 && dir == BACK) {
            rect(canvas, 6, 11, 15, 15, dark(shirt));
            rect(canvas, 8, 12, 13, 14, shirt);
        }

        canvas.restore();
    }


    // 横向きのキャラクターを描画（輪郭なし）
    private void drawSide(Canvas canvas, int dir, int eyeDark, int frame) {

        canvas.save();

        // 左向きは右向きの絵を左右反転する
        if (dir == LEFT) {
            canvas.translate(20f, 0f);
            canvas.scale(-1f, 1f);
        }

        // 地面の影
        rect(canvas, 3, 27, 17, 28, 0x66505F68);

        // 長い髪は体の後ろに描く
        if (hairStyle == 2) {
            rect(canvas, 3, 9, 8, 19, hair);
            rect(canvas, 3, 17, 7, 20, dark(hair));
        }

        // ポニーテールも体の後ろ
        if (hairStyle == 4) {
            rect(canvas, 3, 8, 7, 13, hair);
            rect(canvas, 2, 11, 6, 17, hair);
            rect(canvas, 3, 16, 6, 19, dark(hair));
        }

        // 横向きの脚：直立時は調整済みの旧描画をそのまま再現する。
        // 歩行時だけ足の位置を前後に1ドットずらす。
        int stride = frame == 1 ? 1 : (frame == 2 ? -1 : 0);
        int farLegX = 9 - stride;
        int nearLegX = 10 + stride;

        if (pantsStyle == 0) {
            rect(canvas, farLegX, 19, farLegX + 3, 25, dark(pants));
            rect(canvas, nearLegX, 19, nearLegX + 3, 25, pants);
        } else {
            rect(canvas, 8, 18, 14, 22, pants);
            rect(canvas, farLegX, 21, farLegX + 3, 25, skin);
            rect(canvas, nearLegX, 21, nearLegX + 3, 25, skin);
        }

        // 靴先は進行方向へ1ドットだけ。左向きは既存処理で左右反転。
        // 直立は以前の短いつま先（9〜15ドット幅）にする。
        if (frame == 0) {
            rect(canvas, 9, 24, 13, 27, dark(shoes));
            rect(canvas, 10, 24, 14, 27, shoes);
            rect(canvas, 14, 25, 15, 27, shoes);
            rect(canvas, 9, 27, 15, 28, Color.LTGRAY);
        } else {
            rect(canvas, farLegX, 24, farLegX + 4, 27, dark(shoes));
            rect(canvas, nearLegX, 24, nearLegX + 4, 27, shoes);
            rect(canvas, nearLegX + 4, 25, nearLegX + 5, 27, shoes);
            rect(canvas, farLegX, 27, farLegX + 4, 28, Color.LTGRAY);
            rect(canvas, nearLegX, 27, nearLegX + 5, 28, Color.LTGRAY);
        }

        // 横向きの胴体：頭と脚の中心が自然に揃う幅
        rect(canvas, 7, 11, 14, 20, shirt);
        rect(canvas, 8, 12, 14, 19, shirt);
        rect(canvas, 8, 12, 9, 19, dark(shirt));

        // ジャケットの前開き
        if (shirtStyle == 2) {
            rect(canvas, 13, 13, 14, 19,
                    Color.rgb(224, 227, 230));
        }

        // 横向きの腕：脚とぶつからないx=14以降に固定する。
        // 手先の位置だけを前後・上下に1ドットずつ動かす。
        int handShift = frame == 1 ? 1 : (frame == 2 ? -1 : 0);
        int handX = frame == 1 ? 1 : 0;
        if (shirtStyle == 0) {
            rect(canvas, 13, 13, 16, 16, shirt);
            rect(canvas, 14 + handX, 16, 16 + handX, 20 + handShift, skin);
        } else {
            rect(canvas, 13, 13, 16, 19 + handShift, shirt);
            rect(canvas, 14 + handX, 19 + handShift,
                    16 + handX, 21 + handShift, skin);
        }

        // 首
        rect(canvas, 10, 10, 13, 13, skin);

        // 横向きの頭（輪郭なし）
        rect(canvas, 6, 3, 17, 12, skin);
        rect(canvas, 7, 4, 16, 11, skin);

        // 顔の向きが分かるように鼻を出す
        rect(canvas, 16, 8, 17, 9, skin);

        // 後頭部と頭頂部は全髪型共通（おでこ側までつなげる）
        rect(canvas, 5, 2, 18, 6, hair);
        rect(canvas, 5, 5, 9, 11, hair);

        // 髪型ごとに、横から見える前髪と後ろ髪を描き分ける
        switch (hairStyle) {
            case 0: // ショート：短い前髪
                rect(canvas, 15, 5, 17, 7, hair);
                break;

            case 1: // ボブ：丸みのある前髪・あご付近までの後ろ髪
                rect(canvas, 13, 5, 17, 7, hair);
                rect(canvas, 14, 7, 16, 8, hair);
                rect(canvas, 4, 8, 8, 14, hair);
                rect(canvas, 5, 13, 8, 15, hair);
                break;

            case 2: // ロング：やや長い前髪・肩までの後ろ髪
                rect(canvas, 12, 5, 17, 7, hair);
                rect(canvas, 14, 7, 16, 8, hair);
                rect(canvas, 4, 8, 8, 14, hair);
                break;

            case 3: // センター分け：生え際は覆い、前髪に小さな分け目を作る
                rect(canvas, 11, 5, 13, 7, hair);
                rect(canvas, 15, 5, 17, 7, hair);
                break;

            case 4: // ポニーテール：短めの前髪＋後ろの結び目
                rect(canvas, 14, 5, 17, 7, hair);
                rect(canvas, 5, 8, 8, 11, dark(hair));
                break;
        }

        // 横向きは片目だけ
        rect(canvas, 13, 8, 15, 10, eyeDark);
        rect(canvas, 14, 8, 15, 9, eyes);

        canvas.restore();
    }

}


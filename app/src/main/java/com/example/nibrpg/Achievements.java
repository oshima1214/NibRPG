package com.example.nibrpg;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

/**
 * 実績（トロフィー）の係。
 *   ・解除した実績はスマホの中に保存する（アプリを閉じても残る）
 *   ・タイトルの「はじめる／はじめから」で全部リセット
 *   ・解除したら、画面の右上にポップアップを出す
 * 実績の名前と条件の文は strings.xml の ach_titles / ach_descs（この番号の順）
 */
public class Achievements {

    // 実績の番号（strings.xml の ach_titles・ach_descs と同じ順番）
    public static final int CLEAR_BARE = 0;     // アイテム・装備不使用、ノーダメージでクリア
    public static final int NO_ITEM = 1;        // アイテム不使用でクリア
    public static final int NO_WEAPON = 2;      // 装備不使用でクリア
    public static final int BIG_SPENDER = 3;    // お金をたくさん使ってクリア
    public static final int SAVER = 4;          // お金を節約してクリア
    public static final int COLLECTOR = 5;      // 落とし物を全部集めてクリア
    public static final int CLERK_REGULAR = 6;  // 事務員さんの話を10回聞く
    public static final int LOW_HP_WIN = 7;     // HPが残り1桁で先生に勝つ
    public static final int VENDING_ALL = 8;    // 自販機の商品を全種類買う
    public static final int BOSS_CLEAR = 9;     // 理事長に勝つ
    public static final int COUNT = 10;

    // 条件の数字
    public static final int BIG_SPENDER_MONEY = 2000; // これ以上使ってクリア
    public static final int SAVER_MONEY = 500;        // これ以下しか使わずにクリア
    public static final int CLERK_TALK_COUNT = 10;    // 事務員さんの話を聞く回数
    public static final int LOW_HP = 9;               // このHP以下で勝つ

    private static final String PREFS = "achievements";
    private static final long POPUP_MS = 3000;   // ポップアップを出しておく時間
    private static final long SLIDE_MS = 300;    // すべりこむ・出ていく時間
    private static long nextPopupAt = 0;         // 次のポップアップを出せる時刻（重ならないように順番に出す）

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 解除しているか */
    public static boolean isUnlocked(Context c, int id) {
        return prefs(c).getBoolean("a" + id, false);
    }

    /** 解除した数 */
    public static int unlockedCount(Context c) {
        int n = 0;
        for (int i = 0; i < COUNT; i++) {
            if (isUnlocked(c, i)) n++;
        }
        return n;
    }

    /** 全部リセット（タイトルの「はじめる／はじめから」で呼ぶ） */
    public static void resetAll(Context c) {
        prefs(c).edit().clear().apply();
    }

    /** 実績を解除する。はじめて解除したときだけ保存して、ポップアップを出す */
    public static void unlock(Activity a, int id) {
        if (isUnlocked(a, id)) return;
        prefs(a).edit().putBoolean("a" + id, true).apply();
        String title = a.getResources().getStringArray(R.array.ach_titles)[id];
        showPopup(a, title);
    }

    /** ゲームクリアしたときに呼ぶ：クリアが条件の実績をまとめて調べる */
    public static void checkClear(Activity a) {
        if (!GameState.usedItem && !GameState.usedWeapon && !GameState.tookDamage) unlock(a, CLEAR_BARE);
        if (!GameState.usedItem) unlock(a, NO_ITEM);
        if (!GameState.usedWeapon) unlock(a, NO_WEAPON);
        if (GameState.moneySpent >= BIG_SPENDER_MONEY) unlock(a, BIG_SPENDER);
        if (GameState.moneySpent <= SAVER_MONEY) unlock(a, SAVER);
        if (GameState.foundCount() == MapData.LOST_ITEMS.length) unlock(a, COLLECTOR);
    }

    /** 先生に勝ったときに呼ぶ：HPが残り1桁なら解除 */
    public static void checkWin(Activity a) {
        if (GameState.hp > 0 && GameState.hp <= LOW_HP) unlock(a, LOW_HP_WIN);
    }

    // ===================== ポップアップ =====================

    /** 画面の右上に「実績解除！」を出す（いくつも解除したときは、順番に出す） */
    private static void showPopup(Activity a, String title) {
        float dp = a.getResources().getDisplayMetrics().density;

        // カード：トロフィー ＋「実績解除！」＋ 名前
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding((int) (12 * dp), (int) (8 * dp), (int) (16 * dp), (int) (8 * dp));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xEE1B1030);
        bg.setStroke((int) (2 * dp), 0xFFFFD54F);
        bg.setCornerRadius(8 * dp);
        card.setBackground(bg);
        card.setElevation(12 * dp);

        TextView icon = new TextView(a);
        icon.setText("🏆");
        icon.setTextSize(24);
        card.addView(icon);

        // 文字はドットのフォント（DotGothic16）
        Typeface dot = ResourcesCompat.getFont(a, R.font.dotgothic16);

        LinearLayout texts = new LinearLayout(a);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding((int) (10 * dp), 0, 0, 0);
        TextView head = new TextView(a);
        head.setText(R.string.ach_unlocked);
        head.setTextColor(0xFFFFD54F);
        head.setTextSize(12);
        head.setTypeface(dot);
        TextView name = new TextView(a);
        name.setText(title);
        name.setTextColor(0xFFFFFFFF);
        name.setTextSize(16);
        name.setTypeface(dot);
        texts.addView(head);
        texts.addView(name);
        card.addView(texts);

        // 画面のいちばん上（右上）に重ねる
        ViewGroup root = a.findViewById(android.R.id.content);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        lp.setMargins(0, (int) (16 * dp), (int) (16 * dp), 0);

        // 前のポップアップが出ている間は、待ってから出す
        long now = SystemClock.uptimeMillis();
        long delay = Math.max(0, nextPopupAt - now);
        nextPopupAt = now + delay + POPUP_MS + SLIDE_MS * 2;

        root.postDelayed(() -> {
            card.setVisibility(View.INVISIBLE);
            root.addView(card, lp);
            // 大きさが決まってから、右の外からすべりこませる
            card.post(() -> {
                card.setTranslationX(card.getWidth() + 32 * dp);
                card.setVisibility(View.VISIBLE);
                card.animate().translationX(0).setDuration(SLIDE_MS).start();
                card.postDelayed(() -> card.animate()
                        .translationX(card.getWidth() + 32 * dp)
                        .setDuration(SLIDE_MS)
                        .withEndAction(() -> root.removeView(card))
                        .start(), SLIDE_MS + POPUP_MS);
            });
        }, delay);
    }
}

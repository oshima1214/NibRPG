package com.example.nibrpg;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * 自販機の画面（1Fの道具屋）。
 * 売っている物は ShopData.ITEMS、商品の絵は ShopData.ITEM_IMAGES。
 */
public class VendingActivity extends AppCompatActivity {

    private static final long POPUP_MS = 1600; // ポップアップを出しておく時間

    private final Handler handler = new Handler(Looper.getMainLooper());
    private View popup;           // 買ったときのポップアップ
    private View popGlow;
    private ImageView imgPop;
    private TextView txtPop;
    private TextView txtPopSub;
    private TextView txtLed;      // 所持金・HPの表示（緑の光る文字）
    private TextView txtSlot;     // 取り出し口のメッセージ
    private ImageView[] itemImages;
    private View[] pushButtons;      // 丸いボタン
    private TextView[] pushLabels;   // ボタンの下の「購入」「売切」
    private TextView[] haveTexts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vending);
        // 画面のふち（ステータスバーなど）に文字がかぶらないようにする
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        txtLed = findViewById(R.id.txtLed);
        txtSlot = findViewById(R.id.txtSlot);
        popup = findViewById(R.id.popup);
        popGlow = findViewById(R.id.popGlow);
        imgPop = findViewById(R.id.imgPop);
        txtPop = findViewById(R.id.txtPop);
        txtPopSub = findViewById(R.id.txtPopSub);
        popup.setOnClickListener(v -> hidePopup()); // タップしたら、すぐとじる

        // 商品を、商品の数だけショーケースに並べる
        ShopData.Goods[] items = ShopData.ITEMS;
        ViewGroup row = findViewById(R.id.slotRow);
        LayoutInflater inflater = LayoutInflater.from(this);
        itemImages = new ImageView[items.length];
        pushButtons = new View[items.length];
        pushLabels = new TextView[items.length];
        haveTexts = new TextView[items.length];
        for (int i = 0; i < items.length; i++) {
            View slot = inflater.inflate(R.layout.item_vending, row, false);
            itemImages[i] = slot.findViewById(R.id.imgItem);
            itemImages[i].setImageResource(ShopData.ITEM_IMAGES[i]);
            ((TextView) slot.findViewById(R.id.txtItemName)).setText(items[i].name);
            ((TextView) slot.findViewById(R.id.txtItemDesc)).setText(items[i].desc);
            ((TextView) slot.findViewById(R.id.txtPrice)).setText(getString(R.string.price, items[i].price));
            pushButtons[i] = slot.findViewById(R.id.btnPush);
            pushLabels[i] = slot.findViewById(R.id.txtPushLabel);
            haveTexts[i] = slot.findViewById(R.id.txtHave);
            int index = i; // ラムダの中で使うため
            pushButtons[i].setOnClickListener(v -> onPush(index));
            pushLabels[i].setOnClickListener(v -> onPush(index));
            row.addView(slot);
        }

        // もどる → マップにもどる（マップの画面は開いたままなので、続きから）
        findViewById(R.id.btnExit).setOnClickListener(v -> {
            Sound.se(this, R.raw.se_cancel);
            finish();
        });

        refresh();
    }

    /** 商品のボタンを押したとき */
    private void onPush(int i) {
        ShopData.Goods g = ShopData.ITEMS[i];
        if (GameState.itemCounts[i] >= ShopData.MAX_ITEMS) {
            txtSlot.setText(R.string.vending_full);
            Sound.se(this, R.raw.se_no_money);
        } else if (GameState.money < g.price) {
            txtSlot.setText(R.string.vending_no_money);
            Sound.se(this, R.raw.se_no_money);
        } else {
            GameState.money -= g.price;
            GameState.moneySpent += g.price; // 実績のため
            GameState.itemCounts[i]++;
            // 全種類買ったら実績
            GameState.boughtItems[i] = true;
            boolean all = true;
            for (boolean b : GameState.boughtItems) if (!b) all = false;
            if (all) Achievements.unlock(this, Achievements.VENDING_ALL);
            txtSlot.setText(getString(R.string.vending_get, g.name));
            Sound.se(this, R.raw.se_buy);
            showPopup(i);
            handler.postDelayed(() -> Sound.se(this, R.raw.se_vending_drop), 250); // ガコン
        }
        refresh();
    }

    /**
     * 買った商品を、画面のまん中にポップアップする。
     *   取り出し口のあたり（下）から、小さい状態で飛び出して、
     *   まん中で少し大きくはずんでから、ちょうどいい大きさに止まる
     */
    private void showPopup(int i) {
        handler.removeCallbacksAndMessages(null);
        imgPop.setImageResource(ShopData.ITEM_IMAGES[i]);
        txtPop.setText(ShopData.ITEMS[i].name);

        // 黒い幕をふわっと出す
        popup.setVisibility(View.VISIBLE);
        popup.setAlpha(0f);
        popup.animate().alpha(1f).setDuration(150).start();

        // 商品：下から小さく飛び出して、まん中ではずむ
        imgPop.setTranslationY(dp(220));
        imgPop.setScaleX(0.2f);
        imgPop.setScaleY(0.2f);
        imgPop.setRotation(-20f);
        imgPop.animate().translationY(0f).scaleX(1f).scaleY(1f).rotation(0f)
                .setDuration(500).setInterpolator(new OvershootInterpolator(2.5f)).start();

        // 文字：商品が止まるころに、下からふわっと出る
        for (View t : new View[] { txtPop, txtPopSub }) {
            t.setAlpha(0f);
            t.setTranslationY(dp(10));
            t.animate().alpha(1f).translationY(0f).setStartDelay(350).setDuration(250).start();
        }

        // 後ろの光：大きくなりながら、ゆっくり回る
        popGlow.setScaleX(0.3f);
        popGlow.setScaleY(0.3f);
        popGlow.setRotation(0f);
        popGlow.animate().scaleX(1f).scaleY(1f).rotation(90f)
                .setDuration(POPUP_MS).setInterpolator(new LinearInterpolator()).start();

        // しばらくしたら、自動でとじる
        handler.postDelayed(this::hidePopup, POPUP_MS);
    }

    /** ポップアップをとじる */
    private void hidePopup() {
        handler.removeCallbacksAndMessages(null);
        if (popup.getVisibility() != View.VISIBLE) return;
        popup.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> popup.setVisibility(View.GONE)).start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** 所持金の表示と、ボタンの見た目を今の状態に合わせる */
    private void refresh() {
        txtLed.setText(getString(R.string.vending_led, GameState.money, GameState.hp, GameState.maxHp));
        for (int i = 0; i < ShopData.ITEMS.length; i++) {
            haveTexts[i].setText(getString(R.string.vending_have, GameState.itemCounts[i]));
            if (GameState.itemCounts[i] >= ShopData.MAX_ITEMS) {
                pushLabels[i].setText(R.string.vending_soldout);           // もう持てない
                pushLabels[i].setTextColor(getColor(R.color.rpg_red));
                pushButtons[i].setBackgroundResource(R.drawable.vending_round_off);
            } else if (GameState.money < ShopData.ITEMS[i].price) {
                pushLabels[i].setText(R.string.vending_push);              // お金が足りない（ランプが消えている）
                pushLabels[i].setTextColor(0xFF5A6270);
                pushButtons[i].setBackgroundResource(R.drawable.vending_round_off);
            } else {
                pushLabels[i].setText(R.string.vending_push);              // 買える（ランプが光っている）
                pushLabels[i].setTextColor(0xFFDCE6F5);
                pushButtons[i].setBackgroundResource(R.drawable.vending_round);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sound.bgm(this, R.raw.bgm_shop);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Sound.pauseBgmSoon();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}

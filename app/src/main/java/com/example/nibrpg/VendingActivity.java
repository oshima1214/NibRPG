package com.example.nibrpg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
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

    private TextView txtLed;      // 所持金・HPの表示（緑の光る文字）
    private TextView txtSlot;     // 取り出し口のメッセージ
    private ImageView[] itemImages;
    private TextView[] pushButtons;
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

        // 商品を、商品の数だけショーケースに並べる
        ShopData.Goods[] items = ShopData.ITEMS;
        ViewGroup row = findViewById(R.id.slotRow);
        LayoutInflater inflater = LayoutInflater.from(this);
        itemImages = new ImageView[items.length];
        pushButtons = new TextView[items.length];
        haveTexts = new TextView[items.length];
        for (int i = 0; i < items.length; i++) {
            View slot = inflater.inflate(R.layout.item_vending, row, false);
            itemImages[i] = slot.findViewById(R.id.imgItem);
            itemImages[i].setImageResource(ShopData.ITEM_IMAGES[i]);
            ((TextView) slot.findViewById(R.id.txtItemName)).setText(items[i].name);
            ((TextView) slot.findViewById(R.id.txtItemDesc)).setText(items[i].desc);
            ((TextView) slot.findViewById(R.id.txtPrice)).setText(getString(R.string.price, items[i].price));
            pushButtons[i] = slot.findViewById(R.id.btnPush);
            haveTexts[i] = slot.findViewById(R.id.txtHave);
            int index = i; // ラムダの中で使うため
            pushButtons[i].setOnClickListener(v -> onPush(index));
            row.addView(slot);
        }

        // もどる → マップにもどる（マップの画面は開いたままなので、続きから）
        findViewById(R.id.btnExit).setOnClickListener(v -> finish());

        refresh();
    }

    /** 商品のボタンを押したとき */
    private void onPush(int i) {
        ShopData.Goods g = ShopData.ITEMS[i];
        if (GameState.itemCounts[i] >= ShopData.MAX_ITEMS) {
            txtSlot.setText(R.string.vending_full);
        } else if (GameState.money < g.price) {
            txtSlot.setText(R.string.vending_no_money);
        } else {
            GameState.money -= g.price;
            GameState.itemCounts[i]++;
            txtSlot.setText(getString(R.string.vending_get, g.name));
            drop(itemImages[i]);
        }
        refresh();
    }

    /** 商品がガコンと落ちるような動き */
    private void drop(View v) {
        TranslateAnimation a = new TranslateAnimation(0, 0, 0, v.getHeight() * 0.25f);
        a.setDuration(150);
        a.setRepeatMode(TranslateAnimation.REVERSE);
        a.setRepeatCount(1);
        v.startAnimation(a);
    }

    /** 所持金の表示と、ボタンの見た目を今の状態に合わせる */
    private void refresh() {
        txtLed.setText(getString(R.string.vending_led, GameState.money, GameState.hp, GameState.maxHp));
        for (int i = 0; i < ShopData.ITEMS.length; i++) {
            haveTexts[i].setText(getString(R.string.vending_have, GameState.itemCounts[i]));
            TextView b = pushButtons[i];
            if (GameState.itemCounts[i] >= ShopData.MAX_ITEMS) {
                b.setText(R.string.vending_soldout);           // もう持てない
                b.setBackgroundResource(R.drawable.vending_button_off);
            } else if (GameState.money < ShopData.ITEMS[i].price) {
                b.setText(R.string.vending_push);              // お金が足りない（ランプが消えている）
                b.setBackgroundResource(R.drawable.vending_button_off);
            } else {
                b.setText(R.string.vending_push);              // 買える（ランプが光っている）
                b.setBackgroundResource(R.drawable.vending_button);
            }
        }
    }
}

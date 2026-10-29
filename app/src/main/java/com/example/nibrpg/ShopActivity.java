package com.example.nibrpg;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * 事務室の窓口（1Fの受付）。
 * 事務員さんに「備品申込書」を出して、備品（＝武器。ShopData.WEAPONS）を買う。
 * 買うとすぐ使う（装備する）。一度買った備品は、あとから持ちかえられる。
 */
public class ShopActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView txtTalk;
    private TextView txtStatus;
    private TextView[] buttons; // 品物ごとのボタン（申し込む／持ちかえる／使用中）
    private TextView[] stamps;  // 品物ごとの「受付済」のハンコ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shop);
        // 画面のふち（ステータスバーなど）に文字がかぶらないようにする
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        txtTalk = findViewById(R.id.txtTalk);
        txtStatus = findViewById(R.id.txtStatus);
        txtTalk.setText(R.string.reception_hello);

        // 申込書の行を、備品の数だけ作る
        ShopData.Goods[] goods = ShopData.WEAPONS;
        ViewGroup list = findViewById(R.id.listGoods);
        LayoutInflater inflater = LayoutInflater.from(this);
        buttons = new TextView[goods.length];
        stamps = new TextView[goods.length];
        for (int i = 0; i < goods.length; i++) {
            View row = inflater.inflate(R.layout.item_goods, list, false);
            ((TextView) row.findViewById(R.id.txtName)).setText(goods[i].name);
            ((TextView) row.findViewById(R.id.txtDesc)).setText(goods[i].desc);
            ((TextView) row.findViewById(R.id.txtPrice)).setText(getString(R.string.price, goods[i].price));
            buttons[i] = row.findViewById(R.id.btnBuy);
            stamps[i] = row.findViewById(R.id.txtStamp);
            int index = i; // ラムダの中で使うため
            buttons[i].setOnClickListener(v -> onApply(index));
            list.addView(row);
        }

        // 窓口をはなれる → マップにもどる（マップの画面は開いたままなので、続きから）
        findViewById(R.id.btnExit).setOnClickListener(v -> {
            Sound.se(this, R.raw.se_cancel);
            finish();
        });

        refresh();
    }

    /** 申込書のボタンを押したとき */
    private void onApply(int i) {
        ShopData.Goods g = ShopData.WEAPONS[i];
        if (GameState.weapon == i) {
            GameState.weapon = -1;                                   // 使用中をもう一度押すと、しまって素手にする
            Sound.se(this, R.raw.se_cancel);
            txtTalk.setText(getString(R.string.reception_unequip, g.name));
        } else if (GameState.ownedWeapons[i]) {
            GameState.weapon = i;                                    // 持ちかえる（お金はかからない）
            Sound.se(this, R.raw.se_equip);
            txtTalk.setText(getString(R.string.reception_equip, g.name));
        } else if (GameState.money < g.price) {
            txtTalk.setText(R.string.reception_no_money);
            Sound.se(this, R.raw.se_no_money);
        } else {
            GameState.money -= g.price;
            GameState.moneySpent += g.price; // 実績のため
            GameState.ownedWeapons[i] = true;
            GameState.weapon = i;                                    // 買ったらすぐ使う
            Sound.se(this, R.raw.se_buy);
            txtTalk.setText(getString(R.string.reception_bought, g.name));
            stamp(stamps[i]);
        }
        refresh();
    }

    /** 「受付済」のハンコを、ポンと押す動き */
    private void stamp(TextView s) {
        s.setVisibility(View.VISIBLE);
        s.setAlpha(0f);
        s.setScaleX(2.5f);
        s.setScaleY(2.5f);
        s.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(250).setInterpolator(new OvershootInterpolator(2f)).start();
        // 少ししたら、ハンコを消す（下のボタンが見えるように）
        handler.postDelayed(() -> s.animate().alpha(0f).setDuration(400)
                .withEndAction(() -> s.setVisibility(View.INVISIBLE)).start(), 1200);
    }

    /** 所持金や、ボタンの文字・見た目を今の状態に合わせる */
    private void refresh() {
        String weaponName = GameState.weaponName();
        txtStatus.setText(getString(R.string.reception_status, GameState.money, GameState.attack(),
                weaponName != null ? weaponName : getString(R.string.status_bare)));

        for (int i = 0; i < buttons.length; i++) {
            TextView b = buttons[i];
            b.setAlpha(1f);
            if (GameState.weapon == i) {
                b.setText(R.string.btn_using);                       // 使用中
                b.setBackgroundResource(R.drawable.form_button_filled);
                b.setTextColor(getColor(R.color.white));
            } else if (GameState.ownedWeapons[i]) {
                b.setText(R.string.btn_switch);                      // 持ちかえる
                b.setBackgroundResource(R.drawable.form_button_navy);
                b.setTextColor(getColor(R.color.nib_navy));
            } else {
                b.setText(R.string.btn_apply);                       // 申し込む
                b.setBackgroundResource(R.drawable.form_button);
                b.setTextColor(getColor(R.color.form_red));
                if (GameState.money < ShopData.WEAPONS[i].price) b.setAlpha(0.4f); // お金が足りない
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

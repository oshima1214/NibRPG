package com.example.nibrpg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** 武器屋・道具屋の画面。どちらの店かは、開くときに EXTRA_TYPE で受け取る */
public class ShopActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "shop_type";
    public static final String TYPE_WEAPON = "weapon";
    public static final String TYPE_ITEM = "item";

    private boolean isWeapon;
    private ShopData.Goods[] goods;
    private TextView txtTalk;
    private TextView txtStatus;
    private TextView[] descViews;  // 品物ごとの説明（持っている数を書きかえる）
    private TextView[] buyButtons; // 品物ごとのボタン（「買う」「装備中」などを書きかえる）

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

        // どちらの店か
        isWeapon = TYPE_WEAPON.equals(getIntent().getStringExtra(EXTRA_TYPE));
        goods = isWeapon ? ShopData.WEAPONS : ShopData.ITEMS;

        TextView txtShopName = findViewById(R.id.txtShopName);
        txtTalk = findViewById(R.id.txtTalk);
        txtStatus = findViewById(R.id.txtStatus);
        txtShopName.setText(isWeapon ? R.string.shop_weapon_title : R.string.shop_item_title);
        txtTalk.setText(isWeapon ? R.string.shop_weapon_hello : R.string.shop_item_hello);
        findViewById(R.id.main).setBackgroundResource(isWeapon ? R.color.shop_weapon_bg : R.color.shop_item_bg);

        // 品物の行を、品物の数だけ作る
        ViewGroup list = findViewById(R.id.listGoods);
        LayoutInflater inflater = LayoutInflater.from(this);
        descViews = new TextView[goods.length];
        buyButtons = new TextView[goods.length];
        for (int i = 0; i < goods.length; i++) {
            View row = inflater.inflate(R.layout.item_goods, list, false);
            ((TextView) row.findViewById(R.id.txtName)).setText(goods[i].name);
            ((TextView) row.findViewById(R.id.txtPrice)).setText(getString(R.string.price, goods[i].price));
            descViews[i] = row.findViewById(R.id.txtDesc);
            buyButtons[i] = row.findViewById(R.id.btnBuy);
            int index = i; // ラムダの中で使うため
            buyButtons[i].setOnClickListener(v -> onBuy(index));
            list.addView(row);
        }

        // 店を出る → マップにもどる（マップの画面は閉じていないので、そのまま続きから）
        findViewById(R.id.btnExit).setOnClickListener(v -> finish());

        refresh();
    }

    /** 品物のボタンが押されたとき */
    private void onBuy(int i) {
        ShopData.Goods g = goods[i];
        if (isWeapon) {
            if (GameState.weapon == i) {
                return; // もう装備している
            }
            if (GameState.ownedWeapons[i]) {
                GameState.weapon = i; // 買ってある武器に付けかえる（お金はかからない）
                txtTalk.setText(getString(R.string.shop_equip_done, g.name));
            } else if (GameState.money < g.price) {
                txtTalk.setText(R.string.shop_no_money);
            } else {
                GameState.money -= g.price;
                GameState.ownedWeapons[i] = true;
                GameState.weapon = i; // 買ったらすぐ装備
                txtTalk.setText(getString(R.string.shop_bought_equip, g.name));
            }
        } else {
            if (GameState.itemCounts[i] >= ShopData.MAX_ITEMS) {
                txtTalk.setText(R.string.shop_full);
            } else if (GameState.money < g.price) {
                txtTalk.setText(R.string.shop_no_money);
            } else {
                GameState.money -= g.price;
                GameState.itemCounts[i]++;
                txtTalk.setText(getString(R.string.shop_bought, g.name));
            }
        }
        refresh();
    }

    /** 所持金や、ボタンの文字を今の状態に合わせる */
    private void refresh() {
        String weaponName = GameState.weaponName();
        txtStatus.setText(getString(R.string.status_shop,
                GameState.money, GameState.hp, GameState.maxHp, GameState.attack(),
                weaponName != null ? weaponName : getString(R.string.status_bare)));

        for (int i = 0; i < goods.length; i++) {
            TextView btn = buyButtons[i];
            if (isWeapon) {
                descViews[i].setText(goods[i].desc);
                if (GameState.weapon == i) {
                    btn.setText(R.string.shop_equipped);
                    btn.setAlpha(0.5f);
                } else if (GameState.ownedWeapons[i]) {
                    btn.setText(R.string.shop_equip);
                    btn.setAlpha(1f);
                } else {
                    btn.setText(R.string.shop_buy);
                    btn.setAlpha(GameState.money >= goods[i].price ? 1f : 0.5f); // 買えないときはうすく
                }
            } else {
                descViews[i].setText(getString(R.string.goods_count, goods[i].desc, GameState.itemCounts[i]));
                btn.setText(R.string.shop_buy);
                boolean canBuy = GameState.money >= goods[i].price
                        && GameState.itemCounts[i] < ShopData.MAX_ITEMS;
                btn.setAlpha(canBuy ? 1f : 0.5f);
            }
        }
    }
}

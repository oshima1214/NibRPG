package com.example.nibrpg;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * メニュー画面（マップの「メニュー」ボタンで開く）。
 *   ステータス：主人公の見た目・HP・攻撃力・装備・所持金
 *   持ち物　　：道具を使う／備品（武器）を付けかえる
 *   記録　　　：先生に勝ったかどうか・遊び方・タイトルへもどる（1Fにいるときだけ）
 */
public class StatusActivity extends AppCompatActivity {

    private static final long WALK_MS = 700; // 見本が向きを変える速さ

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int step = 0;
    private CharacterSprite hero; // 主人公の見た目（キャラクリで保存したもの）

    private TextView[] tabs;
    private View[] pages;
    private ImageView imgHero;
    private TextView txtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_status);
        // 画面のふち（ステータスバーなど）に文字がかぶらないようにする
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        hero = CharacterSprite.load(this);
        imgHero = findViewById(R.id.imgHero);
        txtMessage = findViewById(R.id.txtMessage);
        tabs = new TextView[] {
            findViewById(R.id.tabStatus), findViewById(R.id.tabItems), findViewById(R.id.tabRecord),
            findViewById(R.id.tabAch), findViewById(R.id.tabHowTo), findViewById(R.id.tabTitle),
        };
        pages = new View[] {
            findViewById(R.id.pageStatus), findViewById(R.id.pageItems), findViewById(R.id.pageRecord),
            findViewById(R.id.pageAch), findViewById(R.id.pageHowTo), findViewById(R.id.pageTitle),
        };
        for (int i = 0; i < tabs.length; i++) {
            int p = i; // ラムダの中で使うため
            tabs[i].setOnClickListener(v -> selectTab(p));
        }

        // もどる → マップへ
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 遊び方（左の「遊び方」タブで見る）
        TextView txtHowTo = findViewById(R.id.txtHowTo);
        StringBuilder sb = new StringBuilder();
        for (String page : getResources().getStringArray(R.array.tutorial_pages)) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(page);
        }
        txtHowTo.setText(sb);

        // タイトルへもどる（どの階からでもOK。続きは、タイトルの「つづきから」で今の場所から遊べる）
        TextView btnTitle = findViewById(R.id.btnTitle);
        ((TextView) findViewById(R.id.txtTitleNote)).setText(R.string.menu_title_note_ok);
        btnTitle.setOnClickListener(v -> {
            GameState.save(this); // 今の階・場所を保存してから
            Intent intent = new Intent(this, TitleActivity.class);
            // マップなど、開いている画面を全部閉じてからタイトルへ
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        selectTab(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sound.resumeBgm(); // マップの曲をそのまま続ける
        handler.post(walk);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Sound.pauseBgmSoon();
        handler.removeCallbacksAndMessages(null);
    }

    /** 主人公の見本を、くるっと回して見せる（正面 → 右 → 後ろ → 左） */
    private final Runnable walk = new Runnable() {
        @Override
        public void run() {
            step = (step + 1) % 4;
            // 主人公のドット絵と同じ大きさの絵を作って、ぼかさずに大きく表示する
            Bitmap bmp = Bitmap.createBitmap(CharacterSprite.WIDTH, CharacterSprite.HEIGHT, Bitmap.Config.ARGB_8888);
            hero.draw(new Canvas(bmp), 0, 0, 1, step);
            BitmapDrawable d = new BitmapDrawable(getResources(), bmp);
            d.setFilterBitmap(false); // ドット絵をぼかさない
            imgHero.setImageDrawable(d);
            handler.postDelayed(this, WALK_MS);
        }
    };

    /** タブを切りかえる（選んでいるタブに ▶ を付ける） */
    private void selectTab(int p) {
        int[] names = { R.string.menu_status, R.string.menu_items, R.string.menu_record, R.string.menu_ach,
                R.string.menu_howto_tab, R.string.menu_title_tab };
        for (int i = 0; i < tabs.length; i++) {
            boolean on = (i == p);
            tabs[i].setText((on ? "▶ " : "　") + getString(names[i]));
            tabs[i].setTextColor(on ? 0xFFFFD54F : 0xFFFFFFFF);
            pages[i].setVisibility(on ? View.VISIBLE : View.GONE);
        }
        txtMessage.setVisibility(View.GONE);
        if (p == 0) showStatus();
        if (p == 1) showItems();
        if (p == 2) showRecord();
        if (p == 3) showAchievements();
    }

    // ===================== ステータス =====================

    private void showStatus() {
        ((TextView) findViewById(R.id.txtPlace)).setText(getString(R.string.menu_name_place,
                hero.name, getString(MapData.NAMES[GameState.floor])));
        ProgressBar bar = findViewById(R.id.barHp);
        bar.setMax(GameState.maxHp);
        bar.setProgress(GameState.hp);
        ((TextView) findViewById(R.id.txtHp)).setText(
                getString(R.string.menu_hp, GameState.hp, GameState.maxHp));

        int weaponPower = GameState.weapon >= 0 ? ShopData.WEAPONS[GameState.weapon].power : 0;
        ((TextView) findViewById(R.id.txtAttack)).setText(
                getString(R.string.menu_attack, GameState.attack(), GameState.attack() - weaponPower, weaponPower));
        String weaponName = GameState.weaponName();
        ((TextView) findViewById(R.id.txtWeapon)).setText(getString(R.string.menu_weapon,
                weaponName != null ? weaponName : getString(R.string.status_bare)));
        ((TextView) findViewById(R.id.txtMoney)).setText(getString(R.string.menu_money, GameState.money));

        int count = 0;
        for (int f = 1; f < GameState.defeated.length; f++) {
            if (GameState.defeated[f]) count++;
        }
        ((TextView) findViewById(R.id.txtDefeated)).setText(
                getString(R.string.menu_defeated, count, GameState.defeated.length - 1));
    }

    // ===================== 持ち物 =====================

    private void showItems() {
        LinearLayout list = findViewById(R.id.listItems);
        list.removeAllViews();

        // ---- 道具 ----
        list.addView(header(getString(R.string.menu_items_header)));
        boolean any = false;
        for (int i = 0; i < ShopData.ITEMS.length; i++) {
            if (GameState.itemCounts[i] <= 0) continue;
            any = true;
            ShopData.Goods g = ShopData.ITEMS[i];
            int index = i; // ラムダの中で使うため
            boolean full = GameState.hp >= GameState.maxHp;
            list.addView(row(ShopData.ITEM_IMAGES[i], g.name, g.desc,
                    getString(R.string.menu_count, GameState.itemCounts[i]),
                    getString(R.string.menu_use), !full, v -> useItem(index)));
        }
        if (!any) list.addView(note(getString(R.string.menu_no_items)));

        // ---- 備品（武器） ----
        list.addView(header(getString(R.string.menu_weapons_header)));
        any = false;
        for (int i = 0; i < ShopData.WEAPONS.length; i++) {
            if (!GameState.ownedWeapons[i]) continue;
            any = true;
            ShopData.Goods g = ShopData.WEAPONS[i];
            int index = i; // ラムダの中で使うため
            boolean using = (GameState.weapon == i);
            list.addView(row(0, g.name, g.desc, "",
                    getString(using ? R.string.menu_equipped : R.string.menu_equip), !using,
                    v -> equip(index)));
        }
        if (any) {
            // 素手（備品を持っているときだけ出す。押すと備品をしまう）
            boolean bare = (GameState.weapon < 0);
            list.addView(row(0, getString(R.string.status_bare), getString(R.string.menu_bare_desc), "",
                    getString(bare ? R.string.menu_equipped : R.string.menu_equip), !bare,
                    v -> equip(-1)));
        } else {
            list.addView(note(getString(R.string.menu_no_weapons)));
        }
    }

    /** 道具を使う（HPが回復する） */
    private void useItem(int i) {
        if (GameState.itemCounts[i] <= 0) return;
        if (GameState.hp >= GameState.maxHp) {
            message(getString(R.string.menu_hp_full));
            return;
        }
        int before = GameState.hp;
        GameState.hp = Math.min(GameState.maxHp, GameState.hp + ShopData.ITEMS[i].power);
        GameState.itemCounts[i]--;
        GameState.usedItem = true; // 実績のため
        showItems();
        message(getString(R.string.menu_used, ShopData.ITEMS[i].name, GameState.hp - before));
    }

    /** 備品（武器）を付けかえる（i が -1 なら素手にする） */
    private void equip(int i) {
        if (GameState.weapon == i) return;
        GameState.weapon = i; // -1 なら素手
        showItems();
        message(i < 0 ? getString(R.string.menu_bare_msg)
                : getString(R.string.menu_equipped_msg, ShopData.WEAPONS[i].name));
    }

    // ===================== 記録 =====================

    private void showRecord() {
        LinearLayout list = findViewById(R.id.listRecord);
        list.removeAllViews();
        for (int f = 1; f < MapData.FLOORS.length; f++) {
            String floor = getString(R.string.floor_short, f + 1);
            String text;
            int color;
            if (!GameState.visited[f]) {
                text = getString(R.string.menu_record_unknown, floor); // まだ行ったことがない
                color = 0xFF888888;
            } else if (GameState.defeated[f]) {
                text = getString(R.string.menu_record_win, floor, getString(MapData.TEACHER_NAMES[f]));
                color = 0xFFFFD54F;
            } else {
                text = getString(R.string.menu_record_not_yet, floor, getString(MapData.TEACHER_NAMES[f]));
                color = 0xFFFFFFFF;
            }
            TextView t = new TextView(this);
            t.setText(text);
            t.setTextColor(color);
            t.setTextSize(15);
            t.setPadding(0, dp(3), 0, dp(3));
            list.addView(t);
        }
    }

    // ===================== 実績 =====================

    /** 実績の一覧：1つずつ「名前・条件・今の進み具合」を出す。その下に落とし物 */
    private void showAchievements() {
        LinearLayout list = findViewById(R.id.listAchievements);
        list.removeAllViews();

        list.addView(header(getString(R.string.menu_ach_header,
                Achievements.unlockedCount(this), Achievements.COUNT)));
        String[] titles = getResources().getStringArray(R.array.ach_titles);
        String[] descs = getResources().getStringArray(R.array.ach_descs);
        for (int i = 0; i < Achievements.COUNT; i++) {
            boolean got = Achievements.isUnlocked(this, i);
            list.addView(achievementRow(got,
                    got ? titles[i] : getString(R.string.menu_ach_locked), // まだなら名前はかくす
                    descs[i],
                    got ? getString(R.string.ach_done) : progress(i)));
        }

        // ---- 落とし物 ----
        list.addView(header(getString(R.string.menu_lost_header,
                GameState.foundCount(), MapData.LOST_ITEMS.length)));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MapData.LOST_ITEMS.length; i++) {
            if (i > 0) sb.append('　');
            sb.append(GameState.foundLost[i] ? getString(MapData.LOST_ITEMS[i].name) : "？？？");
        }
        list.addView(note(sb.toString()));
    }

    /** 実績の今の進み具合（今回のプレイの記録から） */
    private String progress(int id) {
        switch (id) {
            case Achievements.CLEAR_BARE:
                return getString(R.string.ach_prog_bare,
                        getString(GameState.usedItem ? R.string.ach_used : R.string.ach_not_used),
                        getString(GameState.usedWeapon ? R.string.ach_used : R.string.ach_not_used),
                        getString(GameState.tookDamage ? R.string.ach_damaged : R.string.ach_not_damaged));
            case Achievements.NO_ITEM:
                return getString(R.string.ach_prog_item,
                        getString(GameState.usedItem ? R.string.ach_used : R.string.ach_not_used));
            case Achievements.NO_WEAPON:
                return getString(R.string.ach_prog_weapon,
                        getString(GameState.usedWeapon ? R.string.ach_used : R.string.ach_not_used));
            case Achievements.BIG_SPENDER:
            case Achievements.SAVER:
                return getString(R.string.ach_prog_money, GameState.moneySpent);
            case Achievements.COLLECTOR:
                return getString(R.string.ach_prog_count, GameState.foundCount(), MapData.LOST_ITEMS.length);
            case Achievements.CLERK_REGULAR:
                return getString(R.string.ach_prog_count,
                        Math.min(GameState.clerkTalks, Achievements.CLERK_TALK_COUNT), Achievements.CLERK_TALK_COUNT);
            case Achievements.VENDING_ALL: {
                int n = 0;
                for (boolean b : GameState.boughtItems) if (b) n++;
                return getString(R.string.ach_prog_count, n, GameState.boughtItems.length);
            }
            default:
                return ""; // 進み具合がないもの
        }
    }

    /** 実績1つ分の行：左にトロフィー（またはカギ）、右に名前・条件・進み具合 */
    private View achievementRow(boolean got, String title, String condition, String progress) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(6), dp(6), dp(6), dp(6));

        TextView icon = new TextView(this);
        icon.setText(got ? "🏆" : "🔒");
        icon.setTextSize(22);
        icon.setAlpha(got ? 1f : 0.5f);
        row.addView(icon);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(10), 0, 0, 0);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(got ? 0xFFFFD54F : 0xFFBBBBBB);
        t.setTextSize(15);
        t.getPaint().setFakeBoldText(true);
        texts.addView(t);

        TextView c = new TextView(this);
        c.setText(getString(R.string.ach_condition, condition));
        c.setTextColor(0xFFFFFFFF);
        c.setTextSize(13);
        texts.addView(c);

        if (!progress.isEmpty()) {
            TextView p = new TextView(this);
            p.setText(progress);
            p.setTextColor(got ? 0xFFFFD54F : 0xFF8FD3FF);
            p.setTextSize(12);
            texts.addView(p);
        }
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    // ===================== 部品を作る =====================

    /** 見出し（「道具」「備品」） */
    private TextView header(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFFFFD54F);
        t.setTextSize(14);
        t.setPadding(0, dp(8), 0, dp(4));
        return t;
    }

    /** 「何も持っていない」などの説明 */
    private TextView note(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFFAAAAAA);
        t.setTextSize(13);
        t.setPadding(dp(8), 0, 0, dp(4));
        return t;
    }

    /** 持ち物1行（絵・名前・説明・個数・ボタン） */
    private View row(int image, String name, String desc, String count, String button,
                     boolean enabled, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(4), dp(4), dp(4));

        if (image != 0) {
            ImageView img = new ImageView(this);
            img.setImageResource(image);
            row.addView(img, new LinearLayout.LayoutParams(dp(36), dp(36)));
        }

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(8), 0, dp(8), 0);
        TextView n = new TextView(this);
        n.setText(name);
        n.setTextColor(0xFFFFFFFF);
        n.setTextSize(15);
        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(0xFFAAAAAA);
        d.setTextSize(12);
        texts.addView(n);
        texts.addView(d);
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        if (!count.isEmpty()) {
            TextView c = new TextView(this);
            c.setText(count);
            c.setTextColor(0xFFFFFFFF);
            c.setTextSize(15);
            c.setPadding(0, 0, dp(10), 0);
            row.addView(c);
        }

        TextView b = new TextView(this);
        b.setText(button);
        b.setTextColor(0xFFFFFFFF);
        b.setTextSize(14);
        b.setGravity(Gravity.CENTER);
        b.setBackgroundResource(enabled ? R.drawable.nib_button : R.drawable.rpg_window);
        b.setAlpha(enabled ? 1f : 0.5f);
        b.setPadding(dp(14), dp(6), dp(14), dp(6));
        b.setOnClickListener(onClick);
        row.addView(b, new LinearLayout.LayoutParams(dp(96), LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    /** 下のメッセージ欄に一言出す */
    private void message(String text) {
        txtMessage.setText(text);
        txtMessage.setVisibility(View.VISIBLE);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

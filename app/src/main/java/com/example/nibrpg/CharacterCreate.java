package com.example.nibrpg;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class CharacterCreate extends Activity {
    private CharacterSprite character;
    private AvatarPreview preview;
    private LinearLayout options, tabBar;
    private TextView title, directionLabel;
    private EditText nameInput;
    private final Button[] tabButtons = new Button[7];
    private final Random random = new Random();
    private final ArrayList<SeekBar> activeSliders = new ArrayList<>();
    private int tab = 0, direction = 0, accessory = 0;
    private boolean accessoryPage = false, swipeReady = false;
    private float downX, downY;
    private String draftName = "プレイヤー";

    private static final String[] TABS = {
            "基本", "髪", "目", "肌", "服", "ズボン", "靴"
    };
    private static final String[] TITLES = {
            "基本設定", "髪型・髪色", "目の色", "肌の色",
            "上着の設定", "ボトムスの設定", "靴の色"
    };
    private static final String[] HAIR = {
            "ショート", "ボブ", "ロング",
            "センター分け", "ポニーテール"
    };
    private static final String[] SHIRT = {
            "Tシャツ", "パーカー", "ジャケット"
    };
    private static final String[] PANTS = {
            "ズボン", "スカート"
    };
    private static final String[] DIRECTIONS = {
            "正面", "右", "後ろ", "左"
    };
    private static final String[] ACCESSORIES = {
            "眼鏡", "ピアス", "リボン",
            "キャップ", "ネックレス"
    };
    private static final String[][] ACCESSORY_STYLES = {
            {"なし", "眼鏡", "サングラス"},
            {"なし", "スタッド", "リング"},
            {"なし", "左側", "右側", "中央"},
            {"なし", "前向き", "後ろ向き"},
            {"なし", "ペンダント", "チェーン"}
    };

    // アクセサリー専用カラー：30色のHEX値は重複なし
    private static final int[][] PALETTES = {
            {0xFF222222, 0xFF4B3328, 0xFF645042,
                    0xFF555D66, 0xFFB5B6B8, 0xFF847B73},
            {0xFFC9CFD1, 0xFFC7A45D, 0xFFB88576,
                    0xFFE4E6E8, 0xFF9A7148, 0xFF34363B},
            {0xFFA45E71, 0xFFC58B9E, 0xFFCFB89A,
                    0xFF5B8180, 0xFF485B7B, 0xFF56515A},
            {0xFF2E3A43, 0xFF556648, 0xFFA68E73,
                    0xFF9A4545, 0xFF374A61, 0xFFC0B7A6},
            {0xFFD0C9BE, 0xFFB89A59, 0xFF9FA7AF,
                    0xFFA97A6E, 0xFF6A7775, 0xFF5E5149}
    };
    private static final String[] ACCESSORY_INFO = {
            "眼鏡とサングラス。黒・茶・グレーなど自然な色です。",
            "スタッド・リング。金属系の色を中心にしています。",
            "左・右・中央から選択。キャップとの同時装着はできません。",
            "前向き・後ろ向きから選択。リボンとの同時装着はできません。",
            "ペンダントとチェーンを選べます。"
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.create_character);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        character = CharacterSprite.load(this);
        draftName = character.name == null
                ? "プレイヤー" : character.name;

        tabBar = findViewById(R.id.tabBar);
        options = findViewById(R.id.optionsContainer);
        title = findViewById(R.id.txtPanelTitle);
        directionLabel = findViewById(R.id.txtDirection);
        preview = new AvatarPreview(this);

        ((FrameLayout) findViewById(R.id.previewArea)).addView(
                preview, new FrameLayout.LayoutParams(-1, -1)
        );

        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            Button b = button(TABS[i]);
            b.setTextSize(15);

            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(0, dp(42), 1);
            p.setMargins(dp(1), dp(1), dp(1), dp(1));
            tabBar.addView(b, p);
            tabButtons[i] = b;
            b.setOnClickListener(v -> showTab(index));
        }

        Button left = findViewById(R.id.btnDirectionLeft);
        Button right = findViewById(R.id.btnDirectionRight);
        Button back = findViewById(R.id.btnBack);
        Button randomButton = findViewById(R.id.btnRandom);
        Button save = findViewById(R.id.btnSave);

        for (Button b : new Button[]{
                left, right, back, randomButton, save
        }) decorate(b);

        left.setOnClickListener(v -> setDirection(direction + 3));
        right.setOnClickListener(v -> setDirection(direction + 1));
        back.setOnClickListener(v -> finish());
        randomButton.setOnClickListener(v -> randomizeCharacter());
        save.setOnClickListener(v -> confirmCharacter());

        setDirection(0);
        showTab(0);
    }

    private int dp(float n) {
        return (int) (
                n * getResources().getDisplayMetrics().density + 0.5f
        );
    }

    // 黒背景・白枠・白文字
    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(5));
        d.setStroke(dp(stroke), Color.WHITE);
        return d;
    }

    private void decorate(Button b) {
        StateListDrawable d = new StateListDrawable();
        d.addState(
                new int[]{android.R.attr.state_pressed},
                shape(0xFF252525, 2)
        );
        d.addState(new int[]{}, shape(Color.BLACK, 2));
        b.setBackgroundTintList(null);
        b.setBackground(d);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        decorate(b);
        return b;
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(15);
        t.setPadding(dp(4), dp(6), dp(4), dp(6));
        return t;
    }

    private void addButton(Button b, int height) {
        options.addView(
                b, new LinearLayout.LayoutParams(-1, dp(height))
        );
    }

    private ScrollView scroller() {
        return (ScrollView) options.getParent();
    }

    private void scrollTop() {
        ScrollView v = scroller();
        v.post(() -> v.scrollTo(0, 0));
    }

    private boolean inside(View v, float x, float y) {
        if (v == null || !v.isShown()) return false;
        int[] p = new int[2];
        v.getLocationOnScreen(p);
        return x >= p[0] && y >= p[1]
                && x < p[0] + v.getWidth()
                && y < p[1] + v.getHeight();
    }

    // 基本とアクセサリーの横スワイプ切り替え
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = e.getRawX();
            downY = e.getRawY();
            View panel = (View) title.getParent();

            swipeReady = tab == 0
                    && inside(panel, downX, downY)
                    && (accessoryPage
                    || !inside(nameInput, downX, downY));

            for (SeekBar slider : activeSliders) {
                if (inside(slider, downX, downY)) {
                    swipeReady = false;
                }
            }

        } else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
            float dx = e.getRawX() - downX;
            float dy = e.getRawY() - downY;

            if (swipeReady
                    && Math.abs(dx) > dp(70)
                    && Math.abs(dx) > Math.abs(dy) * 1.5f
                    && ((dx > 0 && !accessoryPage)
                    || (dx < 0 && accessoryPage))) {

                MotionEvent cancel = MotionEvent.obtain(e);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();

                if (dx > 0) showAccessories();
                else showTab(0);

                swipeReady = false;
                return true;
            }
            swipeReady = false;

        } else if (e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            swipeReady = false;
        }
        return super.dispatchTouchEvent(e);
    }

    private void setDirection(int d) {
        direction = d % 4;
        directionLabel.setText(DIRECTIONS[direction]);
        preview.invalidate();
    }

    private void showTab(int index) {
        tab = index;
        accessoryPage = false;
        nameInput = null;
        title.setText(TITLES[index]);
        options.removeAllViews();
        activeSliders.clear();
        scrollTop();

        for (int i = 0; i < tabButtons.length; i++) {
            tabButtons[i].setBackground(
                    shape(Color.BLACK, i == index ? 3 : 1)
            );
        }

        if (index == 0) {
            showBasic();
            return;
        }

        if (index == 1)
            styleChooser("髪型", HAIR, character.hairStyle, 1);
        if (index == 4)
            styleChooser("上着", SHIRT, character.shirtStyle, 4);
        if (index == 5)
            styleChooser("ボトムス", PANTS, character.pantsStyle, 5);

        final int part = index;
        colorPicker(
                getPartColor(part),
                c -> setPartColor(part, c),
                -1, -part
        );
    }

    // 基本設定・名前入力
    private void showBasic() {
        Button next = button("アクセサリー  ▶");
        addButton(next, 42);
        next.setOnClickListener(v -> showAccessories());

        options.addView(label("キャラクター名"));
        nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setTextSize(20);
        nameInput.setTextColor(Color.WHITE);
        nameInput.setHintTextColor(0xFFB8C8D9);
        nameInput.setHint("名前を入力してください");
        nameInput.setBackgroundTintList(
                ColorStateList.valueOf(0xFF4AA8ED)
        );

        GradientDrawable cursor = new GradientDrawable();
        cursor.setColor(Color.WHITE);
        cursor.setSize(dp(2), dp(28));
        if (android.os.Build.VERSION.SDK_INT >= 29)
            nameInput.setTextCursorDrawable(cursor);

        nameInput.setText(draftName);
        nameInput.setSelectAllOnFocus(true);
        options.addView(
                nameInput, new LinearLayout.LayoutParams(-1, dp(54))
        );

        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after
            ) {}

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count
            ) {
                draftName = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        TextView info = label("");
        info.setText(R.string.character_create_info);
        options.addView(info);
        options.setFocusableInTouchMode(true);
        options.requestFocus();
    }

    // 髪型・服装の種類選択
    private void styleChooser(
            String heading, String[] choices,
            int selected, int kind
    ) {
        options.addView(label(heading));
        chooser(
                choices[Math.max(0, Math.min(
                        selected, choices.length - 1
                ))],
                () -> changeStyle(kind, -1),
                () -> changeStyle(kind, 1),
                () -> new AlertDialog.Builder(this)
                        .setTitle(heading + "を選択")
                        .setSingleChoiceItems(
                                choices, selected,
                                (dialog, which) -> {
                                    setStyle(kind, which);
                                    showTab(kind);
                                    preview.invalidate();
                                    dialog.dismiss();
                                }
                        )
                        .setNegativeButton("キャンセル", null)
                        .show()
        );
    }

    private void chooser(
            String name, Runnable previous,
            Runnable next, Runnable select
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Button a = button("◀");
        Button b = button("▶");
        TextView middle = label(name);
        middle.setGravity(Gravity.CENTER);

        row.addView(a, new LinearLayout.LayoutParams(dp(44), dp(40)));
        row.addView(middle, new LinearLayout.LayoutParams(0, dp(40), 1));
        row.addView(b, new LinearLayout.LayoutParams(dp(44), dp(40)));

        options.addView(row);
        a.setOnClickListener(v -> previous.run());
        b.setOnClickListener(v -> next.run());
        middle.setOnClickListener(v -> select.run());
    }

    private void setStyle(int kind, int value) {
        if (kind == 1) character.hairStyle = value;
        if (kind == 4) character.shirtStyle = value;
        if (kind == 5) character.pantsStyle = value;
    }

    private void changeStyle(int kind, int delta) {
        String[] list = kind == 1 ? HAIR
                : kind == 4 ? SHIRT : PANTS;

        int current = kind == 1 ? character.hairStyle
                : kind == 4 ? character.shirtStyle
                : character.pantsStyle;

        setStyle(kind, (current + delta + list.length) % list.length);
        showTab(kind);
        preview.invalidate();
    }

    // アクセサリー画面
    private void showAccessories() {
        tab = 0;
        accessoryPage = true;
        nameInput = null;
        title.setText("アクセサリー設定");
        options.removeAllViews();
        activeSliders.clear();
        scrollTop();

        Button back = button("◀ 基本設定へ戻る");
        addButton(back, 40);
        back.setOnClickListener(v -> showTab(0));

        for (int start = 0; start < ACCESSORIES.length; start += 3) {
            LinearLayout row = new LinearLayout(this);

            for (int i = start;
                 i < Math.min(start + 3, ACCESSORIES.length);
                 i++) {

                final int kind = i;
                Button b = button(ACCESSORIES[i]);
                b.setTextSize(13);
                b.setBackground(
                        shape(Color.BLACK, i == accessory ? 3 : 1)
                );

                LinearLayout.LayoutParams p =
                        new LinearLayout.LayoutParams(0, dp(40), 1);
                p.setMargins(dp(2), dp(2), dp(2), dp(2));
                row.addView(b, p);

                b.setOnClickListener(v -> {
                    accessory = kind;
                    showAccessories();
                });
            }
            options.addView(row);
        }

        final int kind = accessory;
        String[] styles = ACCESSORY_STYLES[kind];
        int current = getAccessoryStyle(kind);
        options.addView(label(ACCESSORIES[kind] + "の種類"));

        chooser(
                styles[current],
                () -> changeAccessory(kind, -1),
                () -> changeAccessory(kind, 1),
                () -> new AlertDialog.Builder(this)
                        .setTitle(ACCESSORIES[kind] + "を選択")
                        .setSingleChoiceItems(
                                styles, current,
                                (dialog, which) -> {
                                    setAccessoryStyle(kind, which);
                                    refresh();
                                    dialog.dismiss();
                                }
                        )
                        .setNegativeButton("キャンセル", null)
                        .show()
        );

        options.addView(label(ACCESSORY_INFO[kind]));

        if (current == 0) {
            options.addView(label(
                    "未装着です。種類を選ぶと色を変更できます。"
            ));
        } else {
            options.addView(label(
                    "↓ 下へスクロールすると色を調整できます"
            ));
            options.addView(label("アクセサリーの色（HSV）"));
            colorPicker(
                    getAccessoryColor(kind),
                    c -> setAccessoryColor(kind, c),
                    kind, kind
            );
        }
        options.addView(label("※ 上下にスクロールできます。"));
    }

    private int getAccessoryStyle(int i) {
        switch (i) {
            case 0: return character.glassesStyle;
            case 1: return character.earringsStyle;
            case 2: return character.ribbonStyle;
            case 3: return character.capStyle;
            default: return character.necklaceStyle;
        }
    }

    private void setAccessoryStyle(int i, int value) {
        switch (i) {
            case 0:
                character.glassesStyle = value;
                break;
            case 1:
                character.earringsStyle = value;
                break;
            case 2:
                character.ribbonStyle = value;
                if (value != 0) character.capStyle = 0;
                break;
            case 3:
                character.capStyle = value;
                if (value != 0) character.ribbonStyle = 0;
                break;
            case 4:
                character.necklaceStyle = value;
                break;
        }
    }

    private void changeAccessory(int i, int delta) {
        int count = ACCESSORY_STYLES[i].length;
        setAccessoryStyle(
                i, (getAccessoryStyle(i) + delta + count) % count
        );
        refresh();
    }

    private int getAccessoryColor(int i) {
        switch (i) {
            case 0: return character.glassesColor;
            case 1: return character.earringsColor;
            case 2: return character.ribbonColor;
            case 3: return character.capColor;
            default: return character.necklaceColor;
        }
    }

    private void setAccessoryColor(int i, int c) {
        switch (i) {
            case 0: character.glassesColor = c; break;
            case 1: character.earringsColor = c; break;
            case 2: character.ribbonColor = c; break;
            case 3: character.capColor = c; break;
            case 4: character.necklaceColor = c; break;
        }
    }

    private int getPartColor(int i) {
        switch (i) {
            case 2: return character.eyes;
            case 3: return character.skin;
            case 4: return character.shirt;
            case 5: return character.pants;
            case 6: return character.shoes;
            default: return character.hair;
        }
    }

    private void setPartColor(int i, int c) {
        switch (i) {
            case 1: character.hair = c; break;
            case 2: character.eyes = c; break;
            case 3: character.skin = c; break;
            case 4: character.shirt = c; break;
            case 5: character.pants = c; break;
            case 6: character.shoes = c; break;
        }
    }

    private interface ColorSetter {
        void set(int color);
    }

    // 類似色の判定基準。小さくすると警告が減る
    private static final double COLOR_THRESHOLD = 85.0;

    private static final double RANDOM_COLOR_THRESHOLD = 140.0;

    private double colorGap(int a, int b) {
        int dr = Color.red(a) - Color.red(b);
        int dg = Color.green(a) - Color.green(b);
        int db = Color.blue(a) - Color.blue(b);

        return Math.sqrt(
                3.0 * dr * dr
                        + 4.0 * dg * dg
                        + 2.0 * db * db
        );
    }

    // target：0〜4=アクセサリー、-1=髪、-3=肌、-4=服
    private String colorConflict(int target, int color) {
        if (target >= 0) {
            // 装着していないものは判定しない
            if (getAccessoryStyle(target) == 0) return null;

            if (target <= 3
                    && colorGap(color, character.hair)
                    < COLOR_THRESHOLD) {
                return "髪色";
            }

            if ((target == 0 || target == 1 || target == 4)
                    && colorGap(color, character.skin)
                    < COLOR_THRESHOLD) {
                return "肌色";
            }

            if (target == 4
                    && colorGap(color, character.shirt)
                    < COLOR_THRESHOLD) {
                return "服の色";
            }

            return null;
        }

        int part = -target;

        for (int i = 0; i < ACCESSORIES.length; i++) {
            if (getAccessoryStyle(i) == 0) continue;

            boolean compare =
                    (part == 1 && i <= 3)
                            || (part == 3 && (i == 0 || i == 1 || i == 4))
                            || (part == 4 && i == 4);

            if (compare && colorGap(
                    color, getAccessoryColor(i)
            ) < COLOR_THRESHOLD) {
                return ACCESSORIES[i] + "の色";
            }
        }

        return null;
    }

    // HSV操作中には警告しない
    private void verifyColor(
            ColorSetter setter,
            int before,
            int after,
            int target,
            boolean applied
    ) {
        if (before == after) return;

        String conflict = colorConflict(target, after);

        if (conflict == null) {
            if (!applied) {
                setter.set(after);
                refresh();
            }
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("色が似ています")
                .setMessage(
                        "選択した色は「" + conflict + "」に近いため、"
                                + "アクセサリーが見えにくくなる可能性があります。\n"
                                + "この色を使いますか？"
                )
                .setNegativeButton("色を選び直す", (d, w) -> {
                    if (applied) {
                        setter.set(before);
                        refresh();
                    }
                })
                .setPositiveButton("そのまま使用", (d, w) -> {
                    setter.set(after);
                    refresh();
                })
                .show();
    }

    // HSV・HEX・プリセット
    private void colorPicker(
            int color, ColorSetter setter,
            int presetKind, int target
    ) {
        LinearLayout sampleRow = new LinearLayout(this);
        sampleRow.setGravity(Gravity.CENTER_VERTICAL);

        View sample = new View(this);
        sample.setBackgroundColor(color);
        TextView hex = label(String.format(
                Locale.US, "#%06X  ✎", color & 0xFFFFFF
        ));

        sampleRow.addView(
                sample, new LinearLayout.LayoutParams(dp(40), dp(24))
        );
        sampleRow.addView(hex);
        options.addView(sampleRow);

        // HEX入力
        hex.setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setSingleLine(true);
            input.setText(
                    hex.getText().toString().replace("  ✎", "")
            );

            new AlertDialog.Builder(this)
                    .setTitle("カラーコードを入力")
                    .setView(input)
                    .setNegativeButton("キャンセル", null)
                    .setPositiveButton("反映", (d, which) -> {
                        String s = input.getText().toString().trim();
                        if (!s.startsWith("#")) s = "#" + s;

                        if (!s.matches("#[0-9a-fA-F]{6}")) {
                            new AlertDialog.Builder(this)
                                    .setMessage("HEXは6桁で入力してね")
                                    .setPositiveButton("OK", null)
                                    .show();
                            return;
                        }

                        verifyColor(
                                setter, color,
                                Color.parseColor(s),
                                target, false
                        );
                    })
                    .show();
        });

        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        SeekBar h = slider(
                "色相", 360, Math.round(hsv[0])
        );
        SeekBar s = slider(
                "彩度", 100, Math.round(hsv[1] * 100)
        );
        SeekBar v = slider(
                "明るさ", 100, Math.round(hsv[2] * 100)
        );

        updateBars(h, s, v);

        final int[] shown = {color};
        final int[] beforeDrag = {color};

        SeekBar.OnSeekBarChangeListener listener =
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar bar, int progress, boolean user
                    ) {
                        if (!user) return;

                        int c = Color.HSVToColor(new float[]{
                                h.getProgress(),
                                s.getProgress() / 100f,
                                v.getProgress() / 100f
                        });

                        shown[0] = c;
                        setter.set(c);
                        sample.setBackgroundColor(c);
                        hex.setText(String.format(
                                Locale.US, "#%06X  ✎", c & 0xFFFFFF
                        ));

                        updateBars(h, s, v);
                        preview.invalidate();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar bar) {
                        beforeDrag[0] = shown[0];
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar bar) {
                        verifyColor(
                                setter, beforeDrag[0],
                                shown[0], target, true
                        );
                    }
                };

        h.setOnSeekBarChangeListener(listener);
        s.setOnSeekBarChangeListener(listener);
        v.setOnSeekBarChangeListener(listener);

        // アクセサリー専用プリセット
        if (presetKind >= 0) {
            options.addView(label("このアクセサリーに合う色"));
            LinearLayout palette = new LinearLayout(this);

            for (int c : PALETTES[presetKind]) {
                View chip = new View(this);
                chip.setBackground(shape(c, 1));

                LinearLayout.LayoutParams p =
                        new LinearLayout.LayoutParams(
                                0, dp(32), 1
                        );
                p.setMargins(dp(2), 0, dp(2), 0);
                palette.addView(chip, p);

                chip.setOnClickListener(w -> verifyColor(
                        setter, getAccessoryColor(presetKind),
                        c, target, false
                ));
            }
            options.addView(palette);
        }
    }

    private SeekBar slider(
            String name, int max, int initial
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView text = label(name);
        row.addView(
                text, new LinearLayout.LayoutParams(dp(58), dp(42))
        );

        FrameLayout area = new FrameLayout(this);
        View bar = new View(this);

        FrameLayout.LayoutParams bp =
                new FrameLayout.LayoutParams(
                        -1, dp(10), Gravity.CENTER_VERTICAL
                );
        bp.setMargins(dp(16), 0, dp(16), 0);
        area.addView(bar, bp);

        SeekBar seek = new SeekBar(this);
        seek.setMax(max);
        seek.setProgress(initial);
        seek.setProgressTintList(
                ColorStateList.valueOf(Color.TRANSPARENT)
        );
        seek.setProgressBackgroundTintList(
                ColorStateList.valueOf(Color.TRANSPARENT)
        );
        seek.setThumbTintList(
                ColorStateList.valueOf(Color.WHITE)
        );
        seek.setTag(bar);
        activeSliders.add(seek);

        area.addView(
                seek, new FrameLayout.LayoutParams(-1, -1)
        );
        row.addView(
                area, new LinearLayout.LayoutParams(0, dp(42), 1)
        );
        options.addView(row);

        return seek;
    }

    private void gradient(SeekBar slider, int... colors) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                colors
        );
        d.setCornerRadius(dp(5));
        ((View) slider.getTag()).setBackground(d);
    }

    private void updateBars(
            SeekBar h, SeekBar s, SeekBar v
    ) {
        float hue = h.getProgress();
        float sat = s.getProgress() / 100f;
        float value = v.getProgress() / 100f;

        gradient(
                h, Color.RED, Color.YELLOW, Color.GREEN,
                Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
        );

        gradient(
                s,
                Color.HSVToColor(new float[]{hue, 0, value}),
                Color.HSVToColor(new float[]{hue, 1, value})
        );

        gradient(
                v, Color.BLACK,
                Color.HSVToColor(new float[]{hue, sat, 1})
        );
    }

    // 設定画面のスクロール位置を維持
    private void refresh() {
        ScrollView scroll = scroller();
        int y = scroll.getScrollY();

        if (accessoryPage) showAccessories();
        else showTab(tab);

        scroll.post(() -> scroll.scrollTo(0, y));
        preview.invalidate();
    }

    // ランダム生成時は色が近すぎない候補を優先
    private int randomPalette(int kind) {
        int[] colors = PALETTES[kind];
        int[] safe = new int[colors.length];
        int count = 0;

        int best = colors[0];
        double bestGap = -1;

        for (int c : colors) {
            double gap = Double.MAX_VALUE;

            // 眼鏡・ピアス・リボン・キャップは髪色と比較
            if (kind <= 3) {
                gap = Math.min(gap,
                        colorGap(c, character.hair));
            }

            // 眼鏡・ピアス・ネックレスは肌色とも比較
            if (kind == 0 || kind == 1 || kind == 4) {
                gap = Math.min(gap,
                        colorGap(c, character.skin));
            }

            // ネックレスは服色とも比較
            if (kind == 4) {
                gap = Math.min(gap,
                        colorGap(c, character.shirt));
            }

            // 十分に違う色だけ候補にする
            if (gap >= RANDOM_COLOR_THRESHOLD) {
                safe[count++] = c;
            }

            // 最も色の差が大きい候補も記録
            if (gap > bestGap) {
                bestGap = gap;
                best = c;
            }
        }

        // 安全な色からランダム選択
        if (count > 0) {
            return safe[random.nextInt(count)];
        }

        // 候補がない場合は最も離れた色
        return best;
    }

    // ランダム生成
    private void randomizeCharacter() {
        int[] hair = {
                0xFF503227, 0xFF22232B, 0xFFB78136,
                0xFFC9A66B, 0xFF943F42
        };
        int[] skin = {
                0xFFF0C9A1, 0xFFE2AD81,
                0xFFC98D61, 0xFFA96F49
        };
        int[] clothes = {
                0xFF326EAB, 0xFF5E936A, 0xFFBD665D,
                0xFF755DA8, 0xFF586274
        };

        character.hairStyle = random.nextInt(HAIR.length);
        character.shirtStyle = random.nextInt(SHIRT.length);
        character.pantsStyle = random.nextInt(PANTS.length);

        character.hair = hair[random.nextInt(hair.length)];
        character.skin = skin[random.nextInt(skin.length)];

        int shirt = random.nextInt(clothes.length);
        int pants;

        do {
            pants = random.nextInt(clothes.length);
        } while (pants == shirt);

        character.shirt = clothes[shirt];
        character.pants = clothes[pants];
        character.eyes = random.nextBoolean()
                ? 0xFF426A96 : 0xFF66492D;
        character.shoes = random.nextBoolean()
                ? 0xFF30343C : 0xFF9C9D9D;

        character.glassesStyle = random.nextInt(3);
        character.earringsStyle = random.nextInt(3);
        character.ribbonStyle = random.nextInt(4);
        character.capStyle = character.ribbonStyle == 0
                ? random.nextInt(3) : 0;
        character.necklaceStyle = random.nextInt(3);

        character.glassesColor = randomPalette(0);
        character.earringsColor = randomPalette(1);
        character.ribbonColor = randomPalette(2);
        character.capColor = randomPalette(3);
        character.necklaceColor = randomPalette(4);

        refresh();
    }

    // キャラの保存
    private void confirmCharacter() {
        String n = draftName.trim();
        final String name = n.isEmpty()
                ? "プレイヤー" : n;

        new AlertDialog.Builder(this)
                .setTitle("キャラクター作成")
                .setMessage(
                        "名前：" + name
                                + "\nこの見た目で決定しますか？"
                )
                .setNegativeButton("キャンセル", null)
                .setPositiveButton("OK", (dialog, which) -> {
                    character.name = name;
                    character.save(this);
                    finish();
                })
                .show();
    }

    // タイトルの曲を、そのまま続ける
    @Override
    protected void onResume() {
        super.onResume();
        Sound.bgm(this, R.raw.bgm_title);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Sound.pauseBgmSoon();
    }

    // 左側のプレビュー
    private class AvatarPreview extends View {
        AvatarPreview(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);

            float scale = Math.min(
                    getWidth() / (CharacterSprite.WIDTH + 4f),
                    getHeight() / (CharacterSprite.HEIGHT + 4f)
            );

            if (scale <= 0) return;

            float x = (
                    getWidth() - CharacterSprite.WIDTH * scale
            ) / 2f;

            float y = (
                    getHeight() - CharacterSprite.HEIGHT * scale
            ) / 2f;

            character.draw(canvas, x, y, scale, direction);
        }
    }
}

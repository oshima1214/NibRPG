package com.example.nibrpg;

import android.graphics.drawable.GradientDrawable;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.Random;

public class CharacterCreate extends Activity {

    private CharacterSprite character;
    private AvatarPreview preview;

    private LinearLayout tabBar;
    private LinearLayout optionsContainer;
    private TextView txtPanelTitle;
    private TextView txtDirection;

    private final Button[] tabButtons = new Button[7];

    private final String[] tabs = {
            "基本", "髪", "目", "肌",
            "服", "ズボン", "靴"
    };

    private final String[] panelNames = {
            "基本設定", "髪型・髪色", "目の色",
            "肌の色", "上着の設定",
            "ボトムスの設定", "靴の色"
    };

    private final String[] hairStyles = {
            "ショート", "ボブ", "ロング",
            "センター分け", "ポニーテール"
    };

    private final String[] shirtStyles = {
            "Tシャツ", "パーカー", "ジャケット"
    };

    private final String[] pantsStyles = {
            "ズボン", "スカート"
    };

    private final String[] directions = {
            "正面", "右", "後ろ", "左"
    };

    private int currentTab = 0;
    private int direction = 0;
    private String draftName = "プレイヤー";

    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_character);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams
                        .SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        character = CharacterSprite.load(this);
        draftName = character.name == null
                ? "プレイヤー" : character.name;

        tabBar = findViewById(R.id.tabBar);
        optionsContainer = findViewById(
                R.id.optionsContainer
        );
        txtPanelTitle = findViewById(
                R.id.txtPanelTitle
        );
        txtDirection = findViewById(
                R.id.txtDirection
        );

        FrameLayout previewArea =
                findViewById(R.id.previewArea);

        preview = new AvatarPreview(this);

        previewArea.addView(
                preview,
                new FrameLayout.LayoutParams(-1, -1)
        );

        // タブを自動生成
        createTabs();

        // キャラクターの向き
        findViewById(R.id.btnDirectionLeft)
                .setOnClickListener(v -> {
                    direction = (direction + 3) % 4;
                    updateDirection();
                });

        findViewById(R.id.btnDirectionRight)
                .setOnClickListener(v -> {
                    direction = (direction + 1) % 4;
                    updateDirection();
                });

        // 戻る
        findViewById(R.id.btnBack)
                .setOnClickListener(v -> finish());

        // ランダム
        findViewById(R.id.btnRandom)
                .setOnClickListener(v -> randomizeCharacter());

        // キャラクター作成
        findViewById(R.id.btnSave)
                .setOnClickListener(v -> confirmCharacter());

        updateDirection();
        showTab(0);
    }

    private int dp(float value) {
        return (int) (
                value *
                        getResources().getDisplayMetrics().density
                        + 0.5f
        );
    }

    // 上部のタブを作る
    private void createTabs() {

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;

            Button button = new Button(this);
            button.setText(tabs[i]);
            button.setTextSize(20);
            button.setTextColor(Color.WHITE);
            button.setAllCaps(false);
            button.setPadding(0, 0, 0, 0);
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setMinHeight(0);
            button.setMinimumHeight(0);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            0, dp(42), 1
                    );

            params.setMargins(
                    dp(1), dp(1), dp(1), dp(1)
            );

            tabBar.addView(button, params);
            tabButtons[i] = button;

            button.setOnClickListener(
                    v -> showTab(index)
            );
        }
    }

    // 選択したタブの設定だけを表示
    private void showTab(int index) {
        currentTab = index;

        for (int i = 0; i < tabButtons.length; i++) {
            int color = i == index
                    ? Color.rgb(90, 126, 194)
                    : Color.rgb(49, 67, 107);

            tabButtons[i].setBackgroundTintList(
                    ColorStateList.valueOf(color)
            );
        }

        txtPanelTitle.setText(panelNames[index]);

        // 前の設定を消す
        optionsContainer.removeAllViews();

        if (index == 0) {
            showBasicSettings();
            return;
        }

        if (index == 1) {
            addStyleChooser(
                    "髪型", hairStyles,
                    character.hairStyle, 1
            );
        } else if (index == 4) {
            addStyleChooser(
                    "上着", shirtStyles,
                    character.shirtStyle, 4
            );
        } else if (index == 5) {
            addStyleChooser(
                    "ボトムス", pantsStyles,
                    character.pantsStyle, 5
            );
        }

        // 選択した部分の色設定
        addColorControls(index);
    }

    // 基本設定
    private void showBasicSettings() {

        addLabel();

        EditText nameInput = new EditText(this);

        nameInput.setSingleLine(true);
        nameInput.setTextSize(20);
        nameInput.setTextColor(Color.WHITE);
// 名前入力カーソルの色
        GradientDrawable cursorDrawable =
                new GradientDrawable();

        cursorDrawable.setColor(
                Color.parseColor("#FFFFFF")
        );

// カーソルの幅と高さ
        cursorDrawable.setSize(dp(2), dp(28));

        if (android.os.Build.VERSION.SDK_INT >= 29) {
            nameInput.setTextCursorDrawable(cursorDrawable);
        }

        nameInput.setHintTextColor(0xFFB8C8D9);
        nameInput.setHint("名前を入力してください");


// 名前入力欄の下線の色
        nameInput.setBackgroundTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#4AA8ED")
                )
        );

        nameInput.setMaxLines(1);
        nameInput.setMaxWidth(dp(400));
        nameInput.setText(draftName);
        nameInput.setSelectAllOnFocus(true);

        optionsContainer.addView(
                nameInput,
                new LinearLayout.LayoutParams(
                        -1, dp(54)
                )
        );

        nameInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        draftName = s.toString();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );


        TextView infoText = new TextView(this);

        infoText.setText(R.string.character_create_info
        );

        infoText.setTextSize(18);
        infoText.setTextColor(Color.WHITE);
        infoText.setPadding(
                dp(4), dp(5), dp(4), dp(5)
        );

        optionsContainer.addView(infoText);


        optionsContainer.setFocusableInTouchMode(true);
        optionsContainer.requestFocus();
    }

    // 文字を追加
    private void addLabel() {
        TextView label = new TextView(this);
        label.setText("キャラクター名");
        label.setTextSize(14);
        label.setTextColor(Color.WHITE);
        label.setPadding(
                dp(4), dp(5), dp(4), dp(5)
        );

        optionsContainer.addView(label);
    }

    // 髪型・上着・ボトムスの種類選択
    private void addStyleChooser(
            String title,
            String[] choices,
            int current,
            int kind) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button previous = smallButton("◀");
        Button next = smallButton("▶");

        TextView selected = new TextView(this);
        selected.setGravity(Gravity.CENTER);
        selected.setTextColor(Color.WHITE);
        selected.setTextSize(15);

        int safeIndex = Math.max(
                0, Math.min(current, choices.length - 1)
        );

        selected.setText(choices[safeIndex]);

        row.addView(
                previous,
                new LinearLayout.LayoutParams(
                        dp(44), dp(40)
                )
        );

        row.addView(
                selected,
                new LinearLayout.LayoutParams(
                        0, dp(40), 1
                )
        );

        row.addView(
                next,
                new LinearLayout.LayoutParams(
                        dp(44), dp(40)
                )
        );

        optionsContainer.addView(row);

        previous.setOnClickListener(
                v -> changeStyle(kind, -1)
        );

        next.setOnClickListener(
                v -> changeStyle(kind, 1)
        );

        // 中央の名前をタップして一覧からも選択可能
        selected.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(title + "を選択")
                .setSingleChoiceItems(
                        choices, safeIndex,
                        (dialog, which) -> {
                            setStyle(kind, which);
                            showTab(kind);
                            preview.invalidate();
                            dialog.dismiss();
                        }
                )
                .setNegativeButton(
                        "キャンセル", null
                )
                .show());
    }

    private Button smallButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setBackgroundTintList(
                ColorStateList.valueOf(0xFF506BA3)
        );
        return button;
    }

    private int getStyle(int kind) {
        if (kind == 1) return character.hairStyle;
        if (kind == 4) return character.shirtStyle;
        return character.pantsStyle;
    }

    private void setStyle(int kind, int value) {
        if (kind == 1) character.hairStyle = value;
        else if (kind == 4) character.shirtStyle = value;
        else if (kind == 5) character.pantsStyle = value;
    }

    private void changeStyle(int kind, int delta) {
        int count = kind == 1 ? hairStyles.length
                : kind == 4 ? shirtStyles.length
                : pantsStyles.length;

        int next = (getStyle(kind) + delta + count)
                % count;

        setStyle(kind, next);
        showTab(kind);
        preview.invalidate();
    }

    private int getPartColor(int part) {
        switch (part) {
            case 2: return character.eyes;
            case 3: return character.skin;
            case 4: return character.shirt;
            case 5: return character.pants;
            case 6: return character.shoes;
            default: return character.hair;
        }
    }

    private void setColor(int part, int color) {
        switch (part) {
            case 1:
                character.hair = color;
                break;
            case 2:
                character.eyes = color;
                break;
            case 3:
                character.skin = color;
                break;
            case 4:
                character.shirt = color;
                break;
            case 5:
                character.pants = color;
                break;
            case 6:
                character.shoes = color;
                break;
        }
    }

    // 選択中のパーツだけHSV設定を表示
    private void addColorControls(int part) {

        int color = getPartColor(part);

        LinearLayout sampleRow =
                new LinearLayout(this);

        sampleRow.setGravity(Gravity.CENTER_VERTICAL);

        View sample = new View(this);
        sample.setBackgroundColor(color);

        TextView hexText = new TextView(this);
        hexText.setTextColor(Color.WHITE);
        hexText.setTextSize(14);
        hexText.setPadding(dp(12), 0, 0, 0);

        sampleRow.addView(
                sample,
                new LinearLayout.LayoutParams(
                        dp(42), dp(23)
                )
        );

        sampleRow.addView(hexText);

        optionsContainer.addView(
                sampleRow,
                new LinearLayout.LayoutParams(
                        -1, dp(33)
                )
        );

        hexText.setText(String.format(
                Locale.US, "#%06X", color & 0xFFFFFF
        ));

        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        SeekBar hue = makeSlider(
                "色相", 360, Math.round(hsv[0])
        );

        SeekBar saturation = makeSlider(
                "彩度", 100, Math.round(hsv[1] * 100)
        );

        SeekBar value = makeSlider(
                "明るさ", 100, Math.round(hsv[2] * 100)
        );

        // 選択中の色に合わせてHSVバーを描画
        updateHsvBars(hue, saturation, value);

        SeekBar.OnSeekBarChangeListener listener =
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar bar,
                            int progress,
                            boolean fromUser) {

                        if (!fromUser) return;

                        float[] newHsv = {
                                hue.getProgress(),
                                saturation.getProgress() / 100f,
                                value.getProgress() / 100f
                        };

                        int newColor =
                                Color.HSVToColor(newHsv);

                        setColor(part, newColor);

                        sample.setBackgroundColor(newColor);

                        hexText.setText(String.format(
                                Locale.US, "#%06X",
                                newColor & 0xFFFFFF
                        ));

                        updateHsvBars(hue, saturation, value);
                        preview.invalidate();
                    }

                    @Override
                    public void onStartTrackingTouch(
                            SeekBar bar) {}

                    @Override
                    public void onStopTrackingTouch(
                            SeekBar bar) {}
                };

        hue.setOnSeekBarChangeListener(listener);
        saturation.setOnSeekBarChangeListener(listener);
        value.setOnSeekBarChangeListener(listener);
    }

    // HSVスライダーの見た目
    private SeekBar makeSlider(
            String label, int max, int progress) {

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);

        row.addView(
                title,
                new LinearLayout.LayoutParams(
                        dp(58), dp(42)
                )
        );

        FrameLayout sliderArea = new FrameLayout(this);

        // 色付きグラデーションバー
        View colorBar = new View(this);
        FrameLayout.LayoutParams barParams =
                new FrameLayout.LayoutParams(
                        -1, dp(10), Gravity.CENTER_VERTICAL
                );
        barParams.setMargins(dp(16), 0, dp(16), 0);
        sliderArea.addView(colorBar, barParams);

        // 既存のSeekBarの操作機能を利用
        SeekBar slider = new SeekBar(this);
        slider.setMax(max);
        slider.setProgress(progress);
        slider.setProgressTintList(
                ColorStateList.valueOf(Color.TRANSPARENT)
        );
        slider.setProgressBackgroundTintList(
                ColorStateList.valueOf(Color.TRANSPARENT)
        );
        slider.setThumbTintList(
                ColorStateList.valueOf(Color.WHITE)
        );

        sliderArea.addView(
                slider,
                new FrameLayout.LayoutParams(-1, -1)
        );

        // グラデーションを更新するための参照
        slider.setTag(colorBar);

        row.addView(
                sliderArea,
                new LinearLayout.LayoutParams(
                        0, dp(42), 1
                )
        );

        optionsContainer.addView(row);
        return slider;
    }

    // グラデーションを設定
    private void setBarGradient(View bar, int[] colors) {
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                colors
        );
        gradient.setCornerRadius(dp(5));
        bar.setBackground(gradient);
    }

    // HSVバーの色を更新
    private void updateHsvBars(
            SeekBar hue,
            SeekBar saturation,
            SeekBar value) {

        float h = hue.getProgress();
        float s = saturation.getProgress() / 100f;
        float v = value.getProgress() / 100f;

        View hueBar = (View) hue.getTag();
        View saturationBar = (View) saturation.getTag();
        View valueBar = (View) value.getTag();

        // 色相：虹色
        setBarGradient(hueBar, new int[]{
                Color.RED,
                Color.YELLOW,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.MAGENTA,
                Color.RED
        });

        // 彩度：無彩色から鮮やかな色へ
        int desaturated = Color.HSVToColor(
                new float[]{h, 0f, v}
        );

        int saturated = Color.HSVToColor(
                new float[]{h, 1f, v}
        );

        setBarGradient(saturationBar, new int[]{
                desaturated,
                saturated
        });

        // 明るさ：黒から最大の明るさへ
        int brightest = Color.HSVToColor(
                new float[]{h, s, 1f}
        );

        setBarGradient(valueBar, new int[]{
                Color.BLACK,
                brightest
        });
    }

    // ランダムに見た目を変更
    private void randomizeCharacter() {

        int[] hairColors = {
                0xFF503227, 0xFF22232B,
                0xFFB78136, 0xFFC9A66B,
                0xFF943F42
        };

        int[] skinColors = {
                0xFFF0C9A1, 0xFFE2AD81,
                0xFFC98D61, 0xFFA96F49
        };

        int[] clothingColors = {
                0xFF326EAB, 0xFF5E936A,
                0xFFBD665D, 0xFF755DA8,
                0xFF586274
        };

        character.hairStyle = random.nextInt(
                hairStyles.length
        );

        character.shirtStyle = random.nextInt(
                shirtStyles.length
        );

        character.pantsStyle = random.nextInt(
                pantsStyles.length
        );

        character.hair = hairColors[
                random.nextInt(hairColors.length)
                ];

        character.skin = skinColors[
                random.nextInt(skinColors.length)
                ];


        // 上着の色をランダムに決める
        int shirtIndex = random.nextInt(clothingColors.length);

        // ボトムスは上着と違う色にする
        int pantsIndex;
        do {
            pantsIndex = random.nextInt(clothingColors.length);
        } while (pantsIndex == shirtIndex);

        character.shirt = clothingColors[shirtIndex];
        character.pants = clothingColors[pantsIndex];


        character.eyes = random.nextBoolean()
                ? 0xFF426A96 : 0xFF66492D;

        character.shoes = random.nextBoolean()
                ? 0xFF30343C : 0xFF9C9D9D;

        preview.invalidate();
        showTab(currentTab);
    }

    private void updateDirection() {
        txtDirection.setText(directions[direction]);
        preview.invalidate();
    }

    // 決定ボタンを押したとき：見た目と名前を保存して、タイトルにもどる
    // （ゲームの進み具合は消さない。続きは、タイトルの「つづきから」で遊べる）
    private void confirmCharacter() {
        String name = draftName.trim();

        if (name.isEmpty()) {
            name = "プレイヤー";
        }

        final String finalName = name;

        new AlertDialog.Builder(this)
                .setTitle("キャラクター作成")
                .setMessage(
                        "名前：" + finalName +
                                "\nこの見た目で決定しますか？"
                )
                .setNegativeButton(
                        "キャンセル", null
                )
                .setPositiveButton(
                        "OK", (dialog, which) -> {
                            character.name = finalName;
                            character.save(this);
                            finish(); // タイトルにもどる
                        }
                )
                .show();
    }

    // 左側のキャラクター表示
    private class AvatarPreview extends View {

        public AvatarPreview(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);

            float scale = Math.min(
                    getWidth() / 24f,
                    getHeight() / 33f
            );

            if (scale <= 0) return;

            float x = (
                    getWidth() - 20f * scale
            ) / 2f;

            float y = (
                    getHeight() - 29f * scale
            ) / 2f;

            character.draw(
                    canvas,
                    x, y, scale, direction
            );
        }
    }
}

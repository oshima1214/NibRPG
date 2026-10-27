package com.example.nibrpg;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

/**
 * キャラクリエイト画面：主人公の髪・目・服の色を選ぶ。
 * タイトル画面の「見た目を変える」からだけ開く。
 *   「決定」→ 色を変えてタイトルにもどる
 *   「やめる」・戻るボタン → 色を変えずにタイトルにもどる
 */
public class CharaCreateActivity extends AppCompatActivity {

    // 選んでいる場所（タブ）
    private static final int HAIR = 0;
    private static final int EYE = 1;
    private static final int CLOTH = 2;

    // 場所ごとの「基本色」
    private static final int[][] PRESETS = {
        { 0xFF302A2C, 0xFF5A3A28, 0xFF9A6234, 0xFFE8C860,   // 髪：黒・こげ茶・茶・金
          0xFFB8BCC8, 0xFFB0383A, 0xFF34508C, 0xFFE58FB0 }, //     銀・赤・青・ピンク
        { 0xFF3C3246, 0xFF7A4A2A, 0xFF3A6AC8, 0xFF3A9A5A,   // 目：黒・茶・青・緑
          0xFFC83A3A, 0xFF8A4AC0, 0xFFD8A830, 0xFF8A8A9A }, //     赤・紫・金・灰
        { 0xFFD6C4A4, 0xFFF0F0F0, 0xFF9A9CA4, 0xFF2A2A32,   // 服：ベージュ・白・灰・黒
          0xFF2E3E78, 0xFFB83A3A, 0xFF4E7A4A, 0xFF7AB8D8 }, //     紺・赤・緑・水色
    };

    private static final long WALK_MS = 300; // 見本が足ぶみする速さ

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int[] colors = new int[3]; // 今選んでいる色（決定するまで GameState は変えない）
    private int part = HAIR;
    private int dir = HeroSprite.DOWN;
    private int step = 0;
    private Bitmap[][] previewSet;
    private boolean settingHex = false; // プログラムで16進の欄を書きかえている間は true

    private ImageView imgPreview;
    private ColorPickerView picker;
    private View swatch;
    private EditText edtHex;
    private TextView txtRgb;
    private GridLayout gridPresets;
    private TextView[] tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chara_create);
        // 画面のふち（ステータスバーなど）に文字がかぶらないようにする
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        colors[HAIR] = GameState.hairColor;
        colors[EYE] = GameState.eyeColor;
        colors[CLOTH] = GameState.clothColor;

        imgPreview = findViewById(R.id.imgPreview);
        picker = findViewById(R.id.picker);
        swatch = findViewById(R.id.swatch);
        edtHex = findViewById(R.id.edtHex);
        txtRgb = findViewById(R.id.txtRgb);
        gridPresets = findViewById(R.id.gridPresets);
        tabs = new TextView[] {
            findViewById(R.id.tabHair), findViewById(R.id.tabEye), findViewById(R.id.tabCloth),
        };

        // タブ（髪・目・服）
        for (int i = 0; i < tabs.length; i++) {
            int p = i; // ラムダの中で使うため
            tabs[i].setOnClickListener(v -> selectPart(p));
        }

        // 色を選ぶ部品をなぞったとき
        picker.setOnColorChangeListener(c -> setColor(c, false));

        // 16進（#RRGGBB）の欄に6文字入れたら、その色にする
        edtHex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (settingHex || s.length() != 6) return;
                try {
                    int c = 0xFF000000 | Integer.parseInt(s.toString(), 16);
                    setColor(c, true);
                } catch (NumberFormatException ignored) {
                    // 0〜9・A〜F 以外の文字が入っているときは何もしない
                }
            }
        });
        edtHex.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                showHex(colors[part]); // 6文字そろっていなければ元にもどす
                return true;
            }
            return false;
        });

        // 向きを変える（◀ ▶）
        findViewById(R.id.btnTurnLeft).setOnClickListener(v -> turn(-1));
        findViewById(R.id.btnTurnRight).setOnClickListener(v -> turn(1));

        // デフォルトに戻す（3か所とも）
        findViewById(R.id.btnDefault).setOnClickListener(v -> {
            colors[HAIR] = GameState.DEFAULT_HAIR;
            colors[EYE] = GameState.DEFAULT_EYE;
            colors[CLOTH] = GameState.DEFAULT_CLOTH;
            selectPart(part);
            rebuildPreview();
        });

        // 決定
        findViewById(R.id.btnOk).setOnClickListener(v -> decide());

        // やめる（色を変えずにタイトルへ。戻るボタンも同じ）
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());

        selectPart(HAIR);
        rebuildPreview();
    }

    /** 見本の足ぶみを始める／止める */
    @Override
    protected void onResume() {
        super.onResume();
        handler.post(walk);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    private final Runnable walk = new Runnable() {
        @Override
        public void run() {
            step = (step + 1) % 4;
            showPreview();
            handler.postDelayed(this, WALK_MS);
        }
    };

    /** タブを切りかえる */
    private void selectPart(int p) {
        part = p;
        for (int i = 0; i < tabs.length; i++) {
            boolean on = (i == p);
            tabs[i].setBackgroundResource(on ? R.drawable.nib_button : R.drawable.rpg_window);
            tabs[i].setTextColor(on ? 0xFFFFFFFF : 0xFF999999);
        }
        picker.setColor(colors[p]);
        showColor(colors[p]);
        showHex(colors[p]);
        makePresets();
    }

    /**
     * 今のタブの色を変える
     * @param fromHex 16進の欄から入れたときは true（入力中の欄を書きかえないため）
     */
    private void setColor(int c, boolean fromHex) {
        colors[part] = c;
        showColor(c);
        if (fromHex) {
            picker.setColor(c);
        } else {
            showHex(c);
        }
        rebuildPreview();
    }

    /** 色見本と RGB の数字を表示する */
    private void showColor(int c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(c);
        d.setStroke(dp(2), 0xFFFFFFFF);
        d.setCornerRadius(dp(4));
        swatch.setBackground(d);
        txtRgb.setText(getString(R.string.chara_rgb, Color.red(c), Color.green(c), Color.blue(c)));
    }

    /** 16進の欄に色を書く */
    private void showHex(int c) {
        settingHex = true;
        edtHex.setText(String.format(Locale.ROOT, "%06X", c & 0xFFFFFF));
        settingHex = false;
    }

    /** 基本色の丸を、今のタブの分だけ並べる */
    private void makePresets() {
        gridPresets.removeAllViews();
        int size = dp(28);
        int margin = dp(4);
        for (int c : PRESETS[part]) {
            View v = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(c);
            d.setStroke(dp(2), (c == colors[part]) ? 0xFFFFD54F : 0xFFFFFFFF);
            v.setBackground(d);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = size;
            lp.height = size;
            lp.setMargins(margin, margin, margin, margin);
            v.setLayoutParams(lp);
            v.setOnClickListener(view -> {
                setColor(c, false);
                picker.setColor(c);
                makePresets(); // 選んだ丸を黄色いふちにする
            });
            gridPresets.addView(v);
        }
    }

    /** 向きを変える（下 → 左 → 上 → 右 の順にくるっと回る） */
    private void turn(int d) {
        int[] order = { HeroSprite.DOWN, HeroSprite.LEFT, HeroSprite.UP, HeroSprite.RIGHT };
        int i = 0;
        while (order[i] != dir) i++;
        dir = order[(i + d + order.length) % order.length];
        showPreview();
    }

    /** 色が変わったら、見本の絵を作り直す */
    private void rebuildPreview() {
        previewSet = HeroSprite.makeSet(getResources(), colors[HAIR], colors[EYE], colors[CLOTH]);
        showPreview();
    }

    /** 見本の絵を、ぼかさずに大きく表示する */
    private void showPreview() {
        if (previewSet == null) return;
        BitmapDrawable d = new BitmapDrawable(getResources(), HeroSprite.pick(previewSet, dir, step));
        d.setFilterBitmap(false);
        imgPreview.setImageDrawable(d);
    }

    /** 決定：選んだ色を GameState に入れて、タイトルにもどる */
    private void decide() {
        GameState.hairColor = colors[HAIR];
        GameState.eyeColor = colors[EYE];
        GameState.clothColor = colors[CLOTH];
        finish();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edtHex.getWindowToken(), 0);
        edtHex.clearFocus();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

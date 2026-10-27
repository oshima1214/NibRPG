package com.example.nibrpg;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

/**
 * 先生のセリフを、1文字ずつ表示する係。
 *   ・まだ出ていない文字は「透明」にしておく（文字が増えても行がガタガタ動かない）
 *   ・新しいセリフを出すと、前のセリフの表示は止まる
 *   ・show() は「全部表示し終わるまでの時間（ミリ秒）」を返す
 *     → その時間がたってからゲームを始めれば、セリフを読んでからスタートになる
 */
public class TypeWriter {

    public static final long CHAR_MS = 45;   // 1文字を出す間隔
    public static final long AFTER_MS = 500; // 全部出たあと、ゲームが始まるまでの間

    /** 文字列のセリフを1文字ずつ出す。全部出し終わる（＋少し間をあける）までの時間を返す */
    public static long show(TextView tv, CharSequence text) {
        final String s = text == null ? "" : text.toString();
        final int n = s.length();
        final Object token = new Object(); // このセリフの「目印」
        tv.setTag(token);

        Runnable step = new Runnable() {
            int shown = 0;

            @Override
            public void run() {
                if (tv.getTag() != token) return; // 別のセリフに変わったら止める
                SpannableString sp = new SpannableString(s);
                if (shown < n) {
                    sp.setSpan(new ForegroundColorSpan(Color.TRANSPARENT), shown, n,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                tv.setText(sp);
                if (shown < n) {
                    shown++;
                    tv.postDelayed(this, CHAR_MS);
                }
            }
        };
        step.run();
        return n * CHAR_MS + AFTER_MS;
    }

    /** strings.xml のセリフを1文字ずつ出す */
    public static long show(TextView tv, int resId) {
        return show(tv, tv.getContext().getText(resId));
    }
}

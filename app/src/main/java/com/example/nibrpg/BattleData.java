package com.example.nibrpg;

/**
 * 戦闘（ことば並べバトル）のデータ。
 * 単語・先生の強さ・ひとことを変えるときは、ここと strings.xml を書きかえるだけでOK
 */
public class BattleData {

    /** 問題1つぶん：答え（ひらがな）とヒント */
    public static class Word {
        public final String answer;
        public final String hint;

        public Word(String answer, String hint) {
            this.answer = answer;
            this.hint = hint;
        }
    }

    // 出す単語（ランダムな順番で出る。全部出たら、また最初から）
    public static final Word[] WORDS = {
        new Word("がっこう",     "毎日かよう場所"),
        new Word("せんせい",     "教室の前に立つ人"),
        new Word("こくばん",     "チョークで書く"),
        new Word("けしごむ",     "まちがいを消す"),
        new Word("えんぴつ",     "書くための道具"),
        new Word("しゅくだい",   "家でやる勉強"),
        new Word("ほうかご",     "授業が終わったあと"),
        new Word("じかんわり",   "1週間の授業の予定"),
        new Word("そつぎょう",   "学校を出ていく日"),
        new Word("しけん",       "実力をためされる"),
        new Word("ともだち",     "いっしょに笑う仲間"),
        new Word("としょしつ",   "本がたくさんある部屋"),
        new Word("たいいく",     "体を動かす授業"),
        new Word("ちこく",       "始まりに間に合わない"),
        new Word("ぶかつ",       "放課後の活動"),
        new Word("ろうか",       "走ってはいけない"),
        new Word("きゅうしょく", "みんなで食べるお昼ごはん"),
        new Word("ぱそこん",     "情報の授業で使う"),
    };

    // ハズレの札に使う文字（ここからランダムに選ぶ）
    public static final String DUMMY_LETTERS =
            "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわん";
    public static final int DUMMY_COUNT = 2;  // ハズレの札の数

    // 制限時間：BASE_TIME_MS ＋ 1文字ごとに PER_LETTER_MS
    public static final long BASE_TIME_MS = 4000;
    public static final long PER_LETTER_MS = 1500;

    // 正解したときのダメージ ＝ 攻撃力 × DAMAGE_RATE（残り時間が半分以上ならさらに1.5倍）
    public static final int DAMAGE_RATE = 2;

    // 階ごとの先生の強さ（0番目は1F＝先生なし）
    public static final int[] TEACHER_HP = { 0, 60, 80, 100, 150 };
    public static final int[] TEACHER_ATTACK = { 0, 12, 15, 18, 22 }; // 時間切れのときの精神的ダメージ

    // この戦闘で遊ぶ階（今は2Fだけ。ほかの階は仮の「勝った／負けた」を選ぶ形）
    public static final boolean[] HAS_BATTLE = { false, true, false, false, false };

    // 時間切れのときの先生のひとこと（strings.xml の string-array）
    public static final int[] TAUNTS = {
        0, R.array.taunts_2f, R.array.taunts_common, R.array.taunts_common, R.array.taunts_common,
    };
}

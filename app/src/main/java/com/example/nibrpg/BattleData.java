package com.example.nibrpg;

/**
 * 戦闘のデータ（階ごとにちがうミニゲーム）。
 *   2F：ことば並べ（BattleActivity）
 *   3F：穴うめ（FillBattleActivity）
 *   4F：板書うつし（MemoryBattleActivity）
 *   5F：よけゲーム（DodgeBattleActivity）
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

    // ===================== 共通 =====================

    // 階ごとの戦闘画面（0番目は1F＝先生なし）
    public static final Class<?>[] SCREENS = {
        null, BattleActivity.class, FillBattleActivity.class,
        MemoryBattleActivity.class, DodgeBattleActivity.class,
    };

    // 戦闘がある階（false の階は、仮の「勝った／負けた」を選ぶ形）
    public static final boolean[] HAS_BATTLE = { false, true, true, true, true };

    // 階ごとの先生の強さ（0番目は1F＝先生なし）
    // 戦闘中のBGM（階ごと。1F は先生なし、5F の校長はよけゲームの画面で決める）
    public static final int[] FLOOR_BGM = { 0, R.raw.bgm_floor2, R.raw.bgm_floor3, R.raw.bgm_floor4, R.raw.bgm_principal };
    public static final int[] TEACHER_HP = { 0, 30, 40, 50, 75 };
    public static final int[] TEACHER_ATTACK = { 0, 12, 15, 14, 22 }; // 失敗したときの精神的ダメージ

    // 正解したときのダメージ ＝ 攻撃力（残り時間が半分以上なら1.5倍。4Fは覚えた数で変わる）

    // 失敗したときの先生のひとこと（strings.xml の string-array）
    public static final int[] TAUNTS = {
        0, R.array.taunts_2f, R.array.taunts_3f, R.array.taunts_4f, R.array.taunts_5f,
    };

    // ===================== 2F：ことば並べ =====================

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

    // ===================== 3F：穴うめ =====================

    // 出す単語（2Fより長め）。□になった所に入る文字を、下の札から選ぶ
    public static final Word[] FILL_WORDS = {
        new Word("ほけんしつ",     "具合が悪いときに行く部屋"),
        new Word("しょくいんしつ", "先生たちがいる部屋"),
        new Word("うんどうかい",   "つなひきやリレーをする日"),
        new Word("ぶんかさい",     "クラスで出し物をする"),
        new Word("きょうかしょ",   "授業で使う本"),
        new Word("にゅうがくしき", "学校生活のはじまりの式"),
        new Word("ちょうれい",     "朝のあつまり"),
        new Word("せいとかい",     "学校をまとめる生徒たち"),
        new Word("れんらくちょう", "家と学校をつなぐノート"),
        new Word("けいじばん",     "お知らせをはる板"),
        new Word("はっぴょう",     "みんなの前で話す"),
        new Word("ぷりんと",       "先生がくばる紙"),
        new Word("しんろそうだん", "将来について話し合う"),
        new Word("ばすてい",       "通学でバスを待つ場所"),
        new Word("じしゅうしつ",   "ひとりで勉強する部屋"),
        new Word("ゆうびんきょく", "手紙を出しに行く所"),
    };

    // 見まちがえやすい文字のなかま。ハズレの札は、ここから選ぶ（だから3Fはむずかしい）
    public static final String[] LOOK_ALIKES = {
        "はばぱほ", "ひびぴ", "ふぶぷ", "へべぺ", "ほぼぽ",
        "かが", "きぎさ", "くぐ", "けげは", "こごに", "さざち", "しじ", "すず", "せぜ", "そぞ",
        "ただ", "ちぢさ", "つづっ", "てで", "とど",
        "やゃ", "ゆゅ", "よょ", "あおめ", "ぬめ", "ねれわ", "るろ", "いり", "うら", "まも", "んそ",
    };
    public static final int FILL_CHOICES = 6;          // 下に並べる札の数
    public static final long FILL_BASE_TIME_MS = 3500; // 制限時間：FILL_BASE_TIME_MS ＋ □1つごとに FILL_PER_BLANK_MS
    public static final long FILL_PER_BLANK_MS = 2000;

    // ===================== 4F：板書うつし =====================

    // 黒板に書かれる文字（教科の1文字）。下のボタンもこの順に並ぶ
    public static final String[] SUBJECTS = { "国", "数", "英", "理", "社", "体", "音", "美" };
    public static final int MEMORY_START = 3;          // はじめに覚える数
    public static final int MEMORY_MAX = 5;            // 一番多いときの数
    public static final long MEMORY_WRITE_MS = 500;    // 1文字書く時間
    public static final long MEMORY_SHOW_MS = 2000;    // 書き終わってから消すまでの時間
    public static final long MEMORY_BASE_TIME_MS = 4000; // 答える時間：MEMORY_BASE_TIME_MS ＋ 1文字ごとに MEMORY_PER_LETTER_MS
    public static final long MEMORY_PER_LETTER_MS = 900;

    // ===================== 5F：よけゲーム =====================

    public static final long DODGE_WAVE_MS = 8000;     // 1回のこうげきの長さ
    public static final long DODGE_WAVE2_MS = 8000;    // 第2形態のこうげきの長さ
    public static final float DODGE_SPEED2 = 1.2f;     // 第2形態の速さ（何倍か）
    public static final float DODGE_DENSITY = 4.0f;    // 物の多さ（1＝少なめ。大きくするほど、たくさん飛んでくる）
    public static final float DODGE_DENSITY2 = 4.0f;   // 第2形態の物の多さ（今は第1形態と同じ）
    public static final long DODGE_PC_INTERVAL_MS = 1500; // パソコンが降ってくる間かく（速さ・多さに関係なく、いつもこの間かく）
    public static final int DODGE_HIT_DAMAGE = 8;      // 物に当たったときのダメージ
    public static final float DODGE_MAX_ASPECT = 1.6f; // いちばんせまくしたときの横幅（高さの何倍か）
    public static final float DODGE_NARROW = 0.5f;     // どれだけせまくするか（0：画面いっぱい 〜 1：DODGE_MAX_ASPECT まで）
    // よけきったときのダメージ ＝ 攻撃力（ノーミスなら1.5倍）

    // ===================== かくしボス：理事長（2Fの理事長室） =====================
    // 4つのミニゲームが順番に出る「最後の試験」。理事長のHPは、ミニゲームが変わっても引きつぐ。
    // 1つのミニゲームで削れるのは HP の4分の1まで。削りきったら次のミニゲームへ

    public static final String EXTRA_BOSS = "boss";   // 理事長戦のとき true
    public static final String EXTRA_STAGE = "stage"; // 何問目か（0〜3）
    public static final Class<?>[] BOSS_STAGES = {
        BattleActivity.class, FillBattleActivity.class, MemoryBattleActivity.class, DodgeBattleActivity.class,
    };
    public static final int BOSS_HP = 200;            // 理事長のHP
    public static final int BOSS_ATTACK = 20;         // 失敗したときのダメージ（よけゲームは DODGE_HIT_DAMAGE）
    public static final long BOSS_MEMORY_SHOW_MS = 1400; // 理事長戦の、書き終わってから消すまでの時間
    public static final int BOSS_MEMORY_MAX = 5;      // 理事長戦の、一番多いときの数（はじめと同じ＝ずっと5文字）
    public static final int BOSS_MEMORY_CLEAR = 4;    // 板書うつしは、素手でもこの回数正解すれば次の問題へ
    public static final int BOSS_MEMORY_START = 5;    // 板書うつしの、はじめに覚える数
    public static final int BOSS_GAME_OVER = 5;       // 攻略失敗画面に渡す番号（理事長）

    // ---- 第1問：ことば並べ（理事長戦だけの単語。2Fより長く、ハズレの札は見まちがえやすい文字） ----
    public static final Word[] BOSS_WORDS = {
        new Word("しゅっせきぼ", "先生が出欠をつける帳面"),
        new Word("しんろしどう", "卒業後の道を相談する"),
        new Word("ぶんぼうぐ",   "ペンやノートのなかま"),
        new Word("だいがくいん", "大学のあと、さらに研究する所"),
        new Word("ひっきしけん", "紙に答えを書くテスト"),
        new Word("めんせつかん", "面接で質問をする人"),
        new Word("しゅうしょく", "会社で働きはじめること"),
        new Word("ないていしき", "入社が決まった人が集まる式"),
        new Word("ぷろぐらむ",   "コンピュータへの命令書"),
        new Word("きーぼーど",   "文字を打つ道具"),
        new Word("りじちょう",   "学校法人のいちばん上の人"),
        new Word("しんがっき",   "春から始まる、新しい学年"),
        new Word("こくごじてん", "ことばの意味を調べる本"),
        new Word("げこうじかん", "学校から帰る時刻"),
    };
    public static final int BOSS_DUMMY_COUNT = 3;          // ハズレの札の数
    public static final long BOSS_BASE_TIME_MS = 3000;     // 制限時間：BOSS_BASE_TIME_MS ＋ 1文字ごとに BOSS_PER_LETTER_MS
    public static final long BOSS_PER_LETTER_MS = 1200;

    // ---- 第2問：穴うめ（理事長戦だけの単語。□は3つ、札は7枚） ----
    public static final Word[] BOSS_FILL_WORDS = {
        new Word("しょうがくきん", "学ぶためのお金の支え"),
        new Word("そつぎょうしき", "学校生活のしめくくりの式"),
        new Word("ちょうしょく",   "朝ごはん"),
        new Word("けっせきとどけ", "休むときに出す紙"),
        new Word("しょくどう",     "お昼ごはんを食べる所"),
        new Word("ぎゃくてん",     "負けていたのに勝つこと"),
        new Word("ちょきんばこ",   "小銭をためる入れ物"),
        new Word("ぴっちゃー",     "野球で球を投げる人"),
        new Word("じょうほうか",   "コンピュータを学ぶ学科"),
        new Word("ゆうしょう",     "大会で一番になること"),
    };
    public static final int BOSS_FILL_BLANKS = 3;
    public static final int BOSS_FILL_CHOICES = 7;
    public static final long BOSS_FILL_BASE_TIME_MS = 3000;
    public static final long BOSS_FILL_PER_BLANK_MS = 1600;

    // ---- 最終問題：よけゲーム（5Fより激しい。3種類がまざる） ----
    // 理事長戦は、ランダムではなく決まった攻撃パターンが順番に来る（DodgeView の P_〜）
    //   チョークの壁 → ペンの雨 → マウスの包囲 → パソコン3連打 → …
    public static final float BOSS_DODGE_SPEED = 1.0f;         // パターンの速さ
    public static final float BOSS_DODGE_SPEED_SHRUNK = 1.25f; // わくがせまくなったあとの速さ
    public static final long BOSS_DODGE_WAVE_MS = 8000;
    public static final int BOSS_SHRINK_HP = 25;           // 理事長のHPがこれ以下になると、わくがせまくなる
    public static final float BOSS_SHRINK_RATE = 0.7f;     // せまくしたときの、たて・よこの長さ（0.7倍 → 広さは約半分）
}

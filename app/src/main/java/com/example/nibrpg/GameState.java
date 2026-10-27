package com.example.nibrpg;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 画面が変わっても残しておきたいゲームの状態。
 * save() でスマホの中に保存し、アプリを開き直したときに load() で読みこむ（オートセーブ）
 */
public class GameState {
    // ---- はじめの値 ----
    private static final int START_MONEY = 500;
    private static final int START_HP = 50;
    private static final int BASE_ATTACK = 5;  // 武器なし（素手）の攻撃力
    public static final int DEFAULT_HAIR = 0xFF302A2C;  // 髪：黒
    public static final int DEFAULT_EYE = 0xFF3C3246;   // 目：黒
    public static final int DEFAULT_CLOTH = 0xFFD6C4A4; // 服：ベージュ

    // ---- デバッグ ----
    // true の間は、タイトルに「デバッグ：階をえらんで始める」が出る。完成したら false にする
    public static final boolean DEBUG = true;
    private static final int DEBUG_MONEY = 9999; // デバッグで始めたときの所持金

    // ---- マップ ----
    public static int floor = 0;   // 今いる階（0＝1F）
    public static int px = -1;     // 主人公の位置（横）。-1は「まだ決まっていない」
    public static int py = -1;     // 主人公の位置（縦）
    public static boolean[] defeated = new boolean[MapData.FLOORS.length]; // 階ごとに、先生に勝ったか
    public static boolean[] visited = firstVisited(); // 行ったことのある階（階段で選べる階）
    public static boolean tutorialDone = false;       // はじめの説明を見たか
    public static boolean bossKey = false;            // 理事長室の鍵を持っているか（校長に勝って、エンドロールのあとにもらう）
    public static boolean doorUnlocked = false;       // 理事長室の扉を、鍵で開けたか（開けるときに演出を出す）
    public static int bossHp = 0;
    public static boolean keyNotice = false;          // タイトルで「理事長室の鍵を手に入れた」を出すか（エンドロールのあと1回だけ）                     // 理事長戦の、今の理事長のHP（ミニゲームが変わっても引きつぐ）
    public static boolean playing = false;            // 遊んでいる途中か（タイトルの「つづきから」を出すか）
    public static int dir = CharacterSprite.FRONT;  // 主人公の向き（CharacterSprite の FRONT・RIGHT・BACK・LEFT）
    public static int step = 0;               // 歩きのコマ（0〜3）

    // ---- 見た目（キャラクリで決める色） ----
    public static int hairColor = DEFAULT_HAIR;
    public static int eyeColor = DEFAULT_EYE;
    public static int clothColor = DEFAULT_CLOTH;

    // ---- 主人公 ----
    public static int money = START_MONEY;
    public static int hp = START_HP;
    public static int maxHp = START_HP;
    public static int weapon = -1;  // 装備している武器（ShopData.WEAPONS の番号）。-1は素手
    public static boolean[] ownedWeapons = new boolean[ShopData.WEAPONS.length]; // 買った武器
    public static int[] itemCounts = new int[ShopData.ITEMS.length];             // 道具の数

    // ---- 実績のための記録（「はじめる／はじめから」でリセット） ----
    public static boolean usedItem = false;    // 道具を使ったか
    public static boolean usedWeapon = false;  // 備品（武器）で攻撃したか
    public static boolean tookDamage = false;  // ダメージを受けたか
    public static int moneySpent = 0;          // 使ったお金の合計
    public static int clerkTalks = 0;          // 事務員さんの話を聞いた回数
    public static boolean[] boughtItems = new boolean[ShopData.ITEMS.length]; // 自販機で買ったことがある商品
    public static boolean[] foundLost = new boolean[MapData.LOST_ITEMS.length]; // 拾った落とし物

    /** 拾った落とし物の数 */
    public static int foundCount() {
        int n = 0;
        for (boolean b : foundLost) if (b) n++;
        return n;
    }

    /** 今の攻撃力（素手の攻撃力＋武器のぶん） */
    public static int attack() {
        return BASE_ATTACK + (weapon >= 0 ? ShopData.WEAPONS[weapon].power : 0);
    }

    // 攻撃のたびに、ダメージを少しばらつかせる（0.2 なら攻撃力の ±20%）
    private static final float ATTACK_SPREAD = 0.2f;
    private static final java.util.Random random = new java.util.Random();

    /** 戦闘で使う攻撃力：今の攻撃力を、ランダムに少し上下させる（最低1） */
    public static int rollAttack() {
        if (weapon >= 0) usedWeapon = true; // 備品で攻撃した（実績のため）
        float rate = 1f + (random.nextFloat() * 2f - 1f) * ATTACK_SPREAD; // 0.8〜1.2倍
        return Math.max(1, Math.round(attack() * rate));
    }

    /** 装備している武器の名前 */
    public static String weaponName() {
        return weapon >= 0 ? ShopData.WEAPONS[weapon].name : null;
    }

    /** はじめは1Fだけ行ったことがある */
    private static boolean[] firstVisited() {
        boolean[] v = new boolean[MapData.FLOORS.length];
        v[0] = true;
        return v;
    }

    /**
     * デバッグ用：好きな階から始める（f は 0＝1F 〜 4＝5F）
     * その階より下の先生には勝ったことにして、階段で行き来できるようにする
     */
    public static void debugStart(int f) {
        reset();
        playing = true;
        tutorialDone = true;     // はじめの説明はとばす
        money = DEBUG_MONEY;     // 備品や道具をすぐ試せるように
        floor = f;
        for (int i = 0; i <= f; i++) visited[i] = true;
        for (int i = 1; i < f; i++) defeated[i] = true;
        px = -1;                 // 位置は MainActivity で階段のとなりに決める
        py = -1;
    }

    /** デバッグ用：理事長室の中から始める（全部の先生に勝って、鍵を持っている状態） */
    public static void debugStartBoss() {
        debugStart(MapData.BOSS_FLOOR);
        for (int i = 0; i < visited.length; i++) visited[i] = true;
        for (int i = 1; i < defeated.length; i++) defeated[i] = true;
        bossKey = true;
        doorUnlocked = true;
        px = 8;  // 理事長室の扉の、内がわ
        py = 7;
    }

    /** 最初からやり直すとき（タイトルの「はじめる」）に呼ぶ。見た目の色は変えない */
    public static void reset() {
        bossKey = false;
        doorUnlocked = false;
        bossHp = 0;
        usedItem = false;
        usedWeapon = false;
        tookDamage = false;
        moneySpent = 0;
        clerkTalks = 0;
        boughtItems = new boolean[ShopData.ITEMS.length];
        foundLost = new boolean[MapData.LOST_ITEMS.length];
        floor = 0;
        px = -1;
        py = -1;
        defeated = new boolean[MapData.FLOORS.length];
        visited = firstVisited();
        tutorialDone = false;
        dir = CharacterSprite.FRONT;
        step = 0;
        // 見た目の色はそのまま（タイトルの「見た目を変える」で決めた色を使い続ける）
        money = START_MONEY;
        hp = START_HP;
        maxHp = START_HP;
        weapon = -1;
        ownedWeapons = new boolean[ShopData.WEAPONS.length];
        itemCounts = new int[ShopData.ITEMS.length];
    }

    // ===================== セーブ・ロード =====================

    private static final String SAVE = "save"; // 保存先の名前
    private static boolean loaded = false;     // アプリを開いてから、もう読みこんだか

    /** 今の状態を保存する（マップの画面を離れるときなどに、自動で呼ぶ） */
    public static void save(Context c) {
        c.getSharedPreferences(SAVE, Context.MODE_PRIVATE).edit()
                .putBoolean("playing", playing)
                .putBoolean("bossKey", bossKey)
                .putBoolean("doorUnlocked", doorUnlocked)
                .putInt("floor", floor)
                .putInt("px", px)
                .putInt("py", py)
                .putInt("dir", dir)
                .putString("defeated", join(defeated))
                .putString("visited", join(visited))
                .putBoolean("tutorialDone", tutorialDone)
                .putInt("money", money)
                .putInt("hp", hp)
                .putInt("maxHp", maxHp)
                .putInt("weapon", weapon)
                .putString("ownedWeapons", join(ownedWeapons))
                .putString("itemCounts", join(itemCounts))
                .putBoolean("usedItem", usedItem)
                .putBoolean("usedWeapon", usedWeapon)
                .putBoolean("tookDamage", tookDamage)
                .putInt("moneySpent", moneySpent)
                .putInt("clerkTalks", clerkTalks)
                .putString("boughtItems", join(boughtItems))
                .putString("foundLost", join(foundLost))
                .apply();
    }

    /** 保存した状態を読みこむ（アプリを開いて、はじめてタイトルが出たときに1回だけ） */
    public static void loadOnce(Context c) {
        if (loaded) return;
        loaded = true;
        SharedPreferences p = c.getSharedPreferences(SAVE, Context.MODE_PRIVATE);
        if (!p.getBoolean("playing", false)) return; // 遊んでいる途中のデータがない
        reset();
        playing = true;
        bossKey = p.getBoolean("bossKey", false);
        doorUnlocked = p.getBoolean("doorUnlocked", false);
        floor = Math.max(0, Math.min(MapData.FLOORS.length - 1, p.getInt("floor", 0)));
        px = p.getInt("px", -1);
        py = p.getInt("py", -1);
        dir = p.getInt("dir", CharacterSprite.FRONT);
        splitTo(p.getString("defeated", ""), defeated);
        splitTo(p.getString("visited", ""), visited);
        tutorialDone = p.getBoolean("tutorialDone", true);
        money = p.getInt("money", START_MONEY);
        maxHp = p.getInt("maxHp", START_HP);
        hp = Math.max(1, Math.min(maxHp, p.getInt("hp", maxHp)));
        weapon = p.getInt("weapon", -1);
        if (weapon >= ShopData.WEAPONS.length) weapon = -1;
        splitTo(p.getString("ownedWeapons", ""), ownedWeapons);
        splitTo(p.getString("itemCounts", ""), itemCounts);
        usedItem = p.getBoolean("usedItem", false);
        usedWeapon = p.getBoolean("usedWeapon", false);
        tookDamage = p.getBoolean("tookDamage", false);
        moneySpent = p.getInt("moneySpent", 0);
        clerkTalks = p.getInt("clerkTalks", 0);
        splitTo(p.getString("boughtItems", ""), boughtItems);
        splitTo(p.getString("foundLost", ""), foundLost);
        visited[0] = true;
    }

    // ---- 配列を「1,0,1」のような文字にして保存し、読むときに元にもどす ----
    // （階や品物の数を変えても、入るぶんだけ読みこむので、エラーにならない）

    private static String join(boolean[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(a[i] ? '1' : '0');
        }
        return sb.toString();
    }

    private static String join(int[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(a[i]);
        }
        return sb.toString();
    }

    private static void splitTo(String s, boolean[] out) {
        if (s == null || s.isEmpty()) return;
        String[] parts = s.split(",");
        for (int i = 0; i < out.length && i < parts.length; i++) out[i] = parts[i].equals("1");
    }

    private static void splitTo(String s, int[] out) {
        if (s == null || s.isEmpty()) return;
        String[] parts = s.split(",");
        for (int i = 0; i < out.length && i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
    }
}

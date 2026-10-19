package com.example.nibrpg;

/** 画面が変わっても残しておきたいゲームの状態 */
public class GameState {
    // ---- はじめの値 ----
    private static final int START_MONEY = 500;
    private static final int START_HP = 50;
    private static final int BASE_ATTACK = 5;  // 武器なし（素手）の攻撃力

    // ---- マップ ----
    public static int floor = 0;   // 今いる階（0＝1F）
    public static int px = -1;     // 主人公の位置（横）。-1は「まだ決まっていない」
    public static int py = -1;     // 主人公の位置（縦）
    public static boolean[] defeated = new boolean[MapData.FLOORS.length]; // 階ごとに、先生に勝ったか
    public static boolean[] visited = firstVisited(); // 行ったことのある階（階段で選べる階）

    // ---- 主人公 ----
    public static int money = START_MONEY;
    public static int hp = START_HP;
    public static int maxHp = START_HP;
    public static int weapon = -1;  // 装備している武器（ShopData.WEAPONS の番号）。-1は素手
    public static boolean[] ownedWeapons = new boolean[ShopData.WEAPONS.length]; // 買った武器
    public static int[] itemCounts = new int[ShopData.ITEMS.length];             // 道具の数

    /** 今の攻撃力（素手の攻撃力＋武器のぶん） */
    public static int attack() {
        return BASE_ATTACK + (weapon >= 0 ? ShopData.WEAPONS[weapon].power : 0);
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

    /** 最初からやり直すとき（タイトルの「はじめる」）に呼ぶ */
    public static void reset() {
        floor = 0;
        px = -1;
        py = -1;
        defeated = new boolean[MapData.FLOORS.length];
        visited = firstVisited();
        money = START_MONEY;
        hp = START_HP;
        maxHp = START_HP;
        weapon = -1;
        ownedWeapons = new boolean[ShopData.WEAPONS.length];
        itemCounts = new int[ShopData.ITEMS.length];
    }
}

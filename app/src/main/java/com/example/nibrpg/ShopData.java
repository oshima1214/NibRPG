package com.example.nibrpg;

/**
 * 武器屋・道具屋で売っている品物。
 * 品物を増やす・変えるときは、ここに1行足す（書きかえる）だけでOK
 */
public class ShopData {

    /** 品物1つぶんのデータ */
    public static class Goods {
        public final String name;   // 名前
        public final int price;     // ねだん（円）
        public final int power;     // 武器：攻撃力が上がる量／道具：HPが回復する量
        public final String desc;   // 説明

        public Goods(String name, int price, int power, String desc) {
            this.name = name;
            this.price = price;
            this.power = power;
            this.desc = desc;
        }
    }

    // 武器屋：買うとすぐ装備する。一度買った武器は、あとで付けかえられる
    public static final Goods[] WEAPONS = {
        new Goods("えんぴつ",       50,  3, "攻撃力 +3。とがらせておいた"),
        new Goods("30cm定規",      150,  7, "攻撃力 +7。リーチが長い"),
        new Goods("Javaの参考書",  400, 15, "攻撃力 +15。オブジェクト指向の重み"),
        new Goods("ノートパソコン", 900, 28, "攻撃力 +28。情報の力で戦う"),
    };

    // 道具屋：HPを回復する道具。1種類につき MAX_ITEMS 個まで持てる
    public static final Goods[] ITEMS = {
        new Goods("栄養バー",          30,  20, "HPを20回復"),
        new Goods("菓子パン",          60,  45, "HPを45回復"),
        new Goods("コーヒー",         100,  70, "HPを70回復。目がさえる"),
        new Goods("エナジードリンク",  150, 999, "HPを全部回復"),
    };

    // 道具の絵（自販機の画面で使う。ITEMS と同じ順番にすること）
    public static final int[] ITEM_IMAGES = {
        R.drawable.item_bar, R.drawable.item_bread, R.drawable.item_coffee, R.drawable.item_energy,
    };

    public static final int MAX_ITEMS = 9;
}

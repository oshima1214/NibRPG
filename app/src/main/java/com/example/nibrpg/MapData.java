package com.example.nibrpg;

/**
 * 各階のマップ。1文字＝1マス。
 *   (空白) = 何もない場所（建物の外）。通れない
 *   # = 壁            . = 床
 *   ! = 扉。扉のかたまり2つが、たがいにつながっている
 *       （上から数えて1つ目と2つ目、3つ目と4つ目…がペア）
 *   / = 階段。上り・下りを選べる（1Fは上りだけ、5Fは下りだけ）。
 *       上りは、その階の先生に勝つまで使えない
 *   % = 武器屋        $ = 道具屋
 *   K = 置き物（通れない。見た目は下の OBJECTS の絵）
 *   T = その階の先生
 *   P = ゲーム開始時の主人公の位置
 * 形を変えたいときは、ここの文字を書きかえるだけでOK（1つの階の中では、1行を全部同じ長さにすること）
 * マップが画面より大きいときは、主人公に合わせて画面が動く
 */
public class MapData {

    public static final String[][] FLOORS = {
        { // 1F：受付（上の壁）の真ん中が武器屋（%）、自販機が道具屋（$）。敵はいない。下のへこみは玄関
            " #              # ",
            " #              # ",
            " #              # ",
            " #              # ",
            " #              # ",
            " #              # ",
            " #              # ",
            " #######%%####### ",
            " #K............K# ",
            " #..............# ",
            " #..............# ",
            " #..............# ",
            " /..........KKK.# ",
            " /..........KKK.# ",
            " #..............# ",
            " #......P.......# ",
            " #.....####.....# ",
            " #.....#  #...$$# ",
            " #.KKKK#  #...$$# ",
            "  #####    #####  ",
            "                  ",
        },
        { // 2F：S先生（ショートヘア）。教室はデスクトップPCの机
            "             ",
            "      ####   ",
            "     #...#   ",
            "     #K../#  ",
            "     #..##   ",
            "     #..#    ",
            "     #..#    ",
            "     #..#    ",
            "     #..#    ",
            "     #K.#    ",
            "     #..#    ",
            "     #..#    ",
            "     #..#    ",
            "     #..#    ",
            "     #.K#    ",
            "     #..#    ",
            "     #..#    ",
            "    #...#    ",
            "   #K...#    ",
            "   #....#    ",
            "    #!!!#    ",
            "             ",
            "             ",
            "    #!!!#    ",
            "   #.....#   ",
            "  #.......#  ",
            " #....T....# ",
            " #.........# ",
            " #.KK...KK.# ",
            " #.........# ",
            " #.KK...KK.# ",
            " #K.......K# ",
            "  #.......#  ",
            "   #######   ",
            "             ",
        },
        { // 3F：E先生。教室はノートパソコンの机
            "             ",
            "      ####   ",
            "     #...#   ",
            "     #K../#  ",
            "     #..##   ",
            "     #..#    ",
            "     #..#    ",
            "     #..#    ",
            "     #..#    ",
            "     #K.#    ",
            "     #..#    ",
            "     #..#    ",
            "     #..#    ",
            "     #..#    ",
            "     #.K#    ",
            "     #..#    ",
            "  !!!#..#    ",
            " #......#    ",
            " #......#    ",
            " #.KKK.K#    ",
            "  #######    ",
            "             ",
            "             ",
            "             ",
            "             ",
            "             ",
            "             ",
            "             ",
            "             ",
            "    #####    ",
            "   #.....#   ",
            "  #.......#  ",
            " #....T....# ",
            " #.........# ",
            " #.KK...KK.# ",
            " #.........# ",
            " #.KK...KK.# ",
            " #K.......K# ",
            "  #.......#  ",
            "   ##!!!##   ",
            "             ",
        },
        { // 4F：S先生（大きめ・短髪）。教室はノートパソコンの机
            "             ",
            "       ####  ",
            "      #...#  ",
            "      #K../# ",
            "      #..##  ",
            "      #..#   ",
            "      !..#   ",
            "      !..#   ",
            "      !..#   ",
            "      #..#   ",
            "      #..#   ",
            "      #.K#   ",
            "      #..#   ",
            "      #K.#   ",
            "      #..#   ",
            "      ####   ",
            "             ",
            "             ",
            "             ",
            "  #######    ",
            " #.......!   ",
            " #.......!   ",
            " #...T...!   ",
            " #.......#   ",
            " #.......#   ",
            " #.KK.KK.#   ",
            " #.......#   ",
            " #.KK.KK.#   ",
            " #.......#   ",
            " #.KK.KK.#   ",
            " #.......#   ",
            " #.KK.KK.#   ",
            " #.......#   ",
            " #K.....K#   ",
            "  #######    ",
            "             ",
        },
        { // 5F：校長室（今は4Fと同じ形）
            "             ",
            "       ####  ",
            "      #...#  ",
            "      #K../# ",
            "      #..##  ",
            "      #..#   ",
            "      !..#   ",
            "      !..#   ",
            "      !..#   ",
            "      #..#   ",
            "      #..#   ",
            "      #.K#   ",
            "      #..#   ",
            "      #K.#   ",
            "      #..#   ",
            "      ####   ",
            "             ",
            "             ",
            "             ",
            "  #######    ",
            " #KK..KK.!   ",
            " #.......!   ",
            " #...T...!   ",
            " #..KKK..#   ",
            " #.......#   ",
            " #.......#   ",
            " #.......#   ",
            " #.KKK...#   ",
            " #.......#   ",
            " #.KKK...#   ",
            " #.......#   ",
            " #.......#   ",
            " #.......#   ",
            " #K.....K#   ",
            "  #######    ",
            "             ",
        },
    };

    /**
     * マップの上に置く絵（受付・自販機・机など）。
     *   left   = 絵の左はしが何マス目か（0.5 のように半分も使える）
     *   bottom = 絵の下はしをそろえる行（この行のマスの下はしに、絵の足もとを合わせる）
     *   width  = 絵の横はばが何マス分か（高さは絵の形に合わせて自動で決まる）
 *   lift   = （省略できる）下はしを何マス持ち上げるか。掲示板やホワイトボードを壁にかけるときに使う
     * 通れるかどうかは、マップの文字（K・%・$・/ など）で決まる。絵は見た目だけ
     */
    public static class Obj {
        public final int image;
        public final float left;
        public final int bottom;
        public final float width;
        public final float lift;  // 下はしを何マス持ち上げるか（壁にかける物に使う）

        public Obj(int image, float left, int bottom, float width) {
            this(image, left, bottom, width, 0f);
        }

        public Obj(int image, float left, int bottom, float width, float lift) {
            this.image = image;
            this.left = left;
            this.bottom = bottom;
            this.width = width;
            this.lift = lift;
        }
    }

    public static final Obj[][] OBJECTS = {
        { // 1F
            new Obj(R.drawable.obj_info_counter, 2.0f, 7, 14.0f), // 受付
            new Obj(R.drawable.obj_plant_table, 1.9f, 8, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_plant_table, 14.9f, 8, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_stairs_door, 0.65f, 13, 1.7f), // 階段の扉
            new Obj(R.drawable.obj_vending_blue_sideways, 14.12f, 18, 1.98f), // 自販機（青・横向き）
            new Obj(R.drawable.obj_desk_oc, 12.0f, 13, 3.0f), // オープンキャンパスの机
            new Obj(R.drawable.obj_bench, 3.0f, 18, 3.0f), // ベンチ
            new Obj(R.drawable.obj_trash, 6.1f, 18, 0.8f), // ゴミ箱
        },
        { // 2F
            new Obj(R.drawable.obj_stairs_door, 8.65f, 3, 1.7f), // 階段の扉
            new Obj(R.drawable.obj_notice_board, 6.2f, 1, 1.6f, 0.3f), // 掲示板（壁）
            new Obj(R.drawable.obj_plant_table, 5.9f, 3, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_plant_table, 5.9f, 9, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_trash, 7.1f, 14, 0.8f), // ゴミ箱
            new Obj(R.drawable.obj_desk_pc, 2.95f, 28, 1.1f), // 学生の机（PC）
            new Obj(R.drawable.obj_desk_pc, 3.95f, 28, 1.1f), // 学生の机（PC）
            new Obj(R.drawable.obj_desk_pc, 7.95f, 28, 1.1f), // 学生の机（PC）
            new Obj(R.drawable.obj_desk_pc, 8.95f, 28, 1.1f), // 学生の机（PC）
            new Obj(R.drawable.obj_desk_pc, 2.95f, 30, 1.1f), // 学生の机（PC）
            new Obj(R.drawable.obj_desk_pc, 3.95f, 30, 1.1f), // 学生の机（PC）
            new Obj(R.drawable.obj_desk_pc, 7.95f, 30, 1.1f), // 学生の机（PC）
            new Obj(R.drawable.obj_desk_pc, 8.95f, 30, 1.1f), // 学生の机（PC）
            new Obj(R.drawable.obj_plant_table, 3.9f, 18, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_plant_table, 1.9f, 31, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_plant_table, 9.9f, 31, 1.2f), // 観葉植物
        },
        { // 3F
            new Obj(R.drawable.obj_stairs_door, 8.65f, 3, 1.7f), // 階段の扉
            new Obj(R.drawable.obj_notice_board, 6.2f, 1, 1.6f, 0.3f), // 掲示板（壁）
            new Obj(R.drawable.obj_plant_table, 5.9f, 3, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_plant_table, 5.9f, 9, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_trash, 7.1f, 14, 0.8f), // ゴミ箱
            new Obj(R.drawable.obj_whiteboard, 5.8f, 29, 3.2f, 0.25f), // ホワイトボード（壁）
            new Obj(R.drawable.obj_desk_laptop, 2.95f, 34, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 3.95f, 34, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 7.95f, 34, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 8.95f, 34, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 2.95f, 36, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 3.95f, 36, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 7.95f, 36, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 8.95f, 36, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_bench, 3.0f, 19, 3.0f), // ベンチ
            new Obj(R.drawable.obj_plant_table, 6.9f, 19, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_plant_table, 1.9f, 37, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_plant_table, 9.9f, 37, 1.2f), // 観葉植物
        },
        { // 4F
            new Obj(R.drawable.obj_stairs_door, 9.65f, 3, 1.7f), // 階段の扉
            new Obj(R.drawable.obj_notice_board, 7.2f, 1, 1.6f, 0.3f), // 掲示板（壁）
            new Obj(R.drawable.obj_plant_table, 6.9f, 3, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_plant_table, 7.9f, 11, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_trash, 7.1f, 13, 0.8f), // ゴミ箱
            new Obj(R.drawable.obj_plant_table, 1.9f, 33, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_plant_table, 7.9f, 33, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_whiteboard, 3.4f, 19, 3.2f, 0.25f), // ホワイトボード（壁）
            new Obj(R.drawable.obj_desk_laptop, 2.95f, 25, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 3.95f, 25, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 5.95f, 25, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 6.95f, 25, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 2.95f, 27, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 3.95f, 27, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 5.95f, 27, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 6.95f, 27, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 2.95f, 29, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 3.95f, 29, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 5.95f, 29, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 6.95f, 29, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 2.95f, 31, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 3.95f, 31, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 5.95f, 31, 1.1f), // 学生の机（ノートPC）
            new Obj(R.drawable.obj_desk_laptop, 6.95f, 31, 1.1f), // 学生の机（ノートPC）
        },
        { // 5F
            new Obj(R.drawable.obj_stairs_door, 9.65f, 3, 1.7f), // 階段の扉
            new Obj(R.drawable.obj_notice_board, 7.2f, 1, 1.6f, 0.3f), // 掲示板（壁）
            new Obj(R.drawable.obj_plant_table, 6.9f, 3, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_plant_table, 7.9f, 11, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_trash, 7.1f, 13, 0.8f), // ゴミ箱
            new Obj(R.drawable.obj_plant_table, 1.9f, 33, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_plant_table, 7.9f, 33, 1.2f), // 観葉植物
            new Obj(R.drawable.obj_bookshelf, 2.0f, 20, 2.0f), // 本棚
            new Obj(R.drawable.obj_bookshelf, 6.0f, 20, 2.0f), // 本棚
            new Obj(R.drawable.obj_principal_desk, 3.8f, 23, 3.4f), // 校長の机
            new Obj(R.drawable.obj_sofa, 2.9f, 27, 3.2f), // ソファ
            new Obj(R.drawable.obj_low_table, 3.0f, 29, 3.0f), // ローテーブル
        },
    };

    // 階の名前（strings.xml）
    public static final int[] NAMES = {
        R.string.floor1, R.string.floor2, R.string.floor3, R.string.floor4, R.string.floor5,
    };

    // 階ごとの床の色・壁の色（あとで画像に差し替える）
    public static final int[] FLOOR_COLORS = {
        0xFFE8D9B5, // 1F：ベージュ
        0xFFCDE8C6, // 2F：うすい緑
        0xFFC6DCEB, // 3F：うすい青
        0xFFDCCBE8, // 4F：うすい紫
        0xFFB23A3A, // 5F：赤いじゅうたん
    };
    // 階ごとの床の画像（32×32の正方形。0 のときは FLOOR_COLORS の色でぬる）
    public static final int[] FLOOR_IMAGES = {
        R.drawable.floor_blue, // 1F
        0,                     // 2F（色でぬる）
        0,                     // 3F（色でぬる）
        0,                     // 4F（色でぬる）
        0,                     // 5F（色でぬる）
    };
    // 階ごとの壁の面の色（奥の壁の、正面に見える部分）
    public static final int[] WALL_COLORS = {
        0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, // 全部の階：白
    };
    // 奥の壁の高さ（マス）。1Fは受付の後ろの壁を、マップの一番上まで高くしている
    public static final int[] WALL_HEIGHT = { 8, 2, 2, 2, 2 };
    // 壁の上面の色（上から見た壁のてっぺん。濃い色にすると、床との区別がはっきりする）
    public static final int WALL_TOP_COLOR = 0xFF3A4150;

    // 階ごとの先生の絵と名前（1Fはいないので0）
    public static final int[] TEACHER_IMAGES = {
        0, R.drawable.s_m_sensei, R.drawable.e_sensei, R.drawable.s_s_sensei, R.drawable.koutyou_sensei,
    };
    public static final int[] TEACHER_NAMES = {
        0, R.string.teacher1, R.string.teacher2, R.string.teacher3, R.string.teacher4,
    };

    // 先生に勝ったときにもらえるお金（1Fと最上階は使わない）
    public static final int[] REWARDS = {
        0, 300, 500, 800, 0,
    };

    // 最上階（ここの先生に勝ったらクリア）
    public static final int LAST = FLOORS.length - 1;
}

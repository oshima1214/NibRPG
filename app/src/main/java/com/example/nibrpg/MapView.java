package com.example.nibrpg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** マップを描くだけの部品。何を描くかは MapData と GameState を見て決める */
public class MapView extends View {

    private static final int VISIBLE_ROWS = 10; // 画面の縦に見えるマスの数

    private final Paint fill = new Paint();                          // 四角をぬる
    private final Paint line = new Paint();                          // マスの線
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);     // マスの上の文字
    private final Paint image = new Paint();                         // 先生の絵
    private final Paint objPaint = new Paint(Paint.FILTER_BITMAP_FLAG);  // 置き物の絵（なめらかに縮める）
    private final Bitmap[] teachers = new Bitmap[MapData.FLOORS.length];
    private final Bitmap[][] objects = new Bitmap[MapData.FLOORS.length][];
    private final Bitmap[] floors = new Bitmap[MapData.FLOORS.length];
    private final RectF dst = new RectF();

    private final CharacterSprite hero; // 主人公の見た目（キャラクリで保存したもの）
    private final Bitmap boss;          // 理事長の絵
    private final Bitmap[] lostImages = new Bitmap[MapData.LOST_ITEMS.length]; // 落とし物の絵

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        hero = CharacterSprite.load(context);
        boss = BitmapFactory.decodeResource(getResources(), R.drawable.boss_rijicho);
        for (int i = 0; i < lostImages.length; i++) {
            lostImages[i] = BitmapFactory.decodeResource(getResources(), MapData.LOST_ITEMS[i].image);
        }
        line.setStyle(Paint.Style.STROKE);
        line.setColor(0x22000000);
        text.setColor(0xFFFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
        text.setFakeBoldText(true);
        image.setFilterBitmap(false); // ドット絵をぼかさない

        for (int f = 0; f < teachers.length; f++) {
            if (MapData.TEACHER_IMAGES[f] != 0) {
                teachers[f] = BitmapFactory.decodeResource(getResources(), MapData.TEACHER_IMAGES[f]);
            }
        }
        // 床の画像を読みこんでおく
        for (int f = 0; f < floors.length; f++) {
            if (MapData.FLOOR_IMAGES[f] != 0) {
                floors[f] = BitmapFactory.decodeResource(getResources(), MapData.FLOOR_IMAGES[f]);
            }
        }
        // 置き物の絵を読みこんでおく（同じ絵は1回だけ読みこんで使い回す）
        java.util.Map<Integer, Bitmap> cache = new java.util.HashMap<>();
        for (int f = 0; f < objects.length; f++) {
            MapData.Obj[] list = MapData.OBJECTS[f];
            objects[f] = new Bitmap[list.length];
            for (int i = 0; i < list.length; i++) {
                int id = list[i].image;
                if (!cache.containsKey(id)) {
                    cache.put(id, BitmapFactory.decodeResource(getResources(), id));
                }
                objects[f][i] = cache.get(id);
            }
        }
    }

    // =========================
    // 歩行アニメーション（重田さん作）
    // =========================

    private static final long WALK_POSE_MS = 150L;

    // 0=直立、1=左足、2=右足
    private int walkFrame = 0;
    private int nextWalkFrame = 1;

    // 歩行ポーズから直立へ戻す
    private final Runnable resetWalkPose = () -> {
        walkFrame = 0;
        invalidate();
    };

    /** 1マス移動に成功したときに呼び出す */
    public void onPlayerStepped() {
        removeCallbacks(resetWalkPose);
        walkFrame = nextWalkFrame;
        nextWalkFrame = (nextWalkFrame == 1) ? 2 : 1;
        invalidate();
        // 一定時間後に直立へ戻る
        postDelayed(resetWalkPose, WALK_POSE_MS);
    }

    /** 会話や別画面への移動時に停止する */
    public void stopWalkAnimation() {
        removeCallbacks(resetWalkPose);
        if (walkFrame != 0) {
            walkFrame = 0;
            invalidate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(resetWalkPose);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int f = GameState.floor;
        String[] map = MapData.FLOORS[f];
        int rows = map.length;
        int cols = map[0].length();

        // 見える列の範囲：理事長室がある階は、部屋の「中」と「外」を分けて、主人公がいる方だけ描く
        int vx0 = 0, vx1 = cols - 1;
        int vy0 = 0, vy1 = rows - 1; // 見える行の範囲
        if (f == MapData.BOSS_FLOOR) {
            if (GameState.px <= MapData.BOSS_ROOM_RIGHT) vx1 = MapData.BOSS_ROOM_RIGHT;
            else vx0 = MapData.BOSS_ROOM_RIGHT + 1;
            // 何もない列（空白だけ）は、はしからけずる（マップが小さく見えないように）
            while (vx0 < vx1 && emptyColumn(map, vx0)) vx0++;
            while (vx1 > vx0 && emptyColumn(map, vx1)) vx1--;
            // 見える列の中で何もない行も、上と下からけずる（理事長室が画面のまん中に来るように）
            while (vy0 < vy1 && emptyRow(map, vy0, vx0, vx1)) vy0++;
            while (vy1 > vy0 && emptyRow(map, vy1, vx0, vx1)) vy1--;
            vy0 = Math.max(0, vy0 - (MapData.WALL_HEIGHT[f] - 1)); // 奥の壁は上にはみ出して描くので、そのぶん上も見せる
        }
        int vcols = vx1 - vx0 + 1;
        int vrows = vy1 - vy0 + 1;

        // 1マスの大きさ：縦に VISIBLE_ROWS マス見える大きさ（横が入りきらないときは横に合わせる）
        float tile = Math.min(getWidth() / (float) vcols, getHeight() / (float) VISIBLE_ROWS);
        // 画面のどこからマップを描くか（大きいマップは主人公を追いかける）
        int[] r = walkRange(map, vx0, vx1);
        float left = camera(getWidth(), tile * vcols, GameState.px - vx0, r[0] - vx0, r[1] - vx0) - vx0 * tile;
        float top = camera(getHeight(), tile * vrows, GameState.py - vy0, r[2] - vy0, r[3] - vy0) - vy0 * tile;
        text.setTextSize(tile * 0.45f);

        // 見える範囲の外は描かない（理事長室の中から廊下、廊下から理事長室が見えないように）
        canvas.save();
        canvas.clipRect(left + vx0 * tile, 0, left + (vx1 + 1) * tile, getHeight());

        // 置き物の絵の下にかくれるマス（記号の絵は描かず、床だけにする）
        boolean[][] covered = coveredTiles(f, rows, cols);

        // ① マスを全部描く
        int tx = -1, ty = -1; // 先生の位置
        int rx = -1, ry = -1; // 理事長の位置
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                char c = map[y].charAt(x);
                roomTile = (f == MapData.BOSS_FLOOR && x <= MapData.BOSS_ROOM_RIGHT);
                drawTile(canvas, c, f, left + x * tile, top + y * tile, tile, covered[y][x]);
                if (c == 'T') { tx = x; ty = y; }
                if (c == 'R') { rx = x; ry = y; }
            }
        }

        // ② 壁の影を床に落とす（壁の下と、左の壁の右がわ）
        fill.setColor(0x46000000); // うすい黒
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (!isInside(map, x, y)) continue;
                float l = left + x * tile;
                float t = top + y * tile;
                if (isWallLike(map, x, y - 1)) canvas.drawRect(l, t, l + tile, t + tile * 0.3f, fill);
                if (isWallLike(map, x - 1, y)) canvas.drawRect(l, t, l + tile * 0.18f, t + tile, fill);
            }
        }

        // ③ 壁を描く（奥の壁は上のマスにはみ出すので、床を全部描いたあとに描く）
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                char c = map[y].charAt(x);
                if (c == '#' || c == '%') { // 受付（%）も壁の一部として描く
                    drawWall(canvas, map, x, y, f, left + x * tile, top + y * tile, tile);
                }
            }
        }

        // ③.5 落とし物（まだ拾っていない物だけ）
        for (int i = 0; i < MapData.LOST_ITEMS.length; i++) {
            MapData.Lost l = MapData.LOST_ITEMS[i];
            if (l.floor != f || GameState.foundLost[i]) continue;
            float cx = left + l.x * tile + tile / 2f;
            float cy = top + l.y * tile + tile / 2f;
            fill.setColor(0x33000000); // 小さなかげ
            dst.set(cx - tile * 0.28f, cy + tile * 0.12f, cx + tile * 0.28f, cy + tile * 0.26f);
            canvas.drawOval(dst, fill);
            float half = tile * 0.3f;
            dst.set(cx - half, cy - half, cx + half, cy + half);
            canvas.drawBitmap(lostImages[i], null, dst, image);
        }

        // ④ 置き物・先生・主人公を、上の行から順に描く（下の行にあるものほど手前に見える）
        boolean showTeacher = tx >= 0 && teachers[f] != null; // 勝ったあとも、先生はその場に残る
        MapData.Obj[] list = MapData.OBJECTS[f];
        for (int y = 0; y < rows; y++) {
            for (int i = 0; i < list.length; i++) {
                if (list[i].bottom == y) drawObject(canvas, objects[f][i], list[i], left, top, tile);
            }
            if (showTeacher && ty == y) drawTeacher(canvas, teachers[f], left, top, tx, ty, tile);
            if (rx >= 0 && ry == y) drawTeacher(canvas, boss, left, top, rx, ry, tile); // 理事長
            if (GameState.py == y) drawPlayer(canvas, left + GameState.px * tile, top + GameState.py * tile, tile);
        }

        canvas.restore();
    }

    /** x0〜x1 の列の中で、y 行目が空白だけか */
    private boolean emptyRow(String[] map, int y, int x0, int x1) {
        for (int x = x0; x <= x1 && x < map[y].length(); x++) {
            if (map[y].charAt(x) != ' ') return false;
        }
        return true;
    }

    /** 空白だけの列か */
    private boolean emptyColumn(String[] map, int x) {
        for (String row : map) {
            if (x < row.length() && row.charAt(x) != ' ') return false;
        }
        return true;
    }

    private boolean roomTile = false; // 今描いているマスが、理事長室の中か（床の色を変える）

    /** 置き物の絵：足もとを bottom の行にそろえ、横はばは width マス分 */
    private void drawObject(Canvas canvas, Bitmap bmp, MapData.Obj o, float left, float top, float s) {
        float w = o.width * s;
        float h = w * bmp.getHeight() / bmp.getWidth();
        float l = left + o.left * s;
        float bottom = top + (o.bottom + 1 - o.lift) * s;
        dst.set(l, bottom - h, l + w, bottom);
        canvas.drawBitmap(bmp, null, dst, objPaint);
    }

    /** 置き物の絵がかぶさるマスを調べる */
    private boolean[][] coveredTiles(int f, int rows, int cols) {
        boolean[][] covered = new boolean[rows][cols];
        MapData.Obj[] list = MapData.OBJECTS[f];
        for (int i = 0; i < list.length; i++) {
            MapData.Obj o = list[i];
            Bitmap bmp = objects[f][i];
            float h = o.width * bmp.getHeight() / bmp.getWidth(); // 絵の高さ（マス）
            int x0 = (int) Math.floor(o.left);
            int x1 = (int) Math.ceil(o.left + o.width) - 1;
            int y0 = (int) Math.floor(o.bottom + 1 - o.lift - h);
            for (int y = Math.max(0, y0); y <= Math.min(rows - 1, o.bottom); y++) {
                for (int x = Math.max(0, x0); x <= Math.min(cols - 1, x1); x++) {
                    covered[y][x] = true;
                }
            }
        }
        return covered;
    }

    /**
     * 画面のどこからマップを描き始めるか（横・縦それぞれで使う）
     *   マップが画面に入りきる → まん中に置く
     *   入りきらない           → 主人公の位置に合わせて動かす。
     *     主人公が行ける一番上（左）にいるときは、マップの一番上（左）まで見せる。
     *     主人公が行ける一番下（右）にいるときは、マップの一番下（右）まで見せる。
     *     （1Fの受付の看板のように、主人公が行けない所にある絵も見えるようにするため）
     */
    private float camera(float screen, float mapSize, int pos, int minPos, int maxPos) {
        if (mapSize <= screen) {
            return (screen - mapSize) / 2f;
        }
        float rate = (maxPos > minPos) ? (pos - minPos) / (float) (maxPos - minPos) : 0.5f;
        rate = Math.max(0f, Math.min(1f, rate));
        return -rate * (mapSize - screen);
    }

    /** 主人公が立てるマスの範囲 { 一番左, 一番右, 一番上, 一番下 }（x0〜x1 の列の中だけで調べる） */
    private int[] walkRange(String[] map, int x0, int x1) {
        int minX = Integer.MAX_VALUE, maxX = -1, minY = Integer.MAX_VALUE, maxY = -1;
        for (int y = 0; y < map.length; y++) {
            for (int x = x0; x <= x1 && x < map[y].length(); x++) {
                char c = map[y].charAt(x);
                if (c == '.' || c == 'P' || c == 'T' || c == 'R') {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return new int[] { minX, maxX, minY, maxY };
    }

    /** 1マス描く（covered のときは、置き物の絵がかぶさるので記号の絵は描かない） */
    private void drawTile(Canvas canvas, char c, int f, float l, float t, float s, boolean covered) {
        if (c == ' ') return; // 何もない場所は描かない（背景の黒のまま）
        if (c == '#') return; // 壁は drawWall で、床を全部描いたあとに描く
        // 壁以外は、まず床を描く
        if (roomTile) {
            rect(canvas, MapData.BOSS_ROOM_FLOOR_COLOR, l, t, s); // 理事長室：暗い色のじゅうたん
            fill.setColor(0x14FFFFFF);
            canvas.drawRect(l, t, l + s, t + s * 0.04f, fill);     // うっすら目地
            canvas.drawRect(l, t, l + s * 0.04f, t + s, fill);
        } else if (floors[f] != null) {
            dst.set(l, t, l + s, t + s);
            canvas.drawBitmap(floors[f], null, dst, image); // 床の画像（ドットをぼかさない）
        } else {
            rect(canvas, MapData.FLOOR_COLORS[f], l, t, s);  // 画像がない階は色でぬる
            canvas.drawRect(l, t, l + s, t + s, line);
        }
        if (covered) return;

        switch (c) {
            case '/': drawStairs(canvas, l, t, s); break;
            case '!': drawDoor(canvas, l, t, s); break;
            case 'L': drawDoor(canvas, l, t, s); break; // 理事長室の扉（見た目はふつうの扉）
            case '%': box(canvas, 0xFF8B5A2B, l, t, s, "武"); break;
            case '$': box(canvas, 0xFF2E7D32, l, t, s, "道"); break;
        }
    }

    /**
     * 壁を1マス描く。まわりのマスを見て、3種類に描き分ける
     *   奥の壁（すぐ下が部屋の中）   → WALL_HEIGHT マス分の高さの白い面。てっぺんに濃い上面、下に幅木
     *   手前の壁（すぐ上が部屋の中） → 濃い上面の細い帯だけ（部屋の中が見えるように）
     *   左右の壁（それ以外）         → 濃い上面。部屋がわのふちを少し明るく
     */
    private void drawWall(Canvas canvas, String[] map, int x, int y, int f, float l, float t, float s) {
        int face = MapData.WALL_COLORS[f];
        int top = MapData.WALL_TOP_COLOR;
        if (isInside(map, x, y + 1)) {
            float t0 = t - s * (MapData.WALL_HEIGHT[f] - 1);
            fill.setColor(face);
            canvas.drawRect(l, t0, l + s, t + s, fill);
            line.setColor(mix(face, 0xFF000000, 0.08f));
            for (float yy = t0 + s * 0.7f; yy < t + s * 0.8f; yy += s * 0.4f) {
                canvas.drawLine(l, yy, l + s, yy, line);
            }
            fill.setColor(top);
            canvas.drawRect(l, t0, l + s, t0 + s * 0.3f, fill);
            fill.setColor(mix(face, 0xFF000000, 0.4f));
            canvas.drawRect(l, t + s * 0.8f, l + s, t + s, fill);
        } else if (isInside(map, x, y - 1)) {
            fill.setColor(top);
            canvas.drawRect(l, t, l + s, t + s * 0.3f, fill);
        } else {
            fill.setColor(top);
            canvas.drawRect(l, t, l + s, t + s, fill);
            fill.setColor(mix(top, 0xFFFFFFFF, 0.25f));
            if (isInside(map, x + 1, y)) canvas.drawRect(l + s * 0.94f, t, l + s, t + s, fill);
            if (isInside(map, x - 1, y)) canvas.drawRect(l, t, l + s * 0.06f, t + s, fill);
        }
        line.setColor(0x22000000);
    }

    /** 壁か、壁にうめこまれたもの（階段・扉・受付）か */
    private boolean isWallLike(String[] map, int x, int y) {
        if (y < 0 || y >= map.length || x < 0 || x >= map[y].length()) return false;
        char c = map[y].charAt(x);
        return c == '#' || c == '%' || c == '/' || c == '!' || c == 'L';
    }

    /** 部屋の中のマスか（床や置き物。壁・何もない所・壁にうめこまれた階段や扉や受付はちがう） */
    private boolean isInside(String[] map, int x, int y) {
        if (y < 0 || y >= map.length || x < 0 || x >= map[y].length()) return false;
        char c = map[y].charAt(x);
        return c == '.' || c == 'P' || c == 'T' || c == 'R' || c == 'K' || c == '$';
    }

    /** 2つの色をまぜる（rate = 0 なら a のまま、1 なら b） */
    private static int mix(int a, int b, float rate) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - rate) + ((b >> 16) & 0xFF) * rate);
        int g = (int) (((a >> 8) & 0xFF) * (1 - rate) + ((b >> 8) & 0xFF) * rate);
        int bl = (int) ((a & 0xFF) * (1 - rate) + (b & 0xFF) * rate);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** 階段：灰色の段を3本 */
    private void drawStairs(Canvas canvas, float l, float t, float s) {
        fill.setColor(0xFF616161);
        canvas.drawRect(l, t, l + s, t + s, fill);
        fill.setColor(0xFFBDBDBD);
        for (int i = 0; i < 3; i++) {
            float y = t + s * (0.12f + i * 0.3f);
            canvas.drawRect(l + s * 0.1f, y, l + s * 0.9f, y + s * 0.18f, fill);
        }
    }

    /** 扉：木の色に、取っ手 */
    private void drawDoor(Canvas canvas, float l, float t, float s) {
        fill.setColor(0xFFA1662F);
        canvas.drawRect(l + s * 0.05f, t + s * 0.05f, l + s * 0.95f, t + s * 0.95f, fill);
        fill.setColor(0xFFFFD54F);
        canvas.drawCircle(l + s * 0.75f, t + s * 0.5f, s * 0.07f, fill);
    }

    /** 主人公：キャラクリで選んだ髪型・色・服装を表示（重田さん作） */
    private void drawPlayer(Canvas canvas, float l, float t, float s) {
        // CharacterSprite のドット絵は幅20、高さ29
        // 高さを約1.45マス分に調整し、足元をマスの下側に合わせる
        float scale = s * 1.45f / 29f;
        float drawX = l + (s - 20f * scale) / 2f;
        float drawY = t + s - 29f * scale;
        // 向きは GameState.dir（押した方向。壁にぶつかっても向きは変わる）
        hero.draw(canvas, drawX, drawY, scale, GameState.dir, walkFrame);
    }

    /** 先生：2マス分の高さで、足もとをマスに合わせて描く（上のマスにはみ出す） */
    private void drawTeacher(Canvas canvas, Bitmap bmp, float left, float top, int x, int y, float s) {
        float h = s * 2;
        float w = bmp.getWidth() * h / bmp.getHeight();
        float cx = left + x * s + s / 2f;
        float bottom = top + (y + 1) * s;
        dst.set(cx - w / 2f, bottom - h, cx + w / 2f, bottom);
        canvas.drawBitmap(bmp, null, dst, image);
    }

    // ---- 小さな道具 ----
    private void rect(Canvas canvas, int color, float l, float t, float s) {
        fill.setColor(color);
        canvas.drawRect(l, t, l + s, t + s, fill);
    }

    private void box(Canvas canvas, int color, float l, float t, float s, String label) {
        float m = s * 0.1f;
        fill.setColor(color);
        canvas.drawRect(l + m, t + m, l + s - m, t + s - m, fill);
        float y = t + s / 2f - (text.descent() + text.ascent()) / 2f;
        canvas.drawText(label, l + s / 2f, y, text);
    }
}

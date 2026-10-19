package com.example.nibrpg;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

/** ゲーム本編：マップを歩いて探索する画面 */
public class MainActivity extends AppCompatActivity {

    private static final long FIRST_WAIT_MS = 300; // 押しっぱなしで歩き始めるまで
    private static final long STEP_MS = 180;       // 押しっぱなしのときの1歩の間かく

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MapView mapView;
    private TextView txtFloor;
    private TextView txtStatus;
    private View msgBox;
    private TextView msgText;
    private LinearLayout msgButtons; // メッセージの下のボタンを入れる所
    private boolean talking = false; // メッセージを出している間は歩けない

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // 画面のふち（ステータスバーなど）に文字がかぶらないようにする
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        mapView = findViewById(R.id.mapView);
        txtFloor = findViewById(R.id.txtFloor);
        txtStatus = findViewById(R.id.txtStatus);
        msgBox = findViewById(R.id.msgBox);
        msgText = findViewById(R.id.msgText);
        msgButtons = findViewById(R.id.msgButtons);

        // 上下左右のボタン
        setupPad(R.id.btnUp, 0, -1);
        setupPad(R.id.btnDown, 0, 1);
        setupPad(R.id.btnLeft, -1, 0);
        setupPad(R.id.btnRight, 1, 0);

        // はじめて来たときは、1FのPの位置からスタート
        if (GameState.px < 0) {
            String[] map = MapData.FLOORS[GameState.floor];
            for (int y = 0; y < map.length; y++) {
                int x = map[y].indexOf('P');
                if (x >= 0) {
                    GameState.px = x;
                    GameState.py = y;
                }
            }
        }
        showFloor();
    }

    /** ボタンを押したら1歩。押しっぱなしなら歩き続ける */
    @SuppressLint("ClickableViewAccessibility")
    private void setupPad(int id, int dx, int dy) {
        Runnable repeat = new Runnable() {
            @Override
            public void run() {
                move(dx, dy);
                handler.postDelayed(this, STEP_MS);
            }
        };
        findViewById(id).setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                move(dx, dy);
                handler.postDelayed(repeat, FIRST_WAIT_MS);
            } else if (e.getAction() == MotionEvent.ACTION_UP
                    || e.getAction() == MotionEvent.ACTION_CANCEL) {
                handler.removeCallbacks(repeat);
            }
            return true;
        });
    }

    /** 1歩動く。行き先のマスによって、することを変える */
    private void move(int dx, int dy) {
        if (talking) return;
        String[] map = MapData.FLOORS[GameState.floor];
        int nx = GameState.px + dx;
        int ny = GameState.py + dy;
        if (ny < 0 || ny >= map.length || nx < 0 || nx >= map[ny].length()) return; // マップの外
        char c = map[ny].charAt(nx);

        switch (c) {
            case ' ':
            case '#':
            case 'K':
                return; // 何もない場所・壁・机は通れない
            case '%':
                openShop(ShopActivity.TYPE_WEAPON);
                return;
            case '$':
                stopWalking();
                showChoices(getString(R.string.vending_ask),
                        new String[] { getString(R.string.btn_yes), getString(R.string.btn_no) },
                        new Runnable[] { this::openVending, null });
                return;
            case 'T':
                if (!GameState.defeated[GameState.floor]) {
                    meetTeacher();
                    return;
                }
                break; // 勝ったあとは、ただの床
            case '/':
                useStairs();    // 階段：上か下かを選ぶ
                return;
            case '!':
                goThroughDoor(nx, ny);
                return;
        }
        GameState.px = nx;
        GameState.py = ny;
        mapView.invalidate(); // 描き直す
    }

    // ===================== お店 =====================

    /** お店を開く。マップの画面は閉じないので、店を出るとそのまま続きから */
    private void openShop(String type) {
        stopWalking();
        Intent intent = new Intent(this, ShopActivity.class);
        intent.putExtra(ShopActivity.EXTRA_TYPE, type);
        startActivity(intent);
    }

    /** 自販機の画面を開く。マップの画面は閉じないので、もどるとそのまま続きから */
    private void openVending() {
        startActivity(new Intent(this, VendingActivity.class));
    }

    /** お店からもどってきたときなどに、所持金などを表示し直す */
    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        mapView.invalidate(); // 戦闘で勝ったら、先生を消すため
    }

    private void updateStatus() {
        txtStatus.setText(getString(R.string.status_map,
                GameState.money, GameState.hp, GameState.maxHp, GameState.attack()));
    }

    // ===================== 階段 =====================

    /**
     * 階段：何階へ行くかを選ぶ。
     *   行ったことのある階（今いる階はのぞく）を全部ならべる
     *   1つ上の階にまだ行ったことがなければ「上の階へ」を出す（何階かは見せない）
     */
    private void useStairs() {
        int f = GameState.floor;
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (int i = 0; i < MapData.FLOORS.length; i++) {
            if (i == f || !GameState.visited[i]) continue;
            int target = i; // ラムダの中で使うため
            labels.add(getString(R.string.floor_short, i + 1));
            actions.add(() -> goToFloor(target));
        }
        if (f < MapData.LAST && !GameState.visited[f + 1]) {
            labels.add(getString(R.string.btn_up));
            actions.add(this::goUp);
        }
        labels.add(getString(R.string.btn_cancel));
        actions.add(null);
        showChoices(getString(R.string.stairs_msg),
                labels.toArray(new String[0]), actions.toArray(new Runnable[0]));
    }

    /** まだ行ったことのない上の階へ。その階に先生がいるときは、勝っていないと行けない */
    private void goUp() {
        int f = GameState.floor;
        boolean noTeacher = MapData.TEACHER_NAMES[f] == 0;
        if (noTeacher || GameState.defeated[f]) {
            goToFloor(f + 1);
        } else {
            String name = getString(MapData.TEACHER_NAMES[f]);
            showInfo(getString(R.string.stairs_locked, name));
        }
    }

    /** 指定した階へ移動して、階段のとなりに立つ */
    private void goToFloor(int target) {
        GameState.floor = target;
        GameState.visited[target] = true;
        standNextTo('/');
        showFloor();
    }

    // ===================== 扉 =====================

    /**
     * 扉に入ったら、ペアになっている扉の内側に出る。
     * 扉のかたまりを上から順に数えて、1つ目と2つ目、3つ目と4つ目…がペア。
     * かたまりの中では、同じ順番の扉どうしがつながる（左の扉→左の扉）
     */
    private void goThroughDoor(int x, int y) {
        List<List<int[]>> groups = findDoorGroups(MapData.FLOORS[GameState.floor]);
        for (int g = 0; g < groups.size(); g++) {
            List<int[]> group = groups.get(g);
            for (int i = 0; i < group.size(); i++) {
                if (group.get(i)[0] == x && group.get(i)[1] == y) {
                    int pair = (g % 2 == 0) ? g + 1 : g - 1;
                    if (pair >= groups.size()) return; // ペアがない扉は開かない
                    List<int[]> other = groups.get(pair);
                    int[] door = other.get(Math.min(i, other.size() - 1));
                    standInside(door[0], door[1]);
                    mapView.invalidate();
                    return;
                }
            }
        }
    }

    /** 扉（!）のかたまりを探す。つながっている ! を1つのかたまりにする */
    private List<List<int[]>> findDoorGroups(String[] map) {
        List<List<int[]>> groups = new ArrayList<>();
        boolean[][] used = new boolean[map.length][map[0].length()];
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length(); x++) {
                if (map[y].charAt(x) != '!' || used[y][x]) continue;
                // 新しいかたまり：となりの ! を全部集める
                List<int[]> group = new ArrayList<>();
                List<int[]> todo = new ArrayList<>();
                todo.add(new int[] { x, y });
                used[y][x] = true;
                while (!todo.isEmpty()) {
                    int[] p = todo.remove(todo.size() - 1);
                    group.add(p);
                    int[][] around = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
                    for (int[] d : around) {
                        int ax = p[0] + d[0];
                        int ay = p[1] + d[1];
                        if (ay < 0 || ay >= map.length || ax < 0 || ax >= map[ay].length()) continue;
                        if (map[ay].charAt(ax) == '!' && !used[ay][ax]) {
                            used[ay][ax] = true;
                            todo.add(new int[] { ax, ay });
                        }
                    }
                }
                // かたまりの中を、上→下、左→右の順に並べる
                group.sort((a, b) -> a[1] != b[1] ? a[1] - b[1] : a[0] - b[0]);
                groups.add(group);
            }
        }
        return groups;
    }

    // ===================== 立つ位置 =====================

    /** 文字 c のマスを探して、そのとなりの床に立つ */
    private void standNextTo(char c) {
        String[] map = MapData.FLOORS[GameState.floor];
        for (int y = 0; y < map.length; y++) {
            int x = map[y].indexOf(c);
            if (x >= 0) {
                standInside(x, y);
                return;
            }
        }
    }

    /** (x, y) のとなりにある床（.）に立つ */
    private void standInside(int x, int y) {
        String[] map = MapData.FLOORS[GameState.floor];
        int[][] around = { { 0, 1 }, { 0, -1 }, { -1, 0 }, { 1, 0 } };
        for (int[] d : around) {
            int ax = x + d[0];
            int ay = y + d[1];
            if (ay < 0 || ay >= map.length || ax < 0 || ax >= map[ay].length()) continue;
            if (map[ay].charAt(ax) == '.') {
                GameState.px = ax;
                GameState.py = ay;
                return;
            }
        }
        GameState.px = x; // となりに床がないときは、そのマスに立つ
        GameState.py = y;
    }

    /** 階の名前を出して、マップを描き直す */
    private void showFloor() {
        txtFloor.setText(MapData.NAMES[GameState.floor]);
        mapView.invalidate();
    }

    // ===================== 先生 =====================

    /** 先生にぶつかったとき（戦闘画面ができるまでの仮の形） */
    private void meetTeacher() {
        // 戦闘画面がある階は、戦闘画面へ（マップの画面は閉じないので、勝ったら続きから）
        if (BattleData.HAS_BATTLE[GameState.floor]) {
            stopWalking();
            startActivity(new Intent(this, BattleActivity.class));
            return;
        }
        // まだ戦闘画面がない階は、仮の「勝った／負けた」を選ぶ
        String name = getString(MapData.TEACHER_NAMES[GameState.floor]);
        showChoices(getString(R.string.meet_teacher, name),
                new String[] { getString(R.string.btn_win), getString(R.string.btn_lose) },
                new Runnable[] { this::win, this::lose });
    }

    private void win() {
        GameState.defeated[GameState.floor] = true;
        if (GameState.floor == MapData.LAST) {
            goTo(new Intent(this, ClearActivity.class)); // 校長に勝ったらクリア
            return;
        }
        int reward = MapData.REWARDS[GameState.floor];
        GameState.money += reward;
        updateStatus();
        String name = getString(MapData.TEACHER_NAMES[GameState.floor]);
        showInfo(getString(R.string.win_teacher, name, reward));
        mapView.invalidate();
    }

    private void lose() {
        Intent intent = new Intent(this, GameOverActivity.class);
        intent.putExtra(GameOverActivity.EXTRA_TEACHER, GameState.floor); // 2F→1人目 … 5F→校長
        goTo(intent);
    }

    // ===================== メッセージ =====================

    /** 「とじる」だけのメッセージ */
    private void showInfo(String text) {
        showChoices(text, new String[] { getString(R.string.btn_close) }, new Runnable[] { null });
    }

    /** ボタンつきのメッセージ。labels[i] のボタンを押すと actions[i] が動く（null なら閉じるだけ） */
    private void showChoices(String text, String[] labels, Runnable[] actions) {
        talking = true;
        msgText.setText(text);
        msgButtons.removeAllViews();
        for (int i = 0; i < labels.length; i++) {
            Runnable action = actions[i];
            TextView btn = new TextView(this);
            btn.setText(labels[i]);
            btn.setTextColor(getColor(R.color.white));
            btn.setTextSize(16);
            btn.getPaint().setFakeBoldText(true);
            btn.setBackgroundResource(R.drawable.rpg_window);
            btn.setPadding(dp(16), dp(8), dp(16), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginStart(dp(8));
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> {
                talking = false;
                msgBox.setVisibility(View.GONE);
                if (action != null) action.run();
            });
            msgButtons.addView(btn);
        }
        msgBox.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** 押しっぱなしで歩き続ける処理を止める（お店や戦闘の画面が何回も開かないように） */
    private void stopWalking() {
        handler.removeCallbacksAndMessages(null);
    }

    // 別の画面へ移動して、今の画面は閉じる
    private void goTo(Intent intent) {
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}

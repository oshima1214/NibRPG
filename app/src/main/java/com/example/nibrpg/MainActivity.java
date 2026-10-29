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

    /** エンドロールのあとに開くとき true：「理事長室の鍵を受け取った」と知らせる */
    public static final String EXTRA_GOT_KEY = "gotKey";

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

        // メニュー（ステータス・持ち物・記録）を開く。閉じるとマップの続きから
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            if (talking) return;
            stopWalking();
            Sound.se(this, R.raw.se_menu_open);
            startActivity(new Intent(this, StatusActivity.class));
        });

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
            // Pがない階（デバッグで2F〜5Fから始めたとき）は、階段のとなりから
            if (GameState.px < 0) standNextTo('/');
        }
        showFloor();

        // ゲームを始めて、はじめて1Fに来たときだけ、遊び方の説明を出す
        if (!GameState.tutorialDone) {
            GameState.tutorialDone = true;
            showTutorial(0);
        }

        // エンドロールのあと：校長先生から理事長室の鍵を受け取ったことを知らせる
        if (getIntent().getBooleanExtra(EXTRA_GOT_KEY, false)) {
            getIntent().removeExtra(EXTRA_GOT_KEY); // 画面を回したときなどに、もう一度出さない
            showInfo(getString(R.string.boss_key_got));
        }
    }

    /** 遊び方の説明を1ページ出す。「次へ」で次のページ、最後は「はじめる」で閉じる */
    private void showTutorial(int page) {
        String[] pages = getResources().getStringArray(R.array.tutorial_pages);
        boolean last = (page == pages.length - 1);
        showChoices(pages[page],
                new String[] { getString(last ? R.string.btn_start_game : R.string.btn_next) },
                new Runnable[] { last ? null : () -> showTutorial(page + 1) });
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
        // 押した方向を向く（壁などで進めなくても、向きは変わる）
        GameState.dir = dx < 0 ? CharacterSprite.LEFT : dx > 0 ? CharacterSprite.RIGHT
                : dy < 0 ? CharacterSprite.BACK : CharacterSprite.FRONT;
        mapView.invalidate();
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
                stopWalking();
                showChoices(getString(R.string.clerk_hello),
                        new String[] { getString(R.string.btn_buy_items), getString(R.string.btn_talk),
                                getString(R.string.btn_nothing) },
                        new Runnable[] { this::openShop, this::talkToClerk, null });
                return;
            case '$':
                stopWalking();
                showChoices(getString(R.string.vending_ask),
                        new String[] { getString(R.string.btn_yes), getString(R.string.btn_no) },
                        new Runnable[] { this::openVending, null });
                return;
            case 'T':
                stopWalking();
                if (!GameState.defeated[GameState.floor]) {
                    meetTeacher();             // まだ勝っていない：勝負するか聞く
                } else {
                    talkToDefeatedTeacher();   // 勝ったあと：ひとこと話す
                }
                return;
            case '/':
                useStairs();    // 階段：上か下かを選ぶ
                return;
            case '!':
                goThroughDoor(nx, ny);
                return;
            case 'L': // 理事長室の扉：鍵がないと開かない
                stopWalking();
                if (!GameState.bossKey) {
                    Sound.se(this, R.raw.se_locked);
                    showInfo(getString(R.string.boss_door_locked));      // 鍵を持っていない
                } else if (!GameState.doorUnlocked) {
                    useKey(nx, ny);                                       // はじめて：鍵を使うか聞く
                } else {
                    goThroughDoor(nx, ny);                                // もう開いている
                }
                return;
            case 'R': // 理事長
                stopWalking();
                meetBoss();
                return;
        }
        GameState.px = nx;
        GameState.py = ny;
        mapView.onPlayerStepped(); // 移動できたときだけ歩行アニメーション（描き直しもする）
        Sound.se(this, R.raw.se_step, 0.4f);
        pickUpLost();
    }

    /** 落とし物のマスに来たら拾う */
    private void pickUpLost() {
        for (int i = 0; i < MapData.LOST_ITEMS.length; i++) {
            MapData.Lost l = MapData.LOST_ITEMS[i];
            if (GameState.foundLost[i] || l.floor != GameState.floor
                    || l.x != GameState.px || l.y != GameState.py) continue;
            GameState.foundLost[i] = true;
            stopWalking();
            Sound.se(this, R.raw.se_pickup);
            showInfo(getString(R.string.lost_found, getString(l.name),
                    GameState.foundCount(), MapData.LOST_ITEMS.length));
            mapView.invalidate();
        }
    }

    // ===================== 理事長 =====================

    /** 理事長に話しかけたとき：最後の試験を受けるか聞く */
    private void meetBoss() {
        showChoices(getString(R.string.boss_talk),
                new String[] { getString(R.string.btn_yes), getString(R.string.btn_no) },
                new Runnable[] { this::startBoss, null });
    }

    /** 理事長戦を始める（ふつうは1問目から、HPは満タン。デバッグ中は、HPを選んでから） */
    private void startBoss() {
        if (GameState.DEBUG) {
            chooseBossHpForDebug();
            return;
        }
        startBossFrom(BattleData.BOSS_HP);
    }

    /** デバッグ：理事長のHPをどこから始めるか選ぶ（HPに合わせて、何問目からかも決まる） */
    private void chooseBossHpForDebug() {
        int quarter = BattleData.BOSS_HP / BattleData.BOSS_STAGES.length;
        int[] hps = { BattleData.BOSS_HP, quarter * 3, quarter * 2, quarter, BattleData.BOSS_SHRINK_HP };
        String[] stageNames = getResources().getStringArray(R.array.boss_stage_names);
        String[] labels = new String[hps.length];
        for (int i = 0; i < hps.length; i++) {
            labels[i] = getString(R.string.debug_boss_hp_item, hps[i], stageNames[bossStageOf(hps[i])]);
        }
        labels[hps.length - 1] = getString(R.string.debug_boss_hp_shrink, BattleData.BOSS_SHRINK_HP);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.debug_boss_hp_title)
                .setItems(labels, (d, which) -> startBossFrom(hps[which]))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    /** 理事長のHPから、何問目かを決める（HP 200〜151：第1問、150〜101：第2問…） */
    private int bossStageOf(int hp) {
        int quarter = BattleData.BOSS_HP / BattleData.BOSS_STAGES.length;
        return Math.min(BattleData.BOSS_STAGES.length - 1, (BattleData.BOSS_HP - hp) / quarter);
    }

    /** 理事長のHPを hp にして、その問題から始める */
    private void startBossFrom(int hp) {
        GameState.bossHp = hp;
        int stage = bossStageOf(hp);
        Intent intent = new Intent(this, BattleData.BOSS_STAGES[stage]);
        intent.putExtra(BattleData.EXTRA_BOSS, true);
        intent.putExtra(BattleData.EXTRA_STAGE, stage);
        startActivity(intent);
    }

    // 倒した先生と話した回数（階ごと。話すたびに次のセリフになる）
    private static final int[] afterTalks = new int[MapData.FLOORS.length];

    /** 倒した先生に話しかけたとき：勝ったあとのセリフ（話すたびに変わる） */
    private void talkToDefeatedTeacher() {
        int f = GameState.floor;
        String name = getString(MapData.TEACHER_NAMES[f]);
        String[] lines = getResources().getStringArray(MapData.AFTER_TALKS[f]);
        showInfo(getString(R.string.teacher_after, name, lines[afterTalks[f] % lines.length]));
        afterTalks[f]++;
    }

    /** 事務員さんの話を聞く（聞くたびに話が変わる。10回で実績） */
    private void talkToClerk() {
        String[] talks = getResources().getStringArray(R.array.clerk_talks);
        showInfo(talks[GameState.clerkTalks % talks.length]);
        GameState.clerkTalks++;
        if (GameState.clerkTalks >= Achievements.CLERK_TALK_COUNT) {
            Achievements.unlock(this, Achievements.CLERK_REGULAR);
        }
    }

    // ===================== お店 =====================

    /** 事務室の窓口（備品を買う画面）を開く。マップの画面は閉じないので、もどるとそのまま続きから */
    private void openShop() {
        startActivity(new Intent(this, ShopActivity.class));
    }

    /** 自販機の画面を開く。マップの画面は閉じないので、もどるとそのまま続きから */
    private void openVending() {
        startActivity(new Intent(this, VendingActivity.class));
    }

    /** お店からもどってきたときなどに、所持金などを表示し直す */
    @Override
    protected void onResume() {
        super.onResume();
        updateBgm();
        updateStatus();
        mapView.invalidate(); // 戦闘や買い物のあとの状態で描き直す
        GameState.save(this); // オートセーブ（戦闘で勝った・買い物をした、などを保存）
    }

    @Override
    protected void onPause() {
        stopWalking();
        super.onPause();
        Sound.pauseBgmSoon();
        GameState.save(this); // オートセーブ（マップを離れるとき・アプリを閉じるとき）
    }

    /** 場所に合わせてBGMを流す（理事長室の中は、不気味な曲） */
    private void updateBgm() {
        boolean inRoom = GameState.floor == MapData.BOSS_FLOOR && GameState.px <= MapData.BOSS_ROOM_RIGHT;
        Sound.bgm(this, inRoom ? R.raw.bgm_rijicho_room : R.raw.bgm_map);
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
        Sound.se(this, R.raw.se_stairs);
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
                    Sound.se(this, R.raw.se_door);
                    updateBgm(); // 理事長室に入った・出たときは曲を変える
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
                char door = map[y].charAt(x); // ! か L（理事長室の扉）
                if ((door != '!' && door != 'L') || used[y][x]) continue;
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
                        if (map[ay].charAt(ax) == door && !used[ay][ax]) {
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
    /** 先生にふれたとき：先生のセリフを出して、戦うかどうかを聞く */
    private void meetTeacher() {
        stopWalking();
        String name = getString(MapData.TEACHER_NAMES[GameState.floor]);
        String line = getResources().getStringArray(R.array.teacher_greet)[GameState.floor];
        showChoices(getString(R.string.teacher_talk, name, line),
                new String[] { getString(R.string.btn_yes), getString(R.string.btn_no) },
                new Runnable[] { this::startBattle, null }); // 「いいえ」なら何もしない
    }

    /** 戦う（「はい」を選んだとき） */
    private void startBattle() {
        // 戦闘画面がある階は、その階の戦闘画面へ（マップの画面は閉じないので、勝ったら続きから）
        //   2F：ことば並べ　3F：穴うめ　4F：板書うつし　5F：よけゲーム
        if (BattleData.HAS_BATTLE[GameState.floor]) {
            startActivity(new Intent(this, BattleData.SCREENS[GameState.floor]));
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

    /** 理事長室の扉：はじめてさわったとき「鍵を使いますか？」と聞く。「はい」で開けて中に入る */
    private void useKey(int x, int y) {
        showChoices(getString(R.string.boss_use_key),
                new String[] { getString(R.string.btn_yes), getString(R.string.btn_no) },
                new Runnable[] { () -> {
                    GameState.doorUnlocked = true; // 一度開けたら、次からはそのまま通れる
                    Sound.se(this, R.raw.se_key);
                    GameState.save(this);
                    goThroughDoor(x, y);
                }, null });
    }

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
                Sound.se(this, R.raw.se_ok);
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
        // 歩行アニメーションも停止
        if (mapView != null) mapView.stopWalkAnimation();
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

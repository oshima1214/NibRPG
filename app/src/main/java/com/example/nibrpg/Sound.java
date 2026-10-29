package com.example.nibrpg;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

/**
 * 音の係（BGM・ジングル・効果音）。音のファイルは res/raw に入っている。
 *   BGM     ：Sound.bgm(this, R.raw.bgm_map)   ずっとくり返す。同じ曲なら続きから流れ続ける
 *   ジングル：Sound.jingle(this, R.raw.jg_win) 1回だけ流す（BGMは止まる）
 *   効果音  ：Sound.se(this, R.raw.se_ok)
 * 画面を離れるとき（onPause）は Sound.pauseBgmSoon() を呼ぶ。
 *   次の画面がすぐ同じ曲を流すなら、止めずにそのまま続く（画面の切りかえで音が切れない）
 */
public class Sound {

    // 音の大きさ（0〜1）
    public static final float BGM_VOLUME = 0.45f;
    public static final float JINGLE_VOLUME = 0.6f;
    public static final float SE_VOLUME = 0.8f;

    /** 戦闘のBGMは、ほかの曲の何倍の大きさで流すか（BGM_VOLUME × この数。1 をこえる分は 1 になる） */
    public static final float BATTLE_BGM_RATE = 1.9f;
    private static final int[] BATTLE_BGM = {
            R.raw.bgm_floor2, R.raw.bgm_floor3, R.raw.bgm_floor4,
            R.raw.bgm_principal, R.raw.bgm_principal2, R.raw.bgm_boss, R.raw.bgm_boss2,
    };

    /** この曲を流すときの音の大きさ */
    private static float bgmVolume(int res) {
        for (int b : BATTLE_BGM) {
            if (b == res) return Math.min(1f, BGM_VOLUME * BATTLE_BGM_RATE);
        }
        return BGM_VOLUME;
    }

    /** 効果音（はじめにまとめて読みこんでおく。読みこみ中は鳴らないため） */
    private static final int[] SE_LIST = {
            R.raw.se_ok, R.raw.se_cancel, R.raw.se_menu_open, R.raw.se_menu_close, R.raw.se_text,
            R.raw.se_achievement, R.raw.se_step, R.raw.se_stairs, R.raw.se_door, R.raw.se_locked,
            R.raw.se_key, R.raw.se_pickup, R.raw.se_buy, R.raw.se_vending_drop, R.raw.se_no_money,
            R.raw.se_equip, R.raw.se_battle_start, R.raw.se_correct, R.raw.se_wrong, R.raw.se_hit_teacher,
            R.raw.se_critical, R.raw.se_damage, R.raw.se_heal, R.raw.se_pause, R.raw.se_retire,
            R.raw.se_tile_place, R.raw.se_tile_remove, R.raw.se_chalk_write, R.raw.se_board_erase,
            R.raw.se_stamp_warn, R.raw.se_stamp, R.raw.se_chime, R.raw.se_ring, R.raw.se_clock_warn,
            R.raw.se_paper, R.raw.se_phase2, R.raw.se_chalk_wall, R.raw.se_pen_lock, R.raw.se_mouse,
            R.raw.se_pc_impact, R.raw.se_shrink, R.raw.se_confiscate, R.raw.se_stage_next,
    };

    private static Context app;
    private static SoundPool pool;
    private static final Map<Integer, Integer> seIds = new HashMap<>();

    private static MediaPlayer bgm;
    private static int bgmRes = 0;
    private static MediaPlayer jingle;

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final Runnable pauseLater = () -> {
        if (bgm != null && bgm.isPlaying()) bgm.pause();
    };

    /** はじめに1回呼ぶ（何回呼んでもよい）。効果音を読みこむ */
    public static void init(Context c) {
        if (pool != null) return;
        app = c.getApplicationContext();
        AudioAttributes attr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attr).build();
        for (int res : SE_LIST) seIds.put(res, pool.load(app, res, 1));
    }

    // ===================== 効果音 =====================

    /** 効果音を鳴らす */
    public static void se(Context c, int res) {
        se(c, res, 1f);
    }

    /** 効果音を鳴らす（rate：ふだんの何倍の大きさか。足音などは小さめに） */
    public static void se(Context c, int res, float rate) {
        init(c);
        Integer id = seIds.get(res);
        if (id == null) {
            seIds.put(res, pool.load(app, res, 1)); // リストにない音：今回は読みこむだけ
            return;
        }
        float v = Math.min(1f, SE_VOLUME * rate);
        pool.play(id, v, v, 1, 0, 1f);
    }

    // ===================== BGM =====================

    /** BGMを流す。同じ曲が流れていれば、そのまま続ける（止まっていたら続きから） */
    public static void bgm(Context c, int res) {
        init(c);
        handler.removeCallbacks(pauseLater);
        stopJingle();
        if (bgm != null && bgmRes == res) {
            if (!bgm.isPlaying()) bgm.start();
            return;
        }
        stopBgm();
        bgm = MediaPlayer.create(app, res);
        if (bgm == null) return;
        bgmRes = res;
        bgm.setLooping(true);
        float v = bgmVolume(res);
        bgm.setVolume(v, v);
        bgm.start();
    }

    /** 止めていたBGMを、続きから流す（メニューから戻ったときなど） */
    public static void resumeBgm() {
        handler.removeCallbacks(pauseLater);
        if (bgm != null && !bgm.isPlaying() && jingle == null) bgm.start();
    }

    /** 画面を離れるとき（onPause）に呼ぶ。少し待って、次の画面が曲を流さなければ止める */
    public static void pauseBgmSoon() {
        handler.removeCallbacks(pauseLater);
        handler.postDelayed(pauseLater, 400);
        if (jingle != null && jingle.isPlaying()) jingle.pause();
    }

    /** すぐにBGMを止める（一時停止メニューなど） */
    public static void pauseBgm() {
        handler.removeCallbacks(pauseLater);
        if (bgm != null && bgm.isPlaying()) bgm.pause();
    }

    /** BGMを終わらせる */
    public static void stopBgm() {
        if (bgm != null) {
            bgm.release();
            bgm = null;
            bgmRes = 0;
        }
    }

    // ===================== ジングル =====================

    /** ジングル（勝ったとき・負けたときなどの短い曲）を1回流す。BGMは終わらせる */
    public static void jingle(Context c, int res) {
        init(c);
        handler.removeCallbacks(pauseLater);
        stopBgm();
        stopJingle();
        MediaPlayer mp = MediaPlayer.create(app, res);
        if (mp == null) return;
        jingle = mp;
        mp.setVolume(JINGLE_VOLUME, JINGLE_VOLUME);
        mp.setOnCompletionListener(p -> {
            p.release();
            if (jingle == p) jingle = null;
        });
        mp.start();
    }

    /** 止めていたジングルを続きから流す */
    public static void resumeJingle() {
        if (jingle != null && !jingle.isPlaying()) jingle.start();
    }

    public static void stopJingle() {
        if (jingle != null) {
            jingle.release();
            jingle = null;
        }
    }
}

package com.example.nibrpg;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

/**
 * 一時停止できるタイマーの係（戦闘画面で使う）。
 *   ・post / postDelayed で「あとでやること」を予約する（ふつうの Handler と同じ使い方）
 *   ・pause() すると、予約した時間のカウントが止まる。resume() で続きから進む
 *   ・now() は「止まっていた時間をのぞいた時刻」。制限時間の計算に使うと、止めている間は時間が減らない
 */
public class PauseHandler {

    /** 予約した1つの仕事 */
    private static class Task {
        Runnable job;     // やること
        Runnable wrapper; // 実際に Handler にわたす物（終わったら予約リストから消す）
        long due;         // やる時刻（now() の時刻で）
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Task> tasks = new ArrayList<>();
    private boolean paused = false;
    private long pausedAt;    // 止めた時刻
    private long pausedTotal; // これまでに止めていた時間の合計

    /** 止まっていた時間をのぞいた、今の時刻（ミリ秒） */
    public long now() {
        return (paused ? pausedAt : SystemClock.uptimeMillis()) - pausedTotal;
    }

    public boolean isPaused() {
        return paused;
    }

    public void post(Runnable job) {
        postDelayed(job, 0);
    }

    public void postDelayed(Runnable job, long delayMs) {
        final Task t = new Task();
        t.job = job;
        t.due = now() + delayMs;
        t.wrapper = () -> {
            tasks.remove(t);
            job.run();
        };
        tasks.add(t);
        if (!paused) handler.postDelayed(t.wrapper, delayMs);
    }

    /** 予約を全部取り消す（引数はふつうの Handler に合わせているだけ） */
    public void removeCallbacksAndMessages(Object token) {
        handler.removeCallbacksAndMessages(null);
        tasks.clear();
    }

    /** 一時停止：予約した仕事を止める */
    public void pause() {
        if (paused) return;
        paused = true;
        pausedAt = SystemClock.uptimeMillis();
        handler.removeCallbacksAndMessages(null); // 予約リストは残しておく
    }

    /** 再開：残り時間で、予約し直す */
    public void resume() {
        if (!paused) return;
        pausedTotal += SystemClock.uptimeMillis() - pausedAt;
        paused = false;
        long n = now();
        for (Task t : new ArrayList<>(tasks)) {
            handler.postDelayed(t.wrapper, Math.max(0, t.due - n));
        }
    }
}

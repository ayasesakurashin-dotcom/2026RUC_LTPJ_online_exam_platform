package com.exam.server.manager;

import java.util.Map;
import java.util.concurrent.*;

public class TimerManager {
    private static TimerManager instance;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> startFutures = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> endFutures   = new ConcurrentHashMap<>();

    private TimerManager() {}

    public static synchronized TimerManager getInstance() {
        if (instance == null) instance = new TimerManager();
        return instance;
    }

    /** 预约考试开始时间；到达后回调 onStart，考试状态置为进行中 */
    public void scheduleStart(String examId, long startTimeMillis, Runnable onStart) {
        long delay = startTimeMillis - System.currentTimeMillis();
        if (delay <= 0) {
            // 已经过了预约时间，立即开始
            scheduler.execute(onStart);
            return;
        }
        ScheduledFuture<?> f = scheduler.schedule(() -> {
            startFutures.remove(examId);
            onStart.run();
        }, delay, TimeUnit.MILLISECONDS);
        startFutures.put(examId, f);
    }

    /** 考试开始后预约结束时间；到达后回调 onEnd，考试状态置为已结束 */
    public void scheduleEnd(String examId, long endTimeMillis, Runnable onEnd) {
        long delay = endTimeMillis - System.currentTimeMillis();
        if (delay <= 0) {
            scheduler.execute(onEnd);
            return;
        }
        ScheduledFuture<?> f = scheduler.schedule(() -> {
            endFutures.remove(examId);
            onEnd.run();
        }, delay, TimeUnit.MILLISECONDS);
        endFutures.put(examId, f);
    }

    public void cancel(String examId) {
        ScheduledFuture<?> sf = startFutures.remove(examId);
        if (sf != null) sf.cancel(false);
        ScheduledFuture<?> ef = endFutures.remove(examId);
        if (ef != null) ef.cancel(false);
    }

    public void shutdown() { scheduler.shutdownNow(); }
}

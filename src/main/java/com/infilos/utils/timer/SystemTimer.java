package com.infilos.utils.timer;

import com.infilos.utils.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * @author infilos on 2020-08-07.
 */

@ThreadSafe
public class SystemTimer implements Timer {
    private final Logger log = LoggerFactory.getLogger(SystemTimer.class);

    private final String executorName;
    private final ExecutorService executor;
    private final DelayQueue<TimerTasks> delayQueue = new DelayQueue<>();
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    private final TimingWheel timingWheel;
    private final TimingTicker timingTicker;

    /**
     * Locks used to protect data structures while ticking
     */
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();

    public SystemTimer(String name, long tickInMills, int wheelSize, long startInMills) {
        this.executorName = name;
        this.executor = Executors.newFixedThreadPool(1, r -> {
            Thread thread = new Thread(r, executorName);
            thread.setDaemon(false);
            thread.setUncaughtExceptionHandler((t, e) -> {
                log.error("Uncaught exception in thread '{}':", executorName, e);
            });
            return thread;
        });
        timingWheel = new TimingWheel(tickInMills, wheelSize, startInMills, taskCounter, delayQueue);
        timingTicker = new TimingTicker(executorName, this);
    }

    public SystemTimer(String name, long tickInMills, int wheelSize) {
        this(name, tickInMills, wheelSize, Clock.now());
    }

    public SystemTimer(String name) {
        this(name, 1L, 20, Clock.now());
    }


    @Override
    public void submit(TimerTask task) {
        readLock.lock();
        try {
            addTimerItem(new TimerItem(task, task.getDelay() + Clock.now()));
        } finally {
            readLock.unlock();
        }
    }

    private final Function<TimerItem, Void> reinsert = timerTaskItem -> {
        addTimerItem(timerTaskItem);
        return null;
    };

    @Override
    public boolean advance(long timeInMills) {
        try {
            TimerTasks bucket = delayQueue.poll(timeInMills, TimeUnit.MILLISECONDS);
            if (bucket!=null) {
                writeLock.lock();
                try {
                    while (bucket!=null) {
                        timingWheel.advanceClock(bucket.getExpiration());
                        bucket.flush(reinsert);
                        bucket = delayQueue.poll();
                    }
                } finally {
                    writeLock.unlock();
                }
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int count() {
        return taskCounter.get();
    }

    @Override
    public Timer startup() {
        timingTicker.start();
        return this;
    }

    @Override
    public void shutdown() {
        timingTicker.shutdown();
        executor.shutdown();
    }

    private void addTimerItem(TimerItem timerTaskItem) {
        if (!timingWheel.add(timerTaskItem)) {
            // Already expired or cancelled
            if (!timerTaskItem.cancelled()) {
                executor.submit(timerTaskItem.getTimerTask());
            }
        }
    }
}

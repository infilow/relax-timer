package com.infilos.utils.timer;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author infilos on 2020-08-07.
 *
 * Hierarchical Timing Wheels.
 */

@NotThreadSafe
public class TimingWheel {
    private final long tickInMills;
    private final int wheelSize;
    private final long interval;
    private final AtomicInteger taskCounter;
    private final DelayQueue<TimerTasks> queue;
    private long currentTime;

    /**
     * overflowWheel can potentially be updated and read by two concurrent threads through submit().
     * Therefore, it needs to be volatile due to the issue of Double-Checked Locking pattern with JVM.
     */
    private volatile TimingWheel overflowWheel;

    private final TimerTasks[] buckets;

    public TimingWheel(
        long tickInMills,
        int wheelSize,
        long startInMills,
        AtomicInteger taskCounter,
        DelayQueue<TimerTasks> queue) {
        this.tickInMills = tickInMills;
        this.wheelSize = wheelSize;
        this.taskCounter = taskCounter;
        this.queue = queue;
        this.interval = tickInMills * wheelSize;
        this.currentTime = startInMills - (startInMills % tickInMills);
        this.buckets = new TimerTasks[wheelSize];
        for(int idx=0; idx<buckets.length; idx++) {
            buckets[idx] = new TimerTasks(taskCounter);
        }
    }

    public boolean add(TimerItem timerTaskItem) {
        long expiration = timerTaskItem.getExpiration();

        if(timerTaskItem.cancelled()) {
            // Cancelled
            return false;
        } else if(expiration < currentTime + tickInMills) {
            // Already expired
            return false;
        } else if(expiration < currentTime + interval) {
            // Put in its own bucket
            long virtualid = expiration / tickInMills;
            TimerTasks bucket = buckets[(int)(virtualid % wheelSize)];
            bucket.add(timerTaskItem);

            // Set the bucket expiration time
            if (bucket.setExpiration(virtualid * tickInMills)) {
                // The bucket needs to be enqueued because it was an expired bucket
                // We only need to enqueue the bucket when its expiration time has changed, i.e. the wheel has advanced
                // and the previous buckets gets reused; further calls to set the expiration within the same wheel cycle
                // will pass in the same value and hence return false, thus the bucket with the same expiration will not
                // be enqueued multiple times.
                queue.offer(bucket);
            }
            return true;
        } else {
            // Out of the interval. Put it into the parent timer
            if (overflowWheel == null) {
                addOverflowWheel();
            }
            return overflowWheel.add(timerTaskItem);
        }
    }

    public void advanceClock(long timeInMillis) {
        if(timeInMillis >= currentTime + tickInMills) {
            currentTime = timeInMillis - (timeInMillis % tickInMills);

            // Try to advance the clock of the overflow wheel if present
            if(overflowWheel != null) {
                overflowWheel.advanceClock(currentTime);
            }
        }
    }

    private void addOverflowWheel() {
        synchronized (this) {
            if(overflowWheel == null) {
                overflowWheel = new TimingWheel(interval,wheelSize,currentTime,taskCounter,queue);
            }
        }
    }
}

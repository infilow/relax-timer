package com.infilos.utils.timer;

import com.infilos.utils.Timer;

/**
 * @author infilos on 2020-08-07.
 */

public final class PeriodTask extends TimerTask {
    private final Timer timer;
    private final long interval;
    private final Runnable task;

    public PeriodTask(Timer timer, Runnable task, long delay, long interval) {
        this.timer = timer;
        this.task = task;
        this.setDelay(delay);
        this.interval = interval;
    }

    @Override
    public void run() {
        try {
            task.run();
        } finally {
            this.setDelay(interval);
            this.timer.submit(this);
        }
    }
}

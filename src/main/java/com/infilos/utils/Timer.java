package com.infilos.utils;

import com.infilos.utils.timer.PeriodTask;
import com.infilos.utils.timer.SystemTimer;
import com.infilos.utils.timer.TimerTask;

/**
 * @author infilos on 2020-08-07.
 */

public interface Timer {

    /**
     * Add a new task to this executor. It will be executed after the task's delay,
     * beginning from the time of submission.
     */
    void submit(TimerTask task);

    default void submit(TimerTask task, long delayInMills) {
        submit(task(task, delayInMills));
    }

    default void submit(Runnable runnable) {
        submit(task(runnable, 0L));
    }

    default void submit(Runnable runnable, long delayInMills) {
        submit(task(runnable, delayInMills));
    }

    default void submit(Runnable runnable, long delayInMills, long intervalInMills) {
        submit(new PeriodTask(this, runnable, delayInMills, intervalInMills));
    }

    /**
     * Advance the internal clock, executing any tasks whose expiration has been reached
     * within the duration of the passed timeout.
     */
    boolean advance(long millis);

    /**
     * Get the number of tasks pending execution.
     */
    int count();

    /**
     * Start ticking the timer service.
     */
    Timer startup();

    /**
     * Shutdown the timer service, leaving pending tasks unexecuted.
     */
    void shutdown();


    /**
     * Below are factories. Usage, eg. Timer.create("TIMER").startup();
     */
    static Timer create(String name, long tickInMills, int wheelSize, long startInMills) {
        return new SystemTimer(name, tickInMills, wheelSize, startInMills);
    }

    static Timer create(String name, long tickInMills, int wheelSize) {
        return new SystemTimer(name, tickInMills, wheelSize);
    }

    static Timer create(String name) {
        return new SystemTimer(name);
    }

    static TimerTask task(TimerTask task, long delayInMills) {
        task.setDelay(delayInMills);
        return task;
    }

    static TimerTask task(Runnable runnable, long delayInMills) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        };
        task.setDelay(delayInMills);
        return task;
    }
}

package com.infilos.utils.timer;

import javax.annotation.Nonnull;

/**
 * @author infilos on 2020-08-07.
 */

public class TimerItem implements Comparable<TimerItem> {
    private final TimerTask task;
    private final long expiration;

    private volatile TimerTasks list;
    TimerItem next;
    TimerItem prev;

    public TimerItem(TimerTask task, long expirationInMills) {
        // if this timerTask is already held by an existing timer task item,
        // setTimerTaskItem will remove it.
        if(task != null) {
            task.setTimerItem(this);
        }

        this.task = task;
        this.expiration = expirationInMills;
    }

    public boolean cancelled() {
        return task.getTimerItem() != this;
    }

    public void remove() {
        TimerTasks current = list;
        // If remove is called when another thread is moving the item from a task entry list to another,
        // this may fail to remove the item due to the change of value of list. Thus, we retry until the list becomes null.
        // In a rare case, this thread sees null and exits the loop, but the other thread insert the item to another list later.
        while (current != null) {
            current.remove(this);
            current = list;
        }
    }

    @Override
    public int compareTo(@Nonnull TimerItem that) {
        return Long.compare(this.expiration, that.expiration);
    }

    long getExpiration() {
        return expiration;
    }

    TimerTask getTimerTask() {
        return task;
    }

    TimerTasks getTimerItems() {
        return list;
    }

    void setTimerItems(TimerTasks list) {
        this.list = list;
    }
}

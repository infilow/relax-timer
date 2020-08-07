package com.infilos.utils.timer;

/**
 * @author infilos on 2020-08-07.
 */

public abstract class TimerTask implements Runnable {
    protected long delayInMillis = 10 * 1000; // default as 10 seconds

    private TimerItem timerItem = null;

    public void cancel() {
        synchronized (this) {
            if (timerItem!=null) {
                timerItem.remove();
            }
            timerItem = null;
        }
    }

    public void setTimerItem(TimerItem item) {
        // if this timerTask is already held by an existing timer task item,
        // we will remove such an item first.
        synchronized (this) {
            if (timerItem!=null && timerItem!=item) {
                timerItem.remove();
            }
            timerItem = item;
        }
    }

    public TimerItem getTimerItem() {
        return timerItem;
    }

    public long getDelay() {
        return delayInMillis;
    }

    public void setDelay(long delayInMillis) {
        this.delayInMillis = delayInMillis;
    }
}

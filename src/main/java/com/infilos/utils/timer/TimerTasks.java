package com.infilos.utils.timer;

import javax.annotation.Nonnull;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * @author infilos on 2020-08-07.
 */

public class TimerTasks implements Delayed {
    private final TimerItem root;
    private final AtomicLong expiration;
    private final AtomicInteger taskCounter;

    /**
     * TimerTasks forms a doubly linked cyclic list using a dummy root entry
     * - root.next points to the head
     * - root.prev points to the tail
     */
    public TimerTasks(AtomicInteger taskCounter) {
        this.root = new TimerItem(null, -1L);
        this.root.next = root;
        this.root.prev = root;
        this.expiration = new AtomicLong(-1L);
        this.taskCounter = taskCounter;
    }

    public boolean setExpiration(long expirationInMillis) {
        return expiration.getAndSet(expirationInMillis)!=expirationInMillis;
    }

    public long getExpiration() {
        return expiration.get();
    }

    public synchronized void foreach(Function<TimerTask, Void> func) {
        TimerItem item = root.next;
        while (item!=root) {
            TimerItem next = item.next;
            if (!item.cancelled()) {
                func.apply(item.getTimerTask());
            }
            item = next;
        }
    }

    /**
     * Add a timer task entry to this list.
     */
    public void add(TimerItem timerItem) {
        boolean done = false;
        while (!done) {
            // Remove the timer task entry if it is already in any other list
            // We do this outside of the sync block below to avoid deadlocking.
            // We may retry until timerTaskEntry.list becomes null.
            timerItem.remove();

            synchronized (this) {
                synchronized (timerItem) {
                    if (timerItem.getTimerItems()==null) {
                        // put the timer task entry to the end of the list. (root.prev points to the tail entry)
                        TimerItem tail = root.prev;
                        timerItem.next = root;
                        timerItem.prev = tail;
                        timerItem.setTimerItems(this);
                        tail.next = timerItem;
                        root.prev = timerItem;
                        taskCounter.incrementAndGet();
                        done = true;
                    }
                }
            }
        }
    }

    /**
     * Remove the specified timer task entry from this list.
     */
    public void remove(TimerItem timerItem) {
        synchronized (this) {
            synchronized (timerItem) {
                if (timerItem.getTimerItems()==this) {
                    timerItem.next.prev = timerItem.prev;
                    timerItem.prev.next = timerItem.next;
                    timerItem.next = null;
                    timerItem.prev = null;
                    timerItem.setTimerItems(null);
                    taskCounter.decrementAndGet();
                }
            }
        }
    }

    /**
     * Remove all task entries and apply the supplied function to each of them.
     */
    public void flush(Function<TimerItem, Void> func) {
        synchronized (this) {
            TimerItem head = root.next;
            while (head!=root) {
                remove(head);
                func.apply(head);
                head = root.next;
            }
            expiration.set(-1L);
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(Long.max(getExpiration() - TimeUnit.NANOSECONDS.toMillis(System.nanoTime()), 0), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@Nonnull Delayed delayed) {
        TimerTasks other;
        if (delayed instanceof TimerTasks) {
            other = (TimerTasks) delayed;
        } else {
            throw new ClassCastException("can not cast to TimerTasks");
        }

        return Long.compare(this.getExpiration(), other.getExpiration());
    }
}

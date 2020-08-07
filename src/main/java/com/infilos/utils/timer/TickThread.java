package com.infilos.utils.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author infilos on 2020-08-07.
 *
 * Shutdownable thread for ticking.
 */

public abstract class TickThread extends Thread {

    private final Logger log = LoggerFactory.getLogger(TickThread.class);

    private final CountDownLatch shutdownInitiated = new CountDownLatch(1);
    private final CountDownLatch shutdownCompleted = new CountDownLatch(1);

    private final boolean isInterruptiable;
    private volatile boolean isStarted = false;

    TickThread(String name, boolean isInterruptiable) {
        this.isInterruptiable = isInterruptiable;
        this.setName(name);
        this.setDaemon(false);
    }

    TickThread(String name) {
        this(name, false);
    }

    public void shutdown() {
        initiateShutdown();
        awaitShutdown();
    }

    public boolean isShutdownComplete() {
        return shutdownCompleted.getCount() == 0;
    }

    public boolean initiateShutdown() {
        synchronized (this) {
            if(isRunning()) {
                log.info("Shutting down...");
                shutdownInitiated.countDown();
                if(isInterruptiable) {
                    interrupt();
                }
                return true;
            } else {
                return false;
            }
        }
    }

    public void awaitShutdown() {
        try {
            if(shutdownInitiated.getCount() != 0) {
                throw new IllegalStateException("initiateShutdown() was not called before awaitShutdown()");
            } else {
                if(isStarted) {
                    shutdownCompleted.await();
                }
            }
            log.info("Shutdown completed");
        } catch (Throwable ex) {
            log.error("Error occured during await shutdown: ", ex);
        }
    }

    /**
     *  Causes the current thread to wait until the shutdown is initiated,
     *  or the specified waiting time elapses.
     */
    public void pause(long timeout, TimeUnit unit) {
        try {
            if(shutdownInitiated.await(timeout, unit)) {
                log.trace("shutdownInitiated latch count reached zero. Shutdown called.");
            }
        } catch (Throwable ex) {
            log.error("Error occured during pausing: ", ex);
        }
    }

    /**
     * This method is repeatedly invoked until the thread shuts down or this method throws an exception.
     */
    public abstract void invoke();

    @Override
    public void run() {
        this.isStarted = true;
        log.info("Starting");
        try {
            while (isRunning()) {
                invoke();
            }
        } catch (Error fatal) {
            shutdownInitiated.countDown();
            shutdownCompleted.countDown();
            log.error("Stopped on error ", fatal);
        } catch (Throwable ex) {
            if(isRunning()) {
                log.error("Error occured during running: ", ex);
            }
        } finally {
            shutdownCompleted.countDown();
        }
        log.info("Stopped");
    }

    public boolean isRunning() {
        return shutdownInitiated.getCount() != 0;
    }
}

package com.infilos.utils.timer;

import java.util.concurrent.TimeUnit;

/**
 * @author infilos on 2020-08-07.
 */

public final class Clock {
    private Clock() {
    }

    /**
     * @return system nano time as milliseconds.
     */
    public static long now() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }
}

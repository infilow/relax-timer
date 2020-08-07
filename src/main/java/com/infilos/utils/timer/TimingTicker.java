package com.infilos.utils.timer;

import com.infilos.utils.Timer;

/**
 * @author infilos on 2020-08-07.
 */

public class TimingTicker extends TickThread {

    private final Timer timer;

    TimingTicker(String name, Timer timer) {
        super(name);
        this.timer = timer;
    }

    @Override
    public void invoke() {
        timer.advance(200L);
    }
}

package com.infilos.utils;

import com.infilos.utils.timer.TimerTask;
import org.awaitility.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author infilos on 2020-08-07.
 */

@RunWith(JUnit4.class)
public class TimerTest {

    private static class TestTask extends TimerTask {
        private final AtomicBoolean completed = new AtomicBoolean(false);

        private final int id;
        private final CountDownLatch latch;
        private final List<Integer> output;

        TestTask(long delayInMills, int id, CountDownLatch latch, List<Integer> output) {
            this.setDelay(delayInMills);
            this.id = id;
            this.latch = latch;
            this.output = output;
        }

        public void run() {
            if (completed.compareAndSet(false, true)) {
                synchronized (output) {
                    output.add(id);
                }
                latch.countDown();
            }
        }
    }

    private Timer timer;

    @Before
    public void setup() {
        timer = Timer.create("test", 1, 3).startup();
    }

    @After
    public void clean() {
        timer.shutdown();
    }

    @Test
    public void testAlreadyExpiredTask() {
        ArrayList<Integer> output = new ArrayList<>();

        List<CountDownLatch> latches = IntStream.range(-5, 0).mapToObj(new IntFunction<CountDownLatch>() {
            @Override
            public CountDownLatch apply(int idx) {
                CountDownLatch latch = new CountDownLatch(1);
                timer.submit(new TestTask(idx, idx, latch, output));
                return latch;
            }
        }).collect(Collectors.toList());

        timer.advance(0);

        latches.forEach(latch -> {
            try {
                assertTrue(latch.await(3, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        assertArrayEquals(new Integer[]{-5, -4, -3, -2, -1}, output.toArray());
    }

    @Test
    public void testTaskExpiration() {
        List<Integer> output = new ArrayList<>();
        List<TestTask> tasks = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();

        List<CountDownLatch> latches = new ArrayList<>();

        IntStream.range(0, 5).forEach(idx -> {
            CountDownLatch latch = new CountDownLatch(1);
            tasks.add(new TestTask(idx, idx, latch, output));
            ids.add(idx);
            latches.add(latch);
        });

        IntStream.range(10, 100).forEach(idx -> {
            CountDownLatch latch = new CountDownLatch(2);
            tasks.add(new TestTask(idx, idx, latch, output));
            tasks.add(new TestTask(idx, idx, latch, output));
            ids.add(idx);
            ids.add(idx);
            latches.add(latch);
        });

        tasks.forEach(task -> timer.submit(task));

        while (timer.advance(2000)) {
        }

        latches.forEach(latch -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Arrays.sort(ids.toArray());
        Arrays.sort(output.toArray());

        assertArrayEquals(ids.toArray(), output.toArray());
    }

    @Test
    public void testPeriodTask() throws InterruptedException {
        Runnable runnable = new Runnable() {
            final AtomicInteger ticked = new AtomicInteger(0);

            @Override
            public void run() {
                ticked.incrementAndGet();
                if(ticked.get() % 3 == 0) {
                    System.out.println("throw...");
                    throw new RuntimeException();
                } else {
                    System.out.println("run...");
                }
            }
        };

        timer.submit(runnable,1000L,5000L);

        AtomicInteger ticked = new AtomicInteger(0);
        IntStream.range(1, 120).forEach(tick -> {
            ticked.incrementAndGet();
            timer.advance(tick * 1000);
        });

        await().atMost(Duration.FIVE_MINUTES).until(() -> ticked.get()==120);

        Timer.create("test-timer").startup().submit(new Runnable() {
            @Override
            public void run() {
                System.out.println("run...");
            }
        });
    }
}
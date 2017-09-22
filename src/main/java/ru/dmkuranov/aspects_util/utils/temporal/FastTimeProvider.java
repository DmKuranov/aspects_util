package ru.dmkuranov.aspects_util.utils.temporal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Предоставляет время с миллисекундной точностью без излишних накладных расходов
 */
public class FastTimeProvider implements Runnable {
    private static final CountDownLatch threadStarted = new CountDownLatch(1);
    private static final AtomicLong currentMs = new AtomicLong();
    private static final AtomicBoolean threadOnline = new AtomicBoolean(false);

    static {
        FastTimeProvider runnable = new FastTimeProvider();
        Thread timeAccountingThread = new Thread(runnable);
        timeAccountingThread.setDaemon(true);
        timeAccountingThread.start();
    }

    public static long getCurrentTimeMs() {
        if (threadOnline.get()) {
            return currentMs.get();
        } else {
            return System.currentTimeMillis();
        }
    }

    @Override
    public void run() {
        try {
            currentMs.set(System.currentTimeMillis());
            threadStarted.countDown();
            threadOnline.set(true);
            while (true) {
                currentMs.set(System.currentTimeMillis());
                JMXHiccupStatsExposer.eventOccured();
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            // swallow it
        }
    }
}

package ru.dmkuranov.aspects_util.utils.temporal;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

public class FastTimeTest {

    //@Test
    public void hiccupsAccuracyExample() throws Exception {
        FastTimeProvider.getCurrentTimeMs();
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName(JMXHiccupStatsExposer.jmxObjectName);
        MBeanInfo mbeanInfo = mbs.getMBeanInfo(name);
        for (int i = 0; i < 1000; i++) {
            System.out.println(
                    String.format("%5d %5d %5d"
                            , mbs.getAttribute(name, "HiccupsPerSecondForLastMinuteAvg")
                            , mbs.getAttribute(name, "HiccupsPerSecondForLast5MinuteAvg")
                            , mbs.getAttribute(name, "HiccupsPerSecondForLast10MinuteAvg")
                    )
            );
            Thread.sleep(1000);
        }
    }

    //@Test
    public void fastTimeVsExplicitTimeMultiExample() throws Exception {
        Callable<Long> systemMillis = new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return System.currentTimeMillis();
            }
        };
        Callable<Long> fastMillis = new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return FastTimeProvider.getCurrentTimeMs();
            }
        };

        StringBuilder meaningImitator = new StringBuilder();
        System.out.println("System.currentTimeMillis():");
        int threads = 1;
        while (threads <= 32) {
            meaningImitator.append(
                    fastTimeVsExplicitTime(threads, systemMillis)
            );
            threads *= 2;
        }

        System.out.println("FastTimeProvider.getCurrentTimeMs():");
        threads = 1;
        while (threads <= 32) {
            meaningImitator.append(
                    fastTimeVsExplicitTime(threads, fastMillis)
            );
            threads *= 2;
        }
        System.out.println("meaningImitator garbage: " + meaningImitator.toString());
    }

    private Long fastTimeVsExplicitTime(final int threadCount, Callable<Long> millisProvider) throws Exception {
        System.gc();
        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch threadSyncLatch = new CountDownLatch(threadCount);
        Map<SynchronizedCallable, Future<Long>> callables = new LinkedHashMap<SynchronizedCallable, Future<Long>>();

        for (int i = 0; i < threadCount; i++) {
            SynchronizedCallable callable = new SynchronizedCallable(threadSyncLatch, millisProvider);
            Future<Long> future = executorService.submit(callable);
            callables.put(callable, future);
        }
        long totalElapsed = 0;
        long totalThroughput = 0;
        long stubSum = 0;
        for (Map.Entry<SynchronizedCallable, Future<Long>> entry : callables.entrySet()) {
            stubSum += entry.getValue().get();
            SynchronizedCallable callable = entry.getKey();
            totalElapsed += callable.getElapsedMs();
            totalThroughput += (long) 1000 * cycles * iterations / callable.getElapsedMs();
        }
        System.out.println(
                String.format("%2d thread: %9d op/s %5.2f ns/op", threadCount, totalThroughput
                        , (double) (1000000 * totalElapsed) / (cycles * iterations * threadCount))
        );
        executorService.shutdown();
        return stubSum;
    }

    final static int cycles = 50;
    final static int iterations = 1000000;

    private static class SynchronizedCallable implements Callable<Long> {
        private final Callable<Long> internalCallable;
        final CountDownLatch threadSyncLatch;
        long startMs;
        long endMs;

        public SynchronizedCallable(CountDownLatch threadSyncLatch, Callable<Long> internalCallable) {
            this.threadSyncLatch = threadSyncLatch;
            this.internalCallable = internalCallable;
        }

        @Override
        public Long call() throws Exception {
            threadSyncLatch.countDown();
            threadSyncLatch.await();
            startMs = System.currentTimeMillis();
            long meaningImitator = 0;
            int fullIterations = cycles * iterations;
            for (int i = 0; i < fullIterations; i++) {
                meaningImitator += internalCallable.call();
            }
            endMs = System.currentTimeMillis();
            return meaningImitator;
        }

        public long getElapsedMs() {
            return endMs - startMs;
        }
    }
}
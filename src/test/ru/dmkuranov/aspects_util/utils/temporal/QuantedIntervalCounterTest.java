package ru.dmkuranov.aspects_util.utils.temporal;

import org.junit.Assert;
import org.junit.Test;
import ru.dmkuranov.aspects_util.utils.temporal.quantedInterval.QuantedIntervalCounter;
import ru.dmkuranov.aspects_util.utils.temporal.quantedInterval.QuantedIntervalCounterArray;
import ru.dmkuranov.aspects_util.utils.temporal.quantedInterval.QuantedIntervalSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class QuantedIntervalCounterTest {

    @Test
    public void allEventsCounted() {
        final int iterations = 100;
        for (int i = 1; i < iterations; i++) {
            QuantedIntervalCounter counter = new QuantedIntervalCounterArray(i, 100);
            for (int j = 0; j < iterations; j++) {
                counter.eventOccured();
            }
            Assert.assertEquals(iterations, counter.getSnapshot().getTotal());
        }
    }

    @Test
    public void quantsDestributionTest() throws Exception {
        int quantCount = 10;
        long quantSizeMs = 200L;
        QuantedIntervalCounter counter = new QuantedIntervalCounterArray(quantSizeMs, quantCount);
        long currentTime = FastTimeProvider.getCurrentTimeMs();
        Thread.sleep(currentTime % quantSizeMs);
        counter.eventOccured();
        Thread.sleep(quantSizeMs);
        counter.eventOccured();
        counter.eventOccured();
        Thread.sleep(quantSizeMs);
        counter.eventOccured();
        counter.eventOccured();
        counter.eventOccured();
        QuantedIntervalSnapshot snapshot = counter.getSnapshot();
        List<QuantedIntervalSnapshot.QuantEntry> entryList = snapshot.getEntries();
        Assert.assertEquals(1L, entryList.get(0).getCount());
        Assert.assertEquals(2L, entryList.get(1).getCount());
        Assert.assertEquals(3L, entryList.get(2).getCount());
        Assert.assertEquals(3, entryList.size());
    }

    @Test
    public void quantArrayOverflowTest() throws Exception {
        int quantCount = 2;
        long quantSizeMs = 200L;
        QuantedIntervalCounter counter = new QuantedIntervalCounterArray(quantSizeMs, quantCount);
        long currentTime = FastTimeProvider.getCurrentTimeMs();
        Thread.sleep(currentTime % quantSizeMs);
        counter.eventOccured();
        Thread.sleep(quantSizeMs);
        counter.eventOccured();
        counter.eventOccured();
        Thread.sleep(quantSizeMs);
        counter.eventOccured();
        counter.eventOccured();
        counter.eventOccured();
        QuantedIntervalSnapshot snapshot = counter.getSnapshot();
        Assert.assertEquals(5, snapshot.getTotal());
        List<QuantedIntervalSnapshot.QuantEntry> entryList = snapshot.getEntries();
        Assert.assertEquals(2L, entryList.get(0).getCount());
        Assert.assertEquals(3L, entryList.get(1).getCount());
    }

    @Test
    public void quantsConcurrentTest() throws Exception {
        int threadCount = 7;
        final long quantSizeMs = 1000L;
        int quantCount = 10;
        final QuantedIntervalCounter counter = new QuantedIntervalCounterArray(1000, quantCount);
        final int addCount = 100;
        final int addCyclesCount = 3;
        final CountDownLatch startLatch = new CountDownLatch(threadCount + 1);
        Runnable addTask = new Runnable() {
            @Override
            public void run() {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    for (int i = 0; i < addCyclesCount; i++) {
                        for (int j = 0; j < addCount; j++) {
                            counter.eventOccured();
                        }
                        Thread.sleep(quantSizeMs);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(addTask);
            threads.add(thread);
            thread.start();
        }
        long currentTime = FastTimeProvider.getCurrentTimeMs();
        Thread.sleep(currentTime % quantSizeMs);
        startLatch.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
        List<QuantedIntervalSnapshot.QuantEntry> entryList = counter.getSnapshot().getEntries();
        long expectedCount = threadCount * addCount;
        Assert.assertEquals(expectedCount, entryList.get(0).getCount());
        Assert.assertEquals(expectedCount, entryList.get(1).getCount());
        Assert.assertEquals(expectedCount, entryList.get(2).getCount());
    }

    @Test
    public void percentilesTest() {
        final int arrayLength = 200;
        long[] inputArray = new long[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            inputArray[i] = arrayLength - i - 1;
        }
        final QuantedIntervalCounter counter = new QuantedIntervalCounterArray(inputArray, 1000);
        QuantedIntervalSnapshot snapshot = counter.getSnapshot();
        Assert.assertEquals(0, snapshot.getMin());
        Assert.assertEquals(arrayLength - 1, snapshot.getMax());
        Assert.assertEquals(99, snapshot.getPercentile50());
        Assert.assertEquals(100, snapshot.getReversePercentile50());
        Assert.assertEquals(197, snapshot.getPercentile99());
        Assert.assertEquals(2, snapshot.getReversePercentile99());
        Assert.assertEquals(198, snapshot.getPercentile995());
        Assert.assertEquals(1, snapshot.getReversePercentile995());
    }

    //@Test
    public void throughputExample() throws Exception {
        final QuantedIntervalCounter counter = new QuantedIntervalCounterArray(500, 60);
        final AtomicBoolean produceEnabled = new AtomicBoolean(true);
        Runnable addTask = new Runnable() {
            @Override
            public void run() {
                while (produceEnabled.get()) {
                    counter.eventOccured();
                }
            }
        };
        for(int i=0;i<16;i++) {
            Thread thread = new Thread(addTask);
            thread.setDaemon(true);
            thread.start();
        }

        for(int i=0; i<300; i++) {
            QuantedIntervalSnapshot snapshot = counter.getSnapshot();
            System.out.println(String.format("%d    %d  %d  %d", snapshot.getTotal(), snapshot.getPerSecondAverage(), snapshot.getPerSecondPercentile95(), snapshot.getPerSecondPercentile99()));
            Thread.sleep(1000);
        }
        produceEnabled.set(false);
    }

}
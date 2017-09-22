package ru.dmkuranov.aspects_util.utils.temporal.quantedInterval;

import ru.dmkuranov.aspects_util.utils.temporal.FastTimeProvider;

import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * На основе массива примитивов
 * Потокобезопасен, синхронизирован read-write lock. Генерирует минимум мусора
 * >10M событий в секунду на windows jdk 1.7.67 i5-4440. узкое место - writeLock.lock()
 */
public class QuantedIntervalCounterArray implements QuantedIntervalCounter {
    private final long quantMs;
    private volatile int currentQuantIndex = 0;
    private volatile long currentQuantLastMs = -1;
    private boolean quantsFilled = false;
    private final long[] quantCountArray;
    private final ReadWriteLock lock;

    private QuantedIntervalCounterArray(long quantMs, long[] quantCountArray, ReadWriteLock lock) {
        if (quantMs < 0 || quantCountArray.length <= 0) {
            throw new IllegalArgumentException();
        }
        this.quantMs = quantMs;
        this.quantCountArray = quantCountArray;
        this.lock = lock;
    }

    public QuantedIntervalCounterArray(long quantMs, int quantCount) {
        this(quantMs, new long[quantCount], new ReentrantReadWriteLock());
    }

    public QuantedIntervalCounterArray(long quantMs, int quantCount, ReadWriteLock lock) {
        this(quantMs, new long[quantCount], lock);
    }

    public QuantedIntervalCounterArray(long[] quantCountArrayInput, long quantMs) {
        this(quantMs, Arrays.copyOf(quantCountArrayInput, quantCountArrayInput.length), new ReentrantReadWriteLock());
        quantsFilled = true;
    }

    @Override
    public void eventOccured() {
        lock.writeLock().lock();
        try {
            adjustCurrentQuantIndex();
            quantCountArray[currentQuantIndex]++;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void adjustCurrentQuantIndex() {
        lock.writeLock().lock();
        try {
            ensureInited();
            long currentMs = FastTimeProvider.getCurrentTimeMs();
            if (currentMs <= currentQuantLastMs) {
                return;
            } else {
                long newQuantLastMs = currentMs - (currentMs % quantMs) + quantMs - 1;
                Math.ceil((currentMs - currentQuantLastMs) / quantMs);
                int nextIndex = currentQuantIndex + Double.valueOf(Math.ceil((double) (currentMs - currentQuantLastMs) / quantMs)).intValue();
                currentQuantLastMs = newQuantLastMs;
                boolean arrayOverflow = false;
                int currentIndex = currentQuantIndex + 1;
                while (true) {
                    if (currentIndex >= quantCountArray.length) {
                        quantsFilled = true;
                        if (!arrayOverflow) {
                            arrayOverflow = true;
                            currentIndex = 0;
                            nextIndex = nextIndex % quantCountArray.length;
                        } else {
                            throw new AssertionError();
                        }
                    }
                    quantCountArray[currentIndex] = 0;
                    if (currentIndex == nextIndex) {
                        currentQuantIndex = currentIndex;
                        return;
                    }
                    currentIndex++;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void ensureInited() {
        if (currentQuantLastMs == -1) {
            long currentMs = FastTimeProvider.getCurrentTimeMs();
            long currentMsRounded = currentMs - (currentMs % quantMs);
            currentQuantLastMs = currentMsRounded + quantMs - 1;
        }
    }

    @Override
    public QuantedIntervalSnapshot getSnapshot() {
        long[] quantCountArrayCopy;
        int currentQuantIndexCopy;
        long currentQuantLastMsCopy;
        adjustCurrentQuantIndex();
        lock.readLock().lock();
        try {
            currentQuantIndexCopy = currentQuantIndex;
            currentQuantLastMsCopy = currentQuantLastMs;
            if (quantsFilled) {
                quantCountArrayCopy = Arrays.copyOf(quantCountArray, quantCountArray.length);
            } else {
                quantCountArrayCopy = Arrays.copyOf(quantCountArray, currentQuantIndexCopy+1);
            }
        } finally {
            lock.readLock().unlock();
        }
        return new QuantedIntervalSnapshot(quantMs, quantCountArrayCopy, currentQuantIndexCopy, currentQuantLastMsCopy);
    }
}

package ru.dmkuranov.aspects_util.utils.temporal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dmkuranov.aspects_util.utils.temporal.quantedInterval.QuantedIntervalCounter;
import ru.dmkuranov.aspects_util.utils.temporal.quantedInterval.QuantedIntervalCounterArray;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JMXHiccupStatsExposer {
    private static final Logger log = LoggerFactory.getLogger(JMXHiccupStatsExposer.class);
    public static final String jmxObjectName = "HiccupStats:type=" + HiccupStats.class.getSimpleName();
    // захват одного быстрее трех
    private static final ReadWriteLock counterLock = new ReentrantReadWriteLock();
    private static final QuantedIntervalCounter hiccupsPerSecondOn1MinuteCounter = new QuantedIntervalCounterArray(1000, 60, counterLock);
    private static final QuantedIntervalCounter hiccupsPerSecondOn5MinuteCounter = new QuantedIntervalCounterArray(1000, 300, counterLock);
    private static final QuantedIntervalCounter hiccupsPerSecondOn10MinuteCounter = new QuantedIntervalCounterArray(1000, 600, counterLock);
    private static final Semaphore semaphore = new Semaphore(0);

    static {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        HiccupStats mbean = new HiccupStats(hiccupsPerSecondOn1MinuteCounter, hiccupsPerSecondOn5MinuteCounter, hiccupsPerSecondOn10MinuteCounter);
        try {
            ObjectName name = new ObjectName(jmxObjectName);
            mbs.registerMBean(mbean, name);
        } catch (Exception e) {
            log.warn("Error exposing hiccup stats mbean: ", e);
        }
        Thread statsCollectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        semaphore.acquire();
                        counterLock.writeLock().lock();
                        try {
                            hiccupsPerSecondOn1MinuteCounter.eventOccured();
                            hiccupsPerSecondOn5MinuteCounter.eventOccured();
                            hiccupsPerSecondOn10MinuteCounter.eventOccured();
                        } finally {
                            counterLock.writeLock().unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    // swallow it
                }

            }
        });
        statsCollectThread.setDaemon(true);
        statsCollectThread.start();
    }

    public static void eventOccured() {
        semaphore.release();
    }


    public interface HiccupStatsMBean {
        long getHiccupsPerSecondForLastMinuteAvg();

        long getHiccupsPerSecondForLast5MinuteAvg();

        long getHiccupsPerSecondForLast10MinuteAvg();
    }

    public static class HiccupStats implements HiccupStatsMBean {
        private QuantedIntervalCounter hiccupsPerSecondOn1MinuteCounter;
        private QuantedIntervalCounter hiccupsPerSecondOn5MinuteCounter;
        private QuantedIntervalCounter hiccupsPerSecondOn10MinuteCounter;

        public HiccupStats(QuantedIntervalCounter hiccupsPerSecondOn1MinuteCounter, QuantedIntervalCounter hiccupsPerSecondOn5MinuteCounter, QuantedIntervalCounter hiccupsPerSecondOn10MinuteCounter) {
            this.hiccupsPerSecondOn1MinuteCounter = hiccupsPerSecondOn1MinuteCounter;
            this.hiccupsPerSecondOn5MinuteCounter = hiccupsPerSecondOn5MinuteCounter;
            this.hiccupsPerSecondOn10MinuteCounter = hiccupsPerSecondOn10MinuteCounter;
        }

        @Override
        public long getHiccupsPerSecondForLastMinuteAvg() {
            return hiccupsPerSecondOn1MinuteCounter.getSnapshot().getPerSecondAverage();
        }

        @Override
        public long getHiccupsPerSecondForLast5MinuteAvg() {
            return hiccupsPerSecondOn5MinuteCounter.getSnapshot().getPerSecondAverage();
        }

        @Override
        public long getHiccupsPerSecondForLast10MinuteAvg() {
            return hiccupsPerSecondOn10MinuteCounter.getSnapshot().getPerSecondAverage();
        }
    }
}

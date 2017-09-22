package ru.dmkuranov.aspects_util.utils.temporal;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Позволяет организовать защиту от слишком частой обработки событий
 * Т.е. например: если событие происходит чаще 2-х раз в секунду
 * или 4-х раз в минуту
 * или 20 раз в сутки
 * его обработка не требуется
 */
public class FrequencyLimiter {
    // TODO перевести учет на QuantedIntervalCounter
    private final NavigableMap<Long, Integer> eventCountPerSecond = new TreeMap<Long, Integer>();
    private final NavigableMap<Integer, Integer> eventCountLimitForInterval = new TreeMap<Integer, Integer>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Long lastPurgedTime = FastTimeProvider.getCurrentTimeMs() / 1000;
    private final AtomicInteger totalEvents = new AtomicInteger(0);
    private final Integer totalEventsLimit;

    /**
     * @param capacity максимальное количество событий(0 - неограничено) после достижения этого порога все события отклоняются
     */
    public FrequencyLimiter(int capacity) {
        totalEventsLimit = capacity;
    }

    /**
     * Не ограничивающий
     * добавить лимиты можно при помощи addLimit
     */
    public FrequencyLimiter() {
        this(0);
    }

    /**
     * @param limits лимиты продолжительность-количество событий
     */
    public FrequencyLimiter(Map<Integer, Integer> limits) {
        this(limits, 0);
    }

    public FrequencyLimiter(Map<Integer, Integer> limits, int capacity) {
        this(capacity);
        eventCountLimitForInterval.putAll(limits);
    }

    /**
     * Задает ограничение количества событий на интервале(продолжительность интервала - в секундах)
     */
    public FrequencyLimiter addLimit(Integer interval, Integer count) {
        lock.writeLock().lock();
        try {
            eventCountLimitForInterval.put(interval, count);
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Случилось событие, посчитать его
     */
    public void eventOccured() {
        if (totalEventsLimit == 0 || totalEvents.get() < totalEventsLimit) {
            totalEvents.incrementAndGet();
            // TODO точно на тысячу делить?
            long currentTime = FastTimeProvider.getCurrentTimeMs() / 1000;
            lock.writeLock().lock();
            try {
                Integer eventsInCurrentTime = eventCountPerSecond.get(currentTime);
                if (eventsInCurrentTime == null) {
                    eventCountPerSecond.put(currentTime, 1);
                } else {
                    eventCountPerSecond.put(currentTime, eventsInCurrentTime + 1);
                }
                purgeJunk(currentTime);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * Текущая плотность событий не превышает допустимую?
     */
    public boolean isProcessingAllowed() {
        Long currentTime = FastTimeProvider.getCurrentTimeMs() / 1000;
        if (totalEventsLimit == 0 || totalEvents.get() < totalEventsLimit) {
            lock.readLock().lock();
            try {
                for (Map.Entry<Integer, Integer> countForInterval : eventCountLimitForInterval.entrySet()) {
                    Integer eventCountLimit = countForInterval.getValue();
                    Integer totalCountForInterval = getEventCountForInterval(currentTime, countForInterval.getKey());
                    if (totalCountForInterval > eventCountLimit) {
                        return false;
                    }
                }
                return true;
            } finally {
                lock.readLock().unlock();
            }
        }
        return false;
    }

    /**
     * Текущая плотность по интервалам
     */
    public Map<Map.Entry<Integer, Integer>, Integer> getCountForIntervals() {
        lock.readLock().lock();
        try {
            Long currentTime = FastTimeProvider.getCurrentTimeMs() / 1000;
            Map<Map.Entry<Integer, Integer>, Integer> result = new LinkedHashMap<Map.Entry<Integer, Integer>, Integer>();
            for (Map.Entry<Integer, Integer> eventCountLimit : eventCountLimitForInterval.entrySet()) {
                result.put(eventCountLimit, getEventCountForInterval(currentTime, eventCountLimit.getKey()));
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Случилось событие, посчитать его
     * Текущая плотность событий не превышает допустимую?
     */
    public boolean eventOccuredProcessingAllowed() {
        eventOccured();
        return isProcessingAllowed();
    }

    @Override
    public String toString() {
        Map<Map.Entry<Integer, Integer>, Integer> result = getCountForIntervals();
        StringBuilder output = new StringBuilder("");
        output.append("seconds  events/allowed \n");
        for (Map.Entry<Map.Entry<Integer, Integer>, Integer> entry : result.entrySet()) {
            output.append(String.format(
                    "  %5d %7d/%7d", entry.getKey().getKey(), entry.getValue(), entry.getKey().getValue()
            )).append("\n");
        }
        return output.toString();
    }

    /**
     * Удаляем ненужные(слишком старые) для заданных интервалов данные
     */
    private void purgeJunk(Long currentTime) {
        if (!lastPurgedTime.equals(currentTime)) {
            Long obsoleteThreshold = currentTime - eventCountLimitForInterval.lastKey();
            Iterator toRemove = eventCountPerSecond.headMap(obsoleteThreshold, false).keySet().iterator();
            while (toRemove.hasNext()) {
                toRemove.next();
                toRemove.remove();
            }
            lastPurgedTime = currentTime;
        }
    }

    private Integer getEventCountForInterval(Long currentTime, Integer interval) {
        Long oldestIntervalTime = currentTime - interval;
        Integer totalCountForInterval = 0;
        for (Integer count : eventCountPerSecond.tailMap(oldestIntervalTime, true).values()) {
            totalCountForInterval += count;
        }
        return totalCountForInterval;
    }
}


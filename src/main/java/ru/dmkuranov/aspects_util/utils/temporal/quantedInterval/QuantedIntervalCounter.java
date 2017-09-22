package ru.dmkuranov.aspects_util.utils.temporal.quantedInterval;

/**
 * Счетчик с распределением по временным интервалам(квантам)
 * позволяет получать статистику по скользящему интервалу:
 * минимум/максимум/среднее/перцентили событий на квант/в секунду
 */
public interface QuantedIntervalCounter {
    /**
     * добавить событие
     */
    void eventOccured();

    /**
     * получить снапшот статистики за интервал измерения
     */
    QuantedIntervalSnapshot getSnapshot();
}

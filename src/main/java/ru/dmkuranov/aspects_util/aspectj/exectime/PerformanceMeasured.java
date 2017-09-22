package ru.dmkuranov.aspects_util.aspectj.exectime;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Пишет в stdout время выполнения аннотированных методов или публичных методов аннотированных классов
 * Если в стеке вызовов несколько аннотированных методов, распределение их времени выполнения выводится вложенно
 * Вероятно, для @Async будет показывать 0
 */
@Target({METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface PerformanceMeasured {
}

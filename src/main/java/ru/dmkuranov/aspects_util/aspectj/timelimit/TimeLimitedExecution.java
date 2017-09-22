package ru.dmkuranov.aspects_util.aspectj.timelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Ограничивает время выполнения метода
 */
@Target({ElementType.METHOD})
@Retention(RUNTIME)
@Inherited
public @interface TimeLimitedExecution {
    /**
     * максимальное время выполнения в миллисикундах
     */
    int value() default 2000;
}


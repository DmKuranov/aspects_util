package ru.dmkuranov.aspects_util.spring.disableexecutioninsessiononexception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * При возникновении исключительной ситуации в одном из методов, будет прекращен вызов всех методов с этим кодом исключения в этой сессии
 * Можно повестить и на класс, в этом случае эффективно для всех публичных методов
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RUNTIME)
public @interface DisableExecutionInSessionOnException {
    /** Код исключения. Если не задан, прекращается вызов только данного метода(или публичных методов класса, если аннотация на классе) */
    String value() default "";
}

package ru.dmkuranov.aspects_util.spring.emailcomplaintonexception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Отправляет диагностическое сообщение при возникновении исключения по электронной почте
 * Включены лимиты на отправку сообщений: не более 1 в минуту, 3-х в час, 5 в сутки
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RUNTIME)
public @interface EmailComplaintOnException {
    /**
     * Адрес e-mail, куда отправлять
     */
    String[] value();

    /**
     * Префикс темы письма
     */
    String subjectPrefix() default "Exception occured ";
}

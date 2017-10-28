package ru.dmkuranov.aspects_util.spring.disableexecutioninsessiononexception;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import javax.servlet.http.HttpSession;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
public class DisableExecutionInSessionOnExceptionAspect implements Ordered {
    private static final Logger log = LoggerFactory.getLogger(DisableExecutionInSessionOnExceptionAspect.class);

    @Autowired(required = false)
    private HttpSession httpSession;
    private static final String disableExecutionAttrNamePrefix="execution-disabled-for-key=";


    @Around("@within(annotation) && execution(public * *(..))")
    public Object classLevel(ProceedingJoinPoint pjp, DisableExecutionInSessionOnException annotation) throws Throwable {
        final String sessionAttrKeyName;
        if("".equals(annotation.value())) {
            sessionAttrKeyName = disableExecutionAttrNamePrefix + pjp.getTarget().getClass().getName();
        } else {
            sessionAttrKeyName = disableExecutionAttrNamePrefix + annotation.value();
        }
        return proceed(pjp, sessionAttrKeyName);
    }

    @Around("@annotation(annotation)")
    public Object methodLevel(ProceedingJoinPoint pjp, DisableExecutionInSessionOnException annotation) throws Throwable {
        final String sessionAttrKeyName;
        if("".equals(annotation.value())) {
            sessionAttrKeyName = disableExecutionAttrNamePrefix + pjp.getStaticPart().toString();
        } else {
            sessionAttrKeyName = disableExecutionAttrNamePrefix + annotation.value();
        }
        return proceed(pjp, sessionAttrKeyName);
    }

    private Object proceed(ProceedingJoinPoint pjp, String sessionAttrKeyName) throws Throwable {
        Object result = null;
        Object attributeValue = null;
        try {
            attributeValue = httpSession.getAttribute(sessionAttrKeyName);
        } catch (Exception e) {
            // Сессии может не быть. Например, в процессе аутентификации
            return pjp.proceed();
        }
        if(!Boolean.TRUE.equals(attributeValue)) {
            try {
                result = pjp.proceed();
            } catch (Exception e) {
                log.warn("Execution disabled on key "+sessionAttrKeyName+" for httpSession.id="+httpSession.getId(), e);
                httpSession.setAttribute(sessionAttrKeyName, true);
                throw e;
            }
        }
        return result;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE+2;
    }
}


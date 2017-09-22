package ru.dmkuranov.aspects_util.aspectj.exectime;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;

@Aspect
public class MethodExecutionTimeAspect implements Ordered {
    //@Around("(within(@ru.dmkuranov.aspects_util.aspectj.exectime.PerformanceMeasured *) && (execution(public * *(..)) || execution(protected * *(..)))) || execution(@ru.dmkuranov.aspects_util.aspectj.exectime.PerformanceMeasured  * *(..))")
    @Around("(classLevelAnnotatedPublicAndProtectedMethods() || methodLevelAnnotated()) && !isControllerAnnotated()")
    public Object measureExecution(ProceedingJoinPoint pjp) throws Throwable {
        CallStack.executionStarted(pjp);
        try {
            Object object = pjp.proceed();
            return object;
        } finally {
            CallStack.executionFinished(pjp);
        }
    }

    @Around("(classLevelAnnotatedPublicAndProtectedMethods() || methodLevelAnnotated()) && isControllerAnnotated()")
    public Object measureExecutionController(ProceedingJoinPoint pjp) throws Throwable {
        CallStack.executionStarted(pjp);
        try {
            Object object = pjp.proceed();
            return object;
        } finally {
            CallStack.executionFinished(pjp);
        }
    }

    @Pointcut("within(@ru.dmkuranov.aspects_util.aspectj.exectime.PerformanceMeasured *) && (execution(public * *(..)) || execution(protected * *(..)))")
    public void classLevelAnnotatedPublicAndProtectedMethods() {}

    @Pointcut("execution(@ru.dmkuranov.aspects_util.aspectj.exectime.PerformanceMeasured  * *(..))")
    public void methodLevelAnnotated() {}

    @Pointcut("within(@org.springframework.stereotype.Controller *)")
    public void isControllerAnnotated() {}

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

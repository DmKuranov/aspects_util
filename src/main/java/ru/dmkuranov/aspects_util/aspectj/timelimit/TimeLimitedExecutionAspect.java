package ru.dmkuranov.aspects_util.aspectj.timelimit;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import ru.dmkuranov.aspects_util.utils.ProceedingJoinPointHelper;

import java.util.concurrent.*;

@Aspect
public class TimeLimitedExecutionAspect implements Ordered {

    private final ExecutorService executorService;

    Logger log = LoggerFactory.getLogger(TimeLimitedExecutionAspect.class);

    public TimeLimitedExecutionAspect() {
        executorService = Executors.newCachedThreadPool(
                new BasicThreadFactory.Builder().namingPattern("TimeLimitedExecutionAspect-pool-%d").build()
        );
    }

    @Around("@annotation(annotation)")
    public Object methodLevel(ProceedingJoinPoint pjp, TimeLimitedExecution annotation) throws Throwable {
        Integer timeLimitMs = annotation.value();
        return proceed(pjp, timeLimitMs);
    }

    private Object proceed(final ProceedingJoinPoint pjp, Integer timeLimitMs) throws Throwable {
        Object result = null;
        Callable<Object> callable = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    return pjp.proceed();
                } catch (Throwable t) {
                    throw new Exception(t);
                }
            }
        };
        Future<Object> future = executorService.submit(callable);
        try {
            result = future.get(timeLimitMs, TimeUnit.MILLISECONDS);
            return result;
        } catch (TimeoutException e) {
            log.error("Execution of " + ProceedingJoinPointHelper.toString(pjp) + " excess " + timeLimitMs + "ms, interrupted");
            future.cancel(true);
            throw e;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}


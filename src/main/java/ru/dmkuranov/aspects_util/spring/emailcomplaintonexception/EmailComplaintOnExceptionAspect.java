package ru.dmkuranov.aspects_util.spring.emailcomplaintonexception;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import ru.dmkuranov.aspects_util.utils.DebugInfoCollectionHelper;
import ru.dmkuranov.aspects_util.utils.temporal.FrequencyLimiter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Aspect
public class EmailComplaintOnExceptionAspect implements Ordered {
    @Autowired(required = false)
    private HttpSession httpSession;
    @Autowired(required = false)
    private HttpServletRequest httpServletRequest;
    private static final Logger log = LoggerFactory.getLogger(EmailComplaintOnExceptionAspect.class);
    @Autowired(required = false)
    private JavaMailSender javaMailSender;
    private final String mailFrom;
    private final FrequencyLimiter limiter = new FrequencyLimiter();

    public EmailComplaintOnExceptionAspect() {
        this(null);
    }

    public EmailComplaintOnExceptionAspect(String mailFrom) {
        this.mailFrom = mailFrom;
        if(StringUtils.isEmpty(mailFrom)) {
            log.info("No mail from address specified.");
        }
        limiter.addLimit(60, 1).addLimit(3600, 3).addLimit(3600*24, 5);
    }

    @Around("@within(annotation) && execution(public * *(..))")
    public Object classLevel(ProceedingJoinPoint pjp, EmailComplaintOnException annotation) throws Throwable {
        return proceed(pjp, annotation);
    }

    @Around("@annotation(annotation)")
    public Object methodLevel(ProceedingJoinPoint pjp, EmailComplaintOnException annotation) throws Throwable {
        return pjp.proceed();
    }

    private Object proceed(ProceedingJoinPoint pjp, final EmailComplaintOnException annotation) throws Throwable {
        try {
            Object result = pjp.proceed();
            return result;
        } catch (Exception e) {
            if (limiter.eventOccuredProcessingAllowed()) {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(mailFrom);
                    message.setTo(annotation.value());
                    String subject = makeMessageSubject(annotation, e);
                    String body = makeMessageBody(e, pjp);
                    log.info(subject + "\n" + body);
                    message.setSubject(subject);
                    message.setText(body);
                    log.warn(message.getText());
                    javaMailSender.send(message);
                } catch (Exception ex) {
                    // swallow it
                }
            }
            throw e;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private String makeMessageSubject(EmailComplaintOnException annotation, Throwable t) {
        String requestUrl = httpServletRequest.getRequestURL().toString();
        String hostName = httpServletRequest.getLocalName();
        return annotation.subjectPrefix() +" "+ t.getClass().getSimpleName() + " on " + hostName + " processing " + requestUrl;
    }

    private String makeMessageBody(Throwable t, ProceedingJoinPoint pjp) {
        StringBuilder result = new StringBuilder(ExceptionUtils.getStackTrace(t));
        result.append("\n");
        result.append(DebugInfoCollectionHelper.getJoinPointInfo(pjp));
        result.append("\n");
        result.append(DebugInfoCollectionHelper.getWebEnvironmentInfo(httpSession, httpServletRequest));
        result.append("\nLimiter stats:\n").append(limiter.toString());
        return result.toString();
    }
}

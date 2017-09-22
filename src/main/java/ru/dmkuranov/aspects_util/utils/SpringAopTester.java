package ru.dmkuranov.aspects_util.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SpringAopTester implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(SpringAopTester.class);
    private boolean closeContextOnFailure;

    public SpringAopTester(boolean closeContextOnFailure) {
        this.closeContextOnFailure = closeContextOnFailure;
    }

    public SpringAopTester() {
        this(true);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent e) {
        boolean transactionalApplyedResult = false;
        try {
            log.warn("Checking Spring AOP support");
            transactionalApplyed();
            log.warn("direct execution fail");
        } catch (IllegalTransactionStateException x) {
            transactionalApplyedResult = true;
            log.info("Direct execution success");
        }
        if(transactionalApplyedResult) {
            try {
                nestedTransactionalApplyed();
                log.warn("Nested execution fail");
            } catch (IllegalTransactionStateException x) {
                log.info("Nested execution success");
                return;
            }
        }
        log.error("Transactional support not consistent. Check environment.");
        if(closeContextOnFailure) {
            ApplicationContext context = e.getApplicationContext();
            if(context instanceof AbstractApplicationContext) {
                log.error("Closing application context");
                ((AbstractApplicationContext) context).close();
            } else {
                log.error("Cannot close context of class "+context.getClass().getCanonicalName());
            }
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void transactionalApplyed() {}

    public void nestedTransactionalApplyed() {
        transactionalApplyed();
    }
}
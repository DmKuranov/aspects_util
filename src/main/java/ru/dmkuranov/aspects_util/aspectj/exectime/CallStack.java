package ru.dmkuranov.aspects_util.aspectj.exectime;

import org.aspectj.lang.ProceedingJoinPoint;
import ru.dmkuranov.aspects_util.utils.ProceedingJoinPointHelper;

public class CallStack {
    private static final ThreadLocal<CallStack> threadCallStack = new ThreadLocal<CallStack>() {
        @Override
        protected CallStack initialValue() {
            return new CallStack();
        }
    };

    private CallStackFrame currentFrame;

    public static void executionStarted(ProceedingJoinPoint pjp) {
        CallStack currentStack = threadCallStack.get();
        CallStackFrame currentStackFrame = currentStack.getCurrentFrame();
        if (currentStackFrame == null) {
            currentStackFrame = new CallStackFrame(getKey(pjp), null);
            currentStack.setCurrentFrame(currentStackFrame);
        } else {
            CallStackFrame newStackFrame = new CallStackFrame(getKey(pjp), currentStackFrame);
            currentStackFrame.addChildFrame(newStackFrame);
            currentStack.setCurrentFrame(newStackFrame);
        }
    }

    public static void executionFinished(ProceedingJoinPoint pjp) {
        CallStack currentStack = threadCallStack.get();
        CallStackFrame currentStackFrame = currentStack.getCurrentFrame();
        CallStackFrame parentStackFrame = currentStackFrame.getParentFrame();
        currentStackFrame.finish();
        if (parentStackFrame == null) {
            System.out.println("Current stack finished:\n" + currentStackFrame + "\n");
            currentStack.setCurrentFrame(null);
        } else {
            currentStack.setCurrentFrame(parentStackFrame);
        }
    }

    public static CallStack getCurrentStack() {
        return threadCallStack.get();
    }

    private static String getKey(ProceedingJoinPoint pjp) {
        return ProceedingJoinPointHelper.toString(pjp);
    }

    public CallStackFrame getCurrentFrame() {
        return currentFrame;
    }

    public void setCurrentFrame(CallStackFrame currentFrame) {
        this.currentFrame = currentFrame;
    }
}

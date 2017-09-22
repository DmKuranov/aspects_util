package ru.dmkuranov.aspects_util.utils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;

public class ProceedingJoinPointHelper {
    public static String toString(ProceedingJoinPoint pjp) {
        // TODO это может долго исполняться
        Signature signature = pjp.getSignature();
        Class declaredClass = signature.getDeclaringType();
        Object targetObject = pjp.getTarget();
        return declaredClass.getSimpleName()
                + ((targetObject != null && !declaredClass.equals(targetObject.getClass())) ? "(" + targetObject.getClass().getSimpleName() + ")" : "")
                + "#" + signature.getName();
    }
}

package com.team3.monew.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Around("execution(* com.team3.monew..service..*(..))")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.info("[START] {}.{}()", className, methodName);

        try {
            Object result = joinPoint.proceed();
            long end = System.currentTimeMillis();

            log.info("[END] {}.{}() {}ms", className, methodName, (end - start));
            return result;
        } catch (Exception e) {
            long end = System.currentTimeMillis();
            log.error("[ERROR] {}.{}() {}ms - {}", className, methodName, (end - start), e.getMessage());
            throw e;
        }
    }
}
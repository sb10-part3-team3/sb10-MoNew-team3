package com.team3.monew.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {

    private static final String CONTROLLER_POINTCUT =
            "@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Controller)";

    // 서비스 내부 호출까지 모두 기록하지 않고, HTTP 요청이 진입하는 컨트롤러 메서드만 기록한다.
    // @RestController와 @Controller를 모두 포함해 REST API와 일반 MVC 컨트롤러에 같은 기준을 적용한다.
    // @target은 런타임 매칭 때문에 자동 설정 빈까지 프록시 후보가 될 수 있어 사용하지 않는다.
    @Around(CONTROLLER_POINTCUT)
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.info("[START] {}.{}()", className, methodName);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            // 정상 종료 시 API 진입점 기준의 처리 시간을 남긴다.
            log.info("[END] {}.{}() {}ms", className, methodName, elapsed);
            return result;
        } catch (Throwable throwable) {
            long elapsed = System.currentTimeMillis() - start;

            // 예외 객체를 마지막 인자로 넘겨 메시지와 stack trace를 함께 남긴다.
            log.error("[ERROR] {}.{}() {}ms", className, methodName, elapsed, throwable);
            throw throwable;
        }
    }
}

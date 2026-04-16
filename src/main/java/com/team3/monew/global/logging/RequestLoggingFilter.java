package com.team3.monew.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String CLIENT_IP = "clientIp";
    private static final String USER_ID = "userId";
    private static final String[] MDC_KEYS = {REQUEST_ID, CLIENT_IP, USER_ID};

    private static final String UNKNOWN_CLIENT_IP = "unknown";
    private static final String REQUEST_USER_ID_HEADER = "MoNew-Request-User-ID";
    private static final String REQUEST_ID_RESPONSE_HEADER = "X-Request-Id";
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String X_REAL_IP_HEADER = "X-Real-IP";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 요청 처리 중 이 필터가 덮어쓸 MDC 값만 미리 저장한다.
        // traceId, spanId처럼 다른 필터나 추적 라이브러리가 넣은 값은 건드리지 않기 위함이다.
        String[] previousMdcValues = Arrays.stream(MDC_KEYS).map(MDC::get).toArray(String[]::new);

        try {
            String requestId = UUID.randomUUID().toString();

            // logback-spring.xml의 %X{...} 패턴이 읽을 요청 단위 값을 MDC에 넣는다.
            // 이후 컨트롤러/서비스/라이브러리에서 찍히는 로그에 같은 requestId와 clientIp가 함께 출력된다.
            MDC.put(REQUEST_ID, requestId);
            MDC.put(CLIENT_IP, extractClientIp(request));
            String userId = extractUserId(request);
            if (userId != null && !userId.isBlank()) {
                MDC.put(USER_ID, userId);
            } else {
                MDC.remove(USER_ID);
            }

            // 클라이언트가 장애 문의 시 서버 로그와 매칭할 수 있도록 요청 ID를 응답에도 내려준다.
            response.setHeader(REQUEST_ID_RESPONSE_HEADER, requestId);
            filterChain.doFilter(request, response);
        } finally {
            // 요청이 끝나면 이 필터가 관리한 MDC 키만 원래 상태로 되돌린다.
            // MDC.clear()를 쓰면 다른 컴포넌트의 MDC 값까지 지워질 수 있다.
            restoreMdcValues(previousMdcValues);
        }
    }

    // 요청에서 로그에 남길 클라이언트 IP를 우선순위에 따라 추출한다.
    private String extractClientIp(HttpServletRequest request) {
        String forwardedClientIp = extractForwardedClientIp(request.getHeader(X_FORWARDED_FOR_HEADER));
        String realIp = sanitizeForMdc(request.getHeader(X_REAL_IP_HEADER));
        String remoteAddr = sanitizeForMdc(request.getRemoteAddr());

        if (forwardedClientIp != null && !forwardedClientIp.isBlank()) {
            return forwardedClientIp;
        }
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        if (remoteAddr != null && !remoteAddr.isBlank()) {
            return remoteAddr;
        }
        return UNKNOWN_CLIENT_IP;
    }

    // 인증 정보가 있으면 우선 사용하고, 없으면 요청 헤더에서 사용자 ID를 추출한다.
    private String extractUserId(HttpServletRequest request) {
        // 인증 컨텍스트에서 얻은 사용자 정보가 있으면 헤더보다 우선한다.
        Principal principal = request.getUserPrincipal();
        String authenticatedUserId = principal == null ? null : sanitizeForMdc(principal.getName());
        if (authenticatedUserId != null && !authenticatedUserId.isBlank()) {
            return authenticatedUserId;
        }

        return sanitizeForMdc(request.getHeader(REQUEST_USER_ID_HEADER));
    }

    // X-Forwarded-For 헤더의 여러 IP 중 첫 번째 유효 값을 반환한다.
    private String extractForwardedClientIp(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }

        // X-Forwarded-For는 "client, proxy1, proxy2" 형태로 누적되므로 첫 번째 값을 클라이언트 IP로 사용한다.
        return Arrays.stream(forwardedFor.split(","))
                .map(this::sanitizeForMdc)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    // 요청 처리 전 MDC 값을 복구해 같은 스레드의 다음 작업에 값이 섞이지 않게 한다.
    private void restoreMdcValues(String[] previousValues) {
        for (int i = 0; i < MDC_KEYS.length; i++) {
            if (previousValues[i] == null) {
                MDC.remove(MDC_KEYS[i]);
            } else {
                MDC.put(MDC_KEYS[i], previousValues[i]);
            }
        }
    }

    // MDC에 넣기 전 줄바꿈을 제거해 로그 라인 위조를 막는다.
    private String sanitizeForMdc(String value) {
        // 헤더 값에 줄바꿈이 섞이면 로그 라인을 위조할 수 있어 MDC 저장 전에 제거한다.
        return value == null ? null : value.replace("\r", "").replace("\n", "").trim();
    }

}

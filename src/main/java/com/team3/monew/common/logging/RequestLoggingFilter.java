package com.team3.monew.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String CLIENT_IP = "clientIp";
    private static final String USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. requestId 생성
            String requestId = UUID.randomUUID().toString();

            // 2. IP 추출
            String clientIp = extractClientIp(request);

            // 3. userId 추출 (헤더 기반)
            String userId = request.getHeader("MoNew-Request-User-ID");

            // 4. MDC 저장
            MDC.put(REQUEST_ID, requestId);
            MDC.put(CLIENT_IP, clientIp);
            if (userId != null) {
                MDC.put(USER_ID, userId);
            }

            // 5. 응답 헤더 추가
            response.setHeader("X-Request-Id", requestId);

            // 6. 다음 필터/컨트롤러 진행
            filterChain.doFilter(request, response);

        } finally {
            // 7. MDC 정리 (중요)
            MDC.clear();
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0];
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
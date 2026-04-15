package com.team3.monew.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

    private final TrustedProxyMatcher trustedProxies;

    public RequestLoggingFilter(@Value("${monew.logging.trusted-proxies:}") String trustedProxyConfig) {
        this.trustedProxies = new TrustedProxyMatcher(trustedProxyConfig);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 이 필터가 사용하는 MDC 키만 복구해 다른 필터/추적 값은 유지한다.
        String[] previousMdcValues = Arrays.stream(MDC_KEYS).map(MDC::get).toArray(String[]::new);

        try {
            String requestId = UUID.randomUUID().toString();
            String remoteAddr = sanitizeIpAddress(request.getRemoteAddr());

            // logback MDC 패턴에 사용할 요청 단위 값을 저장한다.
            MDC.put(REQUEST_ID, requestId);
            MDC.put(CLIENT_IP, extractClientIp(request, remoteAddr));
            putIfNotBlank(USER_ID, extractUserId(request, remoteAddr));

            response.setHeader(REQUEST_ID_RESPONSE_HEADER, requestId);
            filterChain.doFilter(request, response);
        } finally {
            restoreMdcValues(previousMdcValues);
        }
    }

    private String extractClientIp(HttpServletRequest request, String remoteAddr) {
        if (remoteAddr == null) {
            return UNKNOWN_CLIENT_IP;
        }
        if (!trustedProxies.matches(remoteAddr)) {
            return remoteAddr;
        }

        // 직접 연결된 대상이 신뢰 프록시일 때만 forwarded 헤더를 사용한다.
        String forwardedClientIp = extractForwardedClientIp(request.getHeader(X_FORWARDED_FOR_HEADER));
        return firstNonNull(forwardedClientIp, sanitizeIpAddress(request.getHeader(X_REAL_IP_HEADER)), remoteAddr);
    }

    private String extractUserId(HttpServletRequest request, String remoteAddr) {
        // 클라이언트가 보낸 헤더보다 서버에서 인증된 사용자 정보를 우선한다.
        Principal principal = request.getUserPrincipal();
        String authenticatedUserId = principal == null ? null : sanitizeForMdc(principal.getName());
        if (authenticatedUserId != null && !authenticatedUserId.isBlank()) {
            return authenticatedUserId;
        }
        return trustedProxies.matches(remoteAddr) ? sanitizeForMdc(request.getHeader(REQUEST_USER_ID_HEADER)) : null;
    }

    private String extractForwardedClientIp(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }

        // 프록시 체인에서 오른쪽부터 확인해 가장 가까운 신뢰되지 않은 IP를 선택한다.
        List<String> forwardedIps = Arrays.stream(forwardedFor.split(","))
                .map(this::sanitizeIpAddress)
                .filter(Objects::nonNull)
                .toList();

        for (int i = forwardedIps.size() - 1; i >= 0; i--) {
            if (!trustedProxies.matches(forwardedIps.get(i))) {
                return forwardedIps.get(i);
            }
        }
        return forwardedIps.isEmpty() ? null : forwardedIps.get(0);
    }

    private void putIfNotBlank(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }

    private void restoreMdcValues(String[] previousValues) {
        for (int i = 0; i < MDC_KEYS.length; i++) {
            if (previousValues[i] == null) {
                MDC.remove(MDC_KEYS[i]);
            } else {
                MDC.put(MDC_KEYS[i], previousValues[i]);
            }
        }
    }

    private String sanitizeForMdc(String value) {
        // MDC에 저장하기 전 CR/LF를 제거해 로그 주입을 방지한다.
        return value == null ? null : value.replace("\r", "").replace("\n", "").trim();
    }

    private String sanitizeIpAddress(String value) {
        String sanitized = sanitizeForMdc(value);
        return TrustedProxyMatcher.isIpAddress(sanitized) ? sanitized : null;
    }

    private static String firstNonNull(String... values) {
        return Arrays.stream(values).filter(Objects::nonNull).findFirst().orElse(null);
    }
}

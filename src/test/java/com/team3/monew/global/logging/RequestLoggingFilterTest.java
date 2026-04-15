package com.team3.monew.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void ignoresForwardedHeadersWhenRemoteAddressIsNotTrusted() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setRemoteAddr("198.51.100.10");
        request.addHeader("X-Forwarded-For", "203.0.113.77");
        request.addHeader("X-Real-IP", "203.0.113.88");
        request.addHeader("MoNew-Request-User-ID", "spoofed-user");

        filter.doFilter(request, response, assertMdc("198.51.100.10", null));

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("clientIp")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    void usesRightMostUntrustedForwardedAddressWhenRemoteAddressIsTrusted() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter("10.0.0.0/8");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.77, 198.51.100.10");

        filter.doFilter(request, response, assertMdc("198.51.100.10", null));
    }

    @Test
    void usesRealIpHeaderOnlyWhenRemoteAddressIsTrusted() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter("10.0.0.5");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Real-IP", "198.51.100.10");

        filter.doFilter(request, response, assertMdc("198.51.100.10", null));
    }

    @Test
    void ignoresUserIdHeaderWhenRemoteAddressIsNotTrusted() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setRemoteAddr("198.51.100.10");
        request.addHeader("MoNew-Request-User-ID", "spoofed-user");

        filter.doFilter(request, response, assertMdc("198.51.100.10", null));
    }

    @Test
    void usesUserIdHeaderOnlyWhenRemoteAddressIsTrusted() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter("10.0.0.5");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setRemoteAddr("10.0.0.5");
        request.addHeader("MoNew-Request-User-ID", "trusted-user");

        filter.doFilter(request, response, assertMdc("10.0.0.5", "trusted-user"));
    }

    @Test
    void usesPrincipalBeforeUserIdHeader() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter("10.0.0.5");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setRemoteAddr("10.0.0.5");
        request.setUserPrincipal(() -> "authenticated-user");
        request.addHeader("MoNew-Request-User-ID", "header-user");

        filter.doFilter(request, response, assertMdc("10.0.0.5", "authenticated-user"));
    }

    @Test
    void restoresPreviousLoggingMdcValuesAfterRequest() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter("10.0.0.5");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        MDC.put("requestId", "previous-request-id");
        MDC.put("clientIp", "previous-client-ip");
        MDC.put("userId", "previous-user-id");
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("MoNew-Request-User-ID", "trusted-user");

        filter.doFilter(request, response, assertMdc("10.0.0.5", "trusted-user"));

        assertThat(MDC.get("requestId")).isEqualTo("previous-request-id");
        assertThat(MDC.get("clientIp")).isEqualTo("previous-client-ip");
        assertThat(MDC.get("userId")).isEqualTo("previous-user-id");
    }

    @Test
    void preservesMdcValuesOwnedByOtherComponents() throws ServletException, IOException {
        RequestLoggingFilter filter = new RequestLoggingFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        MDC.put("traceId", "trace-123");
        request.setRemoteAddr("198.51.100.10");

        filter.doFilter(request, response, assertMdc("198.51.100.10", null));

        assertThat(MDC.get("traceId")).isEqualTo("trace-123");
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("clientIp")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    private FilterChain assertMdc(String expectedClientIp, String expectedUserId) {
        return (request, response) -> {
            assertThat(MDC.get("requestId")).isNotBlank();
            assertThat(MDC.get("clientIp")).isEqualTo(expectedClientIp);
            assertThat(MDC.get("userId")).isEqualTo(expectedUserId);
        };
    }
}

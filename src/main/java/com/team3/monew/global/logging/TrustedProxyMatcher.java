package com.team3.monew.global.logging;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

final class TrustedProxyMatcher {

    private final List<TrustedProxy> proxies;

    TrustedProxyMatcher(String proxyConfig) {
        this.proxies = parse(proxyConfig);
    }

    // 주어진 IP가 설정된 신뢰 프록시 목록에 포함되는지 확인한다.
    boolean matches(String ipAddress) {
        return ipAddress != null && proxies.stream().anyMatch(proxy -> proxy.matches(ipAddress));
    }

    static boolean isIpAddress(String value) {
        return parseIpAddress(value) != null;
    }

    private List<TrustedProxy> parse(String proxyConfig) {
        if (proxyConfig == null || proxyConfig.isBlank()) {
            return List.of();
        }
        // 설정 값은 쉼표로 구분한 IP 또는 CIDR 범위를 허용한다.
        return Arrays.stream(proxyConfig.split(","))
                .map(String::trim)
                .filter(proxy -> !proxy.isBlank())
                .map(TrustedProxy::parse)
                .toList();
    }

    private static InetAddress parseIpAddress(String value) {
        if (value == null || value.isBlank() || !isIpLiteral(value)) {
            return null;
        }
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static boolean isIpLiteral(String value) {
        String trimmed = value.trim();
        return trimmed.matches("\\d{1,3}(\\.\\d{1,3}){3}") || trimmed.contains(":");
    }

    private record TrustedProxy(BigInteger address, int bits, int prefixLength) {

        // 단일 IP/CIDR 항목을 숫자 기반 prefix matcher로 변환한다.
        private static TrustedProxy parse(String value) {
            String[] parts = value.split("/", -1);
            if (parts.length > 2) {
                throw new IllegalArgumentException("Invalid trusted proxy value: " + value);
            }

            InetAddress address = parseIpAddress(parts[0].trim());
            if (address == null) {
                throw new IllegalArgumentException("Trusted proxy must be an IP address: " + value);
            }

            int bits = address.getAddress().length * 8;
            int prefixLength = parts.length == 1 ? bits : parsePrefixLength(value, parts[1], bits);
            return new TrustedProxy(toBigInteger(address), bits, prefixLength);
        }

        private static int parsePrefixLength(String value, String prefix, int bits) {
            try {
                int prefixLength = Integer.parseInt(prefix.trim());
                if (prefixLength >= 0 && prefixLength <= bits) {
                    return prefixLength;
                }
            } catch (NumberFormatException ignored) {
            }
            throw new IllegalArgumentException("Invalid trusted proxy prefix length: " + value);
        }

        private boolean matches(String ipAddress) {
            InetAddress candidate = parseIpAddress(ipAddress);
            if (candidate == null || candidate.getAddress().length * 8 != bits) {
                return false;
            }
            // XOR로 다른 비트만 남긴 뒤 host bit를 밀어내 network prefix 일치 여부를 확인한다.
            return address.xor(toBigInteger(candidate)).shiftRight(bits - prefixLength).signum() == 0;
        }

        private static BigInteger toBigInteger(InetAddress address) {
            return new BigInteger(1, address.getAddress());
        }
    }
}

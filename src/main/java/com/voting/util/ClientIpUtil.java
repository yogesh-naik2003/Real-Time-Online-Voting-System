package com.voting.util;

import javax.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

public final class ClientIpUtil {
    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private ClientIpUtil() {
    }

    public static String fromRequest(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            String ip = firstPublicValue(value);
            if (ip != null) {
                return normalize(ip);
            }
        }
        return normalize(request.getRemoteAddr());
    }

    public static String normalize(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty() || "unknown".equalsIgnoreCase(ipAddress.trim())) {
            return "-";
        }
        String ip = ipAddress.trim();
        if (ip.startsWith("[") && ip.contains("]")) {
            ip = ip.substring(1, ip.indexOf("]"));
        }
        if (isLoopback(ip)) {
            String lanIp = findLanAddress();
            return lanIp == null ? "127.0.0.1" : lanIp;
        }
        return ip;
    }

    private static String firstPublicValue(String headerValue) {
        if (headerValue == null || headerValue.trim().isEmpty()) {
            return null;
        }
        String[] parts = headerValue.split(",");
        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isLoopback(String ip) {
        String normalized = ip.toLowerCase(Locale.ROOT);
        return "localhost".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "::1".equals(normalized)
                || "0:0:0:0:0:0:0:1".equals(normalized);
    }

    private static String findLanAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface networkInterface : Collections.list(interfaces)) {
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                String name = (networkInterface.getDisplayName() + " " + networkInterface.getName()).toLowerCase(Locale.ROOT);
                if (name.contains("virtual") || name.contains("vmware") || name.contains("virtualbox") || name.contains("hyper-v")) {
                    continue;
                }
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}

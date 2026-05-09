package com.voting.util;

import com.voting.config.AppConfig;

import javax.servlet.http.HttpServletRequest;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

public final class WebUrlUtil {
    private WebUrlUtil() {
    }

    public static String absoluteUrl(HttpServletRequest req, String pathAndQuery) {
        return baseUrl(req) + pathAndQuery;
    }

    public static String baseUrl(HttpServletRequest req) {
        String configuredBaseUrl = AppConfig.get("app.publicBaseUrl", "APP_PUBLIC_BASE_URL");
        if (configuredBaseUrl != null && !configuredBaseUrl.trim().isEmpty()) {
            return trimTrailingSlash(configuredBaseUrl.trim()) + req.getContextPath();
        }

        String scheme = req.getScheme();
        String serverName = req.getServerName();
        if (isLocalhost(serverName)) {
            String lanAddress = findLanAddress();
            if (lanAddress != null) {
                serverName = lanAddress;
            }
        }

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        int port = req.getServerPort();
        if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
            url.append(":").append(port);
        }
        url.append(req.getContextPath());
        return url.toString();
    }

    private static boolean isLocalhost(String host) {
        if (host == null) {
            return true;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return "localhost".equals(normalized) || "127.0.0.1".equals(normalized) || "::1".equals(normalized);
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

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}

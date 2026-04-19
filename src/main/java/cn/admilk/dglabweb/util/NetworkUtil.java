package cn.admilk.dglabweb.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;

public final class NetworkUtil {
    private NetworkUtil() {
    }

    public static String resolveBestLanAddress() {
        String bestSiteLocal = null;
        int bestSiteLocalScore = Integer.MIN_VALUE;
        String bestFallback = null;
        int bestFallbackScore = Integer.MIN_VALUE;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                int interfaceScore = scoreInterface(networkInterface);
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!(address instanceof Inet4Address)
                        || address.isLoopbackAddress()
                        || address.isAnyLocalAddress()
                        || address.isMulticastAddress()) {
                        continue;
                    }
                    String host = address.getHostAddress();
                    if (address.isSiteLocalAddress() && !address.isLinkLocalAddress()) {
                        if (interfaceScore > bestSiteLocalScore) {
                            bestSiteLocal = host;
                            bestSiteLocalScore = interfaceScore;
                        }
                    } else if (!address.isLinkLocalAddress() && interfaceScore > bestFallbackScore) {
                        bestFallback = host;
                        bestFallbackScore = interfaceScore;
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        if (bestSiteLocal != null && !bestSiteLocal.isEmpty()) {
            return bestSiteLocal;
        }
        if (bestFallback != null && !bestFallback.isEmpty()) {
            return bestFallback;
        }
        return "127.0.0.1";
    }

    private static int scoreInterface(NetworkInterface networkInterface) throws SocketException {
        int score = 0;
        if (networkInterface.isVirtual()) {
            score -= 100;
        }
        String text = (safe(networkInterface.getName()) + " " + safe(networkInterface.getDisplayName())).toLowerCase(Locale.ROOT);
        if (containsAny(text, "wifi", "wi-fi", "wlan", "wireless", "ethernet", "eth", "lan")) {
            score += 20;
        }
        if (containsAny(text, "vmware", "virtualbox", "vbox", "hyper-v", "vethernet", "docker", "wsl", "tailscale", "zerotier", "hamachi", "bluetooth")) {
            score -= 120;
        }
        return score;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

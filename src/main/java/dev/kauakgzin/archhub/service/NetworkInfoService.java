package dev.kauakgzin.archhub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

/**
 * Resolves the addresses the Hub is reachable at from other devices on the
 * same network (e.g. a phone), so the dashboard can offer a "scan to open"
 * shortcut instead of requiring someone to type an IP manually.
 */
@Service
public class NetworkInfoService {

    private final int port;

    public NetworkInfoService(@Value("${server.port:8080}") int port) {
        this.port = port;
    }

    public List<String> lanAddresses() {
        try {
            return lanAddresses(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException ex) {
            return List.of();
        }
    }

    public List<String> urls() {
        return lanAddresses().stream().map(ip -> toUrl(ip, port)).toList();
    }

    public Optional<String> primaryUrl() {
        return urls().stream().findFirst();
    }

    List<String> lanAddresses(Enumeration<NetworkInterface> interfaces) {
        List<String> result = new ArrayList<>();
        for (NetworkInterface networkInterface : Collections.list(interfaces)) {
            if (!isUsable(networkInterface)) {
                continue;
            }
            for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                if (isLanIPv4(address)) {
                    result.add(address.getHostAddress());
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    boolean isUsable(NetworkInterface networkInterface) {
        try {
            return networkInterface.isUp() && !networkInterface.isLoopback() && !networkInterface.isVirtual();
        } catch (SocketException ex) {
            return false;
        }
    }

    boolean isLanIPv4(InetAddress address) {
        return address instanceof Inet4Address && !address.isLoopbackAddress();
    }

    static String toUrl(String ip, int port) {
        return "http://%s:%d".formatted(ip, port);
    }
}

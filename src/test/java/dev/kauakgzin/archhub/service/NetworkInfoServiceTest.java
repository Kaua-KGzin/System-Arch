package dev.kauakgzin.archhub.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkInfoServiceTest {

    private final NetworkInfoService service = new NetworkInfoService(8080);

    @Test
    void toUrlBuildsHttpAddressWithPort() {
        assertThat(NetworkInfoService.toUrl("192.168.1.5", 8080)).isEqualTo("http://192.168.1.5:8080");
    }

    @Test
    void lanIPv4AddressIsAccepted() throws Exception {
        InetAddress address = InetAddress.getByName("192.168.1.5");

        assertThat(service.isLanIPv4(address)).isTrue();
    }

    @Test
    void loopbackAddressIsRejected() throws Exception {
        InetAddress address = InetAddress.getByName("127.0.0.1");

        assertThat(service.isLanIPv4(address)).isFalse();
    }

    @Test
    void ipv6AddressIsRejected() throws Exception {
        InetAddress address = InetAddress.getByName("::1");

        assertThat(service.isLanIPv4(address)).isFalse();
    }

    @Test
    void loopbackInterfaceIsNotUsable() throws SocketException {
        NetworkInterface loopback = Mockito.mock(NetworkInterface.class);
        Mockito.when(loopback.isUp()).thenReturn(true);
        Mockito.when(loopback.isLoopback()).thenReturn(true);
        Mockito.when(loopback.isVirtual()).thenReturn(false);

        assertThat(service.isUsable(loopback)).isFalse();
    }

    @Test
    void downInterfaceIsNotUsable() throws SocketException {
        NetworkInterface down = Mockito.mock(NetworkInterface.class);
        Mockito.when(down.isUp()).thenReturn(false);

        assertThat(service.isUsable(down)).isFalse();
    }

    @Test
    void collectsSortedLanAddressesFromUsableInterfaces() throws Exception {
        NetworkInterface wifi = Mockito.mock(NetworkInterface.class);
        Mockito.when(wifi.isUp()).thenReturn(true);
        Mockito.when(wifi.isLoopback()).thenReturn(false);
        Mockito.when(wifi.isVirtual()).thenReturn(false);
        Mockito.when(wifi.getInetAddresses()).thenReturn(addresses("192.168.1.20", "127.0.0.1"));

        NetworkInterface docker = Mockito.mock(NetworkInterface.class);
        Mockito.when(docker.isUp()).thenReturn(true);
        Mockito.when(docker.isLoopback()).thenReturn(false);
        Mockito.when(docker.isVirtual()).thenReturn(true);
        Mockito.when(docker.getInetAddresses()).thenReturn(addresses("172.17.0.1"));

        Enumeration<NetworkInterface> interfaces = Collections.enumeration(List.of(wifi, docker));

        assertThat(service.lanAddresses(interfaces)).containsExactly("192.168.1.20");
    }

    private Enumeration<InetAddress> addresses(String... ips) throws Exception {
        List<InetAddress> list = new java.util.ArrayList<>();
        for (String ip : ips) {
            list.add(InetAddress.getByName(ip));
        }
        return Collections.enumeration(list);
    }
}

package org.iproduct.iot.demo.udp;

import javax.enterprise.inject.Produces;

public class UDPServerFactory {
    private static UDPChatServer server;

    public static UDPChatServer getServer() {
        if (server == null) {
            server = new UDPChatServer();
            server.start();
        }
        return server;
    }
}
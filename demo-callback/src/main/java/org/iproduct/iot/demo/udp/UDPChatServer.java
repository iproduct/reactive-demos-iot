package org.iproduct.iot.demo.udp;

// UDPChatServer.java
// Chat server using UDP communication protocol. Works with multiple clients.
// (c) Copyright IPT - Intellectual Products & Technologies Ltd., 2004-2006.
// All rights reserved. This software program can be compiled and modified only as a part of the 
// "Programming in Java" course provided by IPT - Intellectual Products & Technologies Ltd.,
// for educational purposes only, and provided that this copyright notice is kept unchanged 
// with the program. The program is provided "as is", without express or implied warranty of any 
// kind, including any implied warranty of merchantability, fitness for a particular purpose or 
// non-infringement. Should the Source Code or any resulting software prove defective, the user
// assumes the cost of all necessary servicing, repair, or correction. In no event shall 
// IPT - Intellectual Products & Technologies Ltd. be liable to any party under any legal theory 
// for direct, indirect, special, incidental, or consequential damages, including lost profits, 
// business interruption, loss of business information, or any other pecuniary loss, or for
// personal injuries, arising out of the use of this source code and its documentation, or arising 
// out of the inability to use any resulting program, even if IPT - Intellectual Products & 
// Technologies Ltd. has been advised of the possibility of such damage. 
// Contact information: www.iproduct.org, e-mail: office@iproduct.org 

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_ADDPeer;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.iproduct.iot.demo.jersey.BootstrapJersey;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Model;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEvent;
import javax.ws.rs.sse.SseEventSource;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@ApplicationScoped
@Model
public class UDPChatServer implements Runnable {
    static final int PORT = 4210;
    static final String IP_ADDRESS = "192.168.137.1";
    private static final Logger log = LoggerFactory.getLogger(BootstrapJersey.class);
    private CopyOnWriteArrayList<EventListener<String>> listeners = new CopyOnWriteArrayList<>();
    private byte[] buffer = new byte[2000];
    private DatagramPacket inPacket =
            new DatagramPacket(buffer, buffer.length);
    private static DatagramSocket socket;
    private Thread thread;

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void destroy() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    public void registerListener(EventListener<String> listener) {
        listeners.add(listener);
    }

    public void removeListener(EventListener<String> listener) {
        listeners.remove(listener);
    }

    public void notifyClients(String message, AddressInfo ai) {
        DatagramPacket outPacket =
                DatagramUtility.getDatagramPacket(message, ai.address, ai.port);
        try {
            socket.send(outPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(PORT, InetAddress.getByName(IP_ADDRESS));
            System.out.println("Server started on " + socket.getLocalSocketAddress());
            while (true) {
                socket.receive(inPacket);
                AddressInfo ai = new AddressInfo(inPacket.getAddress(), inPacket.getPort());
                String payload = DatagramUtility.getString(inPacket);
                log.debug(ai + ": " + payload);
                listeners.stream().forEach(listener -> listener.onEvent(payload));
            }
        } catch (SocketException e) {
            log.error("Can't open socket", e);
        } catch (IOException e) {
            log.error("Communication error", e);
        }
    }

    public static void main(String[] args) {
        UDPChatServer server = new UDPChatServer();
        server.start();
        server.registerListener(System.out::println);
    }

}
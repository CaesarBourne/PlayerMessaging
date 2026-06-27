package com.example.messaging.app;

import com.example.messaging.MessageChannel;
import com.example.messaging.Player;
import com.example.messaging.channel.TcpMessageChannel;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * InitiatorProcess
 *
 * <p><b>Responsibility:</b> The <b>separate-process</b> initiator (Requirement 7).
 * A standalone {@code main} in its own JVM (its own PID). It connects a
 * {@link Socket} to the responder process, wraps it in a
 * {@link TcpMessageChannel}, and hands it to a {@link Player} running the
 * {@code initiate(...)} role.</p>
 *
 * <p>Usage: {@code java ... InitiatorProcess [host] [port]}
 * (defaults: localhost 5000).</p>
 */
public final class InitiatorProcess {

    private static final int DEFAULT_PORT = 5000;
    private static final String DEFAULT_HOST = "localhost";
    private static final int TARGET = 10;
    private static final String SEED = "PING";

    public static void main(String[] args) throws IOException, InterruptedException {
        String host = (args.length > 0) ? args[0] : DEFAULT_HOST;
        int port = (args.length > 1) ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        long pid = ProcessHandle.current().pid();

        System.out.println("[Initiator] JVM started, PID " + pid + ", connecting to " + host + ":" + port);

        try (Socket socket = connectWithRetry(host, port);
             MessageChannel channel = new TcpMessageChannel(socket)) {

            System.out.println("[Initiator] Connected to responder. Conversation starting.\n");
            Player alice = new Player("Alice", channel);
            alice.initiate(SEED, TARGET);
        }

        System.out.println("\n[Initiator] JVM PID " + pid + " exiting.");
    }

    /**
     * Connect to the responder, retrying briefly so a small start-up race
     * (initiator launching a moment before the server has bound its port) does
     * not break the demo. This is robustness, not protocol.
     */
    private static Socket connectWithRetry(String host, int port)
            throws IOException, InterruptedException {
        final int attempts = 20;
        final int delayMillis = 250;
        for (int i = 1; i <= attempts; i++) {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 1000);
                return socket;
            } catch (ConnectException refused) {
                if (i == attempts) {
                    throw refused;
                }
                Thread.sleep(delayMillis); // server not up yet; wait and retry
            }
        }
        throw new IOException("Unreachable"); // never reached
    }

    private InitiatorProcess() { /* entry point only */ }
}

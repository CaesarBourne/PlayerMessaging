package com.example.messaging.app;

import com.example.messaging.MessageChannel;
import com.example.messaging.Player;
import com.example.messaging.channel.TcpMessageChannel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ResponderProcess
 *
 * <p><b>Responsibility:</b> The <b>separate-process</b> responder (Requirement 7).
 * It is a standalone {@code main} &mdash; a whole JVM of its own, with its own
 * PID. It opens a {@link ServerSocket}, waits for the initiator process to
 * connect, wraps the accepted socket in a {@link TcpMessageChannel}, and hands
 * it to a {@link Player} running the {@code respond()} role.</p>
 *
 * <p>The {@code Player} here is byte-for-byte the same class used in the
 * same-process app. Only the channel changed.</p>
 *
 * <p>Usage: {@code java ... ResponderProcess [port]} (default port 5000).</p>
 */
public final class ResponderProcess {

    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) throws IOException {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        long pid = ProcessHandle.current().pid();

        System.out.println("[Responder] JVM started, PID " + pid + ", listening on port " + port);

        try (ServerSocket server = new ServerSocket(port)) {
            try (Socket socket = server.accept();
                 MessageChannel channel = new TcpMessageChannel(socket)) {

                System.out.println("[Responder] Initiator connected. Conversation starting.\n");
                Player bob = new Player("Bob", channel);
                bob.respond();
            }
        }

        System.out.println("\n[Responder] JVM PID " + pid + " exiting.");
    }

    private ResponderProcess() { /* entry point only */ }
}

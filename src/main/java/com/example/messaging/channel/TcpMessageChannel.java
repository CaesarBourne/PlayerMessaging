package com.example.messaging.channel;

import com.example.messaging.Message;
import com.example.messaging.MessageChannel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * TcpMessageChannel
 *
 * <p><b>Responsibility:</b> Transport messages between two players that live in
 * <b>separate JVM processes</b> (Requirement 7). It is the inter-process
 * implementation of {@link MessageChannel}, built on a single TCP
 * {@link Socket}.</p>
 *
 * <p>Wire protocol: the simplest thing that works &mdash; one message per line
 * of UTF-8 text. {@code send()} writes the content followed by a newline and
 * flushes; {@code receive()} reads one line. When the peer closes its socket,
 * {@code readLine()} returns {@code null}, which we translate into
 * {@link Optional#empty()} &mdash; the same end-of-conversation signal the
 * in-memory channel produces with its poison pill. Identical contract, totally
 * different mechanism.</p>
 */
public final class TcpMessageChannel implements MessageChannel {

    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private volatile boolean closed = false;

    public TcpMessageChannel(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8));
    }

    @Override
    public synchronized void send(Message message) {
        if (closed) {
            return;
        }
        try {
            // Guard the line-based protocol: a message must never contain a newline.
            out.write(message.content().replace("\n", " ").replace("\r", " "));
            out.write('\n');
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to send over socket", e);
        }
    }

    @Override
    public Optional<Message> receive() {
        try {
            String line = in.readLine();          // null == peer closed the stream
            return (line == null) ? Optional.empty() : Optional.of(new Message(line));
        } catch (IOException e) {
            return Optional.empty();               // a broken connection also ends the conversation
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            socket.close();                        // closing the socket signals EOF to the peer
        } catch (IOException ignored) {
            // closing best-effort; nothing useful to do on failure
        }
    }
}

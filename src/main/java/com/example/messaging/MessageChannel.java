package com.example.messaging;

import java.util.Optional;

/**
 * MessageChannel
 *
 * <p><b>Responsibility:</b> Hide <i>how</i> two players exchange messages. It is
 * the seam that lets the very same {@link Player} run unchanged whether the two
 * players live in one JVM (Requirement 5) or in two separate JVMs (Requirement 7).</p>
 *
 * <p>This is the single most important design decision in the project. {@code Player}
 * depends only on this interface (an abstraction), never on queues or sockets
 * (concrete details). That is the Dependency Inversion Principle, and it is why
 * we need exactly one {@code Player} class instead of two.</p>
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@code InMemoryChannel} &ndash; same process, backed by blocking queues.</li>
 *   <li>{@code TcpMessageChannel} &ndash; separate processes, backed by a socket.</li>
 * </ul>
 *
 * <p>Extends {@link AutoCloseable} so a channel can be used in a
 * try-with-resources block. We deliberately <i>narrow</i> the inherited
 * {@code close()} so it declares no checked exception &mdash; closing a channel
 * should be a clean, no-fuss operation for the caller.</p>
 */
public interface MessageChannel extends AutoCloseable {

    /**
     * Send one message to the peer. Best-effort: if the channel is already
     * closed the call is silently ignored.
     */
    void send(Message message);

    /**
     * Block until a message arrives from the peer.
     *
     * @return the next message, or {@link Optional#empty()} when the peer has
     *         closed the channel and no more messages will ever arrive. The
     *         empty result is the universal "the conversation is over" signal
     *         that both transports translate into their own native form
     *         (a queue poison value, or a socket end-of-stream).
     */
    Optional<Message> receive();

    /**
     * Close this side of the channel and tell the peer no more messages are
     * coming. Must be idempotent (safe to call more than once).
     */
    @Override
    void close();
}

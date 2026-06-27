package com.example.messaging.channel;

import com.example.messaging.Message;
import com.example.messaging.MessageChannel;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * InMemoryChannel
 *
 * <p><b>Responsibility:</b> Transport messages between two players that live in
 * the <b>same JVM</b> (Requirement 5). It is the in-process implementation of
 * {@link MessageChannel}.</p>
 *
 * <p>Mechanism: two {@link BlockingQueue}s, crossed. A channel reads from its
 * {@code inbound} queue and writes to its {@code outbound} queue; the peer's
 * channel uses the same two queues but swapped. {@code receive()} blocks on
 * {@code take()} until something is available, which is exactly the blocking
 * request/response rhythm we want &mdash; and it requires no busy-waiting.</p>
 *
 * <p>End-of-conversation signal: the queue element type is
 * {@code Optional<Message>}. A normal message is an {@code Optional.of(...)};
 * the special {@link Optional#empty()} value acts as a "poison pill" that
 * unblocks the waiting peer and tells it to stop. This keeps the queue's
 * vocabulary identical to {@code MessageChannel.receive()}'s return type.</p>
 */
public final class InMemoryChannel implements MessageChannel {

    /**
     * A connected pair of channels: hand {@code initiatorSide} to the player
     * that will {@code initiate(...)} and {@code responderSide} to the player
     * that will {@code respond()}.
     */
    public record Pair(MessageChannel initiatorSide, MessageChannel responderSide) {}

    /** The poison pill that signals "no more messages". */
    private static final Optional<Message> END = Optional.empty();

    private final BlockingQueue<Optional<Message>> inbound;
    private final BlockingQueue<Optional<Message>> outbound;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private InMemoryChannel(BlockingQueue<Optional<Message>> inbound,
                            BlockingQueue<Optional<Message>> outbound) {
        this.inbound = inbound;
        this.outbound = outbound;
    }

    /**
     * Factory that wires up the two crossed queues and returns both ends.
     * Putting construction here keeps the crossing logic in one obvious place
     * and prevents callers from mis-wiring the queues.
     */
    public static Pair pair() {
        BlockingQueue<Optional<Message>> a2b = new LinkedBlockingQueue<>();
        BlockingQueue<Optional<Message>> b2a = new LinkedBlockingQueue<>();
        MessageChannel initiator = new InMemoryChannel(/* inbound */ b2a, /* outbound */ a2b);
        MessageChannel responder = new InMemoryChannel(/* inbound */ a2b, /* outbound */ b2a);
        return new Pair(initiator, responder);
    }

    @Override
    public void send(Message message) {
        if (closed.get()) {
            return; // best-effort: ignore sends after close
        }
        outbound.add(Optional.of(message)); // queue is unbounded, never blocks
    }

    @Override
    public Optional<Message> receive() {
        try {
            return inbound.take(); // blocks until a value (message or END) is available
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the interrupt flag
            return END;
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            outbound.add(END); // wake the peer's blocked receive() and tell it to stop
        }
    }
}

package com.example.messaging;

import java.util.Optional;

/**
 * Player
 *
 * <p><b>Responsibility:</b> Be a participant in the conversation. A player:
 * <ul>
 *   <li>knows its own {@code name},</li>
 *   <li>holds exactly one {@link MessageChannel} to its peer,</li>
 *   <li>counts how many messages it has {@code sent} and {@code received},</li>
 *   <li>applies the reply rule (Requirement 3), and</li>
 *   <li>plays one of two roles: {@link #initiate} or {@link #respond}.</li>
 * </ul>
 *
 * <p>Crucially, a {@code Player} has <b>no idea</b> whether it is talking to a
 * thread in the same JVM or a process on another machine. All of that lives
 * behind {@link MessageChannel}. That is what keeps this class clean: it
 * contains conversation <i>logic only</i>, no transport plumbing.</p>
 *
 * <p>Why two methods instead of one role flag? Because the method you call
 * <i>is</i> the role. {@code initiate(...)} starts the conversation and owns the
 * stop condition; {@code respond()} purely reacts. The only behaviour they share
 * &mdash; "build the next message from what I just received plus my own counter"
 * &mdash; is factored into the private {@link #buildReply} helper, so the rule
 * exists in exactly one place.</p>
 */
public final class Player {

    /** Separator used when concatenating counters onto a message (Requirement 3). */
    private static final String SEPARATOR = "|";

    private final String name;
    private final MessageChannel channel;

    /** Number of messages this player has sent so far. */
    private int sent = 0;
    /** Number of messages this player has received so far. */
    private int received = 0;

    public Player(String name, MessageChannel channel) {
        this.name = name;
        this.channel = channel;
    }

    /**
     * Role INITIATOR (Requirements 2, 4).
     *
     * <p>Sends the first ("kickoff") message, then keeps the ping-pong going,
     * applying the reply rule to every message it receives, until the stop
     * condition is met: it has sent {@code target} messages <b>and</b> received
     * {@code target} messages back. It then closes the channel, which is how the
     * responder learns it is time to finish (graceful shutdown).</p>
     *
     * @param seed   content of the very first message (not a reply to anything)
     * @param target the stop count for both sent and received (10 in this exercise)
     */
    public void initiate(String seed, int target) {
        // The kickoff is a send, so it counts toward the "sent" total.
        sent++;
        Message first = new Message(seed);
        logSend(first);
        channel.send(first);

        // Continue while EITHER quota is still unfilled.
        while (sent < target || received < target) {
            Optional<Message> incoming = channel.receive();
            if (incoming.isEmpty()) {
                break; // peer closed early; nothing more will arrive
            }
            received++;
            logRecv(incoming.get());

            // Stop condition: both quotas now satisfied -> do NOT reply again.
            if (sent >= target && received >= target) {
                break;
            }

            Message reply = buildReply(incoming.get().content());
            logSend(reply);
            channel.send(reply);
        }

        channel.close();   // signal the responder that the conversation is over
        summary();
    }

    /**
     * Role RESPONDER (Requirement 3).
     *
     * <p>Purely reactive: for every message it receives it sends back a reply,
     * and it stops the moment the channel reports end-of-conversation
     * (an empty {@code receive()}), which the initiator triggers by closing.</p>
     */
    public void respond() {
        Optional<Message> incoming;
        while ((incoming = channel.receive()).isPresent()) {
            received++;
            logRecv(incoming.get());

            Message reply = buildReply(incoming.get().content());
            logSend(reply);
            channel.send(reply);
        }

        channel.close();
        summary();
    }

    /**
     * The shared reply rule (Requirement 3): the new message is the received
     * content concatenated with the counter of the message this player is now
     * sending. Incrementing {@link #sent} first means the appended number is the
     * index of <i>this</i> outgoing message.
     */
    private Message buildReply(String receivedContent) {
        sent++;
        return new Message(receivedContent + SEPARATOR + sent);
    }

    private void logSend(Message m) {
        System.out.printf("[%-5s] --> sent #%-2d : %s%n", name, sent, m.content());
    }

    private void logRecv(Message m) {
        System.out.printf("[%-5s] <-- recv #%-2d : %s%n", name, received, m.content());
    }

    private void summary() {
        System.out.printf(">> %s finished. Totals: sent=%d, received=%d%n", name, sent, received);
    }
}

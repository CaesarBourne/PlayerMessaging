package com.example.messaging;

import java.util.Objects;

/**
 * Message
 *
 * <p><b>Responsibility:</b> Represent a single immutable piece of text that
 * travels from one {@link Player} to another. It carries the conversation
 * payload and nothing else &mdash; it knows nothing about how it is sent,
 * who sends it, or any counters.</p>
 *
 * <p>Modelled as a {@code record} because a message is a pure, immutable
 * <i>value</i>: two messages with the same content are equal, and the content
 * can never change once created. Immutability also makes it safe to hand the
 * same instance between threads in the same-process scenario.</p>
 */
public record Message(String content) {

    /**
     * Compact canonical constructor: validates the invariant that content is
     * never {@code null}. (A record auto-generates the field assignment.)
     */
    public Message {
        Objects.requireNonNull(content, "content must not be null");
    }
}

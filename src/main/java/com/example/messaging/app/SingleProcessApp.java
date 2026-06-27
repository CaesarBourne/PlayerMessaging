package com.example.messaging.app;

import com.example.messaging.Player;
import com.example.messaging.channel.InMemoryChannel;

/**
 * SingleProcessApp
 *
 * <p><b>Responsibility:</b> Wire up the <b>same-process</b> scenario
 * (Requirement 5). It creates one connected {@link InMemoryChannel.Pair},
 * builds two {@link Player}s, and runs each player on its own {@link Thread}
 * inside this single JVM. The shared process id (PID) is printed to prove both
 * players live in the same process.</p>
 *
 * <p>Why two threads? Each player's loop blocks on {@code receive()} waiting for
 * the other. Two independent threads let them block independently, which mirrors
 * the separate-process scenario exactly &mdash; the only thing that changed is
 * the {@link com.example.messaging.MessageChannel} implementation, not the
 * {@code Player}.</p>
 */
public final class SingleProcessApp {

    private static final int TARGET = 10;
    private static final String SEED = "PING";

    public static void main(String[] args) throws InterruptedException {
        long pid = ProcessHandle.current().pid();
        System.out.println("=== SAME-PROCESS demo (Requirement 5) ===");
        System.out.println("Both players run inside this single JVM, PID " + pid + "\n");

        InMemoryChannel.Pair channels = InMemoryChannel.pair();
        Player alice = new Player("Alice", channels.initiatorSide());
        Player bob   = new Player("Bob",   channels.responderSide());

        Thread bobThread   = new Thread(bob::respond, "Bob-thread");
        Thread aliceThread = new Thread(() -> alice.initiate(SEED, TARGET), "Alice-thread");

        bobThread.start();    // responder is ready first
        aliceThread.start();  // initiator kicks off the conversation

        aliceThread.join();   // wait for both to finish before exiting (graceful shutdown)
        bobThread.join();

        System.out.println("\n=== Done. Both players ran in the same PID " + pid + " ===");
    }

    private SingleProcessApp() { /* entry point only */ }
}

# player-messaging

Two `Player` instances exchange messages until the initiator has sent 10 and
received 10, then the program shuts down gracefully. The same `Player` class
runs in **one JVM** (threads + in-memory queues) and in **two separate JVMs**
(sockets). Pure Java, no frameworks, no third-party dependencies.

---

## How to run

```bash
./run.sh same        # Requirement 5: both players in ONE JVM (default)
./run.sh separate    # Requirement 7: each player in its OWN JVM (different PID)
```

`run.sh` builds with `mvn -q -DskipTests compile` and then launches with plain
`java`. The first Maven build needs internet access to download the standard
Maven compiler/resources plugins (normal Maven behaviour); after that it is
fully offline. To change the TCP port for the separate-process demo:
`PORT=6000 ./run.sh separate`.

Requires JDK 17+ and Maven 3.6+.

---

## Requirement → code map

| # | Requirement | Where it lives |
|---|-------------|----------------|
| 1 | Two players | `Player` class; two instances created in each app |
| 2 | One player initiates | `Player.initiate(...)` vs `Player.respond()` |
| 3 | Reply = received + own send counter | `Player.buildReply(...)` |
| 4 | Stop after 10 sent **and** 10 received, graceful | stop condition in `Player.initiate`; `channel.close()` drains both sides |
| 5 | Both players, same process | `SingleProcessApp` + `InMemoryChannel` |
| 6 | Document each class's responsibility | Javadoc `Responsibility:` block on every class |
| 7 | Each player, separate process (different PID) | `ResponderProcess` + `InitiatorProcess` + `TcpMessageChannel` |

---

## The one design idea that matters

A player should not care **how** bytes reach its peer. So transport is hidden
behind a single interface:

```
            +--------------------+
            |   MessageChannel   |   (send / receive / close)
            +--------------------+
              ^                ^
              |                |
   +---------------------+  +----------------------+
   |   InMemoryChannel   |  |  TcpMessageChannel   |
   |  (queues, same JVM) |  |  (socket, two JVMs)  |
   +---------------------+  +----------------------+
```

`Player` depends only on `MessageChannel`. Requirements 5 and 7 are then the
**same program with a different channel plugged in** — which is why there is
exactly one `Player` class, not two. This is the Dependency Inversion Principle
in practice.

---

## Classes and their responsibilities

- **`Message`** — an immutable text payload (a `record`). Knows nothing about
  transport or counters.
- **`MessageChannel`** — the transport seam: `send`, blocking `receive`
  (`Optional.empty()` means "conversation over"), and `close`.
- **`Player`** — a participant: holds a name, a channel, and its sent/received
  counters; applies the reply rule; plays the initiator or responder role.
- **`InMemoryChannel`** — same-process transport: two crossed `BlockingQueue`s;
  an `Optional.empty()` "poison pill" ends the conversation.
- **`TcpMessageChannel`** — separate-process transport: newline-delimited UTF-8
  over one socket; a `null` from `readLine()` ends the conversation.
- **`SingleProcessApp`** — wires the same-process demo: one queue pair, two
  players, two threads, one PID.
- **`ResponderProcess`** — standalone JVM; the socket server; runs `respond()`.
- **`InitiatorProcess`** — standalone JVM; the socket client; runs `initiate()`.

---

## Message flow (why it lands on exactly 10/10)

```
Alice send #1 : PING                 (kickoff, not a reply)
Bob   reply #1: PING|1
Alice reply #2: PING|1|2
...
Alice send #10                       (sent quota full)
Bob   reply #10
Alice recv #10  -> sent==10 && recv==10 -> STOP, close channel
Bob   recv "end-of-stream" -> STOP
```

Each reply appends the sender's own send counter to whatever it just received
(Requirement 3), so the message visibly grows and you can read the whole chain.

---

## Java concepts used (quick tour)

- **`interface` + polymorphism** — `MessageChannel` lets one `Player` work over
  two unrelated transports.
- **`record`** — `Message` is an immutable value with auto-generated
  constructor/equals/hashCode.
- **`Optional<Message>`** — a typed "maybe a message, maybe end-of-stream",
  avoiding `null` as a sentinel in the `Player` logic.
- **Threads (`Thread`, `Runnable`, `join`)** — two independent players in one
  JVM, each blocking on its own `receive()`.
- **`BlockingQueue` / `LinkedBlockingQueue`** — thread-safe hand-off;
  `take()` blocks instead of busy-waiting; the poison-pill pattern ends it.
- **`AtomicBoolean` / `compareAndSet`** — make `close()` idempotent and
  thread-safe without locks.
- **Sockets (`ServerSocket`, `Socket`)** — the simplest cross-process channel.
- **try-with-resources + `AutoCloseable`** — sockets and channels close
  automatically and in the right order.
- **`UncheckedIOException`** — keeps the `MessageChannel` interface free of
  checked exceptions.
- **`ProcessHandle.current().pid()`** — proves "same PID" vs "different PID".

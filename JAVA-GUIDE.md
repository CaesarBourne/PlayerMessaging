# player-messaging — Java From Scratch

**A complete walkthrough of every Java concept in this project, from fundamentals to the last line of code.**

This guide assumes you know almost nothing about Java. It builds up in layers:

| Part | What it covers |
|---|---|
| **1** | What Java *is*: JVM, compiling, packages, classpath, Maven |
| **2** | The problem this project solves, and the one design idea behind it |
| **3** | Every class, line by line, with the fundamentals behind each line |
| **4** | Deep dives: OOP, generics, exceptions, threads, I/O, sockets |
| **5** | The verified message trace — why it lands on exactly 10/10 |
| **6** | Rebuild it from an empty folder, in order |
| **7** | Exercises, common mistakes, glossary |

Read Part 1 → 3 in order. Parts 4–7 are reference you can jump around in.

---
---

# PART 1 — Java Fundamentals You Need Before Line One

## 1.1 What Java actually is

Java is three separate things that people lump together:

1. **The language** — the syntax you type (`class`, `if`, `for`, `interface`).
2. **The bytecode format** — what your `.java` files get compiled *into* (`.class` files).
3. **The JVM (Java Virtual Machine)** — the program that *runs* bytecode.

The important consequence: **Java does not compile to machine code for your CPU.** It compiles to bytecode, which is machine code for an imaginary computer (the JVM). The JVM on your Mac, on Linux, on Windows, all understand the same bytecode. That's "write once, run anywhere."

```
   Hello.java          javac         Hello.class          java
  (text you type)  ───────────►   (bytecode)  ───────────► running program
                    compiler                    JVM
```

Two commands you must know:

```bash
javac Hello.java     # compile:  .java  →  .class
java  Hello          # run:      JVM loads Hello.class and calls its main method
```

Note `java Hello` — no `.class` extension. You give the JVM a **class name**, not a filename.

### The JDK vs the JRE

- **JDK** (Java Development Kit) = compiler (`javac`) + runtime + tools. You need this.
- **JRE** (Java Runtime Environment) = just the runtime (`java`). Enough to run, not to build.

This project needs **JDK 17 or newer**, because it uses `record` (Java 16+) and `ProcessHandle` (Java 9+).

Check what you have:

```bash
java -version
javac -version
```

## 1.2 Everything lives in a class

Java has no free-floating functions. Every piece of code lives inside a type. The smallest legal program:

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
```

Breaking that down word by word, because every word matters:

| Token | Meaning |
|---|---|
| `public` | **Access modifier.** Visible to all other code. Alternatives: `private` (this class only), `protected` (this class + subclasses + same package), or nothing at all (*package-private*: same package only). |
| `class Hello` | Declares a class named `Hello`. **The file must be named `Hello.java`** if the class is `public`. This is enforced by the compiler. |
| `static` | Belongs to the **class itself**, not to any instance. You can call it without creating an object. `main` must be static because when the JVM starts, no objects exist yet. |
| `void` | The **return type**. `void` = returns nothing. |
| `main` | The magic method name the JVM looks for as the entry point. |
| `String[] args` | A **parameter**: an array of strings, the command-line arguments. `String[]` means "array of String". |
| `System.out.println(...)` | `System` is a class; `out` is a static field on it (a `PrintStream`); `println` is a method on that stream. |

**The exact signature `public static void main(String[] args)` is required.** Change any part of it and the JVM won't find your entry point.

## 1.3 Packages — namespaces made of folders

A package groups related classes and prevents name collisions. Two libraries can both have a `Message` class as long as they're in different packages.

```java
package com.example.messaging;
```

This line must be the **first non-comment line** of the file. And the rule is rigid:

> **The package name must match the folder path.**

```
src/main/java/com/example/messaging/Message.java   →  package com.example.messaging;
src/main/java/com/example/messaging/channel/InMemoryChannel.java
                                                   →  package com.example.messaging.channel;
```

The convention `com.example.…` is reversed-domain-name, so `example.com` owns `com.example`. It guarantees global uniqueness.

The **fully qualified name** of a class is package + class: `com.example.messaging.Message`. That's its true identity.

### `import` — a typing shortcut, nothing more

```java
import java.util.Optional;
```

This does **not** "load" or "include" anything (unlike C's `#include`). It only says: "when I write `Optional` below, I mean `java.util.Optional`." Without the import you'd write the full name every time:

```java
java.util.Optional<Message> incoming = channel.receive();   // works, but painful
```

Two special cases used in this project:

```java
import static java.nio.charset.StandardCharsets.UTF_8;   // static import: now write UTF_8, not StandardCharsets.UTF_8
```

And: **classes in `java.lang` are imported automatically** — that's why you never import `String`, `System`, `Thread`, `Object`, or `Integer`.

## 1.4 The classpath — how the JVM finds classes

When you run `java com.example.messaging.app.SingleProcessApp`, the JVM has to find the file `com/example/messaging/app/SingleProcessApp.class`. Where does it look? The **classpath**.

```bash
java -cp target/classes com.example.messaging.app.SingleProcessApp
#       ^^^^^^^^^^^^^^  "start looking for package folders here"
```

So the JVM resolves it to `target/classes/com/example/messaging/app/SingleProcessApp.class`.

This is exactly what `run.sh` does:

```bash
CP="target/classes"
PKG="com.example.messaging.app"
java -cp "$CP" "$PKG.SingleProcessApp"
```

Classpath entries can be folders or `.jar` files, separated by `:` on Mac/Linux (`;` on Windows). This project has **zero third-party jars**, so the classpath is one folder.

## 1.5 Maven — the build tool

You *could* build this project by hand:

```bash
javac -d target/classes $(find src -name "*.java")
```

That works. Maven exists because real projects need dependency downloading, standard layouts, testing, packaging, and reproducible builds. Maven's core idea is **convention over configuration**: if you put files where Maven expects, you write almost no config.

### The standard Maven layout (which this project follows exactly)

```
player-messaging/
├── pom.xml                      ← the build descriptor
├── src/
│   └── main/
│       └── java/                ← ALL production source goes here
│           └── com/example/messaging/...
└── target/                      ← Maven's output folder (generated, never edited, never committed)
    └── classes/                 ← compiled .class files land here
```

`src/main/java` is not a suggestion — Maven looks there by default. If you put source anywhere else you must configure it.

### Reading `pom.xml` line by line

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
```

XML boilerplate declaring which schema this document follows. Copy it verbatim; it never changes.

```xml
    <modelVersion>4.0.0</modelVersion>
```

The POM format version. It has been `4.0.0` for two decades. Always this.

```xml
    <groupId>com.example</groupId>
    <artifactId>player-messaging</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
```

These four are the project's **coordinates** — its unique address in the Maven universe.

- `groupId` — who owns it (reversed domain, like a package).
- `artifactId` — the project's own name.
- `version` — `1.0.0`. A version ending in `-SNAPSHOT` means "in development, may change".
- `packaging` — what to produce. `jar` = a zip of `.class` files. (Others: `war`, `pom`.)

Together: `com.example:player-messaging:1.0.0`.

```xml
    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
```

`maven.compiler.release` = 17 tells `javac`: *compile against the Java 17 API and emit Java 17 bytecode*, even if the JDK installed is newer (mine is 21). This is what makes the build reproducible — someone with JDK 21 produces the same bytecode as someone with JDK 17. It's also why `record` compiles: records need 16+.

`sourceEncoding` = UTF-8 tells the compiler how to read your `.java` files. Without it Maven uses the platform default and warns. Always set it.

```xml
    <!-- No <dependencies> on purpose: the exercise asks for pure Java. -->
</project>
```

There is **no `<dependencies>` block**. Everything this project uses — collections, threads, sockets, `Optional` — ships inside the JDK itself. That's worth internalising: the standard library is very large, and a huge amount can be built without pulling in anything.

### Maven commands you'll actually type

| Command | Effect |
|---|---|
| `mvn compile` | `src/main/java/**.java` → `target/classes/**.class` |
| `mvn clean` | Deletes `target/` |
| `mvn package` | Compile + run tests + build the `.jar` |
| `mvn -q -DskipTests compile` | Quiet compile, skip tests — what `run.sh` uses |

## 1.6 Reading `run.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
```

- `#!/usr/bin/env bash` — the **shebang**: run this file with bash.
- `set -e` exit on any error; `-u` error on undefined variable; `-o pipefail` a pipeline fails if any stage fails. Three characters that turn silent failure into loud failure.
- `cd "$(dirname "$0")"` — `$0` is the script's own path, `dirname` strips the filename, so the script always runs from its own folder no matter where you invoke it from.

```bash
MODE="${1:-same}"       # first argument, defaulting to "same"
PORT="${PORT:-5000}"    # environment variable PORT, defaulting to 5000
```

`${VAR:-default}` is bash's "use this value, or this fallback if unset".

```bash
    java -cp "$CP" "$PKG.ResponderProcess" "$PORT" &
    RESPONDER_PID=$!
```

The trailing `&` runs the JVM **in the background** so the script continues immediately. `$!` captures the PID of that background job. The script then starts a second JVM the same way and `wait`s for both. This is the shell-level mechanism that makes Requirement 7 (two processes) happen — two separate `java` invocations, therefore two separate operating-system processes, therefore two different PIDs.

---
---
# PART 2 — The Problem and the One Idea

## 2.1 The requirements

1. Create **two players**.
2. One player **initiates** by sending a message to the other.
3. When a player **receives** a message, it replies with the received content **concatenated with its own send-counter**.
4. Stop when the initiator has **sent 10 and received 10**, then shut down gracefully.
5. Both players must be able to run in the **same process**.
6. Every class must **document its responsibility**.
7. Each player must also be able to run in its **own process** (different PID).

## 2.2 The naive solution, and why it fails

The obvious first instinct is to write two classes:

```java
class SamePlayer   { /* uses queues */ }
class SocketPlayer { /* uses sockets */ }
```

Now the conversation rules — the reply format, the counters, the stop condition — exist **twice**. Change the reply rule and you must change it in two places, and one day you'll change only one. This is duplication of *logic*, the expensive kind.

## 2.3 The one idea: put an interface where the variation is

Look at what actually differs between requirement 5 and requirement 7. It is *not* the conversation. Alice still sends PING, Bob still replies, they still count to 10. The **only** difference is how bytes get from A to B.

So: name that variation, give it an interface, and let `Player` depend on the interface.

```
                    ┌─────────────────────────┐
                    │      MessageChannel     │   ← an interface: send / receive / close
                    │        (abstract)       │
                    └─────────────────────────┘
                          ▲               ▲
             implements   │               │   implements
                          │               │
        ┌──────────────────────┐   ┌───────────────────────┐
        │   InMemoryChannel    │   │   TcpMessageChannel   │
        │  BlockingQueue ×2    │   │   one TCP Socket      │
        │  same JVM            │   │   two JVMs            │
        └──────────────────────┘   └───────────────────────┘


        ┌──────────┐   uses    ┌─────────────────┐
        │  Player  │──────────►│  MessageChannel │   ← Player knows ONLY this
        └──────────┘           └─────────────────┘
```

`Player` never mentions `BlockingQueue`. It never mentions `Socket`. It never imports either package. Grep the file — the words don't appear. That's the whole trick, and it's why there is exactly **one** `Player` class serving both requirements.

This has a name: the **Dependency Inversion Principle** (the D in SOLID):

> High-level modules should not depend on low-level modules. Both should depend on abstractions.

`Player` (high-level policy: the conversation rules) doesn't depend on `Socket` (low-level detail). Both depend on `MessageChannel`. The dependency arrow that would naturally point *down* from Player to Socket has been **inverted** — now `TcpMessageChannel` points *up* at the interface.

Two related patterns are also in play:

- **Strategy pattern** — `MessageChannel` is a family of interchangeable algorithms; you pick one at runtime.
- **Dependency Injection** — `Player` doesn't create its own channel, it's *given* one via its constructor. The wiring decision lives in the `app` classes, not in `Player`.

## 2.4 File map

```
src/main/java/com/example/messaging/
│
├── Message.java              ← the data being sent          (a value)
├── MessageChannel.java       ← the transport abstraction    (an interface)
├── Player.java               ← the conversation logic       (the only "business" class)
│
├── channel/
│   ├── InMemoryChannel.java  ← transport impl: queues, one JVM
│   └── TcpMessageChannel.java← transport impl: socket, two JVMs
│
└── app/
    ├── SingleProcessApp.java ← wiring for requirement 5
    ├── ResponderProcess.java ← wiring for requirement 7 (server side)
    └── InitiatorProcess.java ← wiring for requirement 7 (client side)
```

Notice the layering, which is deliberate:

- **Core** (top level) — knows nothing about transports. Pure logic and contracts.
- **`channel`** — knows about transports, and about the core. Not about apps.
- **`app`** — knows about everything. This is the only layer allowed to make wiring decisions.

Dependencies point **inward** only. `channel` imports from core; core imports nothing from `channel`. If you ever find core importing from `app`, your layering has broken.

---
---

# PART 3 — Every Class, Line by Line

---

## 3.1 `Message.java` — records and immutability

```java
package com.example.messaging;

import java.util.Objects;

public record Message(String content) {

    public Message {
        Objects.requireNonNull(content, "content must not be null");
    }
}
```

Ten lines. There is more Java in them than you'd think.

### What a `record` is

`record` (Java 16+) declares a class whose *whole purpose* is to hold data. That single line:

```java
public record Message(String content) { }
```

makes the compiler generate all of this for you:

```java
public final class Message {
    private final String content;

    public Message(String content) {           // canonical constructor
        this.content = content;
    }

    public String content() { return content; }   // accessor (note: NOT getContent())

    @Override public boolean equals(Object o) { /* compares content */ }
    @Override public int hashCode()           { /* derived from content */ }
    @Override public String toString()        { return "Message[content=" + content + "]"; }
}
```

Every part of that is meaningful:

- **`final class`** — records cannot be subclassed. No one can extend `Message` and break its guarantees.
- **`private final` fields** — `final` means "assign once, never reassign". Set in the constructor, fixed forever.
- **Accessor named `content()`, not `getContent()`** — records deliberately use the plain field name. That's why `Player` writes `incoming.get().content()`.
- **`equals` compares values** — two separate `Message` objects with the same text are `.equals()` to each other. This is **value semantics**, as opposed to **identity semantics** (the default `Object.equals`, which is `==`, "is it literally the same object in memory").

### The components in the header

`(String content)` is the **record component list**. Each component becomes a field, a constructor parameter, and an accessor method. A record can have several: `record Point(int x, int y) {}`.

### The compact constructor

```java
public Message {
    Objects.requireNonNull(content, "content must not be null");
}
```

Look closely: **no parameter list, no parentheses.** That's the *compact canonical constructor*, a record-only piece of syntax. It runs before the auto-generated field assignment, so you use it for validation and normalisation. You don't write `this.content = content;` — the compiler appends that for you.

The full form would be:

```java
public Message(String content) {                // canonical constructor, written out
    Objects.requireNonNull(content, "...");
    this.content = content;                     // now YOU must remember this line
}
```

The compact form exists so you can't forget the assignment.

### `Objects.requireNonNull`

```java
Objects.requireNonNull(content, "content must not be null");
```

Returns `content` if it's non-null; throws `NullPointerException` with your message if it is null. Why bother, when using a null would throw a NPE anyway?

**Fail fast.** Without it, a null slips into the object and blows up much later — somewhere in the socket writer, with a stack trace pointing at code that isn't the culprit. With it, the exception is thrown at the exact moment the bad value was introduced, with a message naming the field. Debugging time drops from an hour to a second.

This is a general principle: **validate at the boundary, so invalid objects can never exist.**

### Why immutability matters *here specifically*

In `SingleProcessApp`, two threads run at once. The same `Message` object created by Alice's thread is read by Bob's thread. If `Message` had a setter, you'd have a data race: Bob could read `content` half-way through Alice changing it, and you'd need locks.

Because `content` is `final` and set in the constructor, **the object can never be observed in an inconsistent state**. Immutable objects are automatically thread-safe. No locks, no `synchronized`, nothing to get wrong.

This is one of the highest-leverage habits in Java: *make it immutable unless you have a specific reason not to.*

### Note what `Message` does NOT know

It doesn't know who sent it, when, over what, or which number it is. It's just text. Every class here has one job — that's **Single Responsibility**, and it's why `Message` fits in ten lines and needs no tests.

---

## 3.2 `MessageChannel.java` — interfaces, contracts, Optional

```java
package com.example.messaging;

import java.util.Optional;

public interface MessageChannel extends AutoCloseable {

    void send(Message message);

    Optional<Message> receive();

    @Override
    void close();
}
```

### What an interface is

An interface is a **contract**: a list of method signatures with no bodies. It says *what* can be done, never *how*.

```java
void send(Message message);   // signature only — note the semicolon, no { }
```

Any class that says `implements MessageChannel` **must** provide a real body for every method, or the compiler rejects it. This is checked at compile time, which is the point: you cannot ship a half-implemented channel.

### Why an interface here and not an abstract class

| | `interface` | `abstract class` |
|---|---|---|
| Can hold state (fields)? | No (only constants) | Yes |
| Can a class have several? | Yes, unlimited | No — single inheritance only |
| Says | "can do X" | "is a kind of X" |

`InMemoryChannel` and `TcpMessageChannel` share **zero** implementation. One uses queues, the other sockets. There is no common code to inherit, only a common *capability*. That's precisely what an interface is for. Reaching for an abstract class here would burn the single inheritance slot for nothing.

### `extends AutoCloseable`

`AutoCloseable` is a JDK interface with one method, `close()`. Implementing it unlocks the **try-with-resources** syntax:

```java
try (MessageChannel channel = new TcpMessageChannel(socket)) {
    // ... use it ...
}   // close() is called automatically here — even if an exception was thrown
```

Interfaces can `extend` other interfaces (and unlike classes, an interface may extend several).

### The narrowing override — a subtle and clever line

```java
@Override
void close();
```

`AutoCloseable` declares `void close() throws Exception;`. Redeclaring it here **without** `throws Exception` **narrows** the contract.

Why it matters: if `close()` could throw a checked `Exception`, then every single call site in `Player` would need a try/catch:

```java
try { channel.close(); } catch (Exception e) { /* now what? */ }   // noise everywhere
```

By narrowing, `Player.initiate()` gets to say simply `channel.close();`. The rule Java allows: **an overriding method may throw fewer or narrower checked exceptions than the method it overrides, never more.** That's safe, because any code written against the parent contract already handled the wider case.

`@Override` is an annotation — metadata for the compiler. It says "I intend to override an inherited method." If you typo the name (`cloze()`), the compiler errors instead of silently creating a new unrelated method. Always write it.

### `Optional<Message>` — the return type of `receive()`

`receive()` must express two different outcomes: *here's a message*, or *there will never be another message*.

The old way is to return `null` for "nothing" — and `null` is the single most expensive mistake in the language's history (its inventor, Tony Hoare, calls it his "billion-dollar mistake"). The problem: the type `Message` gives no hint that null is possible, so you forget the check, and you get a `NullPointerException` at 3am.

`Optional<T>` is a container holding either one value or nothing, and the type itself tells you so:

```java
Optional<Message> incoming = channel.receive();
if (incoming.isEmpty()) { /* conversation over */ }
Message m = incoming.get();
```

You **cannot** accidentally use the message without acknowledging the empty case, because `Optional<Message>` isn't a `Message` — the compiler forces you through `.get()` / `.isPresent()` / `.orElse(...)`.

The API you'll see in this project:

| Method | Meaning |
|---|---|
| `Optional.of(x)` | Wrap a value (throws if `x` is null) |
| `Optional.empty()` | The empty one |
| `isPresent()` | Is there a value? |
| `isEmpty()` | Is it empty? (Java 11+) |
| `get()` | Unwrap (throws if empty — only call after checking) |

`Optional<Message>` also shows **generics**: `Optional` is a *generic type*, `<Message>` is the *type argument*. `Optional<Message>` and `Optional<String>` are different types, checked at compile time, so you can't put a `String` where a `Message` belongs. Before generics (Java 5) collections held bare `Object` and you cast by hand, discovering mistakes at runtime.

### The documented contract

The three methods have promises attached in the Javadoc that aren't visible in the signatures — and *both* implementations must honour them:

1. **`send`** — best-effort. Sending on a closed channel is silently ignored, not an error.
2. **`receive`** — **blocks** until something arrives. Returns `empty()` when, and only when, the conversation is over.
3. **`close`** — **idempotent**: calling it twice must be safe. Signals the peer.

Point 2 is the load-bearing one. The two implementations achieve "conversation over" by wildly different means — a poison-pill object in a queue vs. TCP end-of-stream — but `Player` sees the same `Optional.empty()` either way. **The interface unifies two unrelated mechanisms into one vocabulary.** That is what a good abstraction does.

Point 3 matters because `close()` really does get called twice in the same-process demo (once by the initiator's loop, and the responder closes its own side too).

---
## 3.3 `Player.java` — state, encapsulation, loops, roles

This is the only class holding actual business rules. Everything else is plumbing.

### The class declaration and fields

```java
public final class Player {

    private static final String SEPARATOR = "|";

    private final String name;
    private final MessageChannel channel;

    private int sent = 0;
    private int received = 0;
```

**`final class`** — cannot be subclassed. Why forbid it? Because subclassing is the tightest coupling in object-oriented programming: a subclass can override `buildReply` and silently break the counter invariant. Josh Bloch's rule from *Effective Java*: *design and document for inheritance, or else prohibit it.* Nothing here is designed for inheritance, so it's prohibited. `final` is the safe default.

**`private static final String SEPARATOR = "|"`** — three modifiers, three separate decisions:

- `private` — nobody outside needs it.
- `static` — one copy shared by the class, not one per Player. A constant doesn't vary per instance.
- `final` — can't be reassigned.

`static final` in `SCREAMING_SNAKE_CASE` is the Java convention for a constant. The value exists as a named thing rather than a magic `"|"` sprinkled through the code, so changing the format is a one-line edit.

**`private final String name;`** — `final` *without* an initialiser is a **blank final**. The compiler then demands that every constructor assigns it exactly once. After construction, `name` can never change. Same for `channel`: a Player is bound to one channel for life.

**`private int sent = 0;`** — deliberately **not** final, because these change. This is the player's mutable state.

**`private` on all fields = encapsulation.** Outside code cannot write `player.sent = 99`. The only way `sent` changes is through this class's own methods, so the invariant "`sent` equals the number of messages actually sent" is guaranteed by construction. Encapsulation isn't about secrecy — it's about being able to *reason* about state because you know every line that touches it.

**`int`** is a **primitive type** — a raw 32-bit signed integer, not an object. Java's eight primitives (`byte short int long float double char boolean`) are stored by value and are fast. Everything else is a **reference type** (an object, accessed via a reference). `int` defaults to `0`; a reference defaults to `null`.

### The constructor

```java
    public Player(String name, MessageChannel channel) {
        this.name = name;
        this.channel = channel;
    }
```

A constructor has **no return type** and is **named exactly like the class**. It runs when you write `new Player(...)`.

`this` is the reference to the object being constructed. It's needed here because the parameter `name` **shadows** the field `name` — inside the constructor, the bare word `name` means the parameter. `this.name` disambiguates: "the field on this object."

The critical design point: the channel is **passed in**, not created here. `Player` never writes `new InMemoryChannel(...)` or `new Socket(...)`. That's **constructor injection**, and it's what makes `Player` reusable across both scenarios — and trivially testable, since a test can pass a fake channel.

### `initiate(...)` — the initiator role

```java
    public void initiate(String seed, int target) {
        sent++;
        Message first = new Message(seed);
        logSend(first);
        channel.send(first);
```

The kickoff. `sent++` is the **post-increment operator**, shorthand for `sent = sent + 1`. It runs *before* the log so the log prints `#1`, matching the message actually being sent.

The kickoff is special: it's the one message that isn't a reply to anything, so it doesn't go through `buildReply`. But it still counts toward the quota, hence the manual `sent++`.

```java
        while (sent < target || received < target) {
```

A `while` loop: test the condition, run the body, repeat. `||` is **logical OR**, and it's **short-circuiting** — if `sent < target` is true, Java never evaluates the right side.

Read the condition aloud: *"keep going while EITHER quota is still unfilled."* This is the correct reading of requirement 4 ("sent 10 **and** received 10" must both hold to stop). By De Morgan's law, `NOT(sent>=t AND received>=t)` is `sent<t OR received<t`. Getting this wrong — using `&&` — would stop as soon as *one* quota filled, which is a real and easy bug.

```java
            Optional<Message> incoming = channel.receive();
            if (incoming.isEmpty()) {
                break;
            }
```

`receive()` **blocks** — this thread stops and consumes no CPU until a message arrives. `break` exits the loop immediately. This branch is the defensive path: the peer died or closed early. Without it, `incoming.get()` on the next line would throw.

```java
            received++;
            logRecv(incoming.get());
```

`incoming.get()` unwraps the `Optional` — safe here, because the emptiness was just checked.

```java
            if (sent >= target && received >= target) {
                break;
            }
```

**The stop condition.** Placed *after* counting the receive but *before* replying — that ordering is what makes the conversation terminate on exactly 10/10 instead of 11/10. If this check were at the top of the loop instead, Alice would fire one extra reply before noticing she was done.

`&&` is logical AND, also short-circuiting.

```java
            Message reply = buildReply(incoming.get().content());
            logSend(reply);
            channel.send(reply);
        }

        channel.close();
        summary();
    }
```

`incoming.get().content()` is **method chaining**: `get()` returns a `Message`, `.content()` reads its text.

`channel.close()` after the loop is the graceful-shutdown mechanism. Alice can't reach into Bob's JVM and tell him to stop — she closes her end, and Bob's blocked `receive()` returns `empty()`. **The close is the goodbye.**

### `respond()` — the responder role

```java
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
```

The loop header packs three things into one line, and it's worth unpacking because this idiom appears constantly in Java I/O code:

```java
while ((incoming = channel.receive()).isPresent())
        └───────── assignment ─────────┘
       └────── the assigned value ──────┘.isPresent()
```

In Java an **assignment is an expression** — it evaluates to the value assigned. So `(incoming = channel.receive())` both stores the result *and* yields it, and `.isPresent()` is then called on it. The inner parentheses are required; without them Java would try `channel.receive().isPresent()` first and assign a boolean.

Written out longhand it's:

```java
while (true) {
    incoming = channel.receive();
    if (!incoming.isPresent()) break;
    ...
}
```

Note `Optional<Message> incoming;` is declared **outside** the loop, without an initialiser, because the loop condition needs it in scope. Java would reject reading it before assignment ("variable might not have been initialized") — the compiler performs **definite assignment analysis** and here it can prove the assignment happens first.

Bob has **no counter logic and no stop condition at all**. He is purely reactive: reply to everything, stop when the channel says stop. All the termination policy lives with the initiator, which is exactly right — one owner per decision.

### Why two methods instead of one method with a flag

You could have written:

```java
public void play(boolean isInitiator) { if (isInitiator) ... else ... }
```

That's worse. The method you call *is* the role; a boolean parameter at the call site (`player.play(true)`) tells the reader nothing. And the two roles genuinely have different needs — `initiate` takes `seed` and `target`, `respond` takes nothing. Forcing them into one signature means unused parameters.

### `buildReply` — requirement 3, in one place

```java
    private Message buildReply(String receivedContent) {
        sent++;
        return new Message(receivedContent + SEPARATOR + sent);
    }
```

The **only** duplicated behaviour between the two roles, factored into one private method. Change the reply format here and both roles change together — there is no way for them to drift apart.

The order matters: `sent++` runs **first**, so the number appended is the index of *this* outgoing message. If you incremented after, every message would carry the previous number.

`receivedContent + SEPARATOR + sent` is **string concatenation**. `+` on a `String` builds a new string; the `int sent` is auto-converted to text. (Behind the scenes, modern Java compiles this into an efficient `StringBuilder`-like operation — you don't pay for naive repeated copying on a single expression.)

`private` — it's an internal helper. Not part of Player's public surface, so it can be renamed or restructured freely.

### The logging methods

```java
    private void logSend(Message m) {
        System.out.printf("[%-5s] --> sent #%-2d : %s%n", name, sent, m.content());
    }
```

`printf` = "print formatted". The format string is a template with **conversion specifiers**:

| Specifier | Meaning |
|---|---|
| `%s` | insert a string |
| `%d` | insert a decimal integer |
| `%-5s` | string, left-aligned (`-`), padded to width 5 |
| `%-2d` | integer, left-aligned, width 2 |
| `%n` | platform-correct newline (better than `\n`) |

The arguments after the format string fill the slots in order. The padding is why the output columns line up: `[Alice]` and `[Bob  ]` are both 7 characters, so the arrows align and the log is readable at a glance. A small thing that makes debugging much easier.

```java
    private void summary() {
        System.out.printf(">> %s finished. Totals: sent=%d, received=%d%n", name, sent, received);
    }
```

Both players print this, and both print `sent=10, received=10` — verified proof that requirement 4 held.

---

## 3.4 `InMemoryChannel.java` — threads, queues, atomics

The same-process transport. This is where concurrency lives.

### The nested record

```java
public final class InMemoryChannel implements MessageChannel {

    public record Pair(MessageChannel initiatorSide, MessageChannel responderSide) {}
```

`implements MessageChannel` — the promise to provide `send`, `receive`, `close`. The compiler enforces it.

`Pair` is a **nested record**: a record declared inside a class. It exists because `pair()` needs to return *two* things, and Java methods return only one. The alternatives are all worse: a `Message[]` array (no names, no types), or a `Map` (no types), or two out-parameters (Java has none).

Naming the two ends `initiatorSide` and `responderSide` means the call site reads as documentation:

```java
Player alice = new Player("Alice", channels.initiatorSide());
```

You'd have to work at it to wire them wrong. Compare `channels[0]`.

Note the declared type is `MessageChannel`, not `InMemoryChannel` — **program to the interface**, even internally.

Nested types are referred to as `InMemoryChannel.Pair` from outside. Because it's declared inside a class body it is implicitly `static` (records always are), so it needs no enclosing instance.

### Fields

```java
    private static final Optional<Message> END = Optional.empty();

    private final BlockingQueue<Optional<Message>> inbound;
    private final BlockingQueue<Optional<Message>> outbound;
    private final AtomicBoolean closed = new AtomicBoolean(false);
```

`END` is a named constant for `Optional.empty()` — the **poison pill**. Naming it makes every use site self-explaining: `outbound.add(END)` reads as intent, not mechanism.

`BlockingQueue<Optional<Message>>` — a generic type with a generic type argument. Read it inside-out: a queue *of* optionals *of* messages. That nesting is deliberate: the queue's element type is exactly `receive()`'s return type, so `receive()` is a one-line pass-through with no translation.

Both queues are `final` — wired once at construction, never re-pointed.

### The private constructor + static factory

```java
    private InMemoryChannel(BlockingQueue<Optional<Message>> inbound,
                            BlockingQueue<Optional<Message>> outbound) {
        this.inbound = inbound;
        this.outbound = outbound;
    }

    public static Pair pair() {
        BlockingQueue<Optional<Message>> a2b = new LinkedBlockingQueue<>();
        BlockingQueue<Optional<Message>> b2a = new LinkedBlockingQueue<>();
        MessageChannel initiator = new InMemoryChannel(/* inbound */ b2a, /* outbound */ a2b);
        MessageChannel responder = new InMemoryChannel(/* inbound */ a2b, /* outbound */ b2a);
        return new Pair(initiator, responder);
    }
```

The constructor is **`private`** — outside code physically cannot call `new InMemoryChannel(...)`. The only way to get one is `InMemoryChannel.pair()`.

This is the **static factory method** pattern, and here it isn't decoration — it's a correctness guarantee. A single `InMemoryChannel` is meaningless; only a *correctly crossed pair* works. If the constructor were public, someone could hand the same queue in as both inbound and outbound and build a channel that talks to itself. Making the constructor private makes that mis-wiring **impossible to express**.

The crossing itself:

```
   a2b  (queue A→B)                     b2a  (queue B→A)
        ┌────────────┐                       ┌────────────┐
Alice   │  outbound  │──► Bob's inbound      │  inbound   │◄── Bob's outbound
        └────────────┘                       └────────────┘
```

Alice writes to `a2b`, Bob reads from `a2b`. Bob writes to `b2a`, Alice reads from `b2a`. Two one-way pipes make one two-way channel. **Alice's `outbound` is Bob's `inbound`** — that single sentence is the whole mechanism.

`new LinkedBlockingQueue<>()` — the `<>` is the **diamond operator**. The compiler infers the type argument from the variable's declared type, so you don't repeat `<Optional<Message>>`.

`/* inbound */` inline comments label the arguments at the call site. Java has no named parameters, so this is the idiomatic way to keep two same-typed arguments from getting swapped.

**Why `static`?** A static method belongs to the class, so you call it without an instance: `InMemoryChannel.pair()`. That's necessary — it's the thing that *creates* instances.

### `send`

```java
    @Override
    public void send(Message message) {
        if (closed.get()) {
            return;
        }
        outbound.add(Optional.of(message));
    }
```

`@Override` — again, compiler-checked: if the interface method were renamed, this wouldn't compile. Catching the mistake at build time instead of runtime.

Early `return` on closed: honours the "best-effort send" contract.

`outbound.add(...)` puts a message in the peer's inbound queue. `LinkedBlockingQueue` is **unbounded by default**, so `add` never blocks or rejects. (If you'd constructed it with a capacity, `add` would throw when full and you'd use `put` to block instead.)

### `receive`

```java
    @Override
    public Optional<Message> receive() {
        try {
            return inbound.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return END;
        }
    }
```

**`take()` is the heart of this class.** It removes the head of the queue, and if the queue is empty it **blocks** — the thread is parked by the OS, using zero CPU, until another thread adds something. Then it wakes.

Contrast with the naive alternative:

```java
while (queue.isEmpty()) { /* spin */ }     // burns a full CPU core doing nothing
```

That's a **busy-wait**, and it's the classic beginner mistake. `take()` gets you blocking hand-off for free, correctly, with no locks written by you.

`BlockingQueue` also guarantees **thread safety**: many threads can `add` and `take` concurrently with no corruption. It's from `java.util.concurrent`, the package written by concurrency experts precisely so ordinary code doesn't hand-roll locks.

#### The `InterruptedException` block — the most-mishandled 3 lines in Java

`take()` declares `throws InterruptedException`, a **checked exception**, so the compiler forces you to deal with it. It's thrown when another thread calls `yourThread.interrupt()` — a cooperative "please stop what you're doing" request.

When Java throws `InterruptedException`, it **clears the thread's interrupt flag**. So if you swallow the exception, the information that someone asked this thread to stop is destroyed, and code further up the stack never finds out.

```java
Thread.currentThread().interrupt();   // put the flag back
```

That line restores it. This is the correct, standard idiom, and the overwhelming majority of production Java gets it wrong with:

```java
catch (InterruptedException e) { }              // ← never do this: silently loses the signal
catch (InterruptedException e) { e.printStackTrace(); }   // ← also wrong
```

Returning `END` afterwards is the right local behaviour too: "I was interrupted, so as far as I'm concerned the conversation is over" — translated into the vocabulary the interface already has.

### `close` and compare-and-set

```java
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            outbound.add(END);
        }
    }
```

`AtomicBoolean` is a boolean wrapped so that its operations are **atomic** — indivisible, never half-done, even with several threads running.

`compareAndSet(false, true)` does this **as one uninterruptible hardware operation**: *if the current value is `false`, set it to `true` and return `true`; otherwise change nothing and return `false`.*

Why not just:

```java
if (!closed) { closed = true; outbound.add(END); }     // BROKEN under concurrency
```

Because between reading `closed` and writing it, another thread can run. Both threads read `false`, both set `true`, both add a poison pill. Two pills means the peer might stop early or leave junk in the queue. This is a **check-then-act race condition**, and it's one of the most common concurrency bugs there is.

`compareAndSet` (CAS) closes the window: exactly one caller ever sees `true` returned, so exactly one pill is added. `close()` becomes **idempotent** — the contract the interface demanded — and it does it **lock-free**, using a single CPU instruction rather than a mutex.

The poison pill itself: `close()` adds `END` to the queue the peer is reading. The peer's blocked `take()` wakes, gets `Optional.empty()`, and its loop ends. That's how a shutdown signal travels through a queue — you can't "interrupt" a queue, so you send a value that *means* stop.

---
## 3.5 `TcpMessageChannel.java` — sockets, streams, I/O

The separate-process transport. Same three methods, completely different machinery.

### Networking in one paragraph

Two programs on a network talk through **sockets**. One side is the **server**: it binds a **port** (a number, 0–65535, identifying which program on the machine) and waits. The other is the **client**: it connects to a host + port. Once connected, both sides hold a `Socket`, and a socket gives you two **streams** — one to read bytes from, one to write bytes to. TCP guarantees the bytes arrive in order and without corruption; it does *not* give you messages. Bytes are a continuous stream with no boundaries — which is why you need a protocol.

### Fields and the constructor

```java
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
```

Those two constructor lines are three wrappers deep. This is the **Decorator pattern**, and it's how all of Java I/O is designed: small single-purpose layers you stack.

```
socket.getInputStream()   →  InputStream       raw bytes off the wire
   wrapped in InputStreamReader                bytes → characters  (needs a charset)
   wrapped in BufferedReader                   characters → lines  (+ buffering)
```

Layer by layer:

- **`InputStream` / `OutputStream`** — the byte layer. Everything binary.
- **`InputStreamReader` / `OutputStreamWriter`** — the **bridge** from bytes to characters. This is where the **charset** is applied. Text is not bytes; a charset is the rule that maps between them.
- **`BufferedReader` / `BufferedWriter`** — add an in-memory buffer, and add `readLine()`.

**Why the buffer matters:** without it, every single character read is a separate system call to the OS — brutally slow. The buffered layer fetches ~8KB at a time and serves reads from memory. Usually a 10–100× difference.

**Why the charset matters:** `UTF_8` is passed explicitly. If you omit it, Java uses the platform default, which can differ between the two machines — and then non-ASCII characters silently corrupt. **Always name the charset.** It comes from the static import:

```java
import static java.nio.charset.StandardCharsets.UTF_8;
```

`StandardCharsets.UTF_8` is a compile-time constant, unlike `Charset.forName("UTF-8")` which can throw at runtime. Prefer the constant.

**`throws IOException`** on the constructor: `getInputStream()` can fail, and `IOException` is *checked*, so it must be either caught or declared. This one is declared, pushing the decision to the caller — which is right, because only the caller knows whether a dead socket is fatal or retryable.

**`volatile boolean closed`** — `volatile` is a memory-visibility instruction to the JVM. Without it, each thread may cache the field in a CPU register and never see another thread's write. `volatile` forces reads and writes to go to main memory, so a change made by one thread is immediately visible to all.

`volatile` gives you **visibility**, not **atomicity**. `closed = true` is a single write, so visibility is all that's needed here. For read-modify-write (like `counter++`) `volatile` is *not* enough — that's why `InMemoryChannel` uses `AtomicBoolean` for its check-then-act, while a plain flag suffices here (the check-then-act is guarded by `synchronized` instead — see below).

### `send`

```java
    @Override
    public synchronized void send(Message message) {
        if (closed) {
            return;
        }
        try {
            out.write(message.content().replace("\n", " ").replace("\r", " "));
            out.write('\n');
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to send over socket", e);
        }
    }
```

**`synchronized`** — acquires this object's intrinsic lock (its *monitor*) for the duration of the method. Only one thread can be inside any `synchronized` method of this object at a time. Here it prevents two threads interleaving their writes and producing a corrupted half-line on the wire. It also makes the `closed` check and the write one atomic unit with respect to `close()`, which is also `synchronized`.

**The newline stripping** — this is the protocol guard:

```java
message.content().replace("\n", " ").replace("\r", " ")
```

The protocol is *one message per line*. If a message body contained a newline, the receiver would read it as **two** messages, and the whole conversation would desynchronise. Stripping newlines makes that impossible.

This is a genuinely important lesson about protocols: **if your framing uses a delimiter, that delimiter must never appear in the payload.** The two real solutions are *escaping* (encode the newline as `\n` literal text and decode on the other side) or *length-prefixing* (write the byte count, then the bytes — no delimiter needed at all). Here the payload is known to be simple, so stripping is honest and adequate.

`.replace(a, b)` returns a **new** String — `String` is immutable in Java, so the chained calls each produce a new value.

**`out.flush()`** — the buffer that makes reads fast also makes writes *lazy*: `write()` only fills the buffer. Without `flush()`, the bytes sit in memory and the peer waits forever for a message that was "sent". **Deadlock by forgotten flush** is a rite of passage in socket programming. Flushing after each message is exactly right for request/response.

**`UncheckedIOException`** — this is the interface-compatibility trick. `out.write` throws `IOException` (checked), but `MessageChannel.send` declares no exceptions. So the checked exception is caught and rethrown wrapped in `UncheckedIOException`, which extends `RuntimeException` and needs no declaration.

The original exception is passed as the second argument — the **cause**. This preserves the full stack trace; you'll see `Caused by: java.net.SocketException: Broken pipe` underneath. Losing the cause when wrapping is a classic way to make a bug undebuggable.

The design judgement here: a socket write failure is not something `Player` can meaningfully recover from, so it should propagate and crash loudly rather than force every call site into a try/catch that would do nothing useful.

### `receive`

```java
    @Override
    public Optional<Message> receive() {
        try {
            String line = in.readLine();
            return (line == null) ? Optional.empty() : Optional.of(new Message(line));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
```

`readLine()` blocks until it has a complete line, and it strips the trailing newline for you. Its return contract: **`null` means end-of-stream** — the peer closed the connection and no more data will ever arrive.

So the translation is one line:

```
TCP end-of-stream (null)   ──►   Optional.empty()
```

Compare with `InMemoryChannel`:

```
poison pill in the queue    ──►   Optional.empty()
```

**Two completely unrelated mechanisms, one shared vocabulary.** `Player` sees only `Optional.empty()` and has no idea which happened. This is the abstraction paying for itself.

The `? :` is the **ternary conditional operator** — `condition ? valueIfTrue : valueIfFalse`. It's an expression, so it can sit directly in the `return`.

The `catch` treats an I/O failure as end-of-conversation too. Defensible: if the connection broke mid-flight there is nothing more to receive, so the observable outcome is the same. (In a system that needed to distinguish clean shutdown from a crash, you would *not* collapse them like this — you'd want the error surfaced.)

### `close`

```java
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
```

Idempotence achieved with `synchronized` + a flag, rather than `AtomicBoolean`. Both are correct; the lock is already needed here to serialise with `send`, so reusing it is simpler than adding an atomic.

`socket.close()` is the goodbye signal: closing the socket sends TCP's FIN packet, which makes the peer's blocked `readLine()` return `null`. **Closing the socket is how one process tells another process to stop.**

The empty catch is one of the very few places an ignored exception is defensible — the socket is being abandoned anyway, and there is no recovery action. Naming the variable `ignored` documents the intent so a reviewer knows it's deliberate rather than sloppy.

---

## 3.6 `SingleProcessApp.java` — threads, lambdas, join

Requirement 5's wiring.

```java
public final class SingleProcessApp {

    private static final int TARGET = 10;
    private static final String SEED = "PING";

    public static void main(String[] args) throws InterruptedException {
        long pid = ProcessHandle.current().pid();
        System.out.println("=== SAME-PROCESS demo (Requirement 5) ===");
        System.out.println("Both players run inside this single JVM, PID " + pid + "\n");
```

`ProcessHandle.current().pid()` (Java 9+) returns the operating-system process ID. Printing it is the *evidence* for the requirement — same PID printed by both players proves one process; two different PIDs in the other demo prove two.

`throws InterruptedException` on `main` — `join()` below can throw it, and for a demo, crashing is a perfectly reasonable response to being interrupted. Declaring it avoids a pointless try/catch.

```java
        InMemoryChannel.Pair channels = InMemoryChannel.pair();
        Player alice = new Player("Alice", channels.initiatorSide());
        Player bob   = new Player("Bob",   channels.responderSide());
```

The wiring. Note this is the **only** place in the whole program that knows both `Player` and a concrete channel type exist together. This class is the **composition root** — the one layer allowed to make wiring decisions. Everything below it stays decoupled.

```java
        Thread bobThread   = new Thread(bob::respond, "Bob-thread");
        Thread aliceThread = new Thread(() -> alice.initiate(SEED, TARGET), "Alice-thread");
```

**Why threads at all?** Because both players block. Alice calls `receive()` and waits for Bob; Bob calls `receive()` and waits for Alice. On a single thread, the first `receive()` would block forever — nobody left to send. Two threads let them wait independently, which is exactly what two processes would do.

`new Thread(Runnable, String name)` — the thread needs something to run, plus a name (which shows up in stack traces and profilers; always name your threads).

**`Runnable`** is a `@FunctionalInterface` — an interface with exactly one abstract method, `void run()`. Any single-method interface can be satisfied by a lambda or a method reference instead of a class.

Two syntaxes are used here, deliberately:

```java
bob::respond                              // METHOD REFERENCE
```

Read as "the method `respond` on the object `bob`". Shorthand for `() -> bob.respond()`. Usable because `respond()` takes no arguments and returns nothing — matching `Runnable.run()` exactly.

```java
() -> alice.initiate(SEED, TARGET)        // LAMBDA
```

An anonymous function: `()` = no parameters, `->` separates params from body, then the body. A method reference doesn't work here because arguments must be supplied, so a lambda wraps the call.

Before Java 8 you'd have written:

```java
new Thread(new Runnable() {
    @Override public void run() { alice.initiate(SEED, TARGET); }
}, "Alice-thread");
```

Same thing, five lines instead of one.

(Note: a lambda can only capture variables that are **final or effectively final** — never reassigned. `alice` qualifies. This restriction exists because the captured value is copied into the lambda; allowing reassignment would make it ambiguous which value the lambda sees.)

```java
        bobThread.start();
        aliceThread.start();
```

**`start()`, not `run()`.** This distinction trips up everyone once:

- `start()` asks the JVM for a **new thread**, which then executes `run()`. Returns immediately.
- `run()` is just a normal method call **on the current thread**. No concurrency at all.

Call `run()` here and the program deadlocks: Bob's loop blocks on `receive()` on the main thread, and Alice never gets to start.

Bob starts first so the responder is listening before the kickoff arrives. With queues it wouldn't actually matter (the message would just sit in the queue), but it mirrors the socket case where ordering *does* matter, and it costs nothing to be consistent.

```java
        aliceThread.join();
        bobThread.join();

        System.out.println("\n=== Done. Both players ran in the same PID " + pid + " ===");
    }

    private SingleProcessApp() { }
}
```

**`join()`** means "block the current thread until that thread finishes." Without it, `main` would reach the last `println` while the conversation was still running, and you'd get the "Done" line in the middle of the log. `join()` is what makes the shutdown *ordered* — requirement 4's "gracefully".

**The private constructor** on a utility class: with no constructor at all, Java generates a public no-arg one, and `new SingleProcessApp()` would compile — meaningless for a class that's only an entry point. A private constructor makes instantiation impossible and states the intent.

---

## 3.7 `ResponderProcess.java` — the server side

```java
public final class ResponderProcess {

    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) throws IOException {
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        long pid = ProcessHandle.current().pid();

        System.out.println("[Responder] JVM started, PID " + pid + ", listening on port " + port);
```

`args` holds the command-line arguments. Run `java ... ResponderProcess 6000` and `args[0]` is the **String** `"6000"` — command-line args are always strings, so `Integer.parseInt` converts. (It throws `NumberFormatException` on garbage; for a demo, crashing is fine.)

`args.length > 0` guards against `ArrayIndexOutOfBoundsException` when no argument is given. Java arrays don't have a `.length()` method — `length` is a field, no parentheses. A small inconsistency you just memorise.

```java
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
```

**`new ServerSocket(port)`** binds the port and starts listening. If another program already holds it you get `BindException: Address already in use` — the most common error when running this twice.

**`server.accept()`** **blocks** until a client connects, then returns a **new** `Socket` representing that specific connection. The `ServerSocket` keeps listening; the returned `Socket` is the conversation. (A real server would loop on `accept()` and hand each socket to a thread. This demo needs exactly one.)

**try-with-resources** — declare resources in the parentheses, and their `close()` is called automatically when the block exits, whether normally or via exception. It replaces the old error-prone pattern:

```java
Socket socket = null;
try { socket = ...; }
finally { if (socket != null) try { socket.close(); } catch (IOException e) {} }   // pre-Java-7 misery
```

Two rules worth knowing:

1. Multiple resources are separated by `;` and **closed in reverse order** of declaration. Here `channel` closes before `socket` — correct, since the channel wraps the socket.
2. The declared type must implement `AutoCloseable`. This is why `MessageChannel extends AutoCloseable`.

The nesting (`ServerSocket` outside, `Socket` inside) reflects lifetime: the listening socket outlives the individual connection.

---

## 3.8 `InitiatorProcess.java` — the client side

```java
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
```

Structurally identical to the responder, and the payoff line is:

```java
Player alice = new Player("Alice", channel);
alice.initiate(SEED, TARGET);
```

Compare it to `SingleProcessApp`:

```java
Player alice = new Player("Alice", channels.initiatorSide());
alice.initiate(SEED, TARGET);
```

**The same class, the same method, the same arguments.** Only the channel differs. Requirements 5 and 7 are the same program with a different plug. That is the entire design justified in two lines.

`throws IOException, InterruptedException` — a method may declare several checked exceptions, comma-separated.

### The retry loop

```java
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
                Thread.sleep(delayMillis);
            }
        }
        throw new IOException("Unreachable");
    }
```

**The problem it solves:** `run.sh` launches both JVMs back to back. The initiator may well try to connect before the responder has finished binding its port — a **start-up race**. Without a retry you'd get an intermittent `ConnectException`, the worst kind of bug: works on your machine, fails in CI.

The `for` loop has three parts: `int i = 1` (init, once), `i <= attempts` (test, before each pass), `i++` (update, after each pass).

`socket.connect(address, 1000)` — the two-step form (`new Socket()` then `connect`) lets you pass a **timeout** in milliseconds, which `new Socket(host, port)` doesn't. Without a timeout a connect to an unreachable host can hang for the OS default, potentially minutes.

`catch (ConnectException refused)` catches **only** connection-refused — the specific, expected, retryable failure. A DNS failure or a permissions error is a different problem and correctly propagates immediately. **Catch the narrowest exception that expresses what you can actually handle**; `catch (Exception e)` here would silently retry 20 times on a typo'd hostname.

`if (i == attempts) throw refused;` — on the final attempt, give up and rethrow rather than swallowing. Rethrowing the original preserves the real diagnostic.

`Thread.sleep(250)` pauses this thread for 250ms. Twenty attempts × 250ms = up to 5 seconds of patience, which is plenty for a JVM to start.

`throw new IOException("Unreachable")` at the end is **dead code** that satisfies the compiler. Java's flow analysis can't prove the loop always returns or throws, so without this line you'd get "missing return statement." A small tax for a strict compiler.

---
---
# PART 4 — Cross-Cutting Concepts, Deep Dive

## 4.1 The four pillars of OOP, as used in this project

**Encapsulation** — hide state, expose behaviour.
`Player`'s `sent` and `received` are `private`. No outsider can corrupt them; the class alone guarantees they mean what they claim. `InMemoryChannel`'s queues are private, so nobody can bypass `send()` and stuff values in directly.

**Abstraction** — expose *what*, hide *how*.
`MessageChannel` says "you can send, receive and close." It says nothing about queues or sockets. `Player` is written entirely against that reduced view of the world.

**Inheritance** — reuse a contract or an implementation.
Used sparingly and deliberately: `implements MessageChannel` twice, and `MessageChannel extends AutoCloseable`. **No class in this project extends another class.** That's healthy. Interface inheritance is cheap; implementation inheritance is expensive and usually the wrong tool. The modern guidance is *favour composition over inheritance* — and note that `Player` **has a** channel (composition) rather than **being a** channel.

**Polymorphism** — one call site, many behaviours.
```java
channel.send(msg);   // Player has no idea which class runs
```
At runtime the JVM inspects the actual object and dispatches to `InMemoryChannel.send` or `TcpMessageChannel.send`. This is **dynamic dispatch**, and it's what lets one `Player` serve both requirements.

## 4.2 The type system

| Category | Examples | Stored as | Default | Can be null? |
|---|---|---|---|---|
| **Primitive** | `int`, `long`, `boolean`, `char`, `double` | the value itself | `0` / `false` | No |
| **Reference** | `String`, `Message`, `Socket`, arrays | a pointer to a heap object | `null` | Yes |

Consequences you meet in this project:

```java
int sent = 0;                // a real zero, never null — no NPE possible
String name;                 // null until assigned
```

**`==` vs `.equals()`** — for references, `==` asks "the same object?" and `.equals()` asks "the same value?" For `String` and `Message`, you almost always want `.equals()`. (For primitives `==` is value comparison and is correct.) Records generate a correct `.equals()` for you, which is one more reason to use them.

**Autoboxing**: `Optional<Message>` can't hold a primitive — generics only work with reference types. If you wanted `Optional<Integer>`, Java would auto-convert (`box`) `int` → `Integer`. Doesn't arise here, but it's why you see `Integer` and `int` as separate things.

## 4.3 Generics

```java
BlockingQueue<Optional<Message>> inbound;
```

Generics let a type be parameterised by another type. `BlockingQueue<T>` is written once and works for any `T`, with **compile-time** type safety:

```java
inbound.add("hello");     // compile error — String is not Optional<Message>
```

Before generics (Java 5), collections held `Object` and you cast on the way out — with `ClassCastException` at runtime if you were wrong. Generics move that entire class of bug to compile time.

**Type erasure**: generics are compile-time only. At runtime the JVM sees a plain `BlockingQueue`; the `<Optional<Message>>` is erased. This is why you can't write `new T[]` or ask an object what its type argument was. It's a compatibility compromise with pre-generics code, and mostly invisible in everyday use.

**The diamond `<>`** — `new LinkedBlockingQueue<>()` — lets the compiler infer the argument from the left-hand side.

## 4.4 Exceptions

Java's hierarchy:

```
Throwable
├── Error                        JVM-level catastrophe (OutOfMemoryError). Don't catch.
└── Exception
    ├── RuntimeException         UNCHECKED — compiler doesn't force handling
    │   ├── NullPointerException
    │   ├── IllegalArgumentException
    │   ├── NumberFormatException
    │   └── UncheckedIOException      ← used in TcpMessageChannel
    └── (everything else)        CHECKED — compiler forces catch-or-declare
        ├── IOException
        ├── InterruptedException
        └── ConnectException     (a subclass of IOException)
```

**Checked** exceptions must be caught or declared with `throws`. The idea: they represent recoverable, expected conditions — a file might not exist, a network might drop. The compiler makes you have a plan.

**Unchecked** exceptions are programming errors that shouldn't normally be caught — a null where one wasn't allowed, an index off the end.

The project shows all three legitimate strategies:

1. **Declare it** — `ResponderProcess.main() throws IOException`. "I can't handle this; the caller (here, the JVM) can crash."
2. **Catch and translate** — `TcpMessageChannel.receive()` catches `IOException` and returns `Optional.empty()`. "I can express this in my own vocabulary."
3. **Catch and wrap** — `send()` wraps `IOException` in `UncheckedIOException`. "This is unrecoverable and shouldn't pollute my interface."

### Rules worth internalising

- **Always preserve the cause** when wrapping: `new UncheckedIOException(msg, e)`. Dropping `e` destroys the stack trace and the ability to diagnose.
- **Catch narrowly**: `catch (ConnectException)` not `catch (Exception)`. A broad catch turns unrelated bugs into silent retries.
- **Never swallow silently** — with the rare exception of a deliberate, named case like `catch (IOException ignored)` when closing something you're abandoning.
- **Restore the interrupt flag**: `Thread.currentThread().interrupt()` in every `InterruptedException` catch you don't rethrow.

## 4.5 Concurrency

### The three problems

1. **Race condition** — two threads interleave and produce a wrong result. Fixed here by `AtomicBoolean.compareAndSet` in `InMemoryChannel.close()` and by `synchronized` in `TcpMessageChannel`.
2. **Visibility** — thread A writes a field, thread B never sees the new value because it's cached in a register. Fixed here by `volatile` on `TcpMessageChannel.closed`.
3. **Deadlock / liveness** — threads waiting on each other forever. Avoided here structurally: only one lock exists per channel and it's never held across a blocking `receive()`.

### The tools, from lightest to heaviest

| Tool | Gives you | Cost | Used here for |
|---|---|---|---|
| **Immutability** (`final` fields) | Everything, free | None | `Message` |
| **`volatile`** | Visibility only | Very low | `TcpMessageChannel.closed` |
| **`Atomic*`** (CAS) | Visibility + atomic RMW, lock-free | Low | `InMemoryChannel.closed` |
| **`synchronized`** | Mutual exclusion + visibility | Moderate | `TcpMessageChannel.send/close` |
| **`java.util.concurrent`** collections | Correct concurrent data structures | Varies | `LinkedBlockingQueue` |

**The order of preference is that order.** Reach for immutability first; hand-written locks last. This project never writes a single `wait()`/`notify()`, because `BlockingQueue` already encapsulates that correctly.

### Blocking vs busy-waiting

```java
inbound.take();                              // blocks: thread parked, 0% CPU
while (queue.isEmpty()) { /* nothing */ }    // busy-wait: 100% of one core, wasted
```

A blocked thread is descheduled by the OS and costs nothing until it's woken. Always prefer a blocking API.

### Thread lifecycle in this project

```
main thread
   │
   ├─ new Thread(...)          NEW
   ├─ bobThread.start()        RUNNABLE → Bob blocks in take()          WAITING
   ├─ aliceThread.start()      RUNNABLE → Alice sends, then blocks      WAITING
   │                           ... they alternate, each waking the other ...
   ├─ aliceThread.join()       main blocks until Alice's run() returns  WAITING
   ├─ bobThread.join()
   └─ print, exit                                                       TERMINATED
```

Only when all **non-daemon** threads finish does the JVM exit. `join()` makes that ordering explicit and printable.

## 4.6 Java I/O in one page

```
                    BYTES                        CHARACTERS
              InputStream / OutputStream     Reader / Writer
                        │                          │
        raw data, files, sockets          text, needs a charset
                        └──── bridged by ──────────┘
                    InputStreamReader / OutputStreamWriter
```

Everything is composed by wrapping (**Decorator pattern**):

```java
new BufferedReader(                     // adds readLine() + buffering
    new InputStreamReader(              // bytes → chars, applying a charset
        socket.getInputStream(),        // raw bytes
        UTF_8))
```

The three rules that prevent 90% of I/O bugs:

1. **Always specify the charset.** Platform defaults differ between machines.
2. **Always buffer.** Unbuffered I/O is slow by orders of magnitude.
3. **Always flush** (writers) **and always close** (use try-with-resources).

## 4.7 Sockets and the message-framing problem

```
    RESPONDER (server)                    INITIATOR (client)
    ─────────────────                     ──────────────────
    new ServerSocket(5000)   ← bind
    server.accept()          ← BLOCKS
                                          new Socket().connect(host:5000)
         ◄──────────────── TCP handshake ────────────────►
    returns a Socket                      holds a Socket
         │                                     │
         └──── two byte streams each way ──────┘
```

**TCP delivers a byte stream, not messages.** Send `"HELLO"` then `"WORLD"` and the peer may read `"HELLOWORLD"`, or `"HEL"` then `"LOWORLD"`. TCP guarantees order and integrity, not boundaries.

So you need **framing**. The three standard options:

| Scheme | How | Trade-off |
|---|---|---|
| **Delimiter** (used here) | End each message with `\n` | Simple, human-readable; delimiter must not appear in payload |
| **Length prefix** | Write the byte count, then the bytes | Fully binary-safe; slightly more code |
| **Fixed size** | Every message is exactly N bytes | Trivial; wasteful and inflexible |

This project uses newline-delimited UTF-8 — the same framing as HTTP headers, SMTP, and Redis's protocol. It's the right choice for readable text, and the `.replace("\n", " ")` in `send()` is what makes it safe.

**End-of-stream**: when one side calls `socket.close()`, TCP sends FIN and the peer's `readLine()` returns `null`. That's the transport-level equivalent of the poison pill.

---
---

# PART 5 — The Verified Trace

This is real output, captured by compiling and running the project — not a sketch.

## 5.1 Same process (`./run.sh same`)

```
=== SAME-PROCESS demo (Requirement 5) ===
Both players run inside this single JVM, PID 2940

[Alice] --> sent #1  : PING
[Bob  ] <-- recv #1  : PING
[Bob  ] --> sent #1  : PING|1
[Alice] <-- recv #1  : PING|1
[Alice] --> sent #2  : PING|1|2
[Bob  ] <-- recv #2  : PING|1|2
[Bob  ] --> sent #2  : PING|1|2|2
[Alice] <-- recv #2  : PING|1|2|2
[Alice] --> sent #3  : PING|1|2|2|3
...
[Alice] --> sent #10 : PING|1|2|2|3|3|4|4|5|5|6|6|7|7|8|8|9|9|10
[Bob  ] <-- recv #10 : PING|1|2|2|3|3|4|4|5|5|6|6|7|7|8|8|9|9|10
[Bob  ] --> sent #10 : PING|1|2|2|3|3|4|4|5|5|6|6|7|7|8|8|9|9|10|10
[Alice] <-- recv #10 : PING|1|2|2|3|3|4|4|5|5|6|6|7|7|8|8|9|9|10|10
>> Bob finished. Totals: sent=10, received=10
>> Alice finished. Totals: sent=10, received=10

=== Done. Both players ran in the same PID 2940 ===
```

**One PID printed at the top and the bottom — requirement 5 proved.**

## 5.2 Why the content grows the way it does

Each reply is `received + "|" + mySendCounter`. Reading the digit pairs:

```
PING                         Alice #1  (kickoff, no counter appended)
PING|1                       Bob   #1
PING|1|2                     Alice #2
PING|1|2|2                   Bob   #2
PING|1|2|2|3                 Alice #3
PING|1|2|2|3|3               Bob   #3
```

After the initial `1`, the numbers come in **pairs** — Alice's *n* then Bob's *n* — because both players' counters advance in lockstep. Alice's kickoff is the one unpaired element, which is why the sequence starts `1` alone.

Final length: 1 seed + 19 appended counters (Alice 2–10 = 9, Bob 1–10 = 10) = the string you see.

## 5.3 The termination logic, step by step

Trace the very last round through `Player.initiate`:

| Step | Code | `sent` | `received` |
|---|---|---|---|
| Alice sends #10 | `buildReply` → `sent++` | 10 | 9 |
| Loop test | `10 < 10 \|\| 9 < 10` → `false \|\| true` → **true**, continue | 10 | 9 |
| Alice blocks | `channel.receive()` | 10 | 9 |
| Bob's reply arrives | `received++` | 10 | 10 |
| **Stop check** | `10 >= 10 && 10 >= 10` → **true** → `break` | 10 | 10 |
| | `channel.close()` → poison pill / socket FIN | | |

Bob, meanwhile, is blocked in `receive()`. The pill (or FIN) arrives, `isPresent()` is `false`, his loop ends, he closes and prints his summary.

**Three details make this land exactly on 10/10:**

1. The kickoff increments `sent` manually — otherwise Alice would send 11 to reach a count of 10.
2. The loop condition uses `||`, so it keeps going until *both* quotas fill.
3. The stop check sits **after** the receive-count and **before** the reply — so Alice never sends an 11th message.

Move any one of those and the counts drift. This is a good example of why terminating protocols are worth tracing on paper.

## 5.4 Separate processes (`./run.sh separate`)

```
[Responder] JVM started, PID 3317, listening on port 5099
[Initiator] JVM started, PID 3336, connecting to localhost:5099
[Responder] Initiator connected. Conversation starting.
[Initiator] Connected to responder. Conversation starting.

[Alice] --> sent #1  : PING
[Bob  ] <-- recv #1  : PING
...
>> Bob finished. Totals: sent=10, received=10
>> Alice finished. Totals: sent=10, received=10

[Responder] JVM PID 3317 exiting.
[Initiator] JVM PID 3336 exiting.
```

**Two different PIDs — 3317 and 3336 — requirement 7 proved.**

The message content is byte-for-byte identical to the same-process run. **The transport changed; the conversation did not.** That is the design working.

One thing you'll notice in the raw terminal: the two processes' output **interleaves unpredictably**, because two independent OS processes are writing to the same terminal with no coordination. In the single-process run the ordering is much tighter, because both threads share one `System.out` (which is internally synchronised). This is not a bug — it's a real and useful illustration that ordering guarantees between processes are weaker than within one.

---
---
# PART 6 — Rebuild It From an Empty Folder

Do this **without looking at the original source**. Use this section only for the order of operations; write the code yourself. If you get stuck, go back to Part 3 for the concept, not the answer.

The build order matters: **innermost first**. Each step compiles on its own.

## Step 0 — Scaffold

```bash
mkdir -p player-messaging/src/main/java/com/example/messaging/{channel,app}
cd player-messaging
```

Create `pom.xml` with the four coordinates, `maven.compiler.release` 17, UTF-8 encoding, and **no dependencies**.

```bash
mvn -q -DskipTests compile     # should succeed with zero sources
```

## Step 1 — `Message.java`

*The data. Depends on nothing.*

- `package com.example.messaging;`
- `public record Message(String content) { }`
- Add a compact constructor with `Objects.requireNonNull`.

**Test it** with a scratch `main`:
```java
Message m = new Message("hi");
System.out.println(m.content());        // hi
System.out.println(m);                  // Message[content=hi]
System.out.println(m.equals(new Message("hi")));   // true  ← value semantics
```

**Concepts to be able to explain:** record, compact constructor, immutability, value vs identity equality.

## Step 2 — `MessageChannel.java`

*The contract. Depends only on `Message`.*

- `public interface MessageChannel extends AutoCloseable`
- `void send(Message message);`
- `Optional<Message> receive();`
- `@Override void close();` ← **no `throws`**, deliberately narrowing

Write the Javadoc contract before the code: what does an empty `receive()` mean? Must `close()` be idempotent? Getting the contract explicit *first* is the actual skill here.

**Concepts:** interface, abstraction, `Optional`, `AutoCloseable`, narrowing an inherited `throws`.

## Step 3 — `Player.java`

*The logic. Depends only on `Message` and `MessageChannel` — never on a transport.*

- `final class`, fields `name`, `channel` (both `final`), `sent`, `received` (both `private int`).
- Constructor taking name + channel (**injection** — do not construct a channel inside).
- `initiate(String seed, int target)`: manual `sent++` for the kickoff, then `while (sent < target || received < target)`, guard on empty receive, count, **stop check before replying**, then `close()` and `summary()`.
- `respond()`: `while ((incoming = channel.receive()).isPresent())`, count, reply, then `close()` and `summary()`.
- `private Message buildReply(String)`: `sent++` **first**, then concatenate.
- `printf` helpers.

**The self-check that matters:** search your finished file for `Queue`, `Socket`, `Thread`. **Zero hits.** If any appear, the abstraction has leaked and steps 4–7 will not work cleanly.

**Concepts:** encapsulation, `final` fields, dependency injection, loop conditions, `Optional` handling, `printf`.

## Step 4 — `channel/InMemoryChannel.java`

*Transport #1. Depends on core.*

- `implements MessageChannel`, `final class`.
- Nested `public record Pair(MessageChannel initiatorSide, MessageChannel responderSide) {}`.
- `private static final Optional<Message> END = Optional.empty();`
- Two `BlockingQueue<Optional<Message>>` fields + an `AtomicBoolean`.
- **Private** constructor.
- `public static Pair pair()` — create two queues, cross them, return both ends.
- `send` → guard on closed, `outbound.add(Optional.of(m))`.
- `receive` → `inbound.take()`, catch `InterruptedException`, **restore the interrupt flag**, return `END`.
- `close` → `if (closed.compareAndSet(false, true)) outbound.add(END);`

**The one thing to get right:** the crossing. Alice's `outbound` **is** Bob's `inbound`. Draw it before you type it.

**Concepts:** `BlockingQueue`, blocking `take()`, poison pill, static factory, private constructor, `AtomicBoolean`/CAS, `InterruptedException` handling.

## Step 5 — `app/SingleProcessApp.java`

*Requirement 5. The composition root.*

- `ProcessHandle.current().pid()` and print it.
- `InMemoryChannel.pair()`, two `Player`s.
- Two `Thread`s — one via method reference `bob::respond`, one via lambda `() -> alice.initiate(SEED, TARGET)`.
- `start()` both (responder first), `join()` both.
- Private constructor.

**Run it.** You should see the 10/10 ping-pong. If it hangs, you almost certainly called `run()` instead of `start()`, or crossed the queues wrong.

**Concepts:** threads, `start()` vs `run()`, `Runnable`, lambdas, method references, `join()`, `ProcessHandle`.

## Step 6 — `channel/TcpMessageChannel.java`

*Transport #2. Same interface, different world.*

- Constructor takes a `Socket`, `throws IOException`; wrap the streams:
  `new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8))` and the writer mirror.
- `volatile boolean closed`.
- `synchronized send`: strip `\n` and `\r`, write, write `'\n'`, **`flush()`**, wrap `IOException` in `UncheckedIOException` **with the cause**.
- `receive`: `readLine()`; `null` → `Optional.empty()`.
- `synchronized close`: flag, `socket.close()`.

**Concepts:** sockets, stream decoration, charsets, buffering, flushing, framing, `synchronized`, `volatile`, checked→unchecked wrapping.

## Step 7 — `app/ResponderProcess.java` and `app/InitiatorProcess.java`

*Requirement 7. Two `main`s, two JVMs.*

Responder:
- Parse port from `args`, print PID.
- `try (ServerSocket server = new ServerSocket(port))`, then nested `try (Socket socket = server.accept(); MessageChannel channel = new TcpMessageChannel(socket))`.
- `new Player("Bob", channel).respond();`

Initiator:
- Parse host + port, print PID.
- `connectWithRetry` — loop, `new Socket()` + `connect(new InetSocketAddress(host, port), 1000)`, catch **only** `ConnectException`, sleep, rethrow on the last attempt.
- `try (socket; channel)`, `new Player("Alice", channel).initiate(SEED, TARGET);`

**Concepts:** `ServerSocket`/`accept()`, client connect with timeout, try-with-resources with multiple resources, targeted exception catching, retry loops, `args` parsing.

## Step 8 — `run.sh`

- `set -euo pipefail`, `cd "$(dirname "$0")"`.
- `mvn -q -DskipTests compile`.
- `same` → one `java` invocation.
- `separate` → two backgrounded `java` invocations (`&`), capture `$!`, `wait` for both.

```bash
chmod +x run.sh
./run.sh same
./run.sh separate
```

## Step 9 — The proof

- Same-process run prints **one** PID.
- Separate-process run prints **two different** PIDs.
- Both print `sent=10, received=10` for both players.
- Both runs produce the **identical** message content.

That last point is the one to sit with. If the strings match across both transports, your abstraction is genuinely doing its job.

---
---

# PART 7 — Exercises, Mistakes, Glossary

## 7.1 Exercises, in increasing difficulty

**1. Make the target configurable.** Read it from `args` in all three apps instead of the `TARGET` constant. *(Touches: args parsing, `Integer.parseInt`, constants vs configuration.)*

**2. Add a timestamp to `Message`.** `record Message(String content, Instant sentAt)`. Notice what breaks: `TcpMessageChannel` must now serialise **two** fields, which forces you to design a real wire format. *(Touches: records with multiple components, serialisation, protocol design.)*

**3. Write a `LoopbackChannel`** where `send` immediately makes the same message available to `receive`, and use it to unit-test `Player` with no threads and no sockets at all. *(This is the payoff of dependency injection — and it's why the design is testable.)*

**4. Replace raw `Thread` with an `ExecutorService`.**
```java
ExecutorService pool = Executors.newFixedThreadPool(2);
pool.submit(bob::respond);
pool.submit(() -> alice.initiate(SEED, TARGET));
pool.shutdown();
pool.awaitTermination(30, TimeUnit.SECONDS);
```
*(Touches: the modern concurrency API — you rarely create `Thread` by hand in production.)*

**5. Switch the framing to length-prefixed binary.** Use `DataOutputStream.writeUTF` / `DataInputStream.readUTF`. Now `send` needs no newline-stripping, and arbitrary content is safe. *(Touches: binary I/O, framing trade-offs.)*

**6. Handle multiple concurrent responders.** Loop on `accept()` and hand each socket to a thread. *(Touches: real server architecture.)*

**7. Add a heartbeat/timeout.** Use `socket.setSoTimeout(ms)` so `readLine()` throws `SocketTimeoutException` instead of blocking forever on a dead peer. *(Touches: liveness, the difference between "slow" and "dead".)*

**8. Add JUnit tests.** Add the `junit-jupiter` dependency to `pom.xml`, write tests for `buildReply`'s counter behaviour and the 10/10 stop condition using the fake channel from exercise 3. *(Touches: Maven dependencies, test scope, `src/test/java`.)*

## 7.2 Mistakes you will probably make once

| Mistake | Symptom | Fix |
|---|---|---|
| `thread.run()` instead of `start()` | Program hangs; no concurrency | `start()` creates a thread; `run()` is a plain call |
| Forgetting `out.flush()` | Peer waits forever | Flush after every message |
| Queues not crossed | Player talks to itself, or hangs | Alice's `outbound` **is** Bob's `inbound` |
| `&&` instead of `\|\|` in the loop condition | Stops early at 10/9 or 9/10 | Continue while **either** quota is unfilled |
| Stop check after sending the reply | Ends at 11 sent | Check **after** counting the receive, **before** replying |
| `sent++` after building the string | Every message carries the previous number | Increment first |
| Swallowing `InterruptedException` | Thread can't be cancelled | `Thread.currentThread().interrupt()` |
| Omitting the charset | Non-ASCII corrupts across machines | Always pass `UTF_8` |
| Newline inside a message body | Peer reads one message as two | Strip, escape, or length-prefix |
| Package doesn't match folder | "class is public, should be declared in a file named…" | Folder path == package name |
| Running the responder twice | `BindException: Address already in use` | Kill the old JVM, or use another port |
| Catching `Exception` in the retry loop | Silently retries 20× on a typo'd host | Catch `ConnectException` only |
| Dropping the cause when wrapping | Useless stack trace | `new UncheckedIOException(msg, e)` |
| Making `Player` import `Socket` | Abstraction leaked; one class per transport again | Depend only on `MessageChannel` |

## 7.3 Concept index — where to see each one in the code

| Concept | File | What to look at |
|---|---|---|
| `record`, compact constructor | `Message` | the whole file |
| Immutability, value equality | `Message` | `final` fields, generated `equals` |
| `Objects.requireNonNull` | `Message` | fail-fast validation |
| Interface, abstraction | `MessageChannel` | method signatures with no bodies |
| `AutoCloseable`, narrowing `throws` | `MessageChannel` | `@Override void close();` |
| `Optional<T>` | `MessageChannel`, `Player` | `receive()`'s return type |
| Generics, diamond `<>` | `InMemoryChannel` | `BlockingQueue<Optional<Message>>` |
| Encapsulation, `final` fields | `Player` | `private final`, `private int` |
| Dependency injection | `Player` | constructor takes a channel |
| Loops, `break`, short-circuit `\|\|`/`&&` | `Player` | `initiate` |
| Assignment-as-expression | `Player` | `while ((incoming = ...).isPresent())` |
| `printf` format specifiers | `Player` | `%-5s`, `%-2d`, `%n` |
| Static factory, private constructor | `InMemoryChannel` | `pair()` |
| Nested record | `InMemoryChannel` | `Pair` |
| `BlockingQueue`, blocking `take()` | `InMemoryChannel` | `receive()` |
| Poison pill | `InMemoryChannel` | `END` |
| `AtomicBoolean`, compare-and-set | `InMemoryChannel` | `close()` |
| `InterruptedException` handling | `InMemoryChannel` | interrupt-flag restore |
| Sockets, `ServerSocket`, `accept` | `ResponderProcess` | try-with-resources block |
| Client connect + timeout | `InitiatorProcess` | `connectWithRetry` |
| Stream decoration, charsets, buffering | `TcpMessageChannel` | constructor |
| Framing, `flush()` | `TcpMessageChannel` | `send` |
| `synchronized`, `volatile` | `TcpMessageChannel` | method modifiers |
| Checked → unchecked wrapping | `TcpMessageChannel` | `UncheckedIOException` |
| Threads, `start`/`join` | `SingleProcessApp` | `main` |
| Lambdas, method references | `SingleProcessApp` | `bob::respond`, `() -> ...` |
| `ProcessHandle` / PID | all three apps | the proof lines |
| try-with-resources, multiple resources | both process apps | nested `try (...)` |
| `main`, `args`, `Integer.parseInt` | both process apps | argument parsing |
| Maven POM, `maven.compiler.release` | `pom.xml` | the whole file |

## 7.4 Glossary

**Abstraction** — exposing what something does while hiding how.
**Atomic** — an operation that cannot be observed half-done.
**Autoboxing** — automatic conversion between `int` and `Integer`.
**Blocking** — a call that parks the thread until it can proceed, using no CPU.
**Bytecode** — the instruction format the JVM executes; what `javac` produces.
**CAS (compare-and-set)** — atomic "if the value is X, make it Y"; lock-free.
**Checked exception** — one the compiler forces you to catch or declare.
**Classpath** — where the JVM looks for `.class` files.
**Composition root** — the one place that wires concrete implementations together.
**Constructor injection** — passing dependencies in through the constructor.
**Decorator** — wrapping an object in another with the same interface, adding behaviour.
**Dependency Inversion** — depend on abstractions, not concretions.
**Effectively final** — a variable never reassigned after initialisation; required for lambda capture.
**Encapsulation** — hiding state behind methods.
**Framing** — marking where one message ends and the next begins in a byte stream.
**Functional interface** — an interface with exactly one abstract method; satisfiable by a lambda.
**Generics** — type parameters checked at compile time.
**Idempotent** — calling it twice has the same effect as calling it once.
**Immutable** — cannot change after construction; inherently thread-safe.
**JVM** — the virtual machine that runs bytecode.
**Lambda** — an anonymous function: `() -> doSomething()`.
**Method reference** — `obj::method`, shorthand for a lambda that just calls that method.
**Package** — a namespace matching the folder structure.
**Poison pill** — a special value sent through a queue meaning "stop".
**Polymorphism** — one call site, behaviour chosen by the runtime type.
**POM** — Maven's `pom.xml` project descriptor.
**Primitive** — a raw value type (`int`, `boolean`, …); not an object, never null.
**Race condition** — a bug where the result depends on thread timing.
**Record** — a concise immutable data class with generated constructor/accessors/equals.
**Socket** — an endpoint of a network connection.
**Static** — belongs to the class, not to any instance.
**Static factory** — a static method that creates instances instead of a public constructor.
**Strategy pattern** — interchangeable implementations selected at runtime.
**`synchronized`** — mutual exclusion via an object's intrinsic lock.
**try-with-resources** — a `try` that auto-closes `AutoCloseable` resources.
**Unchecked exception** — a `RuntimeException`; no compiler obligation.
**`volatile`** — forces reads/writes to main memory so other threads see them.

---

## The three sentences worth remembering

1. **`Player` never mentions a queue or a socket** — that's why one class covers both requirements.
2. **`Optional.empty()` means "the conversation is over"** — a poison pill and a TCP end-of-stream both translate into that one word.
3. **Immutability, then atomics, then locks** — in that order, and you'll write less concurrent code that's wrong.

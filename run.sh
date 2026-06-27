#!/usr/bin/env bash
#
# run.sh - build and run the player-messaging demo.
#
#   ./run.sh same       -> both players in ONE JVM      (Requirement 5)  [default]
#   ./run.sh separate   -> each player in its OWN JVM    (Requirement 7)
#
# Optional: override the TCP port for the separate-process demo:
#   PORT=6000 ./run.sh separate
#
set -euo pipefail

cd "$(dirname "$0")"

MODE="${1:-same}"
PORT="${PORT:-5000}"
CP="target/classes"
PKG="com.example.messaging.app"

echo ">> Building project with Maven (compile only, no jar shipped)..."
mvn -q -DskipTests compile

case "$MODE" in
  same)
    echo
    echo ">> Running SAME-PROCESS demo (Requirement 5)"
    echo "--------------------------------------------------"
    java -cp "$CP" "$PKG.SingleProcessApp"
    ;;

  separate)
    echo
    echo ">> Running SEPARATE-PROCESS demo (Requirement 7)"
    echo "--------------------------------------------------"

    echo ">> Launching RESPONDER in its own JVM..."
    java -cp "$CP" "$PKG.ResponderProcess" "$PORT" &
    RESPONDER_PID=$!
    echo ">> Responder OS PID = $RESPONDER_PID"

    echo ">> Launching INITIATOR in its own JVM..."
    java -cp "$CP" "$PKG.InitiatorProcess" localhost "$PORT" &
    INITIATOR_PID=$!
    echo ">> Initiator OS PID = $INITIATOR_PID"
    echo "   (two distinct PIDs => two distinct processes)"
    echo "--------------------------------------------------"

    # Wait for both JVMs to exit gracefully.
    wait "$INITIATOR_PID"
    wait "$RESPONDER_PID"

    echo "--------------------------------------------------"
    echo ">> Both JVMs finished (PIDs $RESPONDER_PID and $INITIATOR_PID)."
    ;;

  *)
    echo "Usage: $0 [same|separate]" >&2
    exit 1
    ;;
esac

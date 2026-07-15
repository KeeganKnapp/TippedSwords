#!/usr/bin/env bash
# Build/test the mod inside an isolated Podman container instead of on the
# host. Source is bind-mounted read-write (so build/test output lands back
# on disk), but the Gradle cache lives in a named volume so it never touches
# the host's ~/.gradle.
#
# Usage:
#   ./sandbox.sh build        # ./gradlew build
#   ./sandbox.sh test         # ./gradlew test
#   ./sandbox.sh <gradle args...>
#   ./sandbox.sh claude [claude args...]   # claude, permission prompts skipped
#
# `claude` runs with permissions bypassed (baked into the image's own
# ~/.claude, see Dockerfile) since the container can't reach anything
# outside /workspace and its named volumes. It runs as a non-root `claude`
# user (mapped 1:1 onto the host user via --userns=keep-id) because Claude
# Code refuses the bypass-permissions setting when running as root. Its
# login/session state persists in the alchemic-swords-claude-home volume
# across runs; if ANTHROPIC_API_KEY is set on the host it's passed through
# so you don't need to log in at all.
#
# Note: no display/GPU access in the container, so `runClient` won't work
# here — run that on the host once you've reviewed the change.
set -euo pipefail

IMAGE=alchemic-swords-sandbox
VOLUME=alchemic-swords-gradle-sandbox-cache
CLAUDE_VOLUME=alchemic-swords-claude-home

podman build -t "$IMAGE" \
  --build-arg HOST_UID="$(id -u)" \
  --build-arg HOST_GID="$(id -g)" \
  .

if [[ "${1:-}" == "claude" ]]; then
  shift
  CLAUDE_ENV_ARGS=()
  if [[ -n "${ANTHROPIC_API_KEY:-}" ]]; then
    CLAUDE_ENV_ARGS+=(-e ANTHROPIC_API_KEY)
  fi
  exec podman run --rm -it \
    --user claude:claude \
    --userns=keep-id \
    -v "$(pwd)":/workspace:Z \
    -v "$VOLUME":/workspace/.gradle-sandbox:Z \
    -v "$CLAUDE_VOLUME":/home/claude/.claude:Z \
    "${CLAUDE_ENV_ARGS[@]}" \
    --entrypoint claude \
    "$IMAGE" "$@"
fi

podman run --rm -it \
  -v "$(pwd)":/workspace:Z \
  -v "$VOLUME":/workspace/.gradle-sandbox:Z \
  "$IMAGE" "${@:-build}"

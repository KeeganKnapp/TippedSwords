# Sandbox for building/testing the mod without touching the host's
# Java install, ~/.gradle cache, or Minecraft/Fabric assets.
#
# No source is baked into this image — sandbox.sh bind-mounts the real
# worktree over /workspace at run time, so this only needs rebuilding when
# the JDK version or entrypoint changes, not on every source edit.
#
# NOTE: this container has no display/GPU access, so `runClient` (which
# opens an actual Minecraft window) will not work in here. Use it for
# `build` and `test` only; run the graphical client on the host once
# you've reviewed the change.
FROM docker.io/library/eclipse-temurin:25-jdk-jammy

WORKDIR /workspace

# Isolated Gradle home so downloads/caches never touch the host.
ENV GRADLE_USER_HOME=/workspace/.gradle-sandbox

# Claude Code CLI, so it can be run *inside* this container (see
# `sandbox.sh claude`) with permission prompts bypassed. Safe here in a way
# it wouldn't be on the host: this container only ever sees /workspace and
# its own named volumes, so there's nothing outside the sandbox for an
# unattended command to damage.
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL https://claude.ai/install.sh | bash
ENV PATH="/root/.local/bin:${PATH}"
# Claude CLI refuses --dangerously-skip-permissions / defaultMode:
# bypassPermissions when running as root/UID 0, so `sandbox.sh claude` runs
# as this non-root user instead (gradlew build/test below is unaffected and
# still runs as root). UID/GID are passed in at build time to match the
# host user, since sandbox.sh runs the container with --userns=keep-id.
RUN chmod o+x /root && chmod -R o+rX /root/.local
ARG HOST_UID=1000
ARG HOST_GID=1000
RUN groupadd -g "$HOST_GID" claude \
    && useradd -m -u "$HOST_UID" -g "$HOST_GID" -s /bin/bash claude

# Baked default for the claude user's ~/.claude (never the host's): skip
# permission prompts. sandbox.sh mounts a named volume over this directory,
# which Podman seeds from the image on first use, so this file becomes the
# volume's starting state and persists from there.
RUN mkdir -p /home/claude/.claude \
    && printf '{\n  "permissions": {\n    "defaultMode": "bypassPermissions"\n  }\n}\n' > /home/claude/.claude/settings.json \
    && chown -R claude:claude /home/claude/.claude

# Pre-create the Gradle cache mount point owned by the claude user too, so
# if `claude` shells out to ./gradlew it can write to it (the named volume
# is seeded from this directory's ownership on first mount).
RUN mkdir -p /workspace/.gradle-sandbox && chown -R claude:claude /workspace/.gradle-sandbox

ENTRYPOINT ["./gradlew", "--no-daemon"]
CMD ["build"]

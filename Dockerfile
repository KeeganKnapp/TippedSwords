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

# Baked default for the container's own ~/.claude (never the host's):
# skip permission prompts. sandbox.sh mounts a named volume over this
# directory, which Podman seeds from the image on first use, so this file
# becomes the volume's starting state and persists from there.
RUN mkdir -p /root/.claude \
    && printf '{\n  "permissions": {\n    "defaultMode": "bypassPermissions"\n  }\n}\n' > /root/.claude/settings.json

ENTRYPOINT ["./gradlew", "--no-daemon"]
CMD ["build"]

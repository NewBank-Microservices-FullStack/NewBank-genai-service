#!/usr/bin/env sh
# Minimal mvnw: uses system mvn if present, else downloads Maven 3.9.9 locally.
set -e
if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi
M2_VER=3.9.9
M2_DIR="$HOME/.m2/wrapper/apache-maven-$M2_VER"
if [ ! -x "$M2_DIR/bin/mvn" ]; then
  mkdir -p "$M2_DIR"
  curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$M2_VER/apache-maven-$M2_VER-bin.tar.gz" \
    | tar -xz -C "$HOME/.m2/wrapper"
fi
exec "$M2_DIR/bin/mvn" "$@"

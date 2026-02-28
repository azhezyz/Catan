#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_ROOT="$REPO_ROOT/org.eclipse.papyrus.javagen.catan"

cd "$MODULE_ROOT"
echo "[Launcher] Compiling Java sources..."
mvn -q -DskipTests compile
echo "[Launcher] Starting game..."
java -cp target/classes Catan.HumanGameLauncher game.config visualize/state.json

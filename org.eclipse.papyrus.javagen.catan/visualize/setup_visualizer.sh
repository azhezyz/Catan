#!/usr/bin/env bash
set -euo pipefail

VISUALIZE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="$VISUALIZE_DIR/.venv"
CATANATRON_DIR="$VISUALIZE_DIR/catanatron"
CATANATRON_REPO="https://github.com/bcollazo/catanatron.git"
CATANATRON_BRANCH="gym-rendering"

find_python() {
  if command -v python3.11 >/dev/null 2>&1; then
    command -v python3.11
    return 0
  fi

  if command -v python3 >/dev/null 2>&1 && python3 -c 'import sys; raise SystemExit(0 if sys.version_info >= (3, 11) else 1)'; then
    command -v python3
    return 0
  fi

  return 1
}

PYTHON_BIN="$(find_python || true)"
if [[ -z "$PYTHON_BIN" ]]; then
  echo "Python 3.11+ is required for the visualizer." >&2
  echo "Install python3.11 first, then rerun this script." >&2
  exit 1
fi

if [[ -x "$VENV_DIR/bin/python" ]] && ! "$VENV_DIR/bin/python" -c 'import sys; raise SystemExit(0 if sys.version_info >= (3, 11) else 1)'; then
  echo "Existing .venv uses Python < 3.11. Recreating it."
  rm -rf "$VENV_DIR"
fi

if [[ ! -x "$VENV_DIR/bin/python" ]]; then
  echo "Creating virtual environment with $PYTHON_BIN"
  "$PYTHON_BIN" -m venv "$VENV_DIR"
fi

source "$VENV_DIR/bin/activate"
python -m pip install --upgrade pip setuptools wheel
pip install -r "$VISUALIZE_DIR/requirements.txt"

if [[ ! -d "$CATANATRON_DIR/.git" ]]; then
  git clone -b "$CATANATRON_BRANCH" "$CATANATRON_REPO" "$CATANATRON_DIR"
fi

pip install -e "$CATANATRON_DIR[web,gym,dev]"

echo
echo "Visualizer environment is ready."
echo "Activate with: source \"$VENV_DIR/bin/activate\""
echo "Run with: python \"$VISUALIZE_DIR/light_visualizer.py\" \"$VISUALIZE_DIR/base_map.json\" \"$VISUALIZE_DIR/state.json\" --watch"

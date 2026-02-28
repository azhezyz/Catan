# Catan visualizer

This module contains a lightweight Python visualizer for the human-playable Catan launcher.

## Files
- `base_map.json`: fixed board layout
- `state.json`: current board state written by the Java launcher
- `light_visualizer.py`: renders the board and can watch for updates

## Setup
From `org.eclipse.papyrus.javagen.catan/visualize`:

```bash
./setup_visualizer.sh
```

This requires `Python 3.11+`. The `catanatron` branch used by the renderer no longer installs on Python 3.9.

If you want to run the steps manually on Unix-like shells:

```bash
python3.11 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip setuptools wheel
pip install -r requirements.txt
git clone -b gym-rendering https://github.com/bcollazo/catanatron.git
pip install -e "./catanatron[web,gym,dev]"
```

On Windows PowerShell, create and activate the virtual environment with:

```powershell
py -3.11 -m venv .venv
.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip setuptools wheel
pip install -r requirements.txt
git clone -b gym-rendering https://github.com/bcollazo/catanatron.git
pip install -e ".\catanatron[web,gym,dev]"
```

## Run manually

```bash
python light_visualizer.py base_map.json state.json --watch
```

The Java launcher will try to start this automatically when `visualize/.venv` exists.

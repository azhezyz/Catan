# Catan visualizer

This module contains a lightweight Python visualizer for the human-playable Catan launcher.

## Files
- `base_map.json`: fixed board layout
- `state.json`: current board state written by the Java launcher
- `light_visualizer.py`: renders the board and can watch for updates

## Setup
From `org.eclipse.papyrus.javagen.catan/visualize`:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
git clone -b gym-rendering https://github.com/bcollazo/catanatron.git
cd catanatron
pip install -e ".[web,gym,dev]"
cd ..
```

On Windows PowerShell, activate with:

```powershell
.venv\Scripts\Activate.ps1
```

## Run manually

```bash
python light_visualizer.py base_map.json state.json --watch
```

The Java launcher will try to start this automatically if Python is available.

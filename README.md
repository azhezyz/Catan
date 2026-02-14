# Catan

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=azhezyz_Catan&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=azhezyz_Catan)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=azhezyz_Catan&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=azhezyz_Catan)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=azhezyz_Catan&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=azhezyz_Catan)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=azhezyz_Catan&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=azhezyz_Catan)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=azhezyz_Catan&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=azhezyz_Catan)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=azhezyz_Catan&metric=coverage)](https://sonarcloud.io/summary/new_code?id=azhezyz_Catan)


A model-driven Java implementation of **Settlers of Catan**, built with **Eclipse Papyrus UML** and Java code generation.

## Overview
This repository defines the Catan domain model in UML and keeps generated Java artifacts under version control. The project is organized to support a model-first workflow:
- design and maintain game structure in Papyrus UML,
- generate/update Java skeletons from the model,
- implement and evolve gameplay logic in Java.

## Project Scope
The codebase models core Catan concepts such as:
- board topology (`Board`, `Node`, `Path`),
- game entities (`Building` and related classes),
- class relationships derived from UML.

## Core Game Rules
### Turn Flow
1. Roll two dice.
2. Tiles matching the rolled number produce resources.
3. Players with settlements/cities adjacent to producing tiles gain resources.
4. The current player may build if they can afford costs and placement is legal.

### Resource Production
- Settlement: produces `1` resource from each adjacent producing tile.
- City: produces `2` resources from each adjacent producing tile.
- Desert tile: produces no resources.

### Building Costs
- Road: `1 WOOD + 1 BRICK`
- Settlement: `1 WOOD + 1 BRICK + 1 SHEEP + 1 WHEAT`
- City upgrade: `2 WHEAT + 3 ORE`

### Placement Rules (Implemented Scope)
- Roads must be placed on unclaimed paths.
- New roads must connect to the player's existing network.
- Settlements must follow node legality checks (including distance/connectivity constraints handled by game logic).
- Cities can only upgrade the player's own existing settlements.

### Scoring and Win Condition
- Settlement: `1` victory point.
- City: `2` victory points.
- Longest Road holder: `+2` victory points (awarded when road length exceeds 4 and is the highest).
- A player wins immediately upon reaching `10` victory points.

### Setup
1. Build the island map using terrain hexes, sea frame pieces, harbors, and number tokens.
2. Each player takes one color: `5` settlements, `4` cities, `15` roads.
3. Initial placement is done in two setup rounds (snake order):
- Round 1: clockwise, each player places `1` settlement + `1` connected road.
- Round 2: counterclockwise, each player places `1` settlement + `1` connected road.
4. After placing the second settlement, a player receives starting resources from adjacent producing hexes.
5. The robber starts on the desert.

### Standard Turn Structure
1. Roll for production.
2. Trade (domestic and/or maritime).
3. Build and/or buy development cards.
4. Optionally play one development card during your turn (subject to card timing rules).

### Robber and Rolling 7
- If a `7` is rolled, no terrain produces resources.
- Any player with more than `7` resource cards discards half (rounded down).
- The active player moves the robber to a new tile and steals one random resource from an adjacent opponent (if possible).
- The robber blocks resource production on its current tile.

### Building and Placement
- Roads are built on paths (edges), one road per path.
- Settlements are built on intersections and must obey the distance rule (no adjacent settlements/cities on neighboring intersections).
- New settlements must connect to the player's road network.
- Cities upgrade an existing settlement owned by the same player.

### Trading
- Domestic trade: active player may trade with other players.
- Maritime trade: active player may trade with the bank; harbor ownership improves exchange rates.

### Development Cards and Special Cards
- Development cards include Knight, Progress, and Victory Point cards.
- Usually, a bought development card cannot be played in the same turn (except victory declaration timing).
- Longest Road: first road length `>= 5`, worth `2` victory points; can be taken by a longer road.
- Largest Army: first player to play `3` Knights claims it, worth `2` victory points; can be taken by a player with more Knights played.

### End of Game
- The first player to reach `10` victory points on their own turn wins immediately.

## Repository Layout
- `catan/catan.uml` UML model (source of truth)
- `catan/catan.notation` Papyrus diagram notation
- `catan/catan.di` Papyrus model descriptor
- `catan/catan.aird` Sirius/Papyrus representation metadata
- `org.eclipse.papyrus.javagen.catan/src-gen/catan` Java code generated from UML
- `org.eclipse.papyrus.javagen.catan/pom.xml` Maven build configuration

## Tech Stack
- Java 21
- Eclipse Papyrus (UML modeling)
- Maven (build)

## Build
From `org.eclipse.papyrus.javagen.catan`:

```powershell
mvn compile
```

## Development Workflow
1. Open the UML model in Eclipse Papyrus.
2. Update classes, attributes, and associations in `catan.uml`.
3. Regenerate Java code into `src-gen`.
4. Build with Maven and validate compilation.
5. Add/maintain hand-written logic in source folders as the implementation layer evolves.

## Notes
- `src-gen` is generated output and may be overwritten by regeneration.
- Keep model changes and generated code changes synchronized in commits.
- If generated fields/types are placeholders, refine UML typing/mapping before regeneration.

import json
import os
import sys
import time
from typing import Dict, Optional

from PIL import Image
from catanatron.game import Game
from catanatron.gym.envs.pygame_renderer import PygameRenderer
from catanatron.models.board import Board
from catanatron.models.enums import BRICK, CITY, ORE, SETTLEMENT, SHEEP, WHEAT, WOOD
from catanatron.models.map import CatanMap, LandTile, MapTemplate, initialize_tiles
from catanatron.models.player import Color, Player


class CatanBoardVisualizer:
    def __init__(self):
        self.map_data: Optional[Dict] = None
        self.state_data: Optional[Dict] = None
        self.game: Optional[Game] = None

    def load_map_json(self, json_path: str) -> None:
        with open(json_path, "r", encoding="utf-8") as file:
            self.map_data = json.load(file)

    def load_state_json(self, json_path: str) -> None:
        with open(json_path, "r", encoding="utf-8") as file:
            self.state_data = json.load(file)

    def _parse_resource(self, resource_str: Optional[str]) -> Optional[str]:
        if resource_str is None or resource_str == "DESERT":
            return None

        resource_map = {
            "WOOD": WOOD,
            "BRICK": BRICK,
            "SHEEP": SHEEP,
            "WHEAT": WHEAT,
            "ORE": ORE,
        }
        if resource_str not in resource_map:
            raise ValueError(f"Unknown resource: {resource_str}")
        return resource_map[resource_str]

    def _parse_color(self, color_str: str) -> Color:
        color_map = {
            "RED": Color.RED,
            "BLUE": Color.BLUE,
            "ORANGE": Color.ORANGE,
            "WHITE": Color.WHITE,
        }
        if color_str not in color_map:
            raise ValueError(f"Unknown color: {color_str}")
        return color_map[color_str]

    def _create_map_from_json(self) -> CatanMap:
        if self.map_data is None:
            raise ValueError("No map data loaded. Call load_map_json first.")

        tile_coords = []
        resources = []
        numbers = []
        for tile_data in self.map_data["tiles"]:
            coord = (tile_data["q"], tile_data["s"], tile_data["r"])
            if sum(coord) != 0:
                raise ValueError(f"Invalid cube coordinate: {coord}. Sum must be 0.")

            tile_coords.append(coord)
            resource = self._parse_resource(tile_data["resource"])
            resources.append(resource)
            if resource is not None:
                numbers.append(tile_data["number"])

        template = MapTemplate(
            numbers=numbers,
            port_resources=[],
            tile_resources=resources,
            topology={coord: LandTile for coord in tile_coords},
        )

        tiles = initialize_tiles(
            template,
            shuffled_numbers_param=list(reversed(numbers)),
            shuffled_port_resources_param=[],
            shuffled_tile_resources_param=list(reversed(resources)),
        )
        return CatanMap.from_tiles(tiles)

    def _apply_state_to_board(self, board: Board) -> None:
        board.buildings.clear()
        board.roads.clear()

        for building_data in self.state_data.get("buildings", []):
            node_id = building_data["node"]
            color = self._parse_color(building_data["owner"])
            building_type = building_data["type"]
            if building_type == "SETTLEMENT":
                board.buildings[node_id] = (color, SETTLEMENT)
            elif building_type == "CITY":
                board.buildings[node_id] = (color, CITY)
            else:
                raise ValueError(f"Unknown building type: {building_type}")

        for road_data in self.state_data.get("roads", []):
            a = road_data["a"]
            b = road_data["b"]
            color = self._parse_color(road_data["owner"])
            board.roads[(a, b)] = color
            board.roads[(b, a)] = color

    def build_game(self) -> Game:
        catan_map = self._create_map_from_json()
        game = Game(
            players=[Player(Color.BLUE), Player(Color.RED)],
            seed=42,
            catan_map=catan_map,
            initialize=True,
        )
        self._apply_state_to_board(game.state.board)
        self.game = game
        return game

    def render(self, output_dir: str = "scraped_boards", render_scale: float = 1.0) -> str:
        if self.game is None:
            self.build_game()

        renderer = PygameRenderer(render_scale=render_scale)
        rgb_array = renderer.render(self.game)

        os.makedirs(output_dir, exist_ok=True)
        file_count = len(
            [name for name in os.listdir(output_dir) if os.path.isfile(os.path.join(output_dir, name))]
        )
        output_path = os.path.join(output_dir, f"board{file_count}.png")

        image = Image.fromarray(rgb_array)
        image.save(output_path)
        print(f"Board rendered and saved to {output_path}")
        renderer.close()
        return output_path


def visualize_board_from_json(
        map_json_path: str,
        state_json_path: str,
        output_dir: str = "scraped_boards",
        render_scale: float = 1.0,
) -> None:
    visualizer = CatanBoardVisualizer()
    visualizer.load_map_json(map_json_path)
    visualizer.load_state_json(state_json_path)
    visualizer.render(output_dir=output_dir, render_scale=render_scale)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage:")
        print("  python light_visualizer.py base_map.json state.json")
        print("  python light_visualizer.py base_map.json state.json --watch")
        sys.exit(1)

    base_map_path = sys.argv[1]
    state_path = "state.json"
    watch_mode = "--watch" in sys.argv

    if len(sys.argv) >= 3 and sys.argv[2] != "--watch" and sys.argv[2].endswith(".json"):
        state_path = sys.argv[2]

    last_mtime = None
    print("Visualizer started.")
    if watch_mode:
        print("Watch mode enabled. Waiting for state.json changes...")

    while True:
        if os.path.exists(state_path):
            mtime = os.path.getmtime(state_path)
            if (not watch_mode) or (mtime != last_mtime):
                last_mtime = mtime
                visualize_board_from_json(base_map_path, state_path)
        if not watch_mode:
            break
        time.sleep(0.5)

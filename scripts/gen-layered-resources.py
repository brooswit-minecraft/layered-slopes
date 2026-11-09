#!/usr/bin/env python3
"""Generate blockstate, block-model, item-model, lang and tag JSON for the
layered blocks. Re-run after changing MATERIALS or the layer count; output is
committed so the build needs no datagen run.

Usage: python3 scripts/gen-layered-resources.py
"""
import json
from pathlib import Path

MOD = "dynamicterrain"
MAX_LAYERS = 16
# block name -> (display name, texture, tool)
MATERIALS = {
    "layered_dirt": ("Layered Dirt", "minecraft:block/dirt", "shovel"),
    "layered_sand": ("Layered Sand", "minecraft:block/sand", "shovel"),
    "layered_gravel": ("Layered Gravel", "minecraft:block/gravel", "shovel"),
    "layered_stone": ("Layered Stone", "minecraft:block/stone", "pickaxe"),
    "layered_cobblestone": ("Layered Cobblestone", "minecraft:block/cobblestone", "pickaxe"),
    "layered_smooth_stone": ("Layered Smooth Stone", "minecraft:block/smooth_stone", "pickaxe"),
    "layered_deepslate": ("Layered Deepslate", "minecraft:block/deepslate", "pickaxe"),
    "layered_cobbled_deepslate": ("Layered Cobbled Deepslate", "minecraft:block/cobbled_deepslate", "pickaxe"),
    "layered_terracotta": ("Layered Terracotta", "minecraft:block/terracotta", "pickaxe"),
    "layered_red_terracotta": ("Layered Red Terracotta", "minecraft:block/red_terracotta", "pickaxe"),
    "layered_orange_terracotta": ("Layered Orange Terracotta", "minecraft:block/orange_terracotta", "pickaxe"),
    "layered_yellow_terracotta": ("Layered Yellow Terracotta", "minecraft:block/yellow_terracotta", "pickaxe"),
    "layered_brown_terracotta": ("Layered Brown Terracotta", "minecraft:block/brown_terracotta", "pickaxe"),
    "layered_white_terracotta": ("Layered White Terracotta", "minecraft:block/white_terracotta", "pickaxe"),
    "layered_light_gray_terracotta": ("Layered Light Gray Terracotta", "minecraft:block/light_gray_terracotta", "pickaxe"),
    "layered_magenta_terracotta": ("Layered Magenta Terracotta", "minecraft:block/magenta_terracotta", "pickaxe"),
    "layered_light_blue_terracotta": ("Layered Light Blue Terracotta", "minecraft:block/light_blue_terracotta", "pickaxe"),
    "layered_lime_terracotta": ("Layered Lime Terracotta", "minecraft:block/lime_terracotta", "pickaxe"),
    "layered_pink_terracotta": ("Layered Pink Terracotta", "minecraft:block/pink_terracotta", "pickaxe"),
    "layered_gray_terracotta": ("Layered Gray Terracotta", "minecraft:block/gray_terracotta", "pickaxe"),
    "layered_cyan_terracotta": ("Layered Cyan Terracotta", "minecraft:block/cyan_terracotta", "pickaxe"),
    "layered_purple_terracotta": ("Layered Purple Terracotta", "minecraft:block/purple_terracotta", "pickaxe"),
    "layered_blue_terracotta": ("Layered Blue Terracotta", "minecraft:block/blue_terracotta", "pickaxe"),
    "layered_green_terracotta": ("Layered Green Terracotta", "minecraft:block/green_terracotta", "pickaxe"),
    "layered_black_terracotta": ("Layered Black Terracotta", "minecraft:block/black_terracotta", "pickaxe"),
    "layered_red_sand": ("Layered Red Sand", "minecraft:block/red_sand", "shovel"),
    "layered_ice": ("Layered Ice", "minecraft:block/ice", "pickaxe"),
    "layered_packed_ice": ("Layered Packed Ice", "minecraft:block/packed_ice", "pickaxe"),
    "layered_snow_block": ("Layered Snow Block", "minecraft:block/snow", "shovel"),
    "layered_coarse_dirt": ("Layered Coarse Dirt", "minecraft:block/coarse_dirt", "shovel"),
    "layered_podzol": ("Layered Podzol", "minecraft:block/podzol_top", "shovel"),
    "layered_mycelium": ("Layered Mycelium", "minecraft:block/mycelium_top", "shovel"),
    "layered_mud": ("Layered Mud", "minecraft:block/mud", "shovel"),
    "layered_clay": ("Layered Clay", "minecraft:block/clay", "shovel"),
    "layered_sandstone": ("Layered Sandstone", "minecraft:block/sandstone", "pickaxe"),
    "layered_red_sandstone": ("Layered Red Sandstone", "minecraft:block/red_sandstone", "pickaxe"),
    "layered_tuff": ("Layered Tuff", "minecraft:block/tuff", "pickaxe"),
    "layered_andesite": ("Layered Andesite", "minecraft:block/andesite", "pickaxe"),
    "layered_granite": ("Layered Granite", "minecraft:block/granite", "pickaxe"),
    "layered_diorite": ("Layered Diorite", "minecraft:block/diorite", "pickaxe"),
    "layered_netherrack": ("Layered Netherrack", "minecraft:block/netherrack", "pickaxe"),
    "layered_blackstone": ("Layered Blackstone", "minecraft:block/blackstone", "pickaxe"),
}
# Materials whose texture has transparency render translucent.
TRANSLUCENT = {"layered_ice"}
# Grass-topped materials: floor-anchored layers show a biome-tinted grass top and side overlay over
# dirt; ceiling-anchored layers hang as plain dirt (grass does not grow downward).
GRASS = {
    "layered_grass": ("Layered Grass", "shovel"),
}

root = Path(__file__).resolve().parent.parent / "src/main/resources/assets" / MOD
data = Path(__file__).resolve().parent.parent / "src/main/resources/data"


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def block_model(texture, ceiling, n, translucent=False):
    lo, hi = (MAX_LAYERS - n, MAX_LAYERS) if ceiling else (0, n)
    side_uv = [0, 0, 16, n] if ceiling else [0, MAX_LAYERS - n, 16, 16]
    face = lambda uv, cull=None: {"uv": uv, "texture": "#all", **({"cullface": cull} if cull else {})}
    return {
        "parent": "minecraft:block/block",
        **({"render_type": "minecraft:translucent"} if translucent else {}),
        "textures": {"all": texture, "particle": texture},
        "elements": [{
            "from": [0, lo, 0], "to": [16, hi, 16],
            "faces": {
                "down": face([0, 0, 16, 16], "down" if not ceiling or n == MAX_LAYERS else None),
                "up": face([0, 0, 16, 16], "up" if ceiling or n == MAX_LAYERS else None),
                "north": face(side_uv), "south": face(side_uv),
                "west": face(side_uv), "east": face(side_uv),
            },
        }],
    }


def grass_model(ceiling, n, snowy=False):
    if ceiling:
        # Grass does not grow downward, and nothing meaningfully sits "above" a ceiling
        # layer, so snowy is always false there; still generated for both snowy
        # variants because the blockstate must map every anchor/layers/snowy combo.
        return block_model("minecraft:block/dirt", ceiling, n)
    # The grass strip is the top of the side texture, so a layer shows the top n rows: grass over dirt.
    side_uv = [0, 0, 16, n]
    top_cull = "up" if n == MAX_LAYERS else None
    base = {
        "from": [0, 0, 0], "to": [16, n, 16],
        "faces": {
            "down": {"uv": [0, 0, 16, 16], "texture": "#bottom", "cullface": "down"},
            "up": {"uv": [0, 0, 16, 16], "texture": "#top",
                   **({} if snowy else {"tintindex": 0}), **({"cullface": top_cull} if top_cull else {})},
            "north": {"uv": side_uv, "texture": "#side"}, "south": {"uv": side_uv, "texture": "#side"},
            "west": {"uv": side_uv, "texture": "#side"}, "east": {"uv": side_uv, "texture": "#side"},
        },
    }
    if snowy:
        # Matches vanilla grass_block_snow: a plain frosted side texture, no overlay,
        # and the top loses its biome tint (same as vanilla's snowy grass_block model).
        return {
            "parent": "minecraft:block/block",
            "textures": {"particle": "minecraft:block/dirt", "bottom": "minecraft:block/dirt",
                         "top": "minecraft:block/grass_block_top", "side": "minecraft:block/grass_block_snow"},
            "elements": [base],
        }
    overlay = {
        "from": [0, 0, 0], "to": [16, n, 16],
        "faces": {d: {"uv": side_uv, "texture": "#overlay", "tintindex": 0} for d in ("north", "south", "west", "east")},
    }
    return {
        "parent": "minecraft:block/block",
        # The overlay texture has transparent pixels; in the solid layer they render black.
        "render_type": "minecraft:cutout_mipped",
        "textures": {"particle": "minecraft:block/dirt", "bottom": "minecraft:block/dirt",
                     "top": "minecraft:block/grass_block_top", "side": "minecraft:block/grass_block_side",
                     "overlay": "minecraft:block/grass_block_side_overlay"},
        "elements": [base, overlay],
    }


lang = {}
for name, (display, tool) in GRASS.items():
    variants = {}
    for ceiling in (False, True):
        anchor = "ceiling" if ceiling else "floor"
        for n in range(1, MAX_LAYERS + 1):
            model = f"{name}_{anchor}_{n}"
            write(root / "models/block" / f"{model}.json", grass_model(ceiling, n, snowy=False))
            variants[f"anchor={anchor},layers={n},snowy=false"] = {"model": f"{MOD}:block/{model}"}
            if ceiling:
                # Snowy never applies to a ceiling layer; reuse the plain-dirt model so the
                # blockstate still maps every anchor/layers/snowy combination the block declares.
                variants[f"anchor={anchor},layers={n},snowy=true"] = {"model": f"{MOD}:block/{model}"}
            else:
                snowy_model = f"{model}_snowy"
                write(root / "models/block" / f"{snowy_model}.json", grass_model(ceiling, n, snowy=True))
                variants[f"anchor={anchor},layers={n},snowy=true"] = {"model": f"{MOD}:block/{snowy_model}"}
    write(root / "blockstates" / f"{name}.json", {"variants": variants})
    write(root / "models/item" / f"{name}.json", {"parent": f"{MOD}:block/{name}_floor_8"})
    lang[f"block.{MOD}.{name}"] = display

for name, (display, texture, tool) in MATERIALS.items():
    variants = {}
    for ceiling in (False, True):
        anchor = "ceiling" if ceiling else "floor"
        for n in range(1, MAX_LAYERS + 1):
            model = f"{name}_{anchor}_{n}"
            write(root / "models/block" / f"{model}.json", block_model(texture, ceiling, n, name in TRANSLUCENT))
            variants[f"anchor={anchor},layers={n}"] = {"model": f"{MOD}:block/{model}"}
    write(root / "blockstates" / f"{name}.json", {"variants": variants})
    write(root / "models/item" / f"{name}.json", {"parent": f"{MOD}:block/{name}_floor_8"})
    lang[f"block.{MOD}.{name}"] = display

write(root / "lang/en_us.json", lang)
for tool in ("shovel", "pickaxe"):
    names = [n for n, spec in MATERIALS.items() if spec[2] == tool] + [n for n, spec in GRASS.items() if spec[1] == tool]
    if names:
        write(data / f"minecraft/tags/block/mineable/{tool}.json", {"values": [f"{MOD}:{n}" for n in names]})

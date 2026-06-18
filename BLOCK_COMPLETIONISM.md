# Block And Warp Completionism
 
## Quick Summary

- `146` named spreadsheet blocks are present in `CreateBlocks`.
- The spreadsheet placeholder block `???` is still not implemented.
- There is currently no warp system or warp trigger system anywhere under `src/main/java`.

## Blocks

| Block | Implemented | Ability | Requirements | Combos | Notes |
| --- | --- | --- | --- | --- | --- |
| Grass | Yes | N/A | N/A | Missing |  |
| Dirt (unchoosable) | Yes | N/A | N/A | Missing |  |
| Mycelium (unchoosable) | Yes | Missing | N/A | Missing |  |
| Podzol (unchoosable) | Yes | Missing | N/A | N/A |  |
| Farmland (unchoosable) | Yes | N/A | N/A | N/A |  |
| Sand | Yes | N/A | N/A | Missing |  |
| Sandstone | Yes | N/A | N/A | N/A |  |
| Smooth Sandstone | Yes | N/A | N/A | N/A |  |
| Cut Sandstone | Yes | N/A | N/A | N/A |  |
| Red Sand | Yes | N/A | N/A | Missing |  |
| Red Sandstone | Yes | N/A | N/A | N/A |  |
| Smooth Red Sandstone | Yes | N/A | N/A | N/A |  |
| Cut Red Sandstone | Yes | N/A | N/A | N/A |  |
| Chiseled Red Sandstone | Yes | N/A | N/A | N/A |  |
| Snow | Yes | Done | N/A | Missing |  |
| Powdered Snow | Yes | Done | N/A | N/A |  |
| Moss Block | Yes | Done | N/A | N/A |  |
| Dead Bush | Yes | Done | N/A | N/A |  |
| Cactus | Yes | Done | Missing | N/A |  |
| Red Tulip | Yes | Partial | Missing | N/A | Code increases max health only; current health is not raised. |
| Cornflower | Yes | Done | Missing | N/A |  |
| Pink Petals | Yes | Done | Missing | N/A |  |
| Torchflower | Yes | Missing | Missing | N/A |  |
| Carved Pumpkin | Yes | Done | N/A | Missing |  |
| Water | Yes | N/A | N/A | Missing |  |
| Cherry Leaves | Yes | Done | Missing | N/A |  |
| Cherry Log | Yes | Done | Missing | N/A |  |
| Jungle Log | Yes | Done | Missing | N/A |  |
| Horn Coral Block | Yes | N/A | N/A | Missing |  |
| Tube Coral Block | Yes | N/A | N/A | Missing |  |
| Bubble Coral Block | Yes | N/A | N/A | Missing |  |
| Fire Coral Block | Yes | N/A | N/A | N/A |  |
| Bubble Coral Fan | Yes | N/A | Missing | N/A |  |
| Horn Coral Fan | Yes | N/A | Missing | N/A |  |
| Tube Coral Fan | Yes | N/A | Missing | N/A |  |
| Prismarine | Yes | N/A | N/A | Missing |  |
| Netherrack | Yes | Done | N/A | Missing |  |
| Nether Gold Ore | Yes | Done | N/A | Missing |  |
| Nether Quartz Ore | Yes | Done | N/A | Missing |  |
| Crimson Nylium (unchoosable) | Yes | Missing | N/A | N/A |  |
| Warped Nylium (unchoosable) | Yes | Missing | N/A | N/A |  |
| Crimson Hyphae | Yes | Done | N/A | Missing |  |
| Warped Hyphae | Yes | Done | N/A | Missing |  |
| Soul Sand | Yes | Done | N/A | Missing |  |
| Glowstone | Yes | N/A | N/A | N/A |  |
| Candle | Yes | Missing | Missing | N/A |  |
| Magma Block | Yes | N/A | N/A | Missing |  |
| Soul Torch | Yes | Done | N/A | Missing |  |
| Soul Lantern | Yes | Done | N/A | Missing |  |
| Campfire | Yes | Done | N/A | N/A |  |
| Soul Campfire | Yes | Done | N/A | Missing |  |
| Wither Rose | Yes | Missing | Missing | N/A |  |
| Nether Bricks | Yes | N/A | N/A | Missing |  |
| Respawn Anchor | Yes | N/A | N/A | Missing |  |
| Ancient Debris | Yes | Partial | N/A | N/A | Unbreakable is enforced, but the 50-damage break effect is not reachable through normal breaking. |
| Block of Netherite | Yes | Missing | N/A | N/A |  |
| Endstone | Yes | N/A | N/A | N/A |  |
| End Crystal | Yes | Done | Missing | N/A |  |
| Dragon Egg | Yes | Done | N/A | Missing |  |
| Furnace | Yes | Done | N/A | Missing |  |
| Chest | Yes | Missing | N/A | Missing |  |
| Trapped Chest | Yes | Missing | N/A | Missing |  |
| Cauldron | Yes | N/A | N/A | Missing |  |
| Composter | Yes | N/A | N/A | Missing |  |
| Anvil | Yes | Done | N/A | N/A |  |
| Damaged Anvil | Yes | Done | N/A | N/A |  |
| Stonecutter | Yes | N/A | N/A | Missing |  |
| Loom | Yes | N/A | N/A | N/A |  |
| Cartography Table | Yes | N/A | N/A | N/A |  |
| Lectern | Yes | Partial | N/A | Missing | Code adds `+1` damage per card in hand; the sheet says `+8` per card. |
| Brewing Stand | Yes | Missing | N/A | N/A |  |
| Smithing Table | Yes | Done | N/A | N/A |  |
| Enchanting Table | Yes | Missing | N/A | N/A |  |
| Repeater | Yes | Missing | N/A | N/A |  |
| Daylight Sensor | Yes | Missing | N/A | N/A |  |
| Redstone Torch | Yes | Done | N/A | N/A |  |
| Torch | Yes | Done | N/A | N/A |  |
| Copper Torch | Yes | Partial | N/A | N/A | Code blocks the next incoming normal/direct health damage, not the entire next turn, and shield damage still applies. |
| Lightning Rod | Yes | N/A | N/A | N/A |  |
| Dispenser | Yes | Missing | N/A | N/A | Spreadsheet typo normalized from `Dspenser`. |
| Monster Spawner | Yes | Missing | N/A | Missing |  |
| Sculk Sensor | Yes | Missing | N/A | Missing |  |
| Calibrated Sculk Sensor | Yes | Missing | N/A | Missing |  |
| Sculk Shrieker | Yes | Missing | N/A | Missing |  |
| Sculk Catalyst | Yes | Missing | N/A | Missing |  |
| Sculk | Yes | Done | N/A | Missing |  |
| Chiseled Bookshelf | Yes | Missing | N/A | Missing |  |
| Bookshelf | Yes | Missing | N/A | Missing |  |
| Shulker Box | Yes | Done | N/A | Missing |  |
| Vault | Yes | Partial | N/A | N/A | Code removes the Vault itself instead of letting you choose any card from the deck. |
| Conduit | Yes | N/A | N/A | Missing |  |
| Beacon | Yes | N/A | N/A | Missing |  |
| Deepslate | Yes | N/A | N/A | Missing |  |
| Reinforced Deepslate | Yes | Done | N/A | N/A |  |
| Deepslate Bricks | Yes | Done | N/A | N/A |  |
| Deepslate Tiles | Yes | N/A | N/A | N/A |  |
| Deepslate Gold Ore | Yes | Missing | N/A | Missing |  |
| Deepslate Redstone Ore | Yes | N/A | N/A | Missing |  |
| Smooth Stone | Yes | N/A | N/A | N/A |  |
| Stone Bricks | Yes | Missing | N/A | N/A |  |
| Cracked Stone Bricks | Yes | Missing | N/A | N/A |  |
| Cobblestone | Yes | Missing | N/A | N/A |  |
| Polished Blackstone Bricks | Yes | Missing | N/A | Missing |  |
| Glass | Yes | Missing | N/A | N/A |  |
| Glass pane | Yes | Missing | N/A | N/A |  |
| Block of Coal | Yes | Missing | N/A | N/A |  |
| Block of Iron | Yes | N/A | N/A | N/A |  |
| Block of Gold | Yes | Missing | N/A | Missing |  |
| Block of Emerald | Yes | Missing | N/A | N/A |  |
| Block of Lapis Lazuli | Yes | Done | N/A | N/A |  |
| Block of Redstone | Yes | Missing | N/A | Missing |  |
| Block of Diamond | Yes | Missing | N/A | N/A |  |
| Copper Block | Yes | Missing | N/A | Missing |  |
| Chiseled Copper | Yes | Missing | N/A | Missing |  |
| Copper Grate | Yes | Missing | N/A | N/A |  |
| Copper Bulb | Yes | Missing | N/A | N/A |  |
| Copper Lantern | Yes | N/A | N/A | N/A |  |
| Obsidian | Yes | Missing | N/A | Missing |  |
| Crying Obsidian | Yes | Missing | N/A | N/A |  |
| Bedrock | Yes | Missing | N/A | N/A |  |
| Lava | Yes | Missing | N/A | Missing |  |
| TNT | Yes | Done | N/A | Missing |  |
| Pointed Dripstone | Yes | Done | N/A | Missing |  |
| Red Carpet | Yes | Partial | N/A | N/A | Code doubles tracked underneath damage only in the per-turn/derived paths, not every possible block activation. |
| Blue Carpet | Yes | Partial | N/A | N/A | Code doubles tracked underneath defence only in the per-turn/derived paths, not every possible block activation. |
| Green Carpet | Yes | Partial | N/A | N/A | Code doubles tracked underneath healing only in the per-turn/derived paths, not every possible block activation. |
| Red Bed | Yes | Missing | N/A | Missing |  |
| Cake | Yes | Missing | N/A | N/A |  |
| Mushroom Stem | Yes | Done | Missing | N/A |  |
| Cocoa Beans | Yes | N/A | Missing | N/A |  |
| Slime Block | Yes | N/A | N/A | Missing |  |
| Player Head | Yes | Missing | N/A | N/A |  |
| Creeper Head | Yes | Missing | N/A | Missing |  |
| Piglin Head | Yes | Done | N/A | N/A |  |
| Skeleton Skull | Yes | Missing | N/A | Missing |  |
| Wither Skeleton Skull | Yes | Done | N/A | N/A |  |
| Zombie Head | Yes | Missing | N/A | N/A |  |
| Dragon Head | Yes | N/A | Missing | Missing |  |
| Oak Planks | Yes | Missing | N/A | Missing |  |
| Block of Raw Iron | Yes | Done | N/A | N/A |  |
| Block of Raw Gold | Yes | Done | N/A | N/A |  |
| Block of Raw Copper | Yes | Done | N/A | N/A |  |
| ??? | No | Missing | N/A | N/A | Placeholder row only; no matching block exists in the codebase. |
| Pale Oak Log | Yes | Done | Missing | Missing |  |
| Pale Moss Block | Yes | Missing | N/A | N/A |  |
| Pale Moss Carpet (unchoosable) | Yes | Missing | N/A | N/A |  |
| Creaking Heart | Yes | N/A | N/A | Missing |  |

## Warps

There is no current warp state, warp effect handling, or warp trigger detection in the Java source right now, so every warp below is still missing both its effect logic and its combo trigger logic.

| Warp | Implemented | Effect | Trigger Combos | Notes |
| --- | --- | --- | --- | --- |
| Night Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Trial Chamber Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Village House Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| End Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Library Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Witch Hut Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Deep Dark Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Soul Sand Valley Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Blizzard Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Ocean Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Redstone Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Nether Strip Mine Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Nether Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Lush Cave Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Desert Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Strip Mine Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Bed Wars Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Swamp Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Dripstone Cave Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Nether Fortress Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Bastion Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Flower Forest Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Cave Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Mesa Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |
| Pale Garden Warp | No | Missing | Missing | No warp system or warp trigger code exists in `src/main/java` right now. |

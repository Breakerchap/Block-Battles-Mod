# Block And Warp Completionism
 
## Quick Summary

- `146` named spreadsheet blocks are present in `CreateBlocks`.
- The spreadsheet placeholder block `???` is still not implemented.
- `22` warp types now exist in `BattleWarp` / `GameLogic`, with active warp state, trigger detection, effect handling, and optional structure placement.
- Missing warp structure `.nbt` files now log a warning and do not crash the game.
- `Trial Chamber Warp` and `Witch Hut Warp` are still missing.

## Blocks

| Block | Implemented | Ability | Requirements | Combos | Notes |
| --- | --- | --- | --- | --- | --- |
| Grass | Yes | N/A | N/A | Missing |  |
| Dirt (unchoosable) | Yes | N/A | N/A | Missing |  |
| Mycelium (unchoosable) | Yes | Done | N/A | Missing |  |
| Podzol (unchoosable) | Yes | Done | N/A | N/A |  |
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
| Red Tulip | Yes | Done | Missing | N/A |  |
| Cornflower | Yes | Done | Missing | N/A |  |
| Pink Petals | Yes | Done | Missing | N/A |  |
| Torchflower | Yes | Done | Missing | N/A |  |
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
| Crimson Nylium (unchoosable) | Yes | Done | N/A | N/A |  |
| Warped Nylium (unchoosable) | Yes | Done | N/A | N/A |  |
| Crimson Hyphae | Yes | Done | N/A | Missing |  |
| Warped Hyphae | Yes | Done | N/A | Missing |  |
| Soul Sand | Yes | Done | N/A | Missing |  |
| Glowstone | Yes | N/A | N/A | N/A |  |
| Candle | Yes | Done | Missing | N/A |  |
| Magma Block | Yes | N/A | N/A | Missing |  |
| Soul Torch | Yes | Done | N/A | Missing |  |
| Soul Lantern | Yes | Done | N/A | Missing |  |
| Campfire | Yes | Done | N/A | N/A |  |
| Soul Campfire | Yes | Done | N/A | Missing |  |
| Wither Rose | Yes | Done | Missing | N/A |  |
| Nether Bricks | Yes | N/A | N/A | Missing |  |
| Respawn Anchor | Yes | N/A | N/A | Missing |  |
| Ancient Debris | Yes | Done | N/A | N/A |  |
| Block of Netherite | Yes | Done | N/A | N/A |  |
| Endstone | Yes | N/A | N/A | N/A |  |
| End Crystal | Yes | Done | Missing | N/A |  |
| Dragon Egg | Yes | Done | N/A | Missing |  |
| Furnace | Yes | Done | N/A | Missing |  |
| Chest | Yes | Done | N/A | Missing |  |
| Trapped Chest | Yes | Done | N/A | Missing | Friendly Man-made blocks are now stored persistently inside the Trapped Chest and can be inspected through its GUI. |
| Cauldron | Yes | N/A | N/A | Missing |  |
| Composter | Yes | N/A | N/A | Missing |  |
| Anvil | Yes | Done | N/A | N/A |  |
| Damaged Anvil | Yes | Done | N/A | N/A |  |
| Stonecutter | Yes | N/A | N/A | Missing |  |
| Loom | Yes | N/A | N/A | N/A |  |
| Cartography Table | Yes | N/A | N/A | N/A |  |
| Lectern | Yes | Done | N/A | Missing |  |
| Brewing Stand | Yes | Done | N/A | N/A |  |
| Smithing Table | Yes | Done | N/A | N/A |  |
| Enchanting Table | Yes | Done | N/A | N/A |  |
| Repeater | Yes | Done | N/A | N/A |  |
| Daylight Sensor | Yes | Done | N/A | N/A |  |
| Redstone Torch | Yes | Done | N/A | N/A |  |
| Torch | Yes | Done | N/A | N/A |  |
| Copper Torch | Yes | Done | N/A | N/A | Implemented in the current turn model by canceling all queued incoming damage on the next opponent turn. |
| Lightning Rod | Yes | N/A | N/A | N/A |  |
| Dispenser | Yes | Done | N/A | N/A | Spreadsheet typo normalized from `Dspenser`. |
| Monster Spawner | Yes | Done | N/A | Missing |  |
| Sculk Sensor | Yes | Done | N/A | Missing |  |
| Calibrated Sculk Sensor | Yes | Done | N/A | Missing |  |
| Sculk Shrieker | Yes | Done | N/A | Missing |  |
| Sculk Catalyst | Yes | Done | N/A | Missing |  |
| Sculk | Yes | Done | N/A | Missing |  |
| Chiseled Bookshelf | Yes | Done | N/A | Missing | Players now choose the cards through a GUI instead of receiving random ones. |
| Bookshelf | Yes | Done | N/A | Missing |  |
| Shulker Box | Yes | Done | N/A | Missing |  |
| Vault | Yes | Done | N/A | N/A | Players now choose which deck card to remove through a GUI before the reward is applied. |
| Conduit | Yes | N/A | N/A | Missing |  |
| Beacon | Yes | N/A | N/A | Missing |  |
| Deepslate | Yes | N/A | N/A | Missing |  |
| Reinforced Deepslate | Yes | Done | N/A | N/A |  |
| Deepslate Bricks | Yes | Done | N/A | N/A |  |
| Deepslate Tiles | Yes | N/A | N/A | N/A |  |
| Deepslate Gold Ore | Yes | Done | N/A | Missing |  |
| Deepslate Redstone Ore | Yes | N/A | N/A | Missing |  |
| Smooth Stone | Yes | N/A | N/A | N/A |  |
| Stone Bricks | Yes | Done | N/A | N/A |  |
| Cracked Stone Bricks | Yes | Done | N/A | N/A |  |
| Cobblestone | Yes | Done | N/A | N/A |  |
| Polished Blackstone Bricks | Yes | Done | N/A | Missing |  |
| Glass | Yes | Done | N/A | N/A |  |
| Glass pane | Yes | Done | N/A | N/A |  |
| Block of Coal | Yes | Done | N/A | N/A |  |
| Block of Iron | Yes | N/A | N/A | N/A |  |
| Block of Gold | Yes | Done | N/A | Missing |  |
| Block of Emerald | Yes | Done | N/A | N/A |  |
| Block of Lapis Lazuli | Yes | Done | N/A | N/A |  |
| Block of Redstone | Yes | Done | N/A | Missing |  |
| Block of Diamond | Yes | Done | N/A | N/A |  |
| Copper Block | Yes | Done | N/A | Missing |  |
| Chiseled Copper | Yes | Done | N/A | Missing |  |
| Copper Grate | Yes | Done | N/A | N/A |  |
| Copper Bulb | Yes | Done | N/A | N/A |  |
| Copper Lantern | Yes | N/A | N/A | N/A |  |
| Obsidian | Yes | Done | N/A | Missing |  |
| Crying Obsidian | Yes | Partial | N/A | N/A | Friendly broken Otherworldly blocks are now stored persistently and can be viewed through a GUI, but the stored effects are not yet replayed by the Crying Obsidian itself after placement. |
| Bedrock | Yes | Done | N/A | N/A |  |
| Lava | Yes | Done | N/A | Missing |  |
| TNT | Yes | Done | N/A | Missing |  |
| Pointed Dripstone | Yes | Done | N/A | Missing |  |
| Red Carpet | Yes | Done | N/A | N/A |  |
| Blue Carpet | Yes | Done | N/A | N/A |  |
| Green Carpet | Yes | Done | N/A | N/A |  |
| Red Bed | Yes | Done | N/A | Missing |  |
| Cake | Yes | Done | N/A | N/A |  |
| Mushroom Stem | Yes | Done | Missing | N/A |  |
| Cocoa Beans | Yes | N/A | Missing | N/A |  |
| Slime Block | Yes | N/A | N/A | Missing |  |
| Player Head | Yes | Done | N/A | N/A |  |
| Creeper Head | Yes | Done | N/A | Missing |  |
| Piglin Head | Yes | Done | N/A | N/A |  |
| Skeleton Skull | Yes | Done | N/A | Missing |  |
| Wither Skeleton Skull | Yes | Done | N/A | N/A |  |
| Zombie Head | Yes | Done | N/A | N/A |  |
| Dragon Head | Yes | N/A | Missing | Missing |  |
| Oak Planks | Yes | Done | N/A | Missing |  |
| Block of Raw Iron | Yes | Done | N/A | N/A |  |
| Block of Raw Gold | Yes | Done | N/A | N/A |  |
| Block of Raw Copper | Yes | Done | N/A | N/A |  |
| ??? | No | Missing | N/A | N/A | Placeholder row only; no matching block exists in the codebase. |
| Pale Oak Log | Yes | Done | Missing | Missing |  |
| Pale Moss Block | Yes | Done | N/A | N/A |  |
| Pale Moss Carpet (unchoosable) | Yes | Done | N/A | N/A |  |
| Creaking Heart | Yes | N/A | N/A | Missing |  |

## Warps

There is now a real warp system in the Java source: active warp state, trigger detection, rule application, and safe structure loading all exist. The table below tracks how closely each warp matches the spreadsheet behavior right now.

| Warp | Implemented | Effect | Trigger Combos | Notes |
| --- | --- | --- | --- | --- |
| Night Warp | Yes | Done | Done | Prevents healing, doubles mob-head effects, forces no sunlight, and recognizes the listed Grass / Creeper / Zombie / Skeleton combinations. |
| Trial Chamber Warp | No | Missing | Missing | Not present in `BattleWarp` / `GameLogic`. |
| Village House Warp | Yes | Done | Done | Heals both teams for `16` every turn, disables mob-head block effects, forces sunlight, and recognizes the Red Bed village-adjacency combos. |
| End Warp | Yes | Done | Done | Beds explode on placement, no sunlight is forced, and after the warp survives for `3` rounds the lower-health team dies, with ties going against the non-starter. |
| Library Warp | Yes | Done | Done | Each turn now draws the entire remaining deck, refilling first when empty, and the bookshelf / lectern / candle combos are detected. |
| Witch Hut Warp | No | Missing | Missing | Not present in `BattleWarp` / `GameLogic`. |
| Deep Dark Warp | Partial | Partial | Partial | Active warp, no sunlight, and deck-loss damage exist, but it auto-removes a random deck card instead of offering a choice, and the adjacency rule is broader than the sheet. |
| Soul Sand Valley Warp | Yes | Done | Done | Disables per-turn blocks, forces no sunlight, and routes damage through direct-health damage. |
| Blizzard Warp | Yes | Done | Done | Turn-by-turn draw/place scaling, no sunlight, and Water-to-Ice conversion are implemented. |
| Ocean Warp | Partial | Partial | Partial | Fire cleanup, sunlight, farmland conversion, and explosion suppression exist, but the board conversion logic is only applied to tracked/changed positions rather than the whole world. |
| Redstone Warp | Yes | Done | Done | Blocks placed during the warp are activated twice. |
| Nether Strip Mine Warp | Partial | Done | Partial | Lava scaling, Water/Powdered Snow removal, Coral suppression, bed explosions, extra draw, no sunlight, and universal breakability exist; trigger matching is broader than the sheet. |
| Nether Warp | Yes | Done | Done | Lava scaling, Water/Powdered Snow removal, Coral suppression, bed explosions, and no sunlight are implemented. |
| Lush Cave Warp | Yes | Done | Done | Healing now damages the opponent for the same amount. |
| Desert Warp | Partial | Partial | Partial | The low-defence damage rule exists, but it is applied at each player's turn start, and the combo matching is only implemented for the currently modeled sandstone set. |
| Strip Mine Warp | Partial | Done | Partial | Extra draw and universal breakability exist; trigger matching is implemented with a broader adjacency rule. |
| Bed Wars Warp | Partial | Done | Partial | End Stone, Oak Planks, Obsidian, Glass, and Red Bed warp effects all exist, but the trigger logic is a simplified local pattern check. |
| Swamp Warp | Partial | Done | Missing | The active-player damage effect exists, but no trigger combo was provided/implemented so it cannot currently activate automatically. |
| Dripstone Cave Warp | Yes | Done | Done | All blocks become breakable and all damage is doubled. |
| Nether Fortress Warp | Partial | Partial | Done | No sunlight, Nether Bricks unbreakability, lava scaling, Water/Powdered Snow removal, Coral suppression, and bed explosions exist, but the Monster Spawner / Nether Bricks stat adjustments are only partially matched. |
| Bastion Warp | Yes | Done | Done | Shield damage is doubled and blocks next to gold have doubled effects. |
| Flower Forest Warp | Yes | Done | Done | Both teams gain and lose `+70` max health on warp enter/exit, and the health loss can kill. |
| Cave Warp | Yes | Done | Done | Shield damage is halved and all blocks become breakable. |
| Mesa Warp | Partial | Done | Partial | Defence gains/losses are doubled, but trigger matching is implemented with a simplified red-sand / red-sandstone neighborhood rule. |
| Pale Garden Warp | Yes | Done | Done | Healing is inverted, and Pale Oak / Creaking Heart trigger detection exists. |

# Block And Warp Completionism
 
## Quick Summary

- `146` named spreadsheet blocks are present in `CreateBlocks`.
- The spreadsheet placeholder block `???` is intentionally omitted because it does not map to any real block ID.
- `24` warp types now exist in `BattleWarp` / `GameLogic`, with active warp state, trigger detection, effect handling, and optional structure placement.
- Missing warp structure `.nbt` files now log a warning and do not crash the game.
- `Trial Chamber Warp` still lacks a defined board effect in the local notes, while `Witch Hut Warp` now has partial round-effect support.

## Blocks

| Block | Implemented | Ability | Requirements | Combos | Notes |
| --- | --- | --- | --- | --- | --- |
| Grass | Yes | N/A | N/A | Done | Water-to-Farmland, Composter-to-Podzol, Mushroom Stem-to-Mycelium, and the Flower Forest trigger are all implemented. |
| Dirt (unchoosable) | Yes | N/A | N/A | Done | Adjacent Grass, Podzol, and Mycelium now convert Dirt into the matching block. |
| Mycelium (unchoosable) | Yes | Done | N/A | Done | Its Grass-spread combo is implemented through the board-state combo resolver. |
| Podzol (unchoosable) | Yes | Done | N/A | N/A |  |
| Farmland (unchoosable) | Yes | N/A | N/A | N/A |  |
| Sand | Yes | N/A | N/A | Done | Its Desert trigger combos are implemented. |
| Sandstone | Yes | N/A | N/A | N/A |  |
| Smooth Sandstone | Yes | N/A | N/A | N/A |  |
| Cut Sandstone | Yes | N/A | N/A | N/A |  |
| Red Sand | Yes | N/A | N/A | Done | Its Mesa trigger combos are implemented. |
| Red Sandstone | Yes | N/A | N/A | N/A |  |
| Smooth Red Sandstone | Yes | N/A | N/A | N/A |  |
| Cut Red Sandstone | Yes | N/A | N/A | N/A |  |
| Chiseled Red Sandstone | Yes | N/A | N/A | N/A |  |
| Snow | Yes | Done | N/A | Done | Blizzard triggers exist, and the Carved Pumpkin-on-Snow combo now deals the burst damage and clears nearby mob heads. |
| Powdered Snow | Yes | Done | N/A | N/A |  |
| Moss Block | Yes | Done | N/A | N/A |  |
| Dead Bush | Yes | Done | N/A | N/A |  |
| Cactus | Yes | Done | Done | N/A |  |
| Red Tulip | Yes | Done | Done | N/A |  |
| Cornflower | Yes | Done | Done | N/A |  |
| Pink Petals | Yes | Done | Done | N/A |  |
| Torchflower | Yes | Done | Done | N/A |  |
| Carved Pumpkin | Yes | Done | N/A | Done | Iron-underneath and Snow-underneath combo behavior are both implemented. |
| Water | Yes | N/A | N/A | Done | Ocean Warp triggering and Lava-to-Obsidian board conversion are implemented. |
| Cherry Leaves | Yes | Done | Done | N/A |  |
| Cherry Log | Yes | Done | Done | N/A |  |
| Jungle Log | Yes | Done | Done | N/A |  |
| Horn Coral Block | Yes | N/A | N/A | Done | Gains its Water-adjacent per-turn bonus and participates in Ocean Warp detection. |
| Tube Coral Block | Yes | N/A | N/A | Done | Gains its Water-adjacent per-turn bonus and participates in Ocean Warp detection. |
| Bubble Coral Block | Yes | N/A | N/A | Done | Gains its Water-adjacent per-turn bonus and participates in Ocean Warp detection. |
| Fire Coral Block | Yes | N/A | N/A | N/A |  |
| Bubble Coral Fan | Yes | N/A | Done | N/A |  |
| Horn Coral Fan | Yes | N/A | Done | N/A |  |
| Tube Coral Fan | Yes | N/A | Done | N/A |  |
| Prismarine | Yes | N/A | N/A | Done | Its Water-adjacent Ocean Warp trigger is implemented. |
| Netherrack | Yes | Done | N/A | Done | The Lava / Magma Block / Campfire Nether Warp triggers are implemented. |
| Nether Gold Ore | Yes | Done | N/A | Done | Its Bastion and Nether Strip Mine trigger combos now match the listed ore combinations. |
| Nether Quartz Ore | Yes | Done | N/A | Done | Its Nether Strip Mine trigger combos now match the listed ore combinations. |
| Crimson Nylium (unchoosable) | Yes | Done | N/A | N/A |  |
| Warped Nylium (unchoosable) | Yes | Done | N/A | N/A |  |
| Crimson Hyphae | Yes | Done | N/A | Done | Turns Netherrack beneath it into Crimson Nylium. |
| Warped Hyphae | Yes | Done | N/A | Done | Turns Netherrack beneath it into Warped Nylium. |
| Soul Sand | Yes | Done | N/A | Done | Soul Sand Valley Warp trigger combos are implemented. |
| Glowstone | Yes | N/A | N/A | N/A |  |
| Candle | Yes | Done | Done | N/A |  |
| Magma Block | Yes | N/A | N/A | Done | The Water-on-top healing combo is implemented. |
| Soul Torch | Yes | Done | N/A | Done | Soul Sand Valley Warp trigger is implemented. |
| Soul Lantern | Yes | Done | N/A | Done | Soul Sand Valley Warp trigger is implemented. |
| Campfire | Yes | Done | N/A | N/A |  |
| Soul Campfire | Yes | Done | N/A | Done | Soul Sand Valley Warp trigger is implemented. |
| Wither Rose | Yes | Done | Done | N/A |  |
| Nether Bricks | Yes | N/A | N/A | Done | Nether Fortress Warp trigger combos are implemented. |
| Respawn Anchor | Yes | N/A | N/A | Done | The 2-Glowstone combo now revives at half health in Nether-family warps and explodes outside them for `5` to both teams, matching the local sheet behavior. |
| Ancient Debris | Yes | Done | N/A | N/A |  |
| Block of Netherite | Yes | Done | N/A | N/A |  |
| Endstone | Yes | N/A | N/A | N/A |  |
| End Crystal | Yes | Done | Done | N/A |  |
| Dragon Egg | Yes | Done | N/A | Done | The Endstone-underneath End Warp trigger is implemented. |
| Furnace | Yes | Done | N/A | Done | Adjacent Block of Coal now adds `+5` defence each turn and visually turns the block into a Blast Furnace, while adjacent Campfire gives `+6` healing each turn and turns it into a Smoker. |
| Chest | Yes | Done | N/A | Done | Adjacent friendly Man-made blocks are broken and their effects immediately activate for you. |
| Trapped Chest | Yes | Done | N/A | Done | Friendly Man-made blocks are now stored persistently inside the Trapped Chest, suppressed, and can be inspected through its GUI. |
| Cauldron | Yes | N/A | N/A | Done | Water above heals `4` each turn, Lava above doubles Lava damage, Powdered Snow above doubles its delayed hit, and the Witch Hut trigger combo is implemented. |
| Composter | Yes | Done | N/A | Done | Adjacent Natural blocks are absorbed, stored with GUI inspection, and their stored effects now replay from the Composter each turn. |
| Anvil | Yes | Done | N/A | N/A |  |
| Damaged Anvil | Yes | Done | N/A | N/A |  |
| Stonecutter | Yes | Done | N/A | Done | Adjacent Cave blocks are absorbed, stored with GUI inspection, and their stored effects now replay from the Stonecutter each turn. |
| Loom | Yes | N/A | N/A | N/A |  |
| Cartography Table | Yes | N/A | N/A | N/A |  |
| Lectern | Yes | Done | N/A | Done | Its Library Warp adjacency trigger is implemented. |
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
| Monster Spawner | Yes | Done | N/A | Done | The Piglin Head Bastion Warp trigger is implemented. |
| Sculk Sensor | Yes | Done | N/A | Done | Its Deep Dark trigger combos are implemented. |
| Calibrated Sculk Sensor | Yes | Done | N/A | Done | Its Deep Dark trigger combos are implemented. |
| Sculk Shrieker | Yes | Done | N/A | Done | Its Deep Dark trigger combos are implemented. |
| Sculk Catalyst | Yes | Done | N/A | Done | Its Deep Dark trigger combos are implemented. |
| Sculk | Yes | Done | N/A | Done | Its Deep Dark trigger combos are implemented. |
| Chiseled Bookshelf | Yes | Done | N/A | Done | Players now choose the cards through a GUI instead of receiving random ones, and its Library Warp combos are implemented. |
| Bookshelf | Yes | Done | N/A | Done | Its Library Warp triggers, Village House adjacency, and Enchanting Table discount combo are all implemented. |
| Shulker Box | Yes | Done | N/A | Done | The Endstone-underneath End Warp trigger is implemented. |
| Vault | Yes | Done | N/A | N/A | Players now choose which deck card to remove through a GUI before the reward is applied. |
| Conduit | Yes | N/A | N/A | Done | The Water + Prismarine revive combo is implemented and currently consumes the Conduit when it revives its owner at half health. |
| Beacon | Yes | N/A | N/A | Done | Gold, Emerald, Iron, and Netherite support combos are all implemented, and Iron support now reduces defence-damage too. |
| Deepslate | Yes | N/A | N/A | Done | Its Lush Cave and Torch-on-top Cave Warp trigger combos are implemented. |
| Reinforced Deepslate | Yes | Done | N/A | N/A |  |
| Deepslate Bricks | Yes | Done | N/A | N/A |  |
| Deepslate Tiles | Yes | N/A | N/A | N/A |  |
| Deepslate Gold Ore | Yes | Done | N/A | Done | Its listed Strip Mine, Cave, and Lush Cave trigger combos are recognized. |
| Deepslate Redstone Ore | Yes | N/A | N/A | Done | Its listed Strip Mine, Cave, and Lush Cave trigger combos are recognized. |
| Smooth Stone | Yes | N/A | N/A | N/A |  |
| Stone Bricks | Yes | Done | N/A | N/A |  |
| Cracked Stone Bricks | Yes | Done | N/A | N/A |  |
| Cobblestone | Yes | Done | N/A | N/A |  |
| Polished Blackstone Bricks | Yes | Done | N/A | Done | Piglin Head and Gold Block Bastion triggers are both implemented. |
| Glass | Yes | Done | N/A | N/A |  |
| Glass pane | Yes | Done | N/A | N/A |  |
| Block of Coal | Yes | Done | N/A | N/A |  |
| Block of Iron | Yes | N/A | N/A | N/A |  |
| Block of Gold | Yes | Done | N/A | Done | The Player Head adjacency combo that turns nearby blocks into gold is implemented. |
| Block of Emerald | Yes | Done | N/A | N/A |  |
| Block of Lapis Lazuli | Yes | Done | N/A | N/A |  |
| Block of Redstone | Yes | Done | N/A | Done | Redstone Torch and Repeater on top both trigger Redstone Warp. |
| Block of Diamond | Yes | Done | N/A | N/A |  |
| Copper Block | Yes | Done | N/A | Done | Vault on top now triggers Trial Chamber Warp. |
| Chiseled Copper | Yes | Done | N/A | Done | The Honeycomb Block adjacency combo that stops it breaking on activation is implemented. |
| Copper Grate | Yes | Done | N/A | N/A |  |
| Copper Bulb | Yes | Done | N/A | N/A |  |
| Copper Lantern | Yes | N/A | N/A | N/A |  |
| Obsidian | Yes | Done | N/A | Done | The Lightning Rod + adjacent Redstone board-wipe combo now triggers from the Obsidian setup itself. |
| Crying Obsidian | Yes | Done | N/A | N/A | Friendly broken Otherworldly blocks are stored persistently, visible through a GUI, and their stored effects now replay from the Crying Obsidian each turn. |
| Bedrock | Yes | Done | N/A | N/A |  |
| Lava | Yes | Done | N/A | Done | The Water-adjacent conversion into Obsidian is implemented. |
| TNT | Yes | Done | N/A | Done | Lightning Rod above now enlarges the blast, and adjacent Redstone Block upgrades it into a tracked-block board wipe. |
| Pointed Dripstone | Yes | Done | N/A | Done | Its Dripstone Cave Warp trigger combos are implemented. |
| Red Carpet | Yes | Done | N/A | N/A |  |
| Blue Carpet | Yes | Done | N/A | N/A |  |
| Green Carpet | Yes | Done | N/A | N/A |  |
| Red Bed | Yes | Done | N/A | Done | Village House and Bed Wars trigger combos now match the listed local Red Bed patterns. |
| Cake | Yes | Done | N/A | N/A |  |
| Mushroom Stem | Yes | Done | Done | N/A |  |
| Cocoa Beans | Yes | N/A | Done | N/A |  |
| Slime Block | Yes | N/A | N/A | Done | Adjacent Slime Block bonus damage is implemented. |
| Player Head | Yes | Done | N/A | N/A |  |
| Creeper Head | Yes | Done | N/A | Done | Its Night Warp combo is implemented, and Lightning Rod above now doubles the explosion strength while adjacent Redstone Block upgrades it into a tracked-block board wipe. |
| Piglin Head | Yes | Done | N/A | N/A |  |
| Skeleton Skull | Yes | Done | N/A | Done | Its Night Warp combos are implemented. |
| Wither Skeleton Skull | Yes | Done | N/A | N/A |  |
| Zombie Head | Yes | Done | N/A | N/A |  |
| Dragon Head | Yes | N/A | Done | Done | The Dragon Egg support requirement and Endstone-underneath End Warp trigger are both implemented. |
| Oak Planks | Yes | Done | N/A | Done | Its listed Bed Wars trigger combo is implemented. |
| Block of Raw Iron | Yes | Done | N/A | N/A |  |
| Block of Raw Gold | Yes | Done | N/A | N/A |  |
| Block of Raw Copper | Yes | Done | N/A | N/A |  |
| Pale Oak Log | Yes | Done | Done | Done | The Grass-variant placement requirement and Creaking Heart growth combo are both implemented. |
| Pale Moss Block | Yes | Done | N/A | N/A |  |
| Pale Moss Carpet (unchoosable) | Yes | Done | N/A | N/A |  |
| Creaking Heart | Yes | N/A | N/A | Done | The Pale Garden Warp trigger is implemented. |

## Warps

There is now a real warp system in the Java source: active warp state, trigger detection, rule application, and safe structure loading all exist. The table below tracks how closely each warp matches the spreadsheet behavior right now.

| Warp | Implemented | Effect | Trigger Combos | Notes |
| --- | --- | --- | --- | --- |
| Night Warp | Yes | Done | Done | Prevents healing, doubles mob-head effects, forces no sunlight, and recognizes the listed Grass / Creeper / Zombie / Skeleton combinations. |
| Trial Chamber Warp | Partial | Partial | Done | Trigger detection for `Vault` on `Copper Block` exists, but the local sheet data still never defines a board effect beyond the trigger itself. |
| Village House Warp | Yes | Done | Done | Heals both teams for `16` every turn, disables mob-head block effects, forces sunlight, and recognizes the Red Bed village-adjacency combos. |
| End Warp | Yes | Done | Done | Beds explode on placement, no sunlight is forced, and after the warp survives for `3` rounds the lower-health team dies, with ties going against the non-starter. |
| Library Warp | Yes | Done | Done | Each turn now draws the entire remaining deck, refilling first when empty, and the bookshelf / lectern / candle combos are detected. |
| Witch Hut Warp | Yes | Partial | Done | The listed trigger pairs now work, and the round system now supports extra draw, double damage, no damage, extra placement, revive-at-10, deck removal, deck swapping, and no-sunlight effects; the local `5 seconds or skip` effect is still not implemented/rolled. |
| Deep Dark Warp | Yes | Done | Done | Active warp, no sunlight, and the listed cross-type Deep Dark trigger pairs are implemented. Online teams get a removal-choice GUI, while no-player/offline cases correctly fall back to the `30` damage penalty. |
| Soul Sand Valley Warp | Yes | Done | Done | Disables per-turn blocks, forces no sunlight, and routes damage through direct-health damage. |
| Blizzard Warp | Yes | Done | Done | Turn-by-turn draw/place scaling, no sunlight, and Water-to-Ice conversion are implemented. |
| Ocean Warp | Yes | Partial | Done | Fire cleanup, sunlight, farmland conversion, and explosion suppression exist, but the board conversion logic is still applied to tracked/active positions rather than a whole-world sweep. |
| Redstone Warp | Yes | Done | Done | Blocks placed during the warp are activated twice. |
| Nether Strip Mine Warp | Yes | Done | Done | Lava scaling, Water/Powdered Snow removal, Coral suppression, bed explosions, extra draw, no sunlight, universal breakability, and the listed ore trigger combos are implemented. |
| Nether Warp | Yes | Done | Done | Lava scaling, Water/Powdered Snow removal, Coral suppression, bed explosions, and no sunlight are implemented. |
| Lush Cave Warp | Yes | Done | Done | Healing now damages the opponent for the same amount. |
| Desert Warp | Yes | Done | Done | The low-defence damage rule is applied at turn start, which matches the current turn-resolution model. |
| Strip Mine Warp | Yes | Done | Done | Extra draw, universal breakability, and the listed Deepslate-ore trigger combos are implemented. |
| Bed Wars Warp | Yes | Done | Done | End Stone, Oak Planks, Obsidian, Glass, and Red Bed warp effects all exist, and the trigger logic now matches the listed local patterns. |
| Swamp Warp | Partial | Done | Partial | The active-player damage effect exists, but the local notes still do not provide any trigger combo to implement automatically. |
| Dripstone Cave Warp | Yes | Done | Done | All blocks become breakable and all damage is doubled. |
| Nether Fortress Warp | Yes | Done | Done | No sunlight, Nether Bricks unbreakability, lava scaling, Water/Powdered Snow removal, Coral suppression, bed explosions, and the Monster Spawner scaling are all implemented; the listed Nether Bricks defence boost is functionally a no-op because the block's defence stat is `0`. |
| Bastion Warp | Yes | Done | Done | Shield damage is doubled and blocks next to gold have doubled effects. |
| Flower Forest Warp | Yes | Done | Done | Both teams gain and lose `+70` max health on warp enter/exit, and the health loss can kill. |
| Cave Warp | Yes | Done | Done | Shield damage is halved and all blocks become breakable. |
| Mesa Warp | Yes | Done | Done | Defence gains/losses are doubled, and the listed Red Sand / Red Sandstone trigger combos are implemented. |
| Pale Garden Warp | Yes | Done | Done | Healing is inverted, and Pale Oak / Creaking Heart trigger detection exists. |

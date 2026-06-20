# Block Battles

Block Battles is a Fabric mod for Minecraft `26.2` that turns normal block placement into a turn-based PvP card game.

When a battle is running, players join the Red or Blue team, build a shared 12-card deck for their team, draw hands of blocks, and place those blocks to deal damage, gain shield, heal, trigger abilities, and activate warps. When a battle is not running, the world behaves like normal Minecraft and you can still build decks or sort teams ahead of time.

## Current State

- Built for Minecraft `26.2`.
- Team-based battle flow with Red and Blue teams.
- Shared team deck builder GUI.
- In-game encyclopedia GUI for blocks and warps, including combos and implementation status.
- Turn-restricted block placement while a battle is active.
- Team-coloured outlines on tracked battle blocks.
- Active warp system with structure loading and safe missing-structure warnings.
- Detailed implementation tracking in [BLOCK_COMPLETIONISM.md](BLOCK_COMPLETIONISM.md).

## Gameplay Loop

1. Run `/BB reset` to reset the battle state and ensure the Red and Blue scoreboard teams exist.
2. Join a team with `/BB join red` or `/BB join blue`.
3. Open the shared team deck builder with `/BB buildDeck` and pick exactly `12` battle blocks.
4. Start the game with `/BB start`.
5. Each team begins with `200` health and `0` shield.
6. On a normal turn, the active team draws `3` cards, places `1`, then the turn passes.
7. If a draw pile runs out, it refills from that team's saved deck.
8. Warps, block abilities, and debug tools can change the normal flow.

## Commands

### Main Commands

- `/BB join red`
- `/BB join blue`
- `/BB reset`
- `/BB start`
- `/BB end`
- `/BB buildDeck`
- `/BB encyclopedia`
- `/BB encyclopedia give`
- `/BB showScoreboards`

### Debug Commands

- `/BB debug skipTurn`
- `/BB debug setTurn red`
- `/BB debug setTurn blue`
- `/BB debug redrawHand`
- `/BB debug showState`
- `/BB debug setStat red health <value>`
- `/BB debug setStat red maxHealth <value>`
- `/BB debug setStat red shield <value>`
- `/BB debug setStat blue health <value>`
- `/BB debug setStat blue maxHealth <value>`
- `/BB debug setStat blue shield <value>`
- `/BB debug drawBlock red <block>`
- `/BB debug drawBlock blue <block>`
- `/BB debug drawCards red <amount>`
- `/BB debug drawCards blue <amount>`
- `/BB debug clearHand red`
- `/BB debug clearHand blue`
- `/BB debug refillDrawPile red`
- `/BB debug refillDrawPile blue`

`drawBlock` accepts names like `dirt`, `minecraft:dirt`, or `Red Tulip`.

Most debug commands require the battle to be running first.

## Deck Builder

The deck builder GUI shows every registered battle block and its stats, description, classification, and requirement text in item lore.

- Decks are shared per team, not per player.
- A deck must contain exactly `12` selectable battle blocks to save.
- The selected deck is pinned at the top of the GUI.
- Closing the menu without `12` selected cards leaves the previous saved deck unchanged.

## Encyclopedia

Use `/BB encyclopedia` to open a paged in-game reference for both battle blocks and warps.

- The `Blocks` tab shows each block's real item icon, classification, deck status, ability text, requirements, and coloured stats.
- The `Blocks` tab also shows combo text and tracker status for implementation, abilities, requirements, and combos.
- The `Warps` tab shows each warp's icon, sunlight rule, structure id, effect summary, combo summary, and implementation status.
- The encyclopedia can be opened even when a battle is not running.
- `/BB encyclopedia give` gives you a physical encyclopedia book item that opens the same menu on right-click.
- You can also give it directly with `/give @s blockbattles:encyclopedia`.

## Warps

The mod has an active warp system in code now, including trigger detection, gameplay effects, and optional structure placement.

- Missing warp `.nbt` structures only log a warning and do not crash the game.
- Recently added warps include `Village House Warp`, `End Warp`, and `Library Warp`.
- Some planned warps and block behaviors are still unfinished.

For the detailed block-by-block and warp-by-warp implementation matrix, see [BLOCK_COMPLETIONISM.md](BLOCK_COMPLETIONISM.md).

## Development

Useful commands:

- `.\gradlew runClient`
- `.\gradlew test`

Main places to look in the codebase:

- `src/main/java/com/remy/blockbattles/BlockBattlesMod.java`
- `src/main/java/com/remy/blockbattles/game/logic/GameLogic.java`
- `src/main/java/com/remy/blockbattles/game/gui/BattleDeckBuilderMenu.java`
- `BLOCK_COMPLETIONISM.md`

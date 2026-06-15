package com.remy.blockbattles.game.logic;

import java.util.Collections;
import java.util.List;

import com.remy.blockbattles.game.blocks.BattleBlock;

public class DeckManager {
  private static final int DEFAULT_HAND_SIZE = 3;

  public List<BattleBlock> dealCards(BattleTeam team) {
    return dealCards(team, DEFAULT_HAND_SIZE);
  }

  public List<BattleBlock> dealCards(BattleTeam team, int targetHandSize) {
    List<BattleBlock> drawPile = team.getDrawPile();
    List<BattleBlock> hand = team.getHand();

    Collections.shuffle(drawPile);

    while (hand.size() < targetHandSize) {
      if (drawPile.isEmpty()) {
        team.refillDrawPile();
        Collections.shuffle(drawPile);
      }

      if (drawPile.isEmpty()) {
        break;
      }

      hand.add(drawPile.remove(0));
    }

    return hand;
  }
}

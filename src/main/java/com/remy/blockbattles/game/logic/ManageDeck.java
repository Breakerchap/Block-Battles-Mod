package com.remy.blockbattles.game.logic;

import java.util.ArrayList;
import java.util.Collections;

import com.remy.blockbattles.game.blocks.BattleBlock;

public class ManageDeck {

  ArrayList<BattleBlock> dealCards(
      ArrayList<BattleBlock> deck,
      ArrayList<BattleBlock> unusedCards,
      ArrayList<BattleBlock> hand) {

    Collections.shuffle(unusedCards);

    while (hand.size() < 3) {
      if (unusedCards.size() == 0) {
        unusedCards.addAll(deck);
        Collections.shuffle(unusedCards);
      }

      hand.add(unusedCards.get(0));
      unusedCards.remove(0);
    }

    return hand;
  }

  public static void main(String args[]) {
    TeamState teamState = new TeamState();
    ManageDeck deckManager = new ManageDeck();

    deckManager.dealCards(
        teamState.redTeam.deck,
        teamState.redTeam.deck,
        teamState.redTeam.hand);

    deckManager.dealCards(
        teamState.blueTeam.deck,
        teamState.blueTeam.deck,
        teamState.blueTeam.hand);

    System.out.println(teamState.redTeam.hand);
    System.out.println(teamState.blueTeam.hand);
  }
}
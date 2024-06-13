package fivecarddraw;

import java.util.ArrayList;

/**
 * This is a utility class that can be used to assign ranks
 * to poker hands containing up to five cards. 
 */

public class PokerRank {

    
    public static void main(String[] args) {
        System.out.println("(No cards)");
        System.out.println(new PokerRank().getRank());
        System.out.println(new PokerRank().getDescription());
        System.out.println(new PokerRank().getLongDescription());
        System.out.println("\n");
        new PokerRank(new PokerCard(12, 3)).getRank();
        new PokerRank(new PokerCard(2, 2), new PokerCard(12, 1)).getRank();
        new PokerRank(new PokerCard(12, 3), new PokerCard(12, 1)).getRank();
        new PokerRank(new PokerCard(12, 3), new PokerCard(2, 2), new PokerCard(12, 1)).getRank();
        new PokerRank(new PokerCard(12, 3), new PokerCard(12, 2), new PokerCard(12, 1)).getRank();
        new PokerRank(new PokerCard(12, 3), new PokerCard(2, 2), new PokerCard(2, 3), new PokerCard(12, 1)).getRank();
        new PokerRank(new PokerCard(12, 3), new PokerCard(2, 2), new PokerCard(12, 1), new PokerCard(12, 2)).getRank();
        new PokerRank(new PokerCard(12, 3), new PokerCard(2, 2), new PokerCard(12, 1), new PokerCard(12, 2)).getRank();
        new PokerRank(new PokerCard(12, 3), new PokerCard(12, 1), new PokerCard(12, 2), new PokerCard(2, 3)).getRank();
        new PokerRank(new PokerCard(12, 3), new PokerCard(2, 2), new PokerCard(12, 1), new PokerCard(12, 2),
                new PokerCard(12, 0)).getRank();
        new PokerRank(new PokerCard(3, 1), new PokerCard(2, 1), new PokerCard(5, 1), new PokerCard(6, 2),
                new PokerCard(4, 3)).getRank();
        new PokerRank(new PokerCard(3, 1), new PokerCard(2, 1), new PokerCard(5, 1), new PokerCard(14, 2),
                new PokerCard(4, 3)).getRank();
        new PokerRank(new PokerCard(3, 1), new PokerCard(2, 1), new PokerCard(5, 1), new PokerCard(14, 1),
                new PokerCard(4, 1)).getRank();
        new PokerRank(new PokerCard(12, 1), new PokerCard(10, 1), new PokerCard(13, 1), new PokerCard(14, 2),
                new PokerCard(11, 3)).getRank();
        new PokerRank(new PokerCard(12, 1), new PokerCard(10, 1), new PokerCard(13, 1), new PokerCard(14, 1),
                new PokerCard(11, 1)).getRank();
        new PokerRank(new PokerCard(12, 1), new PokerCard(10, 1), new PokerCard(13, 1), new PokerCard(4, 1),
                new PokerCard(11, 1)).getRank();
        new PokerRank(new PokerCard(12, 1), new PokerCard(10, 2), new PokerCard(13, 1), new PokerCard(4, 1),
                new PokerCard(11, 1)).getRank();
        new PokerRank(new PokerCard(10, 2), new PokerCard(10, 1), new PokerCard(13, 1), new PokerCard(14, 1),
                new PokerCard(13, 3)).getRank();
        new PokerRank(new PokerCard(10, 2), new PokerCard(10, 1), new PokerCard(13, 1), new PokerCard(13, 1),
                new PokerCard(10, 3)).getRank();
    }

    public static final int NOTHING = 0; // Codes for the basic types of poker hand.
    public static final int PAIR = 1;
    public static final int TWO_PAIR = 2;
    public static final int TRIPLE = 3;
    public static final int STRAIGHT = 4;
    public static final int FLUSH = 5;
    public static final int FULL_HOUSE = 6;
    public static final int FOUR_OF_A_KIND = 7;
    public static final int STRAIGHT_FLUSH = 8;
    public static final int ROYAL_FLUSH = 9;

    private ArrayList<PokerCard> cards = new ArrayList<PokerCard>(); // The cards in this hand.
    private int rank = -1;
    private String description;
    private String longDescription;

    public PokerRank(PokerCard... card) {
        if (card != null) {
            for (PokerCard c : card)
                add(c);
        }
    }

    public PokerRank(ArrayList<PokerCard> cards) {
        if (cards != null) {
            for (PokerCard c : cards)
                add(c);
        }
    }

    public void add(PokerCard card) {
        if (card == null)
            throw new IllegalArgumentException("Cards can't be null for class PokerRank");
        if (card.getSuit() == PokerCard.JOKER)
            throw new IllegalArgumentException("Class PokerRank does not support jokers.");
        if (cards.size() == 5)
            throw new IllegalArgumentException("PokerRank does not support hands with more than five cards.");
        cards.add(card);
        rank = -1;
    }

    public int getRank() {
        if (rank == -1)
            computeRank();
        return rank;
    }

    public String getDescription() {
        if (rank == -1)
            computeRank();
        return description;
    }

    public String getLongDescription() {
        if (rank == -1)
            computeRank();
        return longDescription;
    }

    public int getHandType() {
        if (rank == -1)
            computeRank();
        return rank >> 20;
    }

    public String getHandTypeAsString() {
        if (cards.size() == 0)
            return "Empty Hand";
        int type = getHandType();
        if (type == PAIR)
            return "Pair";
        if (type == TWO_PAIR)
            return "Two pairs";
        if (type == TRIPLE)
            return "Triple";
        if (type == STRAIGHT)
            return "Straight";
        if (type == FLUSH)
            return "Flush";
        if (type == FULL_HOUSE)
            return "Full House";
        if (type == FOUR_OF_A_KIND)
            return "Four of a kind";
        if (type == STRAIGHT_FLUSH)
            return "Straight Flush";
        if (type == ROYAL_FLUSH)
            return "Royal Flush";
        return "Nothing";
    }

    public ArrayList<PokerCard> getCards() {
        if (rank == -1)
            computeRank();
        return new ArrayList<PokerCard>(cards);
    }

    public String toString() {
        return getDescription();
    }

    private String valueName(PokerCard c) {
        switch (c.getValue()) {
            case 2:
                return "Two";
            case 3:
                return "Three";
            case 4:
                return "Four";
            case 5:
                return "Five";
            case 6:
                return "Six";
            case 7:
                return "Seven";
            case 8:
                return "Eight";
            case 9:
                return "Nine";
            case 10:
                return "Ten";
            case 11:
                return "Jack";
            case 12:
                return "Queen";
            case 13:
                return "King";
            default:
                return "Ace";
        }
    }

    private String pluralValueName(PokerCard c) {
        if (c.getValue() == 6)
            return "Sixes";
        else
            return valueName(c) + "s";
    }

    private String cardValueNames() {
        StringBuffer s = new StringBuffer(valueName(cards.get(0)));
        for (int i = 1; i < cards.size(); i++) {
            s.append(',');
            s.append(valueName(cards.get(i)));
        }
        return s.toString();
    }

    private void computeRank() {
        if (cards.size() == 0) {
            rank = 0;
            description = longDescription = "Empty Hand";
            return;
        }

        ArrayList<PokerCard> newCards = new ArrayList<PokerCard>();
        while (cards.size() > 0) {
            PokerCard maxCard = cards.get(0);
            for (int i = 1; i < cards.size(); i++)
                if (cards.get(i).getValue() > maxCard.getValue() ||
                        cards.get(i).getValue() == maxCard.getValue() && cards.get(i).getSuit() > maxCard.getSuit())
                    maxCard = cards.get(i);
            cards.remove(maxCard);
            newCards.add(maxCard);
        }
        cards = newCards;

        try {
            boolean isFlush = false;
            if (cards.size() == 5) {
                isFlush = cards.get(0).getSuit() == cards.get(1).getSuit()
                        && cards.get(1).getSuit() == cards.get(2).getSuit()
                        && cards.get(1).getSuit() == cards.get(3).getSuit()
                        && cards.get(1).getSuit() == cards.get(4).getSuit();
            }
            boolean isStraight = false;
            if (cards.size() == 5) {
                if (cards.get(0).getValue() == PokerCard.ACE && cards.get(1).getValue() == 5
                        && cards.get(2).getValue() == 4 && cards.get(3).getValue() == 3
                        && cards.get(4).getValue() == 2) {
                    isStraight = true;
                    cards.add(cards.remove(0)); 
                } else { 
                    isStraight = cards.get(0).getValue() == cards.get(1).getValue() + 1
                            && cards.get(1).getValue() == cards.get(2).getValue() + 1
                            && cards.get(2).getValue() == cards.get(3).getValue() + 1
                            && cards.get(3).getValue() == cards.get(4).getValue() + 1;
                }
            }
            if (isFlush) {
                if (isStraight) {
                    if (cards.get(0).getValue() == PokerCard.ACE) {
                        rank = ROYAL_FLUSH;
                        description = longDescription = "Royal Flush";
                    } else {
                        rank = STRAIGHT_FLUSH;
                        description = longDescription = valueName(cards.get(0)) + "-high Straight Flush";
                    }
                } else {
                    rank = FLUSH;
                    description = "Flush";
                    longDescription = "Flush (" + cardValueNames() + ")";
                }
                return;
            }
            if (isStraight) {
                rank = STRAIGHT;
                description = longDescription = valueName(cards.get(0)) + "-high Straight";
                return;
            }

            if (cards.size() >= 4) {
                if (cards.get(0).getValue() == cards.get(1).getValue()
                        && cards.get(1).getValue() == cards.get(2).getValue()
                        && cards.get(2).getValue() == cards.get(3).getValue()) {
                    rank = FOUR_OF_A_KIND;
                    description = longDescription = "Four " + pluralValueName(cards.get(0));
                    if (cards.size() == 5)
                        longDescription = description + " (plus " + valueName(cards.get(4)) + ")";
                    return;
                }
            }
            if (cards.size() == 5
                    && cards.get(1).getValue() == cards.get(2).getValue()
                    && cards.get(2).getValue() == cards.get(3).getValue()
                    && cards.get(3).getValue() == cards.get(4).getValue()) {
                cards.add(cards.remove(0)); // Move first card -- not part of the Quad -- to the end.
                rank = FOUR_OF_A_KIND;
                description = "Four " + pluralValueName(cards.get(0));
                longDescription = description + " (plus " + valueName(cards.get(4)) + ")";
                return;
            }

            int tripleValue = 0; 
            int tripleLocation = -1; 
            for (int i = 0; i <= cards.size() - 3; i++) {
                if (cards.get(i).getValue() == cards.get(i + 1).getValue()
                        && cards.get(i + 1).getValue() == cards.get(i + 2).getValue()) {
                    tripleLocation = i;
                    tripleValue = cards.get(i).getValue();
                    break;
                }
            }
            
            int pairValue1 = 0;
            int pairLoc1 = -1;
            int pairValue2 = 0;
            int pairLoc2 = -1; 
            
            for (int i = 0; i <= cards.size() - 2; i++) {
                if (cards.get(i).getValue() == cards.get(i + 1).getValue() && cards.get(i).getValue() != tripleValue) {
                    pairValue1 = cards.get(i).getValue();
                    pairLoc1 = i;
                    for (int j = i + 2; j <= cards.size() - 2; j++) {
                        if (cards.get(j).getValue() == cards.get(j + 1).getValue()
                                && cards.get(j).getValue() != tripleValue) {
                            pairValue2 = cards.get(j).getValue();
                            pairLoc2 = j;
                            break;
                        }
                    }
                    break;
                }
            }

            if (tripleValue == 0 && pairValue1 == 0) {
                rank = NOTHING;
                description = "High Card (" + valueName(cards.get(0)) + ")";
                longDescription = "High Card (" + cardValueNames() + ")";
                return;
            }

            if (tripleValue > 0) {
                for (int i = 0; i < tripleLocation; i++) {
                    cards.add(cards.remove(0));
                }
                if (pairValue1 > 0) {
                    rank = FULL_HOUSE;
                    description = longDescription = "Full House, " + pluralValueName(cards.get(0))
                            + " and " + pluralValueName(cards.get(4));
                    return;
                } else {
                    rank = TRIPLE;
                    description = longDescription = "Three " + pluralValueName(cards.get(0));
                    if (cards.size() == 4)
                        longDescription = description + " (plus " + valueName(cards.get(3)) + ")";
                    else if (cards.size() == 5)
                        longDescription = description + " (plus " + valueName(cards.get(3))
                                + " and " + valueName(cards.get(4)) + ")";
                    return;
                }
            }

            if (pairLoc1 > 0) {
                PokerCard p2 = cards.remove(pairLoc1 + 1);
                PokerCard p1 = cards.remove(pairLoc1);
                cards.add(0, p2);
                cards.add(0, p1);
            }

            if (pairValue2 == 0) { // There was only one pair.
                rank = PAIR;
                description = longDescription = "Pair of " + pluralValueName(cards.get(0));
                if (cards.size() == 5)
                    longDescription = description + " (plus " + valueName(cards.get(2)) + ","
                            + valueName(cards.get(3)) + "," + valueName(cards.get(4)) + ")";
                else if (cards.size() == 4)
                    longDescription = description + " (plus " + valueName(cards.get(2)) + ","
                            + valueName(cards.get(3)) + ")";
                else if (cards.size() == 3)
                    longDescription = description + " (plus " + valueName(cards.get(2)) + ")";
                return;
            }

            if (pairLoc2 > 2) {
                PokerCard p2 = cards.remove(pairLoc2 + 1);
                PokerCard p1 = cards.remove(pairLoc2);
                cards.add(2, p2);
                cards.add(2, p1);
            }

            rank = TWO_PAIR;
            description = longDescription = "Two Pairs, " + pluralValueName(cards.get(0)) + " and "
                    + pluralValueName(cards.get(2));
            if (cards.size() == 5)
                longDescription = description + " (plus " + valueName(cards.get(4)) + ")";

        } finally {
            rank <<= 20;
            for (int i = 0; i < cards.size(); i++) {
                rank |= cards.get(i).getValue() << 4 * (4 - i);
            }
        }
    }
}

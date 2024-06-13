package fivecarddraw;

import java.io.Serializable;

public class PokerGameState implements Serializable {
    public final static int DEAL = 0;
    public final static int BET_OR_FOLD = 1; 
    public final static int RAISE_CHECK_OR_FOLD_ROUND_1 = 2; 
    public final static int RAISE_CALL_OR_FOLD_ROUND_2 = 3; 
    public final static int DRAW = 4; 
    public final static int WAIT_FOR_DEAL = 5; 
    public final static int WAIT_FOR_BET = 6; 
    public final static int WAIT_FOR_DRAW = 7; 
    public int status; 
    public final PokerCard[] hand; 
    public int money; 
    public int opponentMoney; 
    public int pot; 
    public int amountToCheck; 

    public PokerGameState(PokerCard[] hand, int status, int money, int opponentMoney, int pot) {
        this(hand, status, money, opponentMoney, pot, 0);
    }

    public PokerGameState(PokerCard[] hand, int status, int money, int opponentMoney, int pot, int amountToCheck) {
        this.hand = hand;
        this.status = status;
        this.money = money;
        this.opponentMoney = opponentMoney;
        this.pot = pot;
        this.amountToCheck = amountToCheck;
    }

}

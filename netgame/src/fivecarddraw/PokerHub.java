package fivecarddraw;
import java.io.*;


/**
 * This is the file for the server that manages the game. 
 */
public class PokerHub extends Hub {

    private final PokerDeck deck = new PokerDeck(); 
    private final static int WAITING_FOR_DEAL = 0; 
    private final static int WAITING_FOR_FIRST_BET = 1; 
    private final static int WAITING_FOR_BET_OR_CHECK = 2; 
    private final static int WAITING_FOR_FIRST_DRAW = 3; 
    private final static int WAITING_FOR_SECOND_DRAW = 4; 
    private int status; 
    private int currentPlayer; 
    private int dealer; 
    private boolean firstBettingRound; 
    private int amountNeededToCheck; 
    private boolean previousGameTied = false; 
    private PokerCard[][] hand = new PokerCard[2][5]; 
    private int[] money = new int[2]; 
    private int pot;
    public PokerHub(int port) throws IOException {
        super(port);
    }

    protected void playerConnected(int playerID) {
        if (playerID == 2) {
            shutdownServerSocket();
            dealer = 1;
            currentPlayer = 1;
            money[0] = 1000;
            money[1] = 1000;
            sendToOne(1, new PokerGameState(null, PokerGameState.DEAL, 1000, 1000, 0));
            sendToOne(2, new PokerGameState(null, PokerGameState.WAIT_FOR_DEAL, 1000, 1000, 0));
            sendToAll("Ready to start the first game!");
        }
    }

    protected void playerDisconnected(int playerID) {
        shutDownServer();
    }
    
    protected void messageReceived(int playerID, Object message) {
        if (playerID != currentPlayer) {
            System.out.println("Error: message received from the wrong player.");
            return;
        }
        if (message.equals("deal")) {
            if (status != WAITING_FOR_DEAL) {
                System.out.println("Error: DEAL message received at incorrect time.");
                return;
            }
            deck.shuffle();
            for (int i = 0; i < 5; i++) {
                hand[0][i] = deck.dealCard();
                hand[1][i] = deck.dealCard();
            }
            money[0] -= 5;
            money[1] -= 5;
            if (previousGameTied) {
                pot += 10;
                previousGameTied = false;
            } else
                pot = 10;
            currentPlayer = 3 - dealer;
            status = WAITING_FOR_FIRST_BET; 
            firstBettingRound = true;
            sendState(PokerGameState.BET_OR_FOLD, PokerGameState.WAIT_FOR_BET);
            sendToAll("Cards have been dealt.  $5 ante.");
        } else if (message.equals("fold")) {
            if (status != WAITING_FOR_FIRST_BET && status != WAITING_FOR_BET_OR_CHECK) {
                System.out.println("Error: FOLD message received at incorrect time.");
                return;
            }
            gameOver(3 - currentPlayer, "Your opponent has folded.", "You folded");
        } else if (message instanceof Integer) { 
            if (status != WAITING_FOR_FIRST_BET && status != WAITING_FOR_BET_OR_CHECK) {
                System.out.println("Error: BET message received at incorrect time.");
                return;
            }
            int bet = (Integer) message;
            if (bet < 0 || (status == WAITING_FOR_BET_OR_CHECK && bet < amountNeededToCheck)) {
                System.out.println("Error: Illegal bet amount received.");
                return;
            }
            pot += bet;
            money[currentPlayer - 1] -= bet;
            if (status == WAITING_FOR_FIRST_BET) {
                sendToOne(currentPlayer, "You bet $" + bet);
                sendToOne(3 - currentPlayer, "Your opponent bets $" + bet);
                currentPlayer = 3 - currentPlayer;
                amountNeededToCheck = bet;
                status = WAITING_FOR_BET_OR_CHECK;
                if (firstBettingRound)
                    sendState(PokerGameState.RAISE_CHECK_OR_FOLD_ROUND_1, PokerGameState.WAIT_FOR_BET);
                else
                    sendState(PokerGameState.RAISE_CALL_OR_FOLD_ROUND_2, PokerGameState.WAIT_FOR_BET);
            } else if (status == WAITING_FOR_BET_OR_CHECK) {
                if (bet == amountNeededToCheck) {
                    if (firstBettingRound) {
                        sendToOne(currentPlayer, "You Check.  First round of betting ends.");
                        sendToOne(3 - currentPlayer, "Your opponents Checks.  First round of betting ends.");
                        currentPlayer = 3 - dealer; 
                        status = WAITING_FOR_FIRST_DRAW;
                        sendState(PokerGameState.DRAW, PokerGameState.WAIT_FOR_DRAW);
                    } else {
                        checkCardsAtEndOfGame();
                    }
                } else {
                    amountNeededToCheck = bet - amountNeededToCheck;
                    sendToOne(currentPlayer, "You Check the bet and raise by $" + amountNeededToCheck);
                    sendToOne(3 - currentPlayer, "Your opponent Checks your bet and raises by $" + amountNeededToCheck);
                    currentPlayer = 3 - currentPlayer;
                    if (firstBettingRound)
                        sendState(PokerGameState.RAISE_CHECK_OR_FOLD_ROUND_1, PokerGameState.WAIT_FOR_BET);
                    else
                        sendState(PokerGameState.RAISE_CALL_OR_FOLD_ROUND_2, PokerGameState.WAIT_FOR_BET);
                }
            }
        } else if (message instanceof int[]) { 
            if (status != WAITING_FOR_FIRST_DRAW && status != WAITING_FOR_SECOND_DRAW) {
                System.out.println("Error: DISCARD message received at incorrect time");
                return;
            }
            int[] cardNums = (int[]) message;
            PokerCard[] currentPlayerHand = (currentPlayer == 1) ? hand[0] : hand[1];
            for (int i = 0; i < cardNums.length; i++) {
                currentPlayerHand[cardNums[i]] = deck.dealCard();
            }
            sendToOne(currentPlayer, "You draw " + cardNums.length + " cards");
            sendToOne(3 - currentPlayer, "Your opponent draws " + cardNums.length + " cards");
            if (status == WAITING_FOR_FIRST_DRAW) {
                currentPlayer = 3 - currentPlayer;
                status = WAITING_FOR_SECOND_DRAW;
                sendState(PokerGameState.DRAW, PokerGameState.WAIT_FOR_DRAW);
            } else {
                currentPlayer = 3 - dealer;
                status = WAITING_FOR_FIRST_BET;
                firstBettingRound = false;
                sendState(PokerGameState.BET_OR_FOLD, PokerGameState.WAIT_FOR_BET);
            }
        }
    }
    private void checkCardsAtEndOfGame() {
        PokerRank[] rank = new PokerRank[2];
        for (int i = 0; i < 2; i++) {
            rank[i] = new PokerRank();
            for (PokerCard c : hand[i])
                rank[i].add(c);
        }
        int winner; 
        if (rank[0].getRank() > rank[1].getRank())
            winner = 0; 
        else if (rank[0].getRank() < rank[1].getRank())
            winner = 1;
        else
            winner = -1; 
        sendToOne(1, hand[1].clone()); 
        sendToOne(2, hand[0].clone()); 
        if (winner != -1) { 
            gameOver(winner + 1, rank[winner] + " beats " + rank[1 - winner],
                    rank[winner] + " beats " + rank[1 - winner]);
        } else { 
            sendToAll("The result is a tie.  The pot stays on the table.");
            previousGameTied = true;
            dealer = 3 - dealer; 
            currentPlayer = dealer;
            status = WAITING_FOR_DEAL;
            sendState(PokerGameState.DEAL, PokerGameState.WAIT_FOR_DEAL);
        }
    }

  
    private void gameOver(int winner, String winnerMessage, String loserMessage) {
        sendToOne(winner, "You win. " + winnerMessage);
        sendToOne(3 - winner, "You lose.  " + loserMessage); 
        money[winner - 1] += pot; 
        dealer = 3 - dealer; 
        currentPlayer = dealer; 
        status = WAITING_FOR_DEAL;
        sendState(PokerGameState.DEAL, PokerGameState.WAIT_FOR_DEAL);
    }

    private void sendState(int currentPlayerState, int opponentState) {
        int player1State, player2State; 
        if (currentPlayer == 1) {
            player1State = currentPlayerState;
            player2State = opponentState;
        } else {
            player2State = currentPlayerState;
            player1State = opponentState;
        }
        if (status == WAITING_FOR_BET_OR_CHECK) { 
            sendToOne(1,
                    new PokerGameState(hand[0].clone(), player1State, money[0], money[1], pot, amountNeededToCheck));
            sendToOne(2,
                    new PokerGameState(hand[1].clone(), player2State, money[1], money[0], pot, amountNeededToCheck));
        } else { 
            sendToOne(1, new PokerGameState(hand[0].clone(), player1State, money[0], money[1], pot));
            sendToOne(2, new PokerGameState(hand[1].clone(), player2State, money[1], money[0], pot));
        }
    }

}

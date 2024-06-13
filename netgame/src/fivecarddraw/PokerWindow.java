package fivecarddraw;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Insets;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import java.io.IOException;
import java.util.Optional;

/**
 * Establishes connection to a PokerServer at a specified host and port. 
 */
public class PokerWindow extends Stage {

    private PokerClient connection; // Handles communication with the PokerServer.
    private PokerGameState state; // Represents the state of the game, as seen by the current player.
    private boolean[] discard; 
    private PokerCard[] opponentHand; // The opponent's hand. This variable is null during the playing of a hand. 
    private Canvas canvas; // Canvas where the game is displayed, defined by the inner class, Display.
    private Image cardImages;
    private Button dealButton; 
    private Button drawButton;
    private Button betButton;
    private Button callButton;
    private Button passButton;
    private Button foldButton;
    private Button quitButton;
    private TextField betInput;
    private String message = ""; // text that is displayed on the canvas
    private String messageFromServer = "";
    private String money = "", opponentsMoney = "", pot = "";

    public PokerWindow(final String hubHostName, final int hubPort) {

        cardImages = new Image("fivecarddraw/cards.png");
        messageFromServer = "WAITING FOR CONNECTION";

        canvas = new Canvas(550, 575);
        drawBoard();
        canvas.setOnMousePressed(evt -> doClick(evt.getX(), evt.getY()));

        betInput = new TextField();
        betInput.setEditable(false);
        betInput.setPrefColumnCount(5);
        VBox.setMargin(betInput, new Insets(10, 0, 15, 0));

        VBox controls = new VBox();
        EventHandler<ActionEvent> listener = this::doAction;
        dealButton = makeButton("DEAL", listener, controls);
        drawButton = makeButton("DRAW", listener, controls);
        betButton = makeButton("BET:", listener, controls);
        controls.getChildren().add(betInput);
        passButton = makeButton("PASS", listener, controls);
        callButton = makeButton("CALL", listener, controls);
        foldButton = makeButton("FOLD", listener, controls);
        quitButton = makeButton("QUIT", listener, controls);
        quitButton.setDisable(false);

        BorderPane root = new BorderPane(canvas);
        root.setLeft(controls);

        setScene(new Scene(root));
        setOnHiding(e -> doQuit());
        setTitle("NetPoker");
        setResizable(false);
        setX(200);
        setY(100);
        show();

        new Thread(() -> connect(hubHostName, hubPort)).start();

    } 
    
    private Button makeButton(String text, EventHandler<ActionEvent> listener, VBox box) {
        Button button = new Button(text);
        button.setDisable(true);
        button.setPrefSize(95, 40);
        button.setFont(Font.font(null, FontWeight.BOLD, 15));
        button.setOnAction(listener);
        box.getChildren().add(button);
        VBox.setMargin(button, new Insets(30, 10, 0, 10));
        return button;
    }

    /**
     * When the window is created, this method is called in a separate
     * thread to make the connection to the server. 
     */
    
    private void connect(String hostName, int serverPortNumber) {
        PokerClient c;
        try {
            c = new PokerClient(hostName, serverPortNumber);
            int id = c.getID();
            Platform.runLater(() -> {
                connection = c;
                if (id == 1) {
                    // This is Player #1. Still have to wait for second player to
                    // connect. Change the message display to reflect that fact.
                    messageFromServer = "Waiting for an opponent to connect...";
                    drawBoard();
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Sorry, could not connect to\n"
                                + hostName + " on port " + serverPortNumber
                                + "\nShutting down.");
                alert.showAndWait();
                System.exit(0);
            });
        }
    }

    private class PokerClient extends Client {

        /**
         * Connect to a PokerHub at a specified hostname and port number.
         */
        public PokerClient(String hubHostName, int hubPort) throws IOException {
            super(hubHostName, hubPort);
        }

        protected void messageReceived(final Object message) {
            Platform.runLater(() -> {
                if (message instanceof PokerGameState)
                    newState((PokerGameState) message);
                else if (message instanceof String) {
                    messageFromServer = (String) message;
                    drawBoard();
                } else if (message instanceof PokerCard[]) {
                    opponentHand = (PokerCard[]) message;
                    drawBoard();
                }
            });
        }

        protected void serverShutdown(String message) {
            Platform.runLater(() -> {
                showMessage("Your opponent has quit.\nThe game is over.");
                System.exit(0);
            });
        }

    } 

    private static Font font16 = Font.font(16); // fonts for use in drawBoard
    private static Font font24 = Font.font(24);
    private static Font font38 = Font.font(38);

    private void drawBoard() {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setFill(Color.DARKSEAGREEN);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphics.setStroke(Color.DARKRED);
        graphics.setLineWidth(8);
        graphics.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());

        graphics.setFill(Color.GREEN);
        graphics.setFont(font24);
        graphics.fillText(opponentsMoney, 100, 40);
        graphics.fillText(money, 100, 550);
        graphics.setFont(font38);
        graphics.fillText(pot, 150, 300);
        graphics.setFill(Color.DARKRED);
        graphics.setFont(font16);
        graphics.fillText(message, 30, 355);
        graphics.fillText(messageFromServer, 30, 230);

        if (state == null) {
            // Still waiting for connections. Don't draw anything.
            return;
        }

        if (state.hand == null) {
            // This happens only while waiting for the first hand to be dealt.
            // Draw outlines of the card locations for this player's hand.
            graphics.setStroke(Color.DARKRED);
            graphics.setLineWidth(2);
            for (int x = 25; x < 500; x += 105)
                graphics.strokeRect(x, 380, 80, 124);
        } else {
            // Draw the cards in this player's hand.
            for (int i = 0; i < 5; i++) {
                if (discard != null && discard[i])
                    drawCard(graphics, null, 25 + i * 105, 380);
                else
                    drawCard(graphics, state.hand[i], 25 + i * 105, 380);
            }
        }
        if (state.hand == null) {
            // This happens only while waiting for the first hand to be dealt.
            // Draw outlines of the card locations for the opponent's hand.
            graphics.setStroke(Color.DARKRED);
            graphics.setLineWidth(2);
            for (int x = 25; x < 500; x += 105)
                graphics.strokeRect(x, 70, 80, 124);
            
        } else if (opponentHand == null) {
            // The opponent's hand exists but is unknown. Draw it as face-down cards.
            for (int i = 0; i < 5; i++)
                drawCard(graphics, null, 25 + i * 105, 70);
        } else {
            // The opponent's hand is known. Draw the cards.
            for (int i = 0; i < 5; i++)
                drawCard(graphics, opponentHand[i], 25 + i * 105, 70);
        }
    }

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.showAndWait();
    }

    /**
     * A class to define the action event handler that responds when the user clicks
     * a button. 
     */
    public void doAction(ActionEvent evt) {
        Object src = evt.getSource();
        if (src == quitButton) { // end the program
            doQuit();
        } else if (src == dealButton) {
            // send "deal" as a message to the hub, which will start the next hand of the
            // game
            dealButton.setDisable(true);
            connection.send("deal");
        } else if (src == foldButton) {
            // send "fold" as a message to the hub, which will end the game because this
            // user folded
            foldButton.setDisable(true);
            betButton.setDisable(true);
            passButton.setDisable(true);
            callButton.setDisable(true);
            betInput.setEditable(false);
            betInput.setText("");
            connection.send("fold");
        } else if (src == passButton) {
            // send the integer 0 as a message, indicating that the user places no bet;
            // this is only possible for the first bet in a betting round
            foldButton.setDisable(true);
            betButton.setDisable(true);
            passButton.setDisable(true);
            callButton.setDisable(true);
            betInput.setEditable(false);
            betInput.setText("");
            connection.send(Integer.valueOf(0));
        } else if (src == callButton) {
            // send an integer equal to the minimum possible bet as a message to the hub;
            // this means "see" in the first betting round and "call" in the second, and
            // it will end the current betting round.
            foldButton.setDisable(true);
            betButton.setDisable(true);
            passButton.setDisable(true);
            callButton.setDisable(true);
            betInput.setEditable(false);
            betInput.setText("");
            connection.send(Integer.valueOf(state.amountToCheck));
        } else if (src == drawButton) {
            // Send the list of cards that the user wants to discard as a message to
            // the hub. The cards are recorded in the discard array.
            int ct = 0;
            for (int i = 0; i < 5; i++) { // Count the number of discarded cards.
                if (discard[i])
                    ct++;
            }
            if (ct == 0) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Are you sure you want to draw NO cards?\n"
                                + "If not, click 'No', and select\n"
                                + "the cards that you want to discard.",
                        ButtonType.NO, ButtonType.YES);
                Optional<ButtonType> resp = alert.showAndWait();
                if (!resp.isPresent() || resp.get() == ButtonType.NO)
                    return;
            }
            int[] cardNums = new int[ct];
            int j = 0;
            for (int i = 0; i < 5; i++) { // Put indices of discarded cards into an array to be sent to the hub.
                if (discard[i])
                    cardNums[j++] = i;
            }
            discard = null; // We are finished with the discard array.
            drawButton.setDisable(true);
            connection.send(cardNums);
        } else if (src == betButton) {
            // User wants to place a bet. Check that the bet is legal. If it is,
            // send the bet amount as a message to the hub.
            int amount;
            try {
                amount = Integer.parseInt(betInput.getText().trim());
                if (amount <= 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                showMessage("The bet amount must be\na legal positive integer.");
                betInput.selectAll();
                betInput.requestFocus();
                return;
            }
            if ((state.status == PokerGameState.RAISE_CHECK_OR_FOLD_ROUND_1 ||
                    state.status == PokerGameState.RAISE_CALL_OR_FOLD_ROUND_2)
                    && amount < state.amountToCheck) {
                showMessage("Your bet must be at least " + state.amountToCheck
                        + "\n to match your opponent's bet.");
                betInput.selectAll();
                betInput.requestFocus();
                return;
            }
            betInput.setEditable(false);
            betInput.setText("");
            connection.send(Integer.valueOf(amount));
        }
    }

    /**
     * This method is called when a new PokerGameState is received from the
     * PokerHub.
     */
    private void newState(PokerGameState state) {

        this.state = state;

        dealButton.setDisable(state.status != PokerGameState.DEAL);
        drawButton.setDisable(state.status != PokerGameState.DRAW);
        betButton.setDisable(state.status != PokerGameState.BET_OR_FOLD
                && state.status != PokerGameState.RAISE_CHECK_OR_FOLD_ROUND_1
                && state.status != PokerGameState.RAISE_CALL_OR_FOLD_ROUND_2);
        foldButton.setDisable(state.status != PokerGameState.BET_OR_FOLD
                && state.status != PokerGameState.RAISE_CHECK_OR_FOLD_ROUND_1
                && state.status != PokerGameState.RAISE_CALL_OR_FOLD_ROUND_2);
        passButton.setDisable(state.status != PokerGameState.BET_OR_FOLD);
        callButton.setDisable(state.status != PokerGameState.RAISE_CHECK_OR_FOLD_ROUND_1
                && state.status != PokerGameState.RAISE_CALL_OR_FOLD_ROUND_2);


        if (state.status == PokerGameState.RAISE_CALL_OR_FOLD_ROUND_2)
            callButton.setText("CALL");
        else
            callButton.setText("CHECK");

        if (state.status == PokerGameState.RAISE_CHECK_OR_FOLD_ROUND_1 ||
                state.status == PokerGameState.RAISE_CALL_OR_FOLD_ROUND_2 ||
                state.status == PokerGameState.BET_OR_FOLD) {
            if (!betInput.isEditable()) {
                int suggestedBet;
                if (state.status == PokerGameState.BET_OR_FOLD)
                    suggestedBet = 10;
                else
                    suggestedBet = state.amountToCheck + 10;
                betInput.setText("" + suggestedBet);
                betInput.setEditable(true);
                betInput.selectAll();
                betInput.requestFocus();
            }
        }

        money = "You have $ " + state.money;
        opponentsMoney = "Your opponent has $ " + state.opponentMoney;
        if (state.status != PokerGameState.DEAL && state.status != PokerGameState.WAIT_FOR_DEAL)
            opponentHand = null;
        pot = "Pot:  $ " + state.pot;

        if (state.status == PokerGameState.DRAW && discard == null) {
            discard = new boolean[5];
        }
        
        switch (state.status) {
            case PokerGameState.DEAL:
                message = "Click the DEAL button to start the game.";
                break;
            case PokerGameState.DRAW:
                message = "Click the cards you want to discard, then click DRAW.";
                break;
            case PokerGameState.BET_OR_FOLD:
                message = "Place your BET, PASS, or FOLD.";
                break;
            case PokerGameState.RAISE_CHECK_OR_FOLD_ROUND_1:
                message = "Place your BET, CHECK, or FOLD.";
                break;
            case PokerGameState.RAISE_CALL_OR_FOLD_ROUND_2:
                message = "Place your BET, CALL, or FOLD.";
                break;
            case PokerGameState.WAIT_FOR_BET:
                message = "Waiting for opponent to bet.";
                break;
            case PokerGameState.WAIT_FOR_DEAL:
                message = "Waiting for opponent to deal.";
                break;
            case PokerGameState.WAIT_FOR_DRAW:
                message = "Waiting for opponent to draw.";
                break;
        }

        drawBoard();

    } 

    /**
     * handles the users mouse clicks
     */
    private void doClick(double x, double y) {
        if (state == null || state.status != PokerGameState.DRAW)
            return;
        for (int i = 0; i < 5; i++) {
            if (y > 380 && y < 503 && x > 25 + i * 105 && x < 104 + i * 105) {
                discard[i] = !discard[i];
                drawBoard();
                break;
            }
        }
    }

    private void doQuit() {
        hide(); // Close the window.
        if (connection != null) {
            connection.disconnect();
            try { // Time for the disconnect message to be sent.
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
        System.exit(0);
    }

    public void drawCard(GraphicsContext g, PokerCard card, int x, int y) {
        int cx; // x-coord of upper left corner of the card inside cardsImage
        int cy; // y-coord of upper left corner of the card inside cardsImage
        if (card == null) {
            cy = 4 * 123; // coords for a face-down card.
            cx = 2 * 79;
        } else {
            if (card.getValue() == PokerCard.ACE)
                cx = 0;
            else
                cx = (card.getValue() - 1) * 79;
            switch (card.getSuit()) {
                case PokerCard.CLUBS:
                    cy = 0;
                    break;
                case PokerCard.DIAMONDS:
                    cy = 123;
                    break;
                case PokerCard.HEARTS:
                    cy = 2 * 123;
                    break;
                default: // spades
                    cy = 3 * 123;
                    break;
            }
        }
        g.drawImage(cardImages, cx, cy, 79, 123, x, y, 79, 123);
    }

}

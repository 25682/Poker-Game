package fivecarddraw;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.io.IOException;


public class Main extends Application {

    private static final int DEFAULT_PORT = 32010;
    private static final String DEFAULT_HOST = "localhost";

    public static void main(String[] args) {
        launch(args);
    }

    private Stage window; // The first window that shows on the screen, with connection controls.

    private Label message;
    private TextField listeningPortInput;
    private TextField hostInput;
    private TextField connectPortInput;

    public void start(Stage stage) {

        window = stage;

        Button okBtn = new Button("OK");
        okBtn.setDefaultButton(true);
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);

        message = new Label("Welcome to 5-Card Poker Game!");
        message.setFont(Font.font("Arial", FontWeight.MEDIUM, 24));

        listeningPortInput = new TextField("" + DEFAULT_PORT);
        listeningPortInput.setPrefColumnCount(5);
        hostInput = new TextField("" + DEFAULT_HOST);
        hostInput.setPrefColumnCount(30);
        connectPortInput = new TextField("" + DEFAULT_PORT);
        connectPortInput.setPrefColumnCount(5);

        RadioButton selectServerMode = new RadioButton("Start a new game");
        RadioButton selectClientMode = new RadioButton("Connect to existing game");
       

        ToggleGroup group = new ToggleGroup();
        selectServerMode.setToggleGroup(group);
        selectClientMode.setToggleGroup(group);

        Label listenPortLabel = new Label("Port: ");
        Label hostLabel = new Label("Computer: ");
        Label connectPortLabel = new Label("Port: ");

        HBox row2 = new HBox(listenPortLabel, listeningPortInput);
        HBox row4 = new HBox(hostLabel, hostInput);
        HBox row5 = new HBox(connectPortLabel, connectPortInput);

        VBox inputs = new VBox(15, message, selectServerMode, row2, selectClientMode, row4, row5);
        VBox.setMargin(row2, new Insets(0, 0, 0, 50));
        VBox.setMargin(row4, new Insets(0, 0, 0, 50));
        VBox.setMargin(row5, new Insets(0, 0, 0, 50));
        inputs.setStyle("-fx-padding:20px; -fx-border-color:black; -fx-border-width:2px");
        HBox bottom = new HBox(8, cancelBtn, okBtn);
        bottom.setPadding(new Insets(10, 0, 0, 0));
        bottom.setAlignment(Pos.CENTER);
        BorderPane root = new BorderPane();
        root.setCenter(inputs);
        root.setBottom(bottom);
        root.setPadding(new Insets(15, 15, 10, 15));

        stage.setScene(new Scene(root));
        stage.setTitle("NetPoker");
        stage.setResizable(false);

        cancelBtn.setOnAction(e -> Platform.exit());
        okBtn.setOnAction(e -> doOK(selectServerMode.isSelected()));

        selectServerMode.setOnAction(e -> {
            listeningPortInput.setDisable(false);
            hostInput.setDisable(true);
            connectPortInput.setDisable(true);
            listeningPortInput.setEditable(true);
            hostInput.setEditable(false);
            connectPortInput.setEditable(false);
        });
        selectClientMode.setOnAction(e -> {
            listeningPortInput.setDisable(true);
            hostInput.setDisable(false);
            connectPortInput.setDisable(false);
            listeningPortInput.setEditable(false);
            hostInput.setEditable(true);
            connectPortInput.setEditable(true);
        });

        selectServerMode.setSelected(true);
        hostInput.setDisable(true);
        connectPortInput.setDisable(true);
        hostInput.setEditable(false);
        connectPortInput.setEditable(false);

        stage.show();

    } 

    private void errorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    private void doOK(boolean openAsServer) {
       
        if (openAsServer) {
            int port;
            try {
                port = Integer.parseInt(listeningPortInput.getText().trim());
                if (port <= 0)
                    throw new Exception();
            } catch (Exception e) {
                errorMessage("The value in the \"Listen on port\" box\nis not a legal positive integer!");
                message.setText("Illegal port number.  Please try again!");
                listeningPortInput.selectAll();
                listeningPortInput.requestFocus();
                return;
            }
            Hub hub;
            try {
                hub = new PokerHub(port);
            } catch (IOException e) {
                errorMessage("Sorry, could not listen on port number " + port);
                message.setText("Please try a different port number!");
                listeningPortInput.selectAll();
                listeningPortInput.requestFocus();
                return;
            }
            new PokerWindow("localhost", port);
            window.hide();
        } else {
            String host;
            int port;
            host = hostInput.getText().trim();
            if (host.length() == 0) {
                errorMessage("Please enter the name or IP address\nof the computer that is hosting the game.");
                message.setText("Please enter a computer name!");
                hostInput.requestFocus();
                return;
            }
            try {
                port = Integer.parseInt(connectPortInput.getText().trim());
                if (port <= 0)
                    throw new Exception();
            } catch (Exception e) {
                errorMessage("The value in the \"Port Number\" box\nis not a legal positive integer!");
                message.setText("Illegal port number.  Please try again!");
                connectPortInput.selectAll();
                connectPortInput.requestFocus();
                return;
            }
            new PokerWindow(host, port);
            window.hide();
        }

    } 

} 

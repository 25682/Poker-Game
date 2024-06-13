package fivecarddraw;

import java.io.Serializable;

final class StatusMessage implements Serializable {

    /**
     * The ID number of the player who has connected or disconnected.
     */
    public final int playerID;

    /**
     * True if the player has just connected; false if the player
     * has just disconnected.
     */
    public final boolean connecting;

    /**
     * The list of players after the change has been made.
     */
    public final int[] players;

    public StatusMessage(int playerID, boolean connecting, int[] players) {
        this.playerID = playerID;
        this.connecting = connecting;
        this.players = players;
    }

}


class ForwardedMessage implements Serializable {

    public final Object message; // Original message from a client.
    public final int senderID; // The ID of the client who sent that message.

    /**
     * Create a ForwadedMessage to wrap a message sent by a client.
     * 
     * @param senderID the ID number of the original sender.
     * @param message  the original message.
     */
    public ForwardedMessage(int senderID, Object message) {
        this.senderID = senderID;
        this.message = message;
    }

}

final class DisconnectMessage implements Serializable {

    /**
     * The message associated with the disconnect. When the Hub
     * sends disconnects because it is shutting down, the message
     * is "*shutdown*".
     */
    final public String message;

    /**
     * Creates a DisconnectMessage containing a given String, which
     * is meant to describe the reason for the disconnection.
     */
    public DisconnectMessage(String message) {
        this.message = message;
    }


class ResetSignal {
}

}
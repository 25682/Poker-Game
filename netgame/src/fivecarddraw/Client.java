package fivecarddraw;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This abstract class represents a Client, or Player, that can connect to a
 * Hub(Server). The client sends/receives messages to and from the server, which
 * then sends those messages to the other clients.
 */
abstract public class Client {

    /**
     * A list of players ID number that is generated every time a client has
     * connected to or disconnected from the server.
     */
    protected int[] connectedPlayerIDs = new int[0];

    private volatile boolean autoreset;

    /**
     * a connection is made with the server
     */
    public Client(String hubHostName, int hubPort) throws IOException {
        connection = new ConnectionToHub(hubHostName, hubPort);
    }

    abstract protected void messageReceived(Object message);

    protected void playerConnected(int newPlayerID) {
    }

    protected void playerDisconnected(int departingPlayerID) {
    }

    protected void connectionClosedByError(String message) {
    }

    /**
     * This method is called when the connection to the Hub is closed down
     * because the server is shutting down normally. The method in this class
     * does nothing. Subclasses can override this method to take some action
     * when shutdown occurs. The message will be "*shutdown*" if the message was
     * in fact sent by a Hub that is shutting down in the normal way.
     */
    protected void serverShutdown(String message) {
    }

    /**
     * This method is called after a connection to the server has been opened
     * and after the client has been assigned an ID number. Its purpose is to do
     * extra checking or set up before the connection is fully established. If
     * this method throws an IOException, then the connection is closed and the
     * player is never added to the list of players. The method in this class
     * does nothing. The client and the hub must both be programmed with the
     * same handshake protocol. At the time this method is called, the client's
     * ID number has already been set and can be retrieved by calling the
     * getID() method, but the client has not yet been added to the list of
     * connected players.
     *
     * @param in a stream from which messages from the hub can be read.
     * @param out a stream to which messages to the hub can be written. After
     * writing a message to this stream, it is important to call out.flush() to
     * make sure that the message is actually transmitted.
     * @throws IOException should be thrown if some error occurs that would
     * prevent the connection from being fully established.
     */
    protected void extraHandshake(ObjectInputStream in, ObjectOutputStream out)
            throws IOException {
    }

    // ----------------------- Methods meant to be called by users of this class
    // -----------
    /**
     * This method can be called to disconnect cleanly from the server. If the
     * connection is already closed, this method has no effect.
     */
    public void disconnect() {
        if (!connection.closed) {
            connection.send(new DisconnectMessage("Goodbye Hub"));
        }
    }

    /**
     * This method is called to send a message to the hub. This method simply
     * drops the message into a queue of outgoing messages, and it never blocks.
     * This method throws an IllegalStateException if the connection to the Hub
     * has already been closed.
     *
     * @param message A non-null object representing the message. This object
     * must implement the Serializable interface.
     * @throws IllegalArgumentException if message is null or is not
     * Serializable.
     * @throws IllegalStateException if the connection has already been closed,
     * either by the disconnect() method, because the Hub has shut down, or
     * because of a network error.
     */
    public void send(Object message) {
        if (message == null) {
            throw new IllegalArgumentException("Null cannot be sent as a message.");
        }
        if (!(message instanceof Serializable)) {
            throw new IllegalArgumentException("Messages must implement the Serializable interface.");
        }
        if (connection.closed) {
            throw new IllegalStateException("Message cannot be sent because the connection is closed.");
        }
        connection.send(message);
    }

    public int getID() {
        return connection.id_number;
    }

    public void resetOutput() {
        connection.send(new ResetSignal()); // A ResetSignal in the output stream is seen as a signal to reset.
    }

    
    public void setAutoreset(boolean auto) {
        autoreset = auto; //when set to true, the output stream will be reset before every transmission
    }

    public boolean getAutoreset() {
        return autoreset;
    }

    private final ConnectionToHub connection; 

    private class ConnectionToHub {

        private final int id_number; // The ID of this client, assigned by the server.
        private final Socket socket; // The socket that is connected to the server.
        private final ObjectInputStream in; // A stream for sending messages to the server.
        private final ObjectOutputStream out; // A stream for receiving messages from the server.
        private final SendThread sendThread; // The thread that sends messages to the server.
        private final ReceiveThread receiveThread; // The thread that receives messages from the server.
        private final LinkedBlockingQueue<Object> outgoingMessages; // Queue of messages waiting to be transmitted.
        private volatile boolean closed; // This is set to true when the connection is closing.
        
        /**
         * Constructor opens the connection and sends the string "Hello Hub" to
         * the server. 
         */
        ConnectionToHub(String host, int port) throws IOException {
            outgoingMessages = new LinkedBlockingQueue<Object>();
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject("Hello Hub");
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            try {
                Object response = in.readObject();
                id_number = ((Integer) response).intValue();
            } catch (Exception e) {
                throw new IOException("Illegal response from server.");
            }
            extraHandshake(in, out); // Will throw an IOException if handshake doesn't succeed.
            sendThread = new SendThread();
            receiveThread = new ReceiveThread();
            sendThread.start();
            receiveThread.start();
        }

        void close() {
            closed = true;
            sendThread.interrupt();
            receiveThread.interrupt();
            try {
                socket.close();
            } catch (IOException e) {
            }
        }

        void send(Object message) {
            outgoingMessages.add(message);
        }

        /**
         * This method is called by the threads that do input and output on the
         * connection when an IOException occurs.
         */
        synchronized void closedByError(String message) {
            if (!closed) {
                connectionClosedByError(message);
                close();
            }
        }

        /**
         * This class defines a thread that sends messages to the Hub.
         */
        private class SendThread extends Thread {

            public void run() {
                System.out.println("Client send thread started.");
                try {
                    while (!closed) {
                        Object message = outgoingMessages.take();
                        if (message instanceof ResetSignal) {
                            out.reset();
                        } else {
                            if (autoreset) {
                                out.reset();
                            }
                            out.writeObject(message);
                            out.flush();
                            if (message instanceof DisconnectMessage) {
                                close();
                            }
                        }
                    }
                } catch (IOException e) {
                    if (!closed) {
                        closedByError("IO error occurred while trying to send message.");
                        System.out.println("Client send thread terminated by IOException: " + e);
                    }
                } catch (Exception e) {
                    if (!closed) {
                        closedByError("Unexpected internal error in send thread: " + e);
                        System.out.println("\nUnexpected error shuts down client send thread:");
                        e.printStackTrace();
                    }
                } finally {
                    System.out.println("Client send thread terminated.");
                }
            }
        }

        /**
         * This class defines a thread that reads messages from the Hub.
         */
        private class ReceiveThread extends Thread {

            public void run() {
                System.out.println("Client receive thread started.");
                try {
                    while (!closed) {
                        Object obj = in.readObject();
                        if (obj instanceof DisconnectMessage) {
                            close();
                            serverShutdown(((DisconnectMessage) obj).message);
                        } else if (obj instanceof StatusMessage) {
                            StatusMessage msg = (StatusMessage) obj;
                            connectedPlayerIDs = msg.players;
                            if (msg.connecting) {
                                playerDisconnected(msg.playerID);
                            } else {
                                playerConnected(msg.playerID);
                            }
                        } else {
                            messageReceived(obj);
                        }
                    }
                } catch (IOException e) {
                    if (!closed) {
                        closedByError("IO error occurred while waiting to receive  message.");
                        System.out.println("Client receive thread terminated by IOException: " + e);
                    }
                } catch (Exception e) {
                    if (!closed) {
                        closedByError("Unexpected internal error in receive thread: " + e);
                        System.out.println("\nUnexpected error shuts down client receive thread:");
                        e.printStackTrace();
                    }
                } finally {
                    System.out.println("Client receive thread terminated.");
                }
            }
        }

    } // end nested class ConnectionToHub

}

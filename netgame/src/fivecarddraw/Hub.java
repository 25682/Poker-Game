package fivecarddraw;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Hub{
    
    private TreeMap<Integer, ConnectionToClient> playerConnections;
    private LinkedBlockingQueue<Message> incomingMessages;
    private volatile boolean autoreset;
    private ServerSocket serverSocket;  
    private Thread serverThread; // Accepts connections on serverSocket.
    volatile private boolean shutdown;   
    private int nextClientID = 1;  
   
    public Hub(int port) throws IOException {
        playerConnections = new TreeMap<Integer, ConnectionToClient>();
        incomingMessages = new LinkedBlockingQueue<Message>();
        serverSocket = new ServerSocket(port);
        System.out.println("Listening for client connections on port " + port);
        serverThread = new ServerThread();
        serverThread.start();
        Thread readerThread = new Thread(){
            public void run() {
               while (true) {
                   try {
                       Message msg = incomingMessages.take();
                       messageReceived(msg.playerConnection, msg.message);
                   }
                   catch (Exception e) {
                       System.out.println("Exception while handling received message:");
                       e.printStackTrace();
                   }
               }
           }
        };
        readerThread.setDaemon(true);
        readerThread.start();
    }
    
    protected void messageReceived(int playerID, Object message) {
        sendToAll(new ForwardedMessage(playerID,message));
    }
    
    protected void playerConnected(int playerID) {
    }
    
    protected void playerDisconnected(int playerID) {
    }
        
    /**
     * After a connection request is received, this method is called to do 
     * further verification or set up before the connection is entirely formed.
     */
    protected void extraHandshake(int playerID, ObjectInputStream in, 
                                        ObjectOutputStream out) throws IOException {
    }
    
    /**
     * Gets the ID numbers of the connected clients.
     */
    synchronized public int[] getPlayerList() {
        int[] players = new int[playerConnections.size()];
        int i = 0;
        for (int p : playerConnections.keySet())
            players[i++] = p;
        return players;
    }
    
    // disconnects the client 
    
    public void shutdownServerSocket() {
        if (serverThread == null)
            return;
        incomingMessages.clear();
        shutdown = true;
        try {
            serverSocket.close();
        }
        catch (IOException e) {
        }
        serverThread = null;
        serverSocket = null;
    }
    
    public void restartServer(int port) throws IOException {
        if (serverThread != null && serverThread.isAlive())
            throw new IllegalStateException("Server is already listening for connections.");
        shutdown = false;
        serverSocket = new ServerSocket(port);
        serverThread = new ServerThread();
        serverThread.start();
    }
    
    //disconnects all the clients

    public void shutDownServer() {
        shutdownServerSocket();
        sendToAll(new DisconnectMessage("*shutdown*"));
        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
        }
        for (ConnectionToClient pc : playerConnections.values())
            pc.close();
    }
    
    
    /**
     * Sends message objects to all clients
     */
    synchronized public void sendToAll(Object message) {
        if (message == null)
            throw new IllegalArgumentException("Null cannot be sent as a message.");
        if ( ! (message instanceof Serializable) )
            throw new IllegalArgumentException("Messages must implement the Serializable interface.");
        for (ConnectionToClient pc : playerConnections.values())
            pc.send(message);
    }
    
    
    /**
     * Sends message objects to one client
     */
    synchronized public boolean sendToOne(int recipientID, Object message) {
        if (message == null)
            throw new IllegalArgumentException("Null cannot be sent as a message.");
        if ( ! (message instanceof Serializable) )
            throw new IllegalArgumentException("Messages must implement the Serializable interface.");
        ConnectionToClient pc = playerConnections.get(recipientID);
        if (pc == null)
            return false;
        else {
            pc.send(message);
            return true;
        }
    }
    
    public void resetOutput() {
        ResetSignal rs = new ResetSignal();
        for (ConnectionToClient pc : playerConnections.values())
            pc.send(rs); // A ResetSignal in the output stream is seen as a signal to reset.
    }
    
    public void setAutoreset(boolean auto) {
        autoreset = auto;
    }
    
    public boolean getAutoreset() {
        return autoreset;
    }
    
    synchronized private void messageReceived(ConnectionToClient fromConnection, Object message) {
              // Note: DisconnectMessage is handled in the ConnectionToClient class.
        int sender = fromConnection.getPlayer();
        messageReceived(sender,message);
    }
    
    
    synchronized private void acceptConnection(ConnectionToClient newConnection) {
        int ID = newConnection.getPlayer();
        playerConnections.put(ID,newConnection);
        StatusMessage sm = new StatusMessage(ID,true,getPlayerList());
        sendToAll(sm);
        playerConnected(ID);
        System.out.println("Connection accepted from client number " + ID);
    }
    
    synchronized private void clientDisconnected(int playerID) {
        if (playerConnections.containsKey(playerID)) {
            playerConnections.remove(playerID);
            StatusMessage sm = new StatusMessage(playerID,false,getPlayerList());
            sendToAll(sm);
            playerDisconnected(playerID);
            System.out.println("Connection with client number " + playerID + " closed by DisconnectMessage from client.");
        }
    }
    
    synchronized private void connectionToClientClosedWithError( ConnectionToClient playerConnection, String message ) {
        int ID = playerConnection.getPlayer();
        if (playerConnections.remove(ID) != null) {
            StatusMessage sm = new StatusMessage(ID,false,getPlayerList());
            sendToAll(sm);
        }
    }
    
    private class Message {
        ConnectionToClient playerConnection;
        Object message;
    }
    
    private class ServerThread extends Thread {  // Listens for connection requests from clients.
        public void run() {
            try {
                while ( ! shutdown ) {
                    Socket connection = serverSocket.accept();
                    if (shutdown) {
                        System.out.println("Listener socket has shut down.");
                        break;
                    }
                    new ConnectionToClient(incomingMessages,connection);
                }
            }
            catch (Exception e) {
                if (shutdown)
                    System.out.println("Listener socket has shut down.");
                else
                    System.out.println("Listener socket has been shut down by error: " + e);
            }
        }
    }
    
    
    private class ConnectionToClient { // Handles communication with one client.

        private int playerID;  // The ID number for this player.
        private BlockingQueue<Message> incomingMessages;
        private LinkedBlockingQueue<Object> outgoingMessages;
        private Socket connection;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private volatile boolean closed;  // Set to true when connection is closing normally.
        private Thread sendThread; // Handles setup, then handles outgoing messages.
        private volatile Thread receiveThread; // Created only after connection is open.
        
        ConnectionToClient(BlockingQueue<Message> receivedMessageQueue, Socket connection)  {
            this.connection = connection;
            incomingMessages = receivedMessageQueue;
            outgoingMessages = new LinkedBlockingQueue<Object>();
            sendThread =  new SendThread();
            sendThread.start();
        }
        
        int getPlayer() {
            return playerID;
        }
        
        void close() {
            closed = true;
            sendThread.interrupt();
            if (receiveThread != null)
                receiveThread.interrupt();
            try {
                connection.close();
            }
            catch (IOException e) {
            }
        }
        
        void send(Object obj) { // Just drop message into message output queue.
            if (obj instanceof DisconnectMessage) {
                // A signal to close the connection;
                // discard other waiting messages, if any.
                outgoingMessages.clear();
            }
            outgoingMessages.add(obj);
        }
        
        private void closedWithError(String message) {
            connectionToClientClosedWithError(this, message);
            close();
        }
        
        /**
         * Handles the handshake that  before the connection is opened.
         */
        private class SendThread extends Thread {
            public void run() {
                try {
                    out = new ObjectOutputStream(connection.getOutputStream());
                    in = new ObjectInputStream(connection.getInputStream());
                    String handle = (String)in.readObject(); // first input must be "Hello Hub"
                    if ( ! "Hello Hub".equals(handle) )
                        throw new Exception("Incorrect hello string received from client.");
                    synchronized(Hub.this) {
                        playerID = nextClientID++; // Get a player ID for this player.
                    }
                    out.writeObject(playerID);  // Send playerID to the client.
                    out.flush();
                    extraHandshake(playerID,in,out);  // Does any extra stuff before connection is fully established.
                    acceptConnection(ConnectionToClient.this);
                    receiveThread = new ReceiveThread();
                    receiveThread.start();
                }
                catch (Exception e) {
                    try {
                        closed = true;
                        connection.close();
                    }
                    catch (Exception e1) {
                    }
                    System.out.println("\nError while setting up connection: " + e);
                    e.printStackTrace();
                    return;
                }
                try {
                    while ( ! closed ) {  // Get messages from outgoingMessages queue and send them.
                        try {
                            Object message = outgoingMessages.take();
                            if (message instanceof ResetSignal)
                                out.reset();
                            else {
                                if (autoreset)
                                    out.reset();
                                out.writeObject(message);
                                out.flush();
                                if (message instanceof DisconnectMessage) // A signal to close the connection.
                                    close();
                            }
                        }
                        catch (InterruptedException e) {
                            // should mean that connection is closing
                        }
                    }    
                }
                catch (IOException e) {
                    if (! closed) {
                        closedWithError("Error while sending data to client.");
                        System.out.println("Server send thread terminated by IOException: " + e);
                    }
                }
                catch (Exception e) {
                    if (! closed) {
                        closedWithError("Internal Error: Unexpected exception in output thread: " + e);
                        System.out.println("\nUnexpected error shuts down Server's send thread:");
                        e.printStackTrace();
                    }
                }
            }
        }
      
        private class ReceiveThread extends Thread {
            public void run() {
                try {
                    while ( ! closed ) {
                        try {
                            Object message = in.readObject();
                            Message msg = new Message();
                            msg.playerConnection = ConnectionToClient.this;
                            msg.message = message;
                            if ( ! (message instanceof DisconnectMessage) )
                                incomingMessages.put(msg);
                            else {
                                closed = true;
                                outgoingMessages.clear();
                                out.writeObject("*goodbye*");
                                out.flush();
                                clientDisconnected(playerID);
                                close();
                            }
                        }
                        catch (InterruptedException e) {
                            // should mean that connection is closing
                        }
                    }
                }
                catch (IOException e) {
                    if (! closed) {
                        closedWithError("Error while reading data from client.");
                        System.out.println("Server receive thread terminated by IOException: " + e);
                    }
                }
                catch (Exception e) {
                    if ( ! closed ) {
                        closedWithError("Internal Error: Unexpected exception in input thread: " + e);
                        System.out.println("\nUnexpected error shuts down Server's receive thread:");
                        e.printStackTrace();
                    }
                }
            }
        }
        
    }  

    
}

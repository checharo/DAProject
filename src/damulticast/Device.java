
package damulticast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 *
 * @author cesar
 */
public class Device implements Runnable {
    
    private int id;
    private Game game;
    private ArrayList<RemoteDevice> peers;
    private ServerSocket serverSocket;
    private int messageId;
    
    public static void main(String args[]) {
        
        if (args.length < 1) {
            System.err.println("Usage: device.sh <ip-address>\n"
                + "ip-address: The tracker's ip address");
            System.exit(1);
        }
        
        String serverIP = args[0];
        Device device = new Device();
        
        /* Establish the connection with tracker */
        Socket tracker = null;
        try {
            tracker = new Socket(serverIP, 12345);
            DataInputStream in = new DataInputStream(tracker.getInputStream());
            DataOutputStream out =new DataOutputStream(tracker.getOutputStream());
            
            /* Create a new listening socket from the ephemeral (random) port list */
            int port = device.setListener(0);
            out.writeInt(port);
            
            /* Read the id given and peer list */
            device.setId(in.readInt());
            boolean morePeers = in.readBoolean();
            while (morePeers) {
                RemoteDevice peer = new RemoteDevice(in.readInt(), 
                    in.readUTF(), in.readInt());
                device.getPeers().add(peer);
                morePeers = in.readBoolean();
            }
            
            try {
                tracker.close();
            } catch (IOException ioe) {
                System.err.println("IOException while closing tracker "
                    + ioe.getMessage());
            }
        } catch (UnknownHostException uhe) {
            System.err.println("Unknown host: " + serverIP);
            System.exit(1);
        } catch (IOException ioe) {
            System.err.println("IOException while establishing P2P: " 
                + ioe.getMessage());
            System.exit(1);
        }
        
        /* Start listening to P2P communication */
        Thread t = new Thread(device);
        t.start();
        
        /* Say hello to rest of peers */
        device.sayHello();
        
        /* Read from the command line */
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("device> ");
            try {
                String command = stdIn.readLine();
                if (command.equals("peerlist")) {
                    if (device.getPeers().isEmpty())
                        System.out.println("Peerlist empty");
                    else for (RemoteDevice peer : device.getPeers()) {
                        System.out.println(peer.getId() + " -> " 
                            + peer.getIpAddress() + ":" + peer.getPort());
                    }
                } else if (command.startsWith("send")) {
                    StringTokenizer st = new StringTokenizer(command, " ");
                    st.nextToken();
                    try {
                        int peerId = Integer.parseInt(st.nextToken()); 
                        String header = st.nextToken();
                        String message = st.nextToken();
                        
                        RemoteDevice peer = device.lookUpPeer(peerId);
                        if (peer == null) {
                            System.err.println("No peer entry for " + peerId);
                            continue;
                        }
                        device.send(new Message(peer, header, message));
                    } catch (NoSuchElementException nsee) {
                        System.err.println("usage: send <peerId> <header> <message>");
                    } catch (NumberFormatException nfe) {
                        System.err.println("peer id must be numerical");
                    }
                } else if (command.equals("exit")) {
                    device.sayGoodbye();
                    t.interrupt();
                    break;
                } else {
                    System.out.println("Not a valid command:\npeerlist\nexit");
                }
            } catch (IOException ioe) {
                System.out.println("IOException while reading line: " 
                    + ioe.getMessage());
            }
        }
        
        System.exit(0);
    }
    
    public void run() {
        try {
            this.listen();
        } catch (IOException ioe) {
            System.err.println("General IOException: " + ioe);
        }
    }
    
    public Device() {
        this.id = 0;
        this.game = new Game();
        this.peers = new ArrayList<RemoteDevice>();
        this.messageId = 0;
    }
    
    /**
     * The listen method for the devices. The devices will expect incoming messages
     * one by one, and will process them as they come. There will be no separate
     * threads for receiving and processing since there will be a timeout to process
     * the incoming messages.
     * The logic for handling the messages is in the receiveMessage method. This
     * method should not be edited anymore.
     * @throws IOException In case an unexpected error happens while reading connections
     */
    public void listen() throws IOException {
        
        while(true) {
            Socket peerSocket = serverSocket.accept();
            peerSocket.setSoTimeout(5000);
            DataInputStream in = new DataInputStream(peerSocket.getInputStream());
            DataOutputStream out =new DataOutputStream(peerSocket.getOutputStream());

            try {  
                String ipAddress = peerSocket.getInetAddress().toString();
                int senderId = in.readInt();
                int port = in.readInt();
                RemoteDevice sender = lookUpPeer(senderId);
                
                /* If peer is not found, add it to the list */
                if (sender == null) {
                    sender = new RemoteDevice(senderId, 
                        peerSocket.getInetAddress().getHostAddress(), port);
                    peers.add(sender);
                }
                Message m = new Message(in.readInt(), sender, in.readUTF(), 
                    in.readUTF());
                receiveMessage(m);    
            } catch (SocketTimeoutException ste) {
                System.err.println("Socket timeout from peer: "
                    + peerSocket.getInetAddress().getHostAddress());
            } catch (IOException ioe) {
                System.err.println("IOE exception while receiving message from peer: " 
                    + peerSocket.getInetAddress().getHostAddress());
            } finally {
                peerSocket.close();
            }
        }
    }
    
    /**
     * The sending method that will execute the instructions necessary for a 
     * message to reach the destination Socket. It's the opposite of the listen()
     * method. This method should not be edited anymore.
     * @param m The message to be sent.
     */
    public synchronized void send(Message m) {
        
        RemoteDevice peer = m.getSender();
        
        try {
            Socket peerSocket = new Socket();
            peerSocket.connect(new InetSocketAddress(peer.getIpAddress(), 
                peer.getPort()), 5000);
            
            DataInputStream in = new DataInputStream(peerSocket.getInputStream());
            DataOutputStream out =new DataOutputStream(peerSocket.getOutputStream());
            
            out.writeInt(id);
            out.writeInt(serverSocket.getLocalPort());
            
            messageId++;
            out.writeInt(messageId);
            out.writeUTF(m.getHeader());
            out.writeUTF(m.getMessage());
            
            try {
                peerSocket.close();
            } catch (IOException ioe) {
                System.err.println("IOException while closing peer "
                    + ioe.getMessage());
            }
        } catch (UnknownHostException uhe) {
            System.err.println("Unknown host: " + peer.getIpAddress());
            System.exit(1);
        } catch (IOException ioe) {
            System.err.println("IOException while sending P2P message: " 
                + ioe.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Contains the logic to be implemented when a message from a peer is 
     * received.
     * @param m The incoming message from the peer;
     */
    public synchronized void receiveMessage(Message m) {
        
        RemoteDevice peer = m.getSender();
        System.out.println(peer.getId() + "> " + m.getId() + ":" + m.getHeader() 
            + ":" + m.getMessage());
        
        /* Message handlers for every type of message */
        if (m.getHeader().equals("hello")) {
            if (!peers.contains(peer))
                peers.add(peer);
        } else if (m.getHeader().equals("goodbye")) {
            peers.remove(peer);
        }
    }
    
    /**
     * Sets the serverSocket of this device to be used in the P2P communication
     * @param port The port to be used as listener, 0 if a ephemeral (random) one 
     * is to be assigned.
     * @return The port number that was assigned, same as argument if not ephemeral.
     * @throws IOException if the port could not be opened.
     */
    public int setListener(int port) throws IOException {
        
        serverSocket = new ServerSocket(port);
        return serverSocket.getLocalPort();
    }
    
    /**
     * Send a message notifying all peers you entered the network.
     * The message has the header 'hello'.
     */
    public void sayHello() {
        
        String header = "hello";
        String message = "";
        for (RemoteDevice peer : peers) {
            send(new Message(peer, header, message));
        }
    }
    
    /**
     * Send a message notifying all peers you are exiting the network.
     * The messages has the header 'goodbye'.
     */
    public void sayGoodbye() {
     
        String header = "goodbye";
        String message = "";
        for (RemoteDevice peer : peers) {
            send(new Message(peer, header, message));
        }
    }

    /**
     * @return the peers
     */
    public ArrayList<RemoteDevice> getPeers() {
        return peers;
    }
    
    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }
    
    /**
     * Looks for the peer object in the peer list given the id. 
     * @param id The id of the peer
     * @return The peer object, null if not found.
     */
    public RemoteDevice lookUpPeer(int id) {
        for (RemoteDevice peer : peers) {
            if (peer.getId() == id)
                return peer;
        }
        
        return null;
    }
}

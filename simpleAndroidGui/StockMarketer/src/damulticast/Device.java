
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Class that implements the P2P communication with other peers, and keeps tracks
 * of the Shared Resources and it's locks. For the time being the Shared Resources
 * are just variables with a String key, and a numerical value.
 * @author cesar
 */
public class Device implements Runnable {
    
    /** The id of the device */
    private int id;
    /** The class that keeps the state of shared resources */
    private SharedResources sharedResources;
    /** The peer list */
    private ArrayList<RemoteDevice> peers;
    /** Socket that will use for incoming P2P messages */
    private ServerSocket serverSocket;
    /** A counter for the id of outcoming messages */
    private int messageId;
    
    /**
     * Listening thread for incoming messages. 
     */
    public void run() {
        try {
            this.listen();
        } catch (IOException ioe) {
            System.err.println("General IOException: " + ioe);
        }
    }
    
    public Device() {
        this.id = 0;
        this.sharedResources = new SharedResources();
        this.peers = new ArrayList<RemoteDevice>();
        this.messageId = 0;
    }
    
    /**
     * Establishes the P2P connection with the tracker specified.
     * @param serverIP The ip address of the tracker.
     * @throws UnknownHostException If the tracker can't be found
     * @throws IOException If an IOException occurs in the communication
     */
    public void establishConnection(String serverIP) 
            throws UnknownHostException, IOException {
        
        Socket tracker = new Socket(serverIP, 12345);
        DataInputStream in = new DataInputStream(tracker.getInputStream());
        DataOutputStream out =new DataOutputStream(tracker.getOutputStream());

        /* Create a new listening socket from the ephemeral (random) port list */
        int port = this.setListener(0);
        out.writeInt(port);

        /* Read the id given and peer list */
        this.setId(in.readInt());
        boolean morePeers = in.readBoolean();
        while (morePeers) {
            RemoteDevice peer = new RemoteDevice(in.readInt(), 
                in.readUTF(), in.readInt());
            this.getPeers().add(peer);
            morePeers = in.readBoolean();
        }

        try {
            tracker.close();
        } catch (IOException ioe) {
            System.err.println("IOException while closing tracker "
                + ioe.getMessage());
        }
        
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
        
        RemoteDevice peer = m.getPeer();
        
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
        
        RemoteDevice peer = m.getPeer();
        System.out.println(peer.getId() + "> " + m.getId() + ":" + m.getHeader() 
            + ":" + m.getMessage());
        
        /* Message handlers for every type of message */
        
        /* hello */
        
        if (m.getHeader().equals("hello")) {
            if (!peers.contains(peer))
                peers.add(peer);
        
        /* goodbye */
        
        } else if (m.getHeader().equals("goodbye")) {
            peers.remove(peer);
        
        /* new_resource */
        
        } else if (m.getHeader().equals("new_resource")) {
            StringTokenizer st = new StringTokenizer(m.getMessage(), "|");
            try {
                String key = st.nextToken();
                getSharedResources().setValue(key, Integer.parseInt(st.nextToken()));
                getSharedResources().initLock(key);
            } catch (NoSuchElementException nsee) {
                System.err.println("Incorrect format for new resource.");
            } catch (NumberFormatException nfe) {
                System.err.println("Incorrect format for new resource.");
            }
        
        /* lock_resource */
        
        } else if (m.getHeader().equals("lock_resource")) {
            StringTokenizer st = new StringTokenizer(m.getMessage(), "|");
            try {
                String key = st.nextToken();
                ResourceState lock = sharedResources.getLock(key);
                /* Parse the timestamps */
                Calendar ct = Calendar.getInstance();
                ct.setTimeInMillis(Long.parseLong(st.nextToken()));
                long t = ct.getTimeInMillis();
                Calendar ctj = Calendar.getInstance();    
                /* We get our own timestamp just if we are going to use it */
                if (lock.getState().equals("HELD") || lock.getState().equals("WANTED")) {                    
                    ResourceRequest myRequest = lock.getRequester(-1);
                    ctj.setTimeInMillis(myRequest.getTimestamp());
                }
                long tj = ctj.getTimeInMillis();
                                
                if (lock == null) {
                    /* TODO: This shouldn't happen */
                    System.err.println("Resource does not exist: " + key);
                } else if (lock.getState().equals("HELD") 
                        || (lock.getState().equals("WANTED") && (tj < t))) {
                    lock.getRequestQueue().add(new ResourceRequest(peer, t));
                } else {
                    Message reply = new Message(peer, "lock_ack", key);
                    send(reply);
                }
            } catch (NoSuchElementException nsee) {
                System.err.println("Incorrect format for lock resource.");
            } catch (NumberFormatException nfe) {
                System.err.println("Incorrect format for lock resource.");
            }
        
        /* lock_ack */
        
        } else if (m.getHeader().equals("lock_ack")) {
            
            String key = m.getMessage();
            
            /* Update the number of replies received */       
            ResourceState lock = sharedResources.getLock(key);
            lock.setAcks(lock.getAcks() + 1);
            /* If we have received acks from all peers */
            if (lock.getAcks() == peers.size()) {
                lock.setState("HELD");
                /* This is the "alert" when we finally have the resource locked
                 * in the case of the real application it could instead 
                 * initiate a new thread and call a listener method in the game 
                 * that will execute some code.
                 */
                System.out.println("Resource held " + key + "! :) ");
            }
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
     * Adds a new shared resource, and sends the key and value to the
     * rest of the peers. The message has the header 'new_resource'.
     * @param key The id for the resource
     * @param value The value associated with it
     */
    public void addNewResource(String key, int value) {
        getSharedResources().setValue(key, value);
        getSharedResources().initLock(key);
        String header = "new_resource";
        String message = key + "|" + value;
        for (RemoteDevice peer : peers) {
            send(new Message(peer, header, message));
        }
    }
    
    /**
     * Initiates the lock procedure for a resource. It will change the state to
     * WANTED and will send the respective messages to the peers. This method will
     * be asynchronous, so it will not wait until it gets the resource held. The
     * device will receive an alert that all peers have replied via another message.
     * If the device has requested it before or it is held by the device, it will
     * return an exception as well.
     * The message has the header 'lock_resource'.
     * @param key The id of the resource
     * @throws NullPointerException If the resource does not exist, or it has been
     * requested by the device already.
     */
    public void lockResource(String key) throws NullPointerException {
        ResourceState lock = getSharedResources().getLock(key);
        if (lock == null) {
            throw new NullPointerException("Resource does not exist: " + key);
        } else if (lock.getState().equals("WANTED") || lock.getState().equals("HELD")) {
            throw new NullPointerException("The resource has been requested already.");
        }
        Calendar timestamp = Calendar.getInstance();
        lock.setState("WANTED");
        /* We add ourselves to the list just to keep track of timestamp */
        RemoteDevice thisDevice = new RemoteDevice(-1);
        lock.getRequestQueue().add(new ResourceRequest(thisDevice, timestamp.getTimeInMillis()));
        
        String header = "lock_resource";
        String message = key + "|" + timestamp.getTimeInMillis();
        for (RemoteDevice peer : peers) {
            send(new Message(peer, header, message));
        }
    }
    
    /**
     * Initiates the release procedure for a resource. It will change the state to
     * RELEASED and will process the request queue in case there are others waiting
     * for the lock. If the resource was not held, or if the resource does not exist
     * it will throw an Exception.
     * The message has the header 'release_resource'.
     * @param key The id of the resource
     * @throws NullPointerException If the resource does not exist, or it is not
     * held by the device.
     */
    public void releaseResource(String key) throws NullPointerException {
        ResourceState lock = getSharedResources().getLock(key);
        if (lock == null) {
            throw new NullPointerException("Resource does not exist: " + key);
        } else if (!lock.getState().equals("HELD")) {
            throw new NullPointerException("The resource is not being held.");
        }
        
        lock.setState("RELEASED");
        lock.setAcks(0);
        /* We remove ourselves from the queue and send pending replies */
        ArrayList<ResourceRequest> locks = lock.getRequestQueue();
        locks.remove(0);
        while (!locks.isEmpty()) {
            ResourceRequest req = locks.remove(0);
            Message reply = new Message(req.getRequester(), "lock_ack", key);
            send(reply);
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

    /**
     * @return the game
     */
    public SharedResources getSharedResources() {
        return sharedResources;
    }
}

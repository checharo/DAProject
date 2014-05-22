
package damulticast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
    /** A listener for the events in the protocol (the actual program running on a device) */
    private RicartListener listener;
    
    public static final int serverPort = 12345;
    
    /**
     * Listening thread for incoming messages. 
     */
    @Override
    public void run() {
        try {
            this.listen();
        } catch (IOException ioe) {
            System.err.println("General IOException: " + ioe);
        }
    }
    
    public Device(RicartListener listener) {
        this.id = 0;
        this.sharedResources = new SharedResources();
        this.peers = new ArrayList<RemoteDevice>();
        this.messageId = 0;
        this.listener = listener;
    }
    
    /**
     * Establishes the P2P connection with the tracker specified.
     * @param serverIP The ip address of the tracker.
     * @throws UnknownHostException If the tracker can't be found
     * @throws IOException If an IOException occurs in the communication
     */
    public void establishConnection(String serverIP) 
            throws UnknownHostException, IOException {
        
        Socket tracker = new Socket(serverIP, serverPort);
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
     * the incoming messages. This method should not be called directly by the application
     * rather, start a new Thread and run it.
     * The logic for handling the messages is in the receiveMessage method. This
     * method should not be edited anymore.
     * @throws IOException In case an unexpected error happens while reading connections
     */
    private void listen() throws IOException {
        
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
                
                /* If the sender is the tracker */
                if (senderId == -2) {
                    sender = new RemoteDevice(-2, 
                            peerSocket.getInetAddress().getHostAddress(), port);
                }
                /* If peer is not found, add it to the list */
                if (sender == null) {
                    sender = new RemoteDevice(senderId, 
                        peerSocket.getInetAddress().getHostAddress(), port);
                    peers.add(sender);
                }
                Message m = new Message(in.readInt(), sender, in.readUTF(), 
                    in.readUTF());
                /* If the message is sync then process the reply as soon as possible */
                if (!m.getHeader().startsWith("sync-")) {
                    receiveMessage(m);    
                } else {
                    Message reply = receiveMessageSync(m);
                    out.writeInt(id);
                    out.writeUTF(reply.getHeader());
                    out.writeUTF(reply.getMessage());
                }
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
     * method. It will act asynchronously for all messages, except if they are
     * prefixed with 'sync-' in the header. 
     * @param m The message to be sent.
     * @throws SocketTimeoutException If during the communication a message takes
     * more than 5 seconds to get through.
     * @throws IOException If an IO error happens during the communication
     * @throws UnkownHostException This shouldn't happen when using IP addresses.
     */
    public synchronized void send(Message m) throws SocketTimeoutException,
            IOException, UnknownHostException {
        
        RemoteDevice peer = m.getPeer();
        Message reply = null;
        
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
        
        /* If the message is sync, implement the receive instruction */
        if (m.getHeader().startsWith("sync-")) {
            int replyID = in.readInt();
            String replyHeader = in.readUTF();
            String replyMessage = in.readUTF();
            receiveMessage(new Message(peer, replyHeader, replyMessage));
        }

        try {
            peerSocket.close();
        } catch (IOException ioe) {
            System.err.println("IOException while closing peer "
                + ioe.getMessage());
        }
    }
    
    /**
     * Contains the logic to be implemented when a message from a peer is 
     * received. The receive logic of sync reply messages MUST NOT use the send 
     * method again, or it will be deadlocked. 
     * @param m The incoming message from the peer;
     */
    public synchronized void receiveMessage(Message m) {
        
        RemoteDevice peer = m.getPeer();
        /* Debug */
        if (!m.getHeader().equals("ping")) {
            System.out.println(peer.getId() + "> " + m.getId() + ":" + m.getHeader() 
                + ":" + m.getMessage());
        }
        
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
                                
                if (lock.getState().equals("HELD") 
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
            } catch (SocketTimeoutException ste) {
                System.err.println("Timeout for the message: " + m.getId() 
                    + "." + ste.getMessage());
            } catch (UnknownHostException uhe) {
                System.err.println("UHE for message: " + m.getId() 
                    + "." + uhe.getMessage());
            } catch (IOException ioe) {
                System.err.println("IOE for message: " + m.getId() 
                    + "." + ioe.getMessage());
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
                /* We send the event to the main device so it knows we have
                 * held the device.
                 */
                listener.lockGranted(key);
            }
     
        /* ping, the pings include the list of peers the tracker considers have
         * disconnected */
        
        } else if (m.getHeader().equals("ping")) {
            
            StringTokenizer st = new StringTokenizer(m.getMessage(), "|");
            while (st.hasMoreTokens()) {
                RemoteDevice dpeer = lookUpPeer(Integer.parseInt(st.nextToken()));
                peers.remove(dpeer);
                System.err.println("Tracker has dismissed peer " + dpeer.getId());
            }
        
        /* reply-askstate */
        
        } else if (m.getHeader().equals("reply-askstate")) {
            if (!m.getMessage().equals("")) {
                StringTokenizer st = new StringTokenizer(m.getMessage(), "&");
                while (st.hasMoreTokens()) {
                    StringTokenizer st2 = new StringTokenizer(st.nextToken(), "|");
                    String key = st2.nextToken();
                    String value = st2.nextToken();
                    System.out.println("Adding new resource: " + key + "=" + value);
                    if (sharedResources.getValues().containsKey(key)) {
                        System.err.println("Resource " + key + " was already created,"
                            + " it will not be overwritten.");
                    } else {
                        sharedResources.setValue(key, Integer.parseInt(value));
                        sharedResources.initLock(key);
                    }
                    /* We notify the application in case it is useful */
                    listener.resourceUpdate(key, Integer.parseInt(value));
                }
            }
            
        /* update_resource */
        
        } else if (m.getHeader().equals("update_resource")) {
            StringTokenizer st = new StringTokenizer(m.getMessage(), "|");
            try {
                String key = st.nextToken();
                String value = st.nextToken();
                
                /* By setting the value we create it in case we don't have
                 * created it yet. */
                sharedResources.setValue(key, Integer.parseInt(value));
                System.out.println("Peer " + peer.getId() + " updated resource"
                    + " " + key + "=" + value);
                /* But we need to check that the lock is initialized. */
                if (sharedResources.getLock(key) == null) {
                    sharedResources.initLock(key);
                }
            } catch (NoSuchElementException nsee) {
                System.err.println("Incorrect format for lock resource.");
            } catch (NumberFormatException nfe) {
                System.err.println("Incorrect format for lock resource.");
            }
        }
    }
    
    /**
     * Contains the logic to be implemented when a message from a peer is 
     * received. This method is special for sync replies, since it returns the 
     * reply rather than calling for the send message again. 
     * @param m The incoming message from the peer;
     */
    public synchronized Message receiveMessageSync(Message m) {
        
        RemoteDevice peer = m.getPeer();
        /* Debug */
        if (!m.getHeader().equals("ping")) {
            System.out.println(peer.getId() + "> " + m.getId() + ":" + m.getHeader() 
                + ":" + m.getMessage());
        }
        
        /* Message handlers for every type of message */
        
        /* sync-askstate */
        
        if (m.getHeader().equals("sync-askstate")) {
            /* Update the number of replies received */ 
            String replyHeader = "reply-askstate";
            String replyMessage = "";
            HashMap<String, Integer> resources = new HashMap<String, Integer>(sharedResources.getValues());
            
            for (String key : resources.keySet()) {
                replyMessage += key + "|" + resources.get(key) + "&";
            }
            if (!replyMessage.equals("")) {
                replyMessage = replyMessage.substring(0, replyMessage.length() - 1);
            }
            
            
            return new Message(peer, replyHeader, replyMessage);     
        } else {
            return null;
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
     * The message has the header 'hello'. If an Exception occurs while saying
     * hello to one or many of the peers, it will just print it on the err stream
     * but will not stop. The recommendation would be to stop the device.
     */
    public void sayHello() {
        
        String header = "hello";
        String message = "";
        ArrayList<RemoteDevice> peersCopy = new ArrayList<RemoteDevice>(peers);
        for (RemoteDevice peer : peersCopy) try {
            send(new Message(peer, header, message));
        } catch (SocketTimeoutException ste) {
            System.err.println("Timeout when saying hello to: " + peer.getId() 
                + "." + ste.getMessage());
        } catch (UnknownHostException uhe) {
            System.err.println("UHE when saying hello to: " + peer.getId() 
                + "." + uhe.getMessage());
        } catch (IOException ioe) {
            System.err.println("IOE when saying hello to: " + peer.getId() 
                + "." + ioe.getMessage());
        }
    }
    
    /**
     * Send a message notifying all peers you are exiting the network.
     * The messages has the header 'goodbye'. If an error happens while saying 
     * goodbye it will just print it.
     */
    public void sayGoodbye() throws SocketTimeoutException, UnknownHostException, 
            IOException {
     
        String header = "goodbye";
        String message = "";
        ArrayList<RemoteDevice> peersCopy = new ArrayList<RemoteDevice>(peers);
        for (RemoteDevice peer : peersCopy) try {
            send(new Message(peer, header, message));
        } catch (SocketTimeoutException ste) {
            System.err.println("Timeout when saying goodbye to: " + peer.getId() 
                + "." + ste.getMessage());
        } catch (UnknownHostException uhe) {
            System.err.println("UHE when saying goodbye to: " + peer.getId() 
                + "." + uhe.getMessage());
        } catch (IOException ioe) {
            System.err.println("IOE when saying goodbye to: " + peer.getId() 
                + "." + ioe.getMessage());
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
        ArrayList<RemoteDevice> peersCopy = new ArrayList<RemoteDevice>(peers);
        for (RemoteDevice peer : peersCopy) try {
            send(new Message(peer, header, message));
        } catch (SocketTimeoutException ste) {
            System.err.println("Timeout notifying new resource to: " + peer.getId() 
                + "." + ste.getMessage());
        } catch (UnknownHostException uhe) {
            System.err.println("UHE notifying new resource to: " + peer.getId() 
                + "." + uhe.getMessage());
        } catch (IOException ioe) {
            System.err.println("IOE notifying new resource to: " + peer.getId() 
                + "." + ioe.getMessage());
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
     * @throws SocketTimeoutException If during the communication a message takes
     * more than 5 seconds to get through.
     * @throws IOException If an IO error happens during the communication
     * @throws UnkownHostException This shouldn't happen when using IP addresses.
     * 
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
        ArrayList<RemoteDevice> peersCopy = new ArrayList<RemoteDevice>(peers);
        
        /* If peers copy is emtpy grant the lock right away */
        if (peersCopy.isEmpty()) {
            lock.setState("HELD");
            listener.lockGranted(key);
        } else for (RemoteDevice peer : peersCopy) {
            try {
                send(new Message(peer, header, message));
            } catch (IOException ioe) {
                System.err.println("Could not reach peer: " + peer.getId() + ", "
                        + ioe.getMessage());
                /* The algorithm will ignore the peer for the voting */
                lock.setAcks(lock.getAcks() + 1);
            }
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
     * @throws SocketTimeoutException If during the communication a message takes
     * more than 5 seconds to get through.
     * @throws IOException If an IO error happens during the communication
     * @throws UnkownHostException This shouldn't happen when using IP addresses.
     */
    public void releaseResource(String key) throws NullPointerException, 
            SocketTimeoutException, UnknownHostException, IOException {
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
     * Updates the value of a resource, and sends the corresponding messages to the
     * rest of the peers. 
     * The message has the header 'update_resource'.
     * @param key The id of the resource
     * @throws NullPointerException If the resource does not exist, or it is not
     * held by the device.
     */
    public void updateResource(String key, int value) throws NullPointerException, 
            UnknownHostException {
        ResourceState lock = getSharedResources().getLock(key);
        if (lock == null) {
            throw new NullPointerException("Resource does not exist: " + key);
        } else if (!lock.getState().equals("HELD")) {
            throw new NullPointerException("The resource is not being held.");
        }
        
        getSharedResources().setValue(key, value);
        
        ArrayList<RemoteDevice> peersCopy = new ArrayList<RemoteDevice>(peers);
        for (RemoteDevice peer : peersCopy) {
            Message m = new Message(peer, "update_resource", key + "|" + value);
            try {
                send(m);
            } catch (IOException ioe) {
                System.err.println("The resource update could not be sent to "
                    + "the peer " + peer.getId());
            }
        }
    }
    
    /**
     * Asks for the state of the game to one of the peers. If a peer does not 
     * respond it tries with the next one. If all the peers don't respond the
     * the corresponding exception is thrown.
     * @throws SocketTimeoutException If all peers timeout.
     * @throws IOException If communication with all peers fail.
     */
    public void askForState() throws SocketTimeoutException, IOException {
        
        /* Ask the game's state to the peers synchronously until one of them 
         * replies. */
        String header = "sync-askstate";
        String message = "";
        ArrayList<RemoteDevice> peersCopy = new ArrayList<RemoteDevice>(peers);
        for (RemoteDevice peer : peersCopy) {
            try {
                send(new Message(peer, header, message));
                /* As soon as we receive a succesful reply, break */
                 break;
            } catch (SocketTimeoutException ste) {
                System.err.println("Peer " + peer.getId() + " is not responding.");
                /* If it's the last peer throw the exception */
                if (peers.isEmpty()) {
                    throw ste;
                }
            } catch (IOException ioe) {
                System.err.println("Peer " + peer.getId() + " is not responding.");
                /* If it's the last peer throw the exception */
                if (peers.isEmpty()) {
                    throw ioe;
                }
            }
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
        ArrayList<RemoteDevice> peersCopy = new ArrayList<RemoteDevice>(peers);
        for (RemoteDevice peer : peersCopy) {
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

package damulticast;


import damulticast.Device;
import damulticast.Message;
import damulticast.RemoteDevice;
import damulticast.ResourceState;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Implements the command line demo for devices in the P2P network.
 * @author cesar
 */
public class Device_Main {
    
    public static void main(String args[]) {
        
        if (args.length < 1) {
            System.err.println("Usage: device.sh <ip-address>\n"
                + "ip-address: The tracker's ip address");
            System.exit(1);
        }
        
        String serverIP = args[0];
        serverIP="localhost";
        Device device = new Device();
        
        /* Establish the connection with tracker */
        try {
            device.establishConnection(serverIP);
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
                
                /* peerlist, prints the peerlist on screen */
                
                if (command.equals("peerlist")) {
                    if (device.getPeers().isEmpty())
                        System.out.println("Peerlist empty");
                    else for (RemoteDevice peer : device.getPeers()) {
                        System.out.println(peer.getId() + " -> " 
                            + peer.getIpAddress() + ":" + peer.getPort());
                    }
                
                /* send, sends a customized message to the peers */
                
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
                
                /* new, sends a message notifying of a new shared resource */
                
                } else if (command.startsWith("new")) {
                    StringTokenizer st = new StringTokenizer(command, " ");
                    st.nextToken();
                    try {
                        st = new StringTokenizer(st.nextToken(), "|");
                        String key = st.nextToken();
                        int value = Integer.parseInt(st.nextToken());
                        if (device.getSharedResources().hasValue(key)) {
                            System.err.println("The system already has a value for: " 
                                + key);
                        } else {
                            device.addNewResource(key, value);
                        }
                    } catch (NoSuchElementException nsee) {
                        System.err.println("usage: new <resource_name>|<value>");
                    } catch (NumberFormatException nfe) {
                        System.err.println("value must be numerical");
                    }
                
                /* resources, prints a list of the shared resources and their 
                 * state IN THIS DEVICE */
                
                } else if (command.startsWith("resources")) {
                    HashMap<String, Integer> values = device.getSharedResources().getValues();
                    if (values.isEmpty()) {
                        System.out.println("Resources list is empty.");
                    } else for (String key : values.keySet()) {
                        String entry = key + ":" + values.get(key);
                        HashMap<String, ResourceState> locks = device.getSharedResources().getLocks();
                        try {
                            entry += ":" + locks.get(key).getState();
                        } catch (NullPointerException npe) {
                            entry += ":LOCK_NOT_INITIALIZED";
                        }
                        System.out.println(entry);
                    }
                            
                /* lock, initiates the lock procedure for a shared resource */
                
                } else if (command.startsWith("lock")) {
                    HashMap<String, Integer> values = device.getSharedResources().getValues();
                    StringTokenizer st = new StringTokenizer(command, " ");
                    st.nextToken();
                    try {
                        String key = st.nextToken();
                        if (device.getSharedResources().hasValue(key)) {
                            device.lockResource(key);
                        } else {                           
                            System.err.println("Resource not found: " 
                                + key);
                        }
                    } catch (NoSuchElementException nsee) {
                        System.err.println("usage: lock <resource_name>|<value>");
                    } catch (NullPointerException npe) {
                        /* Resource was wanted or held already, or does not exist */
                        System.err.println(npe.getMessage());
                    }
                            
                /* release, releases the lock for this resource and processes 
                 * the request queue */
                
                } else if (command.startsWith("release")) {
                    HashMap<String, Integer> values = device.getSharedResources().getValues();
                    StringTokenizer st = new StringTokenizer(command, " ");
                    st.nextToken();
                    try {
                        String key = st.nextToken();
                        if (device.getSharedResources().hasValue(key)) {
                            device.releaseResource(key);
                        } else {                           
                            System.err.println("Resource not found: " 
                                + key);
                        }
                    } catch (NoSuchElementException nsee) {
                        System.err.println("usage: release <resource_name>");
                    } catch (NullPointerException npe) {
                        /* Resource was wanted or held already, or does not exist */
                        System.err.println(npe.getMessage());
                    }
                            
                /* exit, exits the P2P interaction */
                
                } else if (command.equals("exit")) {
                    device.sayGoodbye();
                    t.interrupt();
                    break;
                } else {
                    System.out.println("Not a valid command:\npeerlist\nexit\n"
                        + "new\nresources\nlock\nrelease");
                }
            } catch (IOException ioe) {
                System.out.println("IOException while reading line: " 
                    + ioe.getMessage());
            }
        }
        
        System.exit(0);
    }
}

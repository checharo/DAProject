
package damulticast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Class that implements the tracker of the P2P network. The tracker's sole purpose
 * is to initialize the peer list in every device joining the P2P network. Once
 * this is done, it closes the connection. This is the only process that will 
 * listen on a fixed port (12345), the rest of the ports will be ephemeral.
 * @author cesar
 */
public class Tracker implements Runnable {
    
    /** Stores the list of peers in the network */
    private ArrayList<RemoteDevice> peers;
    /** Stores the server port for the tracker */
    public static final int serverPort = 12345;
    
    /**
     * Listening thread for new peers. 
     */
    public void run() {
        
        try {
            this.listen();
        } catch (IOException ioe) {
            System.err.println("General IOException: " + ioe);
        }
    }
    
    public Tracker() {
        this.peers = new ArrayList<RemoteDevice>();
    }
    
    /**
     * The listen method for the servicing new peers. 
     * @throws IOException In case an unexpected error happens while reading connections
     */
    public void listen() throws IOException {
        int id = 0;
        ServerSocket listenSocket = new ServerSocket(serverPort);
        while(true) {
            Socket clientSocket = listenSocket.accept();
            clientSocket.setSoTimeout(5000);
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out =new DataOutputStream(clientSocket.getOutputStream());

            try {  
                String ipAddress = clientSocket.getInetAddress().getHostAddress();
                
                /* Ask peer which port will it use for the P2P communication */
                int assignedPort = in.readInt();
                /* Assign an id to peer, send it and close connection */
                id++;
                out.writeInt(id);
                RemoteDevice newClient = new RemoteDevice(id, ipAddress, assignedPort);
                
                /* Send the peer list */
                ArrayList<RemoteDevice> peersCopy = new ArrayList<RemoteDevice>(peers);
                for (RemoteDevice peer : peersCopy) {
                    out.writeBoolean(true);
                    out.writeInt(peer.getId());
                    out.writeUTF(peer.getIpAddress());
                    out.writeInt(peer.getPort());    
                }
                out.writeBoolean(false);
                
                /* Add new peer to peer list */
                getPeers().add(newClient);
            } catch (SocketTimeoutException ste) {
                System.err.println("Socket timeout from peer: "
                    + clientSocket.getInetAddress().getHostAddress());
            } catch (IOException ioe) {
                System.err.println("IOE exception while initializing peer: " 
                    + clientSocket.getInetAddress().getHostAddress());
            } finally {
                clientSocket.close();
            }
        }
    }
    
    /**
     * Pings a peer to test if it's still alive
     * @throws IOException In case an unexpected error happens while reading connections
     */
    public void ping(RemoteDevice peer) throws IOException {
        
        try {
            Socket peerSocket = new Socket();
            peerSocket.connect(new InetSocketAddress(peer.getIpAddress(), 
                peer.getPort()), 5000);
            
            DataInputStream in = new DataInputStream(peerSocket.getInputStream());
            DataOutputStream out =new DataOutputStream(peerSocket.getOutputStream());
            
            /* The tracker will use the special ID -2 */
            out.writeInt(-2);
            out.writeInt(serverPort);
            
            out.writeInt(0);
            out.writeUTF("ping");
            out.writeUTF("pong");
            
            try {
                peerSocket.close();
            } catch (IOException ioe) {
                System.err.println("IOException while closing peer "
                    + ioe.getMessage());
            }
        /* The peer appears to be disconnected, remove it from the peerlist */    
        } catch (SocketTimeoutException ste) {
            System.out.println("Peer " + peer.getId() + " is no longer responding.");
            peers.remove(peer);
        } catch (UnknownHostException uhe) {
            System.err.println("Unknown host: " + peer.getIpAddress());
        } catch (IOException ioe) {
            System.out.println("Peer " + peer.getId() + " is no longer responding.");
            peers.remove(peer);
        }
    }

    /**
     * @return the peers
     */
    public ArrayList<RemoteDevice> getPeers() {
        return peers;
    }
    
}

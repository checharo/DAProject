
package damulticast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 *
 * @author cesar
 */
public class Tracker implements Runnable {
    
    private ArrayList<RemoteDevice> peers;
    
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
    
    public void listen() throws IOException {
        int serverPort = 12345;
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
                for (RemoteDevice peer : getPeers()) {
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

    /*
    /**
     * Checks whether a port has been used by a previous peer.
     * @param port The port to query
     * @return true if it has been used, false otherwise
     * This method has been commented since this actions seems unnecessary
    public boolean portIsAssigned(int port) {
        
        for (RemoteDevice rd : peers) {
            if (rd.getPort() == port)
                return true;
        }
        
        return false;
    }
    */ 

    /**
     * @return the peers
     */
    public ArrayList<RemoteDevice> getPeers() {
        return peers;
    }
    
}

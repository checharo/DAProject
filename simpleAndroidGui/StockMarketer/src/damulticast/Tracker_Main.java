
package damulticast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Implements the command line demo for the tracker in the P2P network.
 * @author cesar
 */
public class Tracker_Main {
    
    public static void main(String args[]) {
        
        Tracker tracker = new Tracker();
        Thread t = new Thread(tracker);
        t.start();
        
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("tracker> ");
            try {
                String command = stdIn.readLine();
                if (command.equals("peerlist")) {
                    if (tracker.getPeers().isEmpty())
                        System.out.println("Peerlist empty");
                    else for (RemoteDevice peer : tracker.getPeers()) {
                        System.out.println(peer.getId() + " -> " 
                            + peer.getIpAddress() + ":" + peer.getPort());
                    }
                } else if (command.equals("exit")) {
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
}

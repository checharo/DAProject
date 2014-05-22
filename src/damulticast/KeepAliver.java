
package damulticast;

import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author cesar
 */
public class KeepAliver implements Runnable {
    
    private Tracker t;
    
    /* The time each cycle of keep alive will last */
    public static final int DELAY = 5000;
    
    @Override
    public void run() {
               
        while (true) {
            /* Create a copy of the list to avoid problems with multiple threads
             * modifying it.
             */
            ArrayList<RemoteDevice> peers = new ArrayList<RemoteDevice>(t.getPeers());
            try {
                Thread.sleep(DELAY);    
            /* If interrupted just stop the cycle */
            } catch (InterruptedException ie) {
                break;
            }
            
            ArrayList<RemoteDevice> peersCopy = new ArrayList<RemoteDevice>(peers);
            for (RemoteDevice peer : peersCopy) try {
                t.ping(peer);
            } catch (IOException ioe) {
                System.err.println("Error while pinging peer " + peer.getId());
            }
        }
    }
    
    public KeepAliver(Tracker t) {
        this.t = t;
    }
}

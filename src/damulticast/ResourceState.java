
package damulticast;

import java.util.ArrayList;

/**
 * Stores the state of a shared resource in a device. It is very important to note
 * that the state of the resources corresponds to the state in the device ONLY. 
 * This means that if a resource appears as RELEASED in this device, it does not
 * mean it might not be HELD or WANTED in another device (check the algorithm for
 * details). It also holds the queue in case requests have to be queued and the
 * number of acknowledgements for the WANTED request that have been received.
 * @author cesar
 */
public class ResourceState {

    /** The state of the resource (RELEASED, WANTED, or HELD) */
    private String state;
    /** The queue of requests, if the device wants it it will always be first */
    private ArrayList<ResourceRequest> requestQueue;
    /** The number of replies received for a request of a shared resource */
    private long acks;

    public ResourceState(String state, ArrayList<ResourceRequest> requestQueue) {
        this.state = state;
        this.requestQueue = requestQueue;
    }
    
    public ResourceState(String state) {
        this.state = state;
        this.requestQueue = new ArrayList<ResourceRequest>();
    }
    
    /**
     * Returns a ResourceRequest given the id of the requester from the queue.
     * @param id
     * @return The requester or null if not found
     */
    public ResourceRequest getRequester(int id) {
        for (ResourceRequest requester : requestQueue) {
            if (requester.getRequester().getId() == id)
                return requester;
        }
        return null;
    }
    
    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @return the requestQueue
     */
    public ArrayList<ResourceRequest> getRequestQueue() {
        return requestQueue;
    }

    /**
     * @param requestQueue the requestQueue to set
     */
    public void setRequestQueue(ArrayList<ResourceRequest> requestQueue) {
        this.requestQueue = requestQueue;
    }

    /**
     * @return the acks
     */
    public long getAcks() {
        return acks;
    }

    /**
     * @param acks the acks to set
     */
    public void setAcks(long acks) {
        this.acks = acks;
    }
    
    
}

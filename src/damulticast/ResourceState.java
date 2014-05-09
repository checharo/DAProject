
package damulticast;

import java.util.ArrayList;

/**
 *
 * @author cesar
 */
public class ResourceState {

    private String state;
    private ArrayList<ResourceRequest> requestQueue;
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
     * Returns a ResourceRequest given the id of the requester
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


package damulticast;

import java.util.ArrayList;

/**
 *
 * @author cesar
 */
public class ResourceState {

    private String state;
    private ArrayList<ResourceRequest> requestQueue;

    public ResourceState(String state, ArrayList<ResourceRequest> requestQueue) {
        this.state = state;
        this.requestQueue = requestQueue;
    }
    
    public ResourceState(String state) {
        this.state = state;
        this.requestQueue = new ArrayList<ResourceRequest>();
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
    
    
}

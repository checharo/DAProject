package damulticast;

/**
 * Encapsulates a reference to a request of a shared resource. This is used for
 * two things. One is to store the requests that have to be added to the queue
 * according to the algorithm, each request will have it's own timestamp. The second
 * usage is to store the timestamp of the own device's request, in order to compare
 * it to the timestamp of the peers. 
 * @author cesar
 */
public class ResourceRequest {
    
    /** The reference to the requester of the resource */
    private RemoteDevice requester;
    /** The timestamp for the request */
    private long timestamp;
    
    public ResourceRequest(RemoteDevice requester, long timestamp) {
        this.requester = requester;
        this.timestamp = timestamp;
    }
    
    /**
     * @return the requester
     */
    public RemoteDevice getRequester() {
        return requester;
    }

    /**
     * @param requester the requester to set
     */
    public void setRequester(RemoteDevice requester) {
        this.requester = requester;
    }

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    
}

package damulticast;

/**
 *
 * @author cesar
 */
public class ResourceRequest {
    
    private RemoteDevice requester;
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

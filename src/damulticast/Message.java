
package damulticast;

/**
 * Encapsulates messages exchanged between the P2P peers. Each message has an id
 * associated with the peer (each peer has its own counter). The header is used
 * for a receiver to know how to process the message.
 * @author cesar
 */
public class Message {
    
    /** The id of the message with respect of this peer */
    private int id;
    /** Contains the id of the peer of the message, or the receiver if sending */
    private RemoteDevice peer;
    /** Contains the header of the message */
    private String header;
    /** Contains the content of the message */
    private String message;

    public Message(RemoteDevice peer, String header, String message) {
        this.peer = peer;
        this.header = header;
        this.message = message;
    }
    
    public Message(int id, RemoteDevice peer, String header, String message) {
        this.id = id;
        this.peer = peer;
        this.header = header;
        this.message = message;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the senderId
     */
    public RemoteDevice getPeer() {
        return peer;
    }

    /**
     * @param senderId the senderId to set
     */
    public void setSenderId(RemoteDevice sender) {
        this.peer = sender;
    }

    /**
     * @return the header
     */
    public String getHeader() {
        return header;
    }

    /**
     * @param header the header to set
     */
    public void setHeader(String header) {
        this.header = header;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Equals method. A message will be equal to another just if the id and 
     * sender are the same.
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Message other = (Message) obj;
        if (this.id != other.id) {
            return false;
        }
        if (!this.peer.equals(other.getPeer())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + this.id;
        hash = 13 * hash + (this.peer != null ? this.peer.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "Message{" + "id=" + id + ", peer=" + peer.getId()+ ", header=" 
            + header + ", message=" + message + '}';
    }
    
}

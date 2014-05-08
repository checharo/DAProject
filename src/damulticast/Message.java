
package damulticast;

/**
 *
 * @author cesar
 */
public class Message {
    
    private int id;
    private RemoteDevice sender;
    private String header;
    private String message;

    public Message(RemoteDevice sender, String header, String message) {
        this.sender = sender;
        this.header = header;
        this.message = message;
    }
    
    public Message(int id, RemoteDevice sender, String header, String message) {
        this.id = id;
        this.sender = sender;
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
    public RemoteDevice getSender() {
        return sender;
    }

    /**
     * @param senderId the senderId to set
     */
    public void setSenderId(RemoteDevice sender) {
        this.sender = sender;
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
        if (!this.sender.equals(other.getSender())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Message{" + "id=" + id + ", sender=" + sender.getId()+ ", header=" + header + ", message=" + message + '}';
    }
    
}

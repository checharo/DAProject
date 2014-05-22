
package damulticast;

/**
 *
 * @author cesar
 */
public interface RicartListener {
    
    public void resourceUpdate(String resource, int value);
    
    public void lockGranted(String resource);
}

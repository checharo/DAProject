package damulticast;

import java.util.HashMap;

/**
 * Contains the keys and values of the shared resources in the P2P network. 
 * It also contains the resource state for the locks for each resource. For the
 * time being the shared resources are just values in a hashmap, identified by
 * a key (String). The lock and its state are encapsulated in a ResourceState
 * object.
 * @author cesar
 */
public class SharedResources {
   
    private HashMap<String, Integer> values;
    private HashMap<String, ResourceState> locks;
    
    public SharedResources() {
        values = new HashMap();
        locks = new HashMap();
    }
    
    public int getValue(String key) {
        return getValues().get(key).intValue();
    }
    
    public void setValue(String key, int i) {
        getValues().put(key, new Integer(i));
    }
    
    public ResourceState getLock(String key) {
        return getLocks().get(key);
    }
    
    public boolean hasValue(String key) {
        return getValues().containsKey(key);
    }
    
    public void initLock(String key) {
        getLocks().put(key, new ResourceState("RELEASED"));
    }
    
    public void setLock(String key, ResourceState state) {
        getLocks().put(key, state);
    }

    /**
     * @return the values
     */
    public HashMap<String, Integer> getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(HashMap<String, Integer> values) {
        this.values = values;
    }

    /**
     * @return the locks
     */
    public HashMap<String, ResourceState> getLocks() {
        return locks;
    }

    /**
     * @param locks the locks to set
     */
    public void setLocks(HashMap<String, ResourceState> locks) {
        this.locks = locks;
    }
}

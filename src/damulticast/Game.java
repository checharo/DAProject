package damulticast;


import java.util.HashMap;


/**
 *
 * @author cesar
 */
public class Game {
   
    private HashMap<String, Integer> values;
    
    public Game() {
        values = new HashMap();
    }
    
    public int getValue(String key) {
        return values.get(key).intValue();
    }
    
    public void setValue(String key, int i) {
        values.put(key, new Integer(i));
    }
}

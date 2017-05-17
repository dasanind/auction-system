import java.io.PrintStream;

public class PropagateItemEvent implements Event {

	public void publish(int serverPort, Item itemDetails, PrintStream pout) {
		
	}
	
    public void propagate(Item item, PrintStream pout, String eventType) {
    	item.sendItem(pout, eventType);
    }
}
import java.io.PrintStream;

public class BidUpdateEvent implements Event {

	public void publish(int ServerPort, Item item, PrintStream pout) {
		item.sendItem(pout, "bidUpdateEvent");
	}
	
    public void propagate(Item itemDetails, PrintStream pout) {
    	
    }
}

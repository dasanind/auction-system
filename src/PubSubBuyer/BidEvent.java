import java.io.PrintStream;

public class BidEvent implements Event {

	public void publish(int ServerPort, Item item, PrintStream pout) {
		item.sendItem(pout, "bidEvent");
	}
	
    public void propagate(Item itemDetails, PrintStream pout) {
    	
    }
}
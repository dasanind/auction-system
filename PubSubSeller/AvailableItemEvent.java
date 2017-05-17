import java.io.PrintStream;

public class AvailableItemEvent implements Event {

	public void publish(int serverPort, Item item, PrintStream pout) {
		item.sendItem(pout, "availableItemEvent");
	}
	
    public void propagate(Item itemDetails, PrintStream pout) {
    	
    }
}
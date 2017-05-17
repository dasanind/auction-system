import java.io.PrintStream;

public class ViewMatchedInterestEvent implements Event {

	public void publish(int ServerPort, Item item, PrintStream pout) {
		item.sendItem(pout, "viewMatchedInterestEvent");
	}
	
    public void propagate(Item itemDetails, PrintStream pout) {
    	
    }
}
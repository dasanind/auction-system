import java.io.PrintStream;

public class DeclareInterestEvent implements Event {

	public void publish(int ServerPort, Item item, PrintStream pout) {
		item.sendItem(pout, "declareInterestEvent");
	}
	
    public void propagate(Item itemDetails, PrintStream pout) {
    	
    }
}
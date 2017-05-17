import java.io.PrintStream;

public class SaleFinalizedEvent implements Event {

	public void publish(int ServerPort, Item itemDetails, PrintStream pout) {
		itemDetails.sendItem(pout, "saleFinalizedEvent");
	}
	
    public void propagate(Item itemDetails, PrintStream pout) {
    	
    }
}
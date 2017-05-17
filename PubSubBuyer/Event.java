import java.io.PrintStream;

public interface Event {
    public void publish(int ServerPort, Item itemDetails, PrintStream pout); 
    public void propagate(Item itemDetails, PrintStream pout);
}
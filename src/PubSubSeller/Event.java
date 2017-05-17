import java.io.PrintStream;

public interface Event {
    public void publish(int ServerPort, Item item, PrintStream pout); 
    public void propagate(Item item, PrintStream pout);
}
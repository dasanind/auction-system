import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;


public class ListenerThreadSeller extends Thread {
    
    int serverPortClient;
    ArrayList<Item> addedItemList;
    int serverPort;
    String brokerServerName;
    
    Socket server;
    BufferedReader din;
    PrintStream pout;  
    
/*  Constructor for the ListenerThreadSeller class */    
	public ListenerThreadSeller(int serverPortClient, ArrayList<Item> addedItemList, int serverPort, String brokerServerName) {
    	this.serverPortClient = serverPortClient;
    	this.addedItemList = addedItemList;
    	this.serverPort = serverPort;
    	this.brokerServerName = brokerServerName;
    }
	
    synchronized public void run() {
        try {
        	ServerSocket listener = new ServerSocket(serverPortClient);
				while(true) {
					Socket aServer = listener.accept();
					handleserver(aServer);
					aServer.close();
				}
		}
		catch (Exception e) {
//			System.err.println("listener aborted in ListenerThreadSeller: "+ e);
		}
    }
    
 /* Converts the cents to dollars*/
    public String centsToDollar(int val) {
    	int dollar = val/100;
    	int cent = val%100;
    	String value = dollar + "." + cent;
    	return (value);
    	
    }
    
 /* Get the list of subscribers to the item */	
	public ArrayList<String> getBidSubscribers(String bidUpdateSubscribersSize, String bidUpdateSubscribers) {
		ArrayList<String> bidUpdateSubscribersList;
		//converting string to arrayList
        if(Integer.parseInt(bidUpdateSubscribersSize) == 0) {
        	bidUpdateSubscribersList = new ArrayList<String>();
        } else {
        	String temp = bidUpdateSubscribers.replace("[", "").replace("]", "");
        	bidUpdateSubscribersList = new ArrayList<String>(Arrays.asList(temp.split(", ")));
        }
        return bidUpdateSubscribersList;
	}
	
	public void getSocket() throws IOException { 
			server = new Socket(brokerServerName, serverPort);
			din = new BufferedReader(new InputStreamReader(server.getInputStream()));
			pout = new PrintStream(server.getOutputStream());
    }
	
/* Update the bid on an item */	
	public void updateBid(Item addedItem) throws IOException {
        getSocket();     
        BidUpdateEvent updateBidForItem= new BidUpdateEvent();
		updateBidForItem.publish(serverPort, addedItem, pout);
    }
	 
  
	synchronized void handleserver(Socket theServer) {
		try {				
			BufferedReader din = new BufferedReader(new InputStreamReader(theServer.getInputStream()));
			ArrayList<String> bidUpdateSubscribersList = new ArrayList<String>();
			
			String getline = din.readLine();
			StringTokenizer st = new StringTokenizer(getline, ":");
			String tag = st.nextToken();
			if(tag.equals("BidUpdated")) {
				String itemId = st.nextToken();
				Iterator<Item> addedItemItr = addedItemList.iterator();
				while(addedItemItr.hasNext()) {
					Item addedItem = (Item) addedItemItr.next(); 
					if(addedItem.getItemId().equalsIgnoreCase(itemId)) {
						addedItem.setBuyerId(st.nextToken());
						String currentBidString = st.nextToken();
						int currentBidcents = Integer.parseInt(currentBidString);
						String currentBidDollar = centsToDollar(currentBidcents);
						addedItem.setCurrentBid(currentBidString);
						addedItem.setItemStatus(st.nextToken());
						String bidUpdateSubscribers = st.nextToken();
						String bidUpdateSubscribersSize = st.nextToken();
						bidUpdateSubscribersList = getBidSubscribers(bidUpdateSubscribersSize, bidUpdateSubscribers);
						addedItem.setBidUpdateSubscribers(bidUpdateSubscribersList);
						
						System.out.println("\nBid received for the following item:\n" + "ItemId: "+ itemId + 
								"\nName: "+ addedItem.getItemName() + "\nBidder: " + addedItem.getBuyerId() +
								" " + "\nBid Amount: " + currentBidDollar);
						updateBid(addedItem);
					}
				}
			} else if(tag.equals("UpdateSubscriber")) {
				String itemId = st.nextToken();
				Iterator<Item> addedItemItr = addedItemList.iterator();
				while(addedItemItr.hasNext()) {
					Item addedItem = (Item) addedItemItr.next(); 
					if(itemId.equalsIgnoreCase(addedItem.getItemId())) {
						addedItem.setBuyerId(st.nextToken());
						String currentBidString = st.nextToken();
						
						addedItem.setItemStatus(st.nextToken());
						String bidUpdateSubscribers = st.nextToken();
						String bidUpdateSubscribersSize = st.nextToken();
						bidUpdateSubscribersList = getBidSubscribers(bidUpdateSubscribersSize, bidUpdateSubscribers);
						addedItem.setBidUpdateSubscribers(bidUpdateSubscribersList);
						
						String minBidString = addedItem.getMinBid();
						int minBidCents = Integer.parseInt(minBidString);
						String minBidDollar = centsToDollar(minBidCents);
						if(addedItem.getItemStatus().equalsIgnoreCase("New")) {
							addedItem.setCurrentBid(currentBidString);
							System.out.println("Item " + itemId + " returned to market with status " + addedItem.getItemStatus() +
									" and minimum bid " + minBidDollar + "."); 
						}
						updateBid(addedItem);
					}
				}
			}
		} catch (IOException e) {
//			System.err.println(e);
		}		
	}

}


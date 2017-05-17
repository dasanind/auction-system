import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;


public class ListenerThreadBuyer extends Thread {
    
    int serverPortClient;
    ArrayList<Item> matchedItemList;
    ArrayList<Item> bidList; 
    int serverPort;
    String brokerServerName;
    
    Socket server;
    BufferedReader din;
    PrintStream pout; 
 
/*  Constructor for the ListenerThreadBuyer class */    
	public ListenerThreadBuyer(int serverPortClient, ArrayList<Item> matchedItemList, ArrayList<Item> bidList,
			int serverPort, String brokerServerName) {
    	this.serverPortClient = serverPortClient;
    	this.matchedItemList = matchedItemList;
    	this.bidList = bidList;
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
//			System.err.println("listener aborted in ListenerThreadBuyer: "+ e);
		}
    }
    
/*  Compare the bid received from the seller with your own bid. If its greater than your bid
 *  than print update received and if the buyer is bidding in automatic mode then  update 
 *  the bid if its less than or equal to the max bid amount specified by the buyer and publish it. */        
    public boolean compareBids(String itemId, int receivedBid, String isMe) throws IOException {
    	boolean highestBid = false;
    	Iterator<Item> bidListItr = bidList.iterator();
		while(bidListItr.hasNext()) {
			Item bidItem = (Item) bidListItr.next();  
			int presentBid = Integer.parseInt(bidItem.getCurrentBid());
			
			String itmId = bidItem.getItemId();
			if((itmId.equalsIgnoreCase(itemId)) && (presentBid <= receivedBid) && (isMe.equalsIgnoreCase("notme"))){
				bidItem.setRecentBid(""+receivedBid);
				if(bidItem.getBidMode().equalsIgnoreCase("A")) {
					bidAutomatic(bidItem, receivedBid);
				}
				highestBid = true;
			}
		}
		return highestBid;
    }
    
/*  Converts the cents to dollars*/
    public String centsToDollar(int val) {
    	int dollar = val/100;
    	int cent = val%100;
    	String value = dollar + "." + cent;
    	return (value);
    	
    }
    
	public void getSocket() throws IOException { 
		server = new Socket(brokerServerName, serverPort);
		din = new BufferedReader(new InputStreamReader(server.getInputStream()));
		pout = new PrintStream(server.getOutputStream());
	}
	
/*  Bid automatically on an item whose bid mode is A */    	
    public void bidAutomatic(Item bidItem, int receivedBid) throws IOException {
    	getSocket(); 
    	BidEvent publishBid= new BidEvent();
    	String maxBidAmount = bidItem.getMaxBidAmount();
    	String increment = bidItem.getIncrement();
    	int myBid = receivedBid + Integer.parseInt(increment);
    	if(myBid <= Integer.parseInt(maxBidAmount)) {
    		bidItem.setCurrentBid("" + myBid);
    		bidItem.setRecentBid(""+myBid);
			publishBid.publish(serverPort, bidItem, pout);
    	}
    	server.close();
    }
    
/*  Remove item from the bidList */    
    synchronized void removeItemBidList(String itemId) {
     	Iterator<Item> bidListItr = bidList.iterator();
    	while(bidListItr.hasNext()) {
			Item bidItem = (Item) bidListItr.next();  
			String itmId = bidItem.getItemId();
			if(itemId.equalsIgnoreCase(itmId)){
				bidListItr.remove();
			}
		}
    }
    
/*  Remove item from the matchedItemList */  
    synchronized void removeItemMatchedItemList(String itemId) {
     	Iterator<Item> matchedItemListItr = matchedItemList.iterator();
    	while(matchedItemListItr.hasNext()) {
			Item matchedItem = (Item) matchedItemListItr.next();  
			String itmId = matchedItem.getItemId();
			if(itemId.equalsIgnoreCase(itmId)){
				matchedItemListItr.remove();
				
			}
		}
    }
    
	synchronized void handleserver(Socket theServer) {
		try {				
			BufferedReader din = new BufferedReader(new InputStreamReader(theServer.getInputStream()));
			String sellerId = "";
			String itemId = "";
			String name = "";
			String itemAttribute = "";
			String currentBidString = "";
			String status = "";
			
			String getline = din.readLine();
			StringTokenizer st = new StringTokenizer(getline, ":");
			String tag = st.nextToken();
			if(tag.equals("BidUpdate")) {
				//Tokenize the string recieved
				sellerId = st.nextToken();
				itemId = st.nextToken();
				name = st.nextToken();	
				itemAttribute = st.nextToken();
				currentBidString = st.nextToken();
				status = st.nextToken();
				String isMe = st.nextToken();
				int currentBid = Integer.parseInt(currentBidString);
				
				boolean highestBid = compareBids(itemId, currentBid, isMe);
				
				if(highestBid) {
					System.out.println("Received updated bid of " + centsToDollar(currentBid) + " for item " + itemId + ".");
				}
			} else if(tag.equals("SaleFinalize")) {
				String text = st.nextToken();
				itemId = st.nextToken();
				removeItemBidList(itemId);
				//remove from matched item list
				removeItemMatchedItemList(itemId);
				System.out.println(text + itemId);
			} else if(tag.equals("SellerLoggedOut")) {
				itemId = st.nextToken();
				removeItemBidList(itemId);
				//remove from matched item list
				removeItemMatchedItemList(itemId);
				System.out.println("Item " + itemId + " is no longer available for sale.");
			} else if(tag.equals("BuyerLoggedOut")) {
				itemId = st.nextToken();
				removeItemBidList(itemId);
				System.out.println("The bid for Item " + itemId + " is reset.");
			}
		} catch (IOException e) {
//			System.err.println(e);
		}		
	}

}


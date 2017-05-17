import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class PubSubSeller {
	
	int brokerServerPortNum;
    String brokerServerName;
    int brokerCapacity;
    int numberOfBrokers;
    
	BufferedReader din;
    PrintStream pout;                
    int brokerId;
    int serverPort;
    int indexOfBroker;
    static ArrayList<Item> addedItemList = new ArrayList<Item>();
    
    Socket server;
 
/*  Constructor for the PubSubSeller class */    
    public PubSubSeller(int brokerServerPortNum, String brokerServerName, int brokerCapacity, int numberOfBrokers){ 
    	this.brokerServerPortNum = brokerServerPortNum;
    	this.brokerServerName = brokerServerName;
    	this.brokerCapacity = brokerCapacity;
    	this.numberOfBrokers = numberOfBrokers;
    }
                  	        	                    	                
    public void getSocket() throws IOException { 
    	serverPort = brokerServerPortNum + (brokerId * brokerCapacity) + indexOfBroker + 1000;
		server = new Socket(brokerServerName, serverPort);
		din = new BufferedReader(new InputStreamReader(server.getInputStream()));
		pout = new PrintStream(server.getOutputStream());
    }

/*  Connect to the broker and open a connection */    
    public void connectToBroker () {
    	boolean flag = false;
    	int serverPortClient = 0;
    	
    	while (!flag) {
    		Random rand = new Random();
        	brokerId = rand.nextInt(numberOfBrokers);
        	Random randIndex = new Random();
        	indexOfBroker = randIndex.nextInt(brokerCapacity);
        	try {
	        	getSocket();
	        	pout.println("hello" + ":" + "seller_" + serverPort);
	        	pout.flush(); 
	        	try {
	        		serverPortClient = brokerServerPortNum + (brokerId * brokerCapacity) + indexOfBroker + 2000;
	    			ServerSocket listener = new ServerSocket(serverPortClient);
    				Socket aClient = listener.accept();
    				BufferedReader din = new BufferedReader(new InputStreamReader(aClient.getInputStream()));
    				String ack = din.readLine();
    	        	if(ack.equalsIgnoreCase("Connected")) {
    	        		System.out.println("Seller "+ "seller_" + serverPort + " " + ack + ".");
    	        		flag = true;
    	        	} 
    				aClient.close();
    				if(flag) {
    					listener.close();
    					new ListenerThreadSeller(serverPortClient, addedItemList, serverPort, brokerServerName).start();
    				}
	    		}
	    		catch (Exception e) {
//	    			System.err.println("Error: "+ e);
	    		}
	        	
        	} catch (IOException e) {
//        		System.err.println(e);
        	}
    	}
    	
    }
    
/*  Add the new items as entered by the seller */    
    public void addNewItem(String itemId, String name, String attributes, String minBid)
            throws IOException {
        getSocket();     
        Item item = new Item();
        AvailableItemEvent publishItem= new AvailableItemEvent();
        String sellerId = "seller_" + serverPort;
        item.setItemId(itemId + serverPort);
        item.setItemName(name.trim());
        item.setItemAttribute(attributes.trim());
        item.setMinBid(minBid);
        item.setSellerId(sellerId);
        int currentBid = Integer.parseInt(minBid) - 1;
        item.setCurrentBid(""+currentBid);
        item.setItemStatus("New");
        ArrayList<String> bidUpdateSubscribers = new ArrayList<String>();
        item.setBidUpdateSubscribers(bidUpdateSubscribers);
        updateItemList(item);
       
        publishItem.publish(serverPort, item, pout);
    }
    
/*  Update the addedItemList when an item is added by the seller */    
    synchronized public void updateItemList(Item item) {
    	 addedItemList.add(item);
	}
    
/*  View the items the seller has sold or offered for sale */     
    synchronized public void viewAvailableItems() {
    	Iterator<Item> addedItemListItr = addedItemList.iterator();
    	System.out.println("\nThe available items are:");
		while(addedItemListItr.hasNext()) {
			Item addedItem = (Item) addedItemListItr.next();  
			String minBidString = addedItem.getMinBid();
			int minBidCents = Integer.parseInt(minBidString);
			String minBidDollar = centsToDollar(minBidCents);
			String currentBidString = addedItem.getCurrentBid();
			int currentBidcents = Integer.parseInt(currentBidString);
			String currentBidDollar = centsToDollar(currentBidcents);
			
			if(minBidCents > currentBidcents) {
				System.out.println("Item Id: "+ addedItem.getItemId() + "\nName: " + addedItem.getItemName() + 
						"\nAttributes: " + addedItem.getItemAttribute() + "\nMin Bid: "+ minBidDollar +
						"\nStatus: " + addedItem.getItemStatus() + "\n");
			} else {
				System.out.println("Item Id: "+ addedItem.getItemId() + "\nName: " + addedItem.getItemName() + 
						"\nAttributes: " + addedItem.getItemAttribute() + "\nMin Bid: "+ minBidDollar +
						"\nBidder: " + addedItem.getBuyerId() + "\nCurrent Bid: " + currentBidDollar + 
						"\nStatus: " + addedItem.getItemStatus() + "\n");
			}
		}
    }

/*  Converts the cents to dollars */
    public String centsToDollar(int val) {
    	int dollar = val/100;
    	int cent = val%100;
    	String value = dollar + "." + cent;
    	return (value);
    	
    }
    
/*  Close the sale of an item */     
    synchronized public void finalizeSaleOfItem(String itemId) throws IOException {
    	getSocket(); 
    	SaleFinalizedEvent sellItem= new SaleFinalizedEvent();
    	Iterator<Item> addedItemListItr = addedItemList.iterator();
    	boolean flag = false;
    	while(addedItemListItr.hasNext()) {
			Item addedItem = (Item) addedItemListItr.next();  
			String itmId = addedItem.getItemId();
			String status = addedItem.getItemStatus();
			if(itmId.equalsIgnoreCase(itemId)) {
				flag = true;
				if(!((status.equalsIgnoreCase("New")) || (status.equalsIgnoreCase("Sold")))) {
				addedItem.setItemStatus("Sold");
				sellItem.publish(serverPort, addedItem, pout);
				} else if(status.equalsIgnoreCase("New"))
					System.out.println("You cannot sell a item that has not been bidden.");
				else if(status.equalsIgnoreCase("Sold"))
					System.out.println("You cannot sell a item that has aleady been sold.");
			}  
		}
    	if(!flag) {
    		System.out.println("Sale of Item " + itemId + " has failed.");
    	}
    }
    
/*  Log out and leave the market */ 
    public void logOut() throws IOException {
    	getSocket(); 
    	String sellerId = "seller_" + serverPort;
    	pout.println("logOutSeller:" + sellerId);
        pout.flush();   
        String result = din.readLine();
        StringTokenizer st = new StringTokenizer(result, ":");
        String tag = st.nextToken();
        if(tag.equals("Success")) {
        	System.out.println(st.nextToken()); 
        	System.exit(0);
        }
    }
    
/*  User Menu provided to the seller */ 
    public void userMenu() {
    	System.out.println("Options : \n 1. Publish a New Available Item." +
				 					 "\n 2. View Items Offered for Sale." +
				 				     "\n 3. Publish a Finalized Sale." +
				 					 "\n 4. Leave the Market.");
    	System.out.println("\nPlease enter your choice");
    }

    public static void main(String[] args) {    
    	
    	int brokerServerPortNum = 0;
    	String brokerServerName = "";
    	int numberOfBrokers = 0;
    	int brokerCapacity = 0;
    	int itemCount = 0;
    	
    	//getting the port number, the server name, number of brokers, and capacity of brokers from
    	//the config file
    	try{
    		FileInputStream fstream = new FileInputStream(args[0]);
    		DataInputStream in = new DataInputStream(fstream);
    		BufferedReader br = new BufferedReader(new InputStreamReader(in));
    		String strLine;    		      		  
    		while ((strLine = br.readLine()) != null)   {
    			StringTokenizer st = new StringTokenizer(strLine);
    			String tag = st.nextToken();  
    			if(tag.equals("brokerServerPortNum")) {
    				brokerServerPortNum = Integer.parseInt(st.nextToken());    				
    			} else if(tag.equals("brokerServerName")) {
    				brokerServerName = st.nextToken();    				
    			} else if(tag.equalsIgnoreCase("numberOfBrokers")) {
    				numberOfBrokers = Integer.parseInt(st.nextToken());
    			} else if(tag.equalsIgnoreCase("brokerCapacity")) {
    				brokerCapacity = Integer.parseInt(st.nextToken());
    			}
    		}
    		in.close();
		}catch (Exception e){//Catch exception if any
//			System.err.println("Error: " + e.getMessage());
		}
    	
    	PubSubSeller mySeller = new PubSubSeller(brokerServerPortNum, brokerServerName, brokerCapacity, numberOfBrokers);
    	
    	mySeller.connectToBroker();
    	
    	System.out.println("Seller ready to take inputs........");
    	
        try {
        	BufferedReader stdinp = new BufferedReader(new InputStreamReader(System.in));
        	
        	while (true) {    
        		boolean done = false;
        		mySeller.userMenu();
        		try {        			
        		String echoline = stdinp.readLine();
        		StringTokenizer st = null;
        		if(echoline.equals(null) || echoline.equals("")) {
        			
        		} else {
        			st = new StringTokenizer(echoline);       		
        			String tag = st.nextToken();        		
        		if(tag.equalsIgnoreCase("1")) {
        			while(!done) {
        				String name = "";
	        			while(name.equals("")) {
	        				System.out.println("Enter Item Name: ");
	        				 name = stdinp.readLine();
	        			} 
	        			
	        			System.out.println("Enter Item's Attributes (Attributes are comma separated): ");
	        			String attributes = stdinp.readLine();
	        			if(attributes.equals("")) {
	        				attributes = "%%unknown%%";
	        			} 
	        			
	        			boolean matches = false;
	        			String minBid = "";
	        			while(!matches) {
	        				System.out.println("Enter Minimum Bid in dollars and cents: ");
		        			String minBidEntry = stdinp.readLine();
		        			matches = Pattern.matches("^[0-9]+\\.[0-9][0-9]$", minBidEntry);
		        			if(matches) {
			        			String[] splitMinBidEntry = minBidEntry.split("\\.");
			        			minBid = splitMinBidEntry[0] + splitMinBidEntry[1];
		        			}
	        			} 
	        			
	        			itemCount = itemCount + 1;
	        			String itemId = "" + itemCount;
	        			mySeller.addNewItem(itemId, name, attributes, minBid);  
	        			
	        			boolean flag = false;
	        			while(!flag) {
	        				System.out.println("Do you want to add another item? Type Yes or No ");
	        				String doneAdding = stdinp.readLine();
	        				if(doneAdding.equalsIgnoreCase("No") || doneAdding.equalsIgnoreCase("Yes"))
		        				flag = true;
		        			if(doneAdding.equalsIgnoreCase("No"))
		        				done = true;
	        			}
	        			
        			} 
        		} else if(tag.equalsIgnoreCase("2")) {
        			mySeller.viewAvailableItems();
        		} else if(tag.equalsIgnoreCase("3")) {
        			while(!done) {
	        			String itemId = "";
	        			while(itemId.equals("")) {
	        				System.out.println("Enter Item Id: ");
	        				itemId = stdinp.readLine();
	        			} 
	        			mySeller.finalizeSaleOfItem(itemId);
	        			boolean flag = false;
	        			while(!flag) {
	        				System.out.println("Do you want to close another sale? Type Yes or No ");
	        				String doneFinalizing = stdinp.readLine();
	        				if(doneFinalizing.equalsIgnoreCase("No") || doneFinalizing.equalsIgnoreCase("Yes"))
		        				flag = true;
		        			if(doneFinalizing.equalsIgnoreCase("No"))
		        				done = true;
	        			}
        			}
        		} else if(tag.equalsIgnoreCase("4")) {
        			System.out.println("Logging out...");
        			mySeller.logOut();
        		}
        		else {
        			System.out.println("Malformed Query");
        		}
        		}
        		} 
        		catch (Exception e) {        			        			        			        			                   
                }
        	}
        } catch (Exception e) {
//            System.err.println("Server aborted:" + e);
        }
    }
}

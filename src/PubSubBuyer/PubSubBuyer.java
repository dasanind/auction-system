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


public class PubSubBuyer {
	
	int brokerServerPortNum;
    String brokerServerName;
    int brokerCapacity;
    int numberOfBrokers;
    
	BufferedReader din;
    PrintStream pout;                
    int brokerId;
    int serverPort;
    int indexOfBroker;
    
    static ArrayList<Item> matchedItemList = new ArrayList<Item>();
    static ArrayList<Item> bidList = new ArrayList<Item>();
    
    Socket server;
    
/*  Constructor for the PubSubBuyer class */ 
    public PubSubBuyer(int brokerServerPortNum, String brokerServerName, int brokerCapacity, int numberOfBrokers){ 
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
    	
    	while (!flag) {
    		Random rand = new Random();
        	brokerId = rand.nextInt(numberOfBrokers);
        	Random randIndex = new Random();
        	indexOfBroker = randIndex.nextInt(brokerCapacity);
        	try {
	        	getSocket();
	        	pout.println("hello" + ":" + "buyer_" + serverPort);
	        	pout.flush(); 
	        	
	        	try {
	        		int serverPortClient = listenOnPort();
	    			ServerSocket listener = new ServerSocket(serverPortClient);
    				Socket aClient = listener.accept();
    				BufferedReader din = new BufferedReader(new InputStreamReader(aClient.getInputStream()));
    				String ack = din.readLine();
    	        	if(ack.equalsIgnoreCase("Connected")) {
    	        		System.out.println("Buyer "+ "buyer_" + serverPort + " " + ack + ".");
    	        		flag = true;
    	        	} 
    				aClient.close();
    				
    				if(flag) {
    					listener.close();
    					new ListenerThreadBuyer(serverPortClient, matchedItemList, bidList, serverPort, brokerServerName).start();
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
    
    public int listenOnPort() {
    	int serverPortClient = brokerServerPortNum + (brokerId * brokerCapacity) + indexOfBroker + 2000;
    	return serverPortClient;
    }

/*  Add the interests as entered by the buyer */ 
    public void specifyInterest(String name, String attributes, String minBid)
            throws IOException {
        getSocket();     
        Item item = new Item();
        DeclareInterestEvent declareInterest= new DeclareInterestEvent();
        String buyerId = "buyer_" + serverPort;
        item.setItemName(name.trim());
        item.setItemAttribute(attributes.trim());
        item.setMinBid(minBid);
        item.setBuyerId(buyerId);
        ArrayList<String> bidUpdateSubscribers = new ArrayList<String>();
        item.setBidUpdateSubscribers(bidUpdateSubscribers);
        declareInterest.publish(serverPort, item, pout);
    }
    
/*  Bid on the item manually */ 
    public void bidItemManual(String itemId, String bidAmount, String bidMode)
            throws IOException {
        getSocket();     
        Item item = new Item();
        BidEvent publishBid= new BidEvent();
        String buyerId = "buyer_" + serverPort;
        Iterator<Item> bidListItr = bidList.iterator();
		while(bidListItr.hasNext()) {
			Item bidItem = (Item) bidListItr.next();  
			String itmId = bidItem.getItemId();
			if(itmId.equalsIgnoreCase(itemId)){
				if(bidItem.getBidMode().equalsIgnoreCase("A")) {
					System.out.println("Turning bid mode of item Id " + itemId + " to manual.");
				}
			}
		}
        item.setItemId(itemId.trim());
        item.setCurrentBid(bidAmount);
        item.setBuyerId(buyerId);
        item.setBidMode(bidMode);
        ArrayList<String> bidUpdateSubscribers = new ArrayList<String>();
        item.setBidUpdateSubscribers(bidUpdateSubscribers);
        
        updateMatchedOrBidedItemList(item, "bid");
        
        publishBid.publish(serverPort, item, pout);
        
        System.out.println(din.readLine());     
    }
    
/*  Bid on the item automatically */
    public void bidItemAutomatic(String itemId, String maxBidAmount, String increment, String bidMode) throws IOException {
    	getSocket();   
    	Item item = new Item();
    	BidEvent publishBid= new BidEvent();
    	String buyerId = "buyer_" + serverPort;
    	int minBid = 0;
        
        Iterator<Item> bidListItr = bidList.iterator();
    	boolean contains = false;
		while(bidListItr.hasNext()) {
			Item bidItem = (Item) bidListItr.next();  
			String itmId = bidItem.getItemId();
			if(itemId.equalsIgnoreCase(itmId)){
				if(bidItem.getBidMode().equalsIgnoreCase("M")) {
					String currentBid  = bidItem.getCurrentBid();
					String recentBid = bidItem.getRecentBid();
					if(recentBid != null) {
						minBid = Integer.parseInt(recentBid);
					} else {
						minBid = Integer.parseInt(currentBid);
					}
					contains = true;
				}
			}
		}
		if(!contains) {
	        Iterator<Item> matchedItemListItr = matchedItemList.iterator();
	 		while(matchedItemListItr.hasNext()) {
	 			Item matchedItem = (Item) matchedItemListItr.next(); 
	 			String matchedItemId = matchedItem.getItemId();
	 			if(itemId.equalsIgnoreCase(matchedItemId)) {
	 				minBid = Integer.parseInt(matchedItem.getMinBid());
	 			}
	 		}
		}
		
		int bidAmount = minBid + Integer.parseInt(increment);
		if(bidAmount <= Integer.parseInt(maxBidAmount)) {
			item.setItemId(itemId.trim());
	        item.setBuyerId(buyerId);
	        ArrayList<String> bidUpdateSubscribers = new ArrayList<String>();
	        item.setBidUpdateSubscribers(bidUpdateSubscribers);
	        item.setBidMode(bidMode);
			item.setCurrentBid("" + bidAmount);
			item.setMaxBidAmount(maxBidAmount);
			item.setIncrement(increment);
			updateMatchedOrBidedItemList(item, "bid");
			publishBid.publish(serverPort, item, pout);
			System.out.println(din.readLine()); 
		} else {
			System.out.println("Your bid is more than the maximum bid amount you specified.");
		}
		
    }
    
/*  View the items that matched the buyers interests */
    public void viewMatchedInterest() throws IOException {
    	getSocket();
    	Item item = new Item();
    	ViewMatchedInterestEvent viewMatches = new ViewMatchedInterestEvent();
    	String buyerId = "buyer_" + serverPort;
    	item.setBuyerId(buyerId);
    	ArrayList<String> bidUpdateSubscribers = new ArrayList<String>();
        item.setBidUpdateSubscribers(bidUpdateSubscribers);
    	viewMatches.publish(serverPort, item, pout);
    	
    	String size = din.readLine();  
    	int sizeOfMatchList = Integer.parseInt(size);
		System.out.println("Matched Items:\n");
		if(sizeOfMatchList == 0) {
			System.out.println("No Matches Found");
		}
		for(int i = 0; i < sizeOfMatchList; i++) {
			String result = din.readLine();
			 
			 StringTokenizer st = new StringTokenizer(result, ":");
			 st.nextToken();
			 st.nextToken();
			 st.nextToken();
			 String itemId = st.nextToken();
			 String itemName = st.nextToken();
			 String itemAttribute = st.nextToken();
			 String minBidString = st.nextToken();
			 int minBidcents = Integer.parseInt(minBidString);
			 String minBidDollar = centsToDollar(minBidcents);
			 String currentBidString = st.nextToken();
			 int currentBidcents = Integer.parseInt(currentBidString);
			 String currentBidDollar = centsToDollar(currentBidcents);
			 if(minBidcents > currentBidcents) {
				 System.out.println("Id:" + itemId + "\nName: " + itemName + "\nAttributes: " + itemAttribute 
						 + "\nMinimum Bid: " + minBidDollar + "\n");
			 } else {
				 System.out.println("Id:" + itemId + "\nName: " + itemName + "\nAttributes: " + itemAttribute 
					 + "\nMinimum Bid: " + minBidDollar + "\nCurrent Bid: " + currentBidDollar + "\n");
			 }
			 
			 Item itemMatched = new Item();
			 itemMatched.setItemId(itemId);
		     itemMatched.setItemName(itemName);
		     itemMatched.setItemAttribute(itemAttribute);
		     itemMatched.setMinBid(minBidString);
		     itemMatched.setMinBid(minBidString);
	         ArrayList<String> bidUpdateSubscriberList = new ArrayList<String>();
	         itemMatched.setBidUpdateSubscribers(bidUpdateSubscriberList);
	         updateMatchedOrBidedItemList(itemMatched, "match");
		}
    }

/*  Update the matchedItem List when the buyer views the items that matched her interests  
 *  Update the bidItemList when the buyer bids on the item of her interest */
    synchronized void updateMatchedOrBidedItemList(Item item, String type) {
    	if(type.equalsIgnoreCase("match")){
    		if(matchedItemList.isEmpty()) { 
    			matchedItemList.add(item);
    		} else {
    			if(!(matchedItemList.contains(item)) ){
    				matchedItemList.add(item);
    			}
    		}
    	} else if(type.equalsIgnoreCase("bid")){
    		if(bidList.isEmpty()) { 
    			bidList.add(item);
    		} else {
    			Iterator<Item> bidListItr = bidList.iterator();
    	    	boolean contains = false;
    			while(bidListItr.hasNext()) {
    				Item bidItem = (Item) bidListItr.next();  
    				String itmId = bidItem.getItemId();
    				String itemId = item.getItemId();
    				if(itmId.equalsIgnoreCase(itemId)){
    					bidItem.setCurrentBid(item.getCurrentBid());
    					bidItem.setBidMode(item.getBidMode());
    					contains = true;
    				}
    			}
    			if(!contains) {
    				bidList.add(item);
    			}
    		}
    	}
	}
    
/*  Converts the cents to dollars*/
    public String centsToDollar(int val) {
    	int dollar = val/100;
    	int cent = val%100;
    	String value = dollar + "." + cent;
    	return (value);
    	
    }
    
/*  Log out and leave the market */
    public void logOut() throws IOException  {
		getSocket();
    	String buyerId = "buyer_" + serverPort;
    	pout.println("logOutBuyer:" + buyerId);
        pout.flush();   
        String result = "";
		result = din.readLine();
        StringTokenizer st = new StringTokenizer(result, ":");
        String tag = st.nextToken();
        if(tag.equals("Success")) {
        	System.out.println(st.nextToken()); 
        	System.exit(0);
        }
    }
    
/*  User Menu provided to the buyer */ 
    public void userMenu() {
    	System.out.println("Options : \n 1. Specify a New Interest." +
				 					 "\n 2. View Items Matching Interest." +
				 				     "\n 3. Publish a New Bid." +
				 					 "\n 4. Leave the Market.");
    	System.out.println("\nPlease enter your choice");
    }

    public static void main(String[] args) {    
    	 
    	int brokerServerPortNum = 0;
    	String brokerServerName = "";
    	int numberOfBrokers = 0;
    	int brokerCapacity = 0;
    	
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
    	
    	PubSubBuyer myBuyer = new PubSubBuyer(brokerServerPortNum, brokerServerName, brokerCapacity, numberOfBrokers);
    	
    	myBuyer.connectToBroker();
    	
    	System.out.println("Buyer ready to take inputs........");
        try {
        	BufferedReader stdinp = new BufferedReader(new InputStreamReader(System.in));
        	
        	while (true) {   
        		boolean done = false;
        		myBuyer.userMenu();
        		try {        			
	        		String echoline = stdinp.readLine();
	        		StringTokenizer st = null;
	        		if(echoline.equals(null) || echoline.equals("")) {
	        			
	        		} else {
	        			st = new StringTokenizer(echoline);       		
	        			String tag = st.nextToken();        		
		        		if(tag.equalsIgnoreCase("1")) {
		        			while(!done) {
			        			System.out.println("Enter Item Name (Press Enter to Skip): ");
			        			String name = stdinp.readLine();
			        			if(name.equals("")) {
			        				name = "%%na%%";
			        			} 
			        			System.out.println("Enter Item's Attributes (Attributes are comma separated. Press Enter to Skip): ");
			        			String attributes = stdinp.readLine();
			        			if(attributes.equals("")) {
			        				attributes = "%%na%%";
			        			} 
			        			boolean matches = false;
			        			String minBid = "";
			        			while(!matches) {
			        				System.out.println("Enter Minimum Bid (Press Enter to Skip):: ");
				        			String minBidEntry = stdinp.readLine();
				        			if(minBidEntry.equals("")) {
				        				matches = true;
				        				minBid = "-1";
				        			} else {
					        			matches = Pattern.matches("^[0-9]+\\.[0-9][0-9]$", minBidEntry);
					        			if(matches) {
						        			String[] splitMinBidEntry = minBidEntry.split("\\.");
						        			minBid = splitMinBidEntry[0] + splitMinBidEntry[1];
					        			}
				        			}
			        			} 
			        			
			        			myBuyer.specifyInterest(name, attributes, minBid); 
			        			
			        			boolean flag = false;
			        			while(!flag) {
			        				System.out.println("Do you want to specify another interest? Type Yes or No ");
			        				String doneSpecifying = stdinp.readLine();
			        				if(doneSpecifying.equalsIgnoreCase("No") || doneSpecifying.equalsIgnoreCase("Yes"))
				        				flag = true;
				        			if(doneSpecifying.equalsIgnoreCase("No"))
				        				done = true;
			        			}
			        			
		        			} 
		        		} else if(tag.equalsIgnoreCase("2")) {
		        			myBuyer.viewMatchedInterest();
		        		} else if(tag.equalsIgnoreCase("3")) {
		        			String bidMode = "";
		        			while(!((bidMode.equalsIgnoreCase("A")) || (bidMode.equalsIgnoreCase("M")))) {
		        				System.out.println("Do you want bid manually or automatically? (Enter M for manual and A for automatic)");
		        				bidMode = stdinp.readLine(); 
		        			} 
		        			if(bidMode.equalsIgnoreCase("M")) {
			        			while(!done) {
				        			String itemId = "";
				        			while(itemId.equals("")) {
				        				System.out.println("Enter Item Id: ");
				        				itemId = stdinp.readLine();
				        			} 
				        			
				        			boolean matches = false;
				        			String bidAmount = "";
				        			while(!matches) {
				        				System.out.println("Enter bid amount in dollars and cents: ");
					        			String bidAmountEntry = stdinp.readLine();
					        			matches = Pattern.matches("^[0-9]+\\.[0-9][0-9]$", bidAmountEntry);
					        			if(matches) {
						        			String[] splitBidAmountEntry = bidAmountEntry.split("\\.");
						        			bidAmount = splitBidAmountEntry[0] + splitBidAmountEntry[1];
					        			}
				        			} 
				        			myBuyer.bidItemManual(itemId, bidAmount, bidMode);
				        			boolean flag = false;
				        			while(!flag) {
				        				System.out.println("Do you want to publish another manual bid? Type Yes or No ");
				        				String doneBidding = stdinp.readLine();
				        				if(doneBidding.equalsIgnoreCase("No") || doneBidding.equalsIgnoreCase("Yes"))
					        				flag = true;
					        			if(doneBidding.equalsIgnoreCase("No"))
					        				done = true;
				        			}
			        			}
		        			} else if(bidMode.equalsIgnoreCase("A")) {
		        				while(!done) {
			        				String itemId = "";
				        			while(itemId.equals("")) {
				        				System.out.println("Enter Item Id: ");
				        				itemId = stdinp.readLine();
				        			}
				        			
				        			boolean matches = false;
				        			String maxBidAmount = "";
				        			while(!matches) {
				        				System.out.println("Enter maximum bid amount in dollars and cents: ");
					        			String maxBidAmountEntry = stdinp.readLine();
					        			matches = Pattern.matches("^[0-9]+\\.[0-9][0-9]$", maxBidAmountEntry);
					        			if(matches) {
						        			String[] splitMaxBidAmountEntry = maxBidAmountEntry.split("\\.");
						        			maxBidAmount = splitMaxBidAmountEntry[0] + splitMaxBidAmountEntry[1];
					        			}
				        			} 
				        			
				        			boolean match = false;
				        			String increment = "";
				        			while(!match) {
				        				System.out.println("Enter increment in dollars and cents: ");
					        			String incrementEntry = stdinp.readLine();
					        			match = Pattern.matches("^[0-9]+\\.[0-9][0-9]$", incrementEntry);
					        			if(match) {
						        			String[] splitIncrementEntry = incrementEntry.split("\\.");
						        			increment = splitIncrementEntry[0] + splitIncrementEntry[1];
					        			}
				        			}
				        			myBuyer.bidItemAutomatic(itemId, maxBidAmount, increment, bidMode);
				        			boolean flag = false;
				        			while(!flag) {
				        				System.out.println("Do you want to publish another automatic bid? Type Yes or No ");
				        				String doneBidding = stdinp.readLine();
				        				if(doneBidding.equalsIgnoreCase("No") || doneBidding.equalsIgnoreCase("Yes"))
					        				flag = true;
					        			if(doneBidding.equalsIgnoreCase("No"))
					        				done = true;
				        			}
			        			}
		        			}
		        		} else if(tag.equalsIgnoreCase("4")) {
		        			System.out.println("Logging out...");
		        			myBuyer.logOut();
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

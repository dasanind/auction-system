import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;

public class ConnectingThread extends Thread {
    
    int myId;
    int numberOfBrokers;
    int serverPortChild;
    String brokerServerName;
    int brokerServerPortNum;
    int branchingFactor;
    int childIndex;
    boolean is_root;
    ArrayList<Item> currentItemList;
    ArrayList<Item> currentInterestList;
    ArrayList<String>  connectedClientList;
    
    PrintStream poutParent; 
    PrintStream poutChild; 
    
/*  Constructor for the ConnectingThread class */ 
	public ConnectingThread(int myId, int numberOfBrokers, int serverPortChild, String brokerServerName, int brokerServerPortNum, 
			int branchingFactor, int childIndex, boolean is_root, ArrayList<Item> currentItemList,
			ArrayList<Item> currentInterestList, ArrayList<String>  connectedClientList) {
    	this.myId = myId;
        this.numberOfBrokers = numberOfBrokers;
        this.serverPortChild = serverPortChild;
        this.brokerServerName = brokerServerName;
        this.brokerServerPortNum = brokerServerPortNum;
        this.branchingFactor = branchingFactor;
        this.childIndex = childIndex;
        this.is_root = is_root;
        this.currentItemList = currentItemList;
        this.currentInterestList = currentInterestList;
        this.connectedClientList = connectedClientList;
    }
	
    synchronized public void run() {
        try {
        	ServerSocket listener = new ServerSocket(serverPortChild);
			while(true) {
				Socket aServer = listener.accept();
				handleserver(aServer);
				aServer.close();
			}
		}
		catch (Exception e) {
//			System.err.println("Server aborted in Connecting Thread: "+ e);
		}
    }
   
/* 	Getting the socket to connect to the parent */	
	public void getParent() {
		int serverPort = myId + 700 + brokerServerPortNum;
	  	try {
	  		Socket serverParentSocket = new Socket(brokerServerName, serverPort);
	  		poutParent = new PrintStream(serverParentSocket.getOutputStream());
	  	} catch (IOException e1) {
//	  		System.err.println("Unable to talk to server: ");
	  		getParent();
	  	}
	}
	
/* 	Getting the id of the parent */	
	public int getParentId(int id) {
		int temp = id % branchingFactor;
		if(temp == 0) {
			return (id/branchingFactor) - 1; 
		} 
		return id/branchingFactor; 
	}
	
/* 	Getting the id of the child */	
	public int getChildId(int childIndex) {
		int childId = (myId * branchingFactor)  + childIndex + 1;
		return childId;
	}
	
/* 	Getting the print stream for the child */	
	public PrintStream getPoutBrokerChild(int childIndex) {
		PrintStream poutBrokerChild = null;
		try {
			int serverPortSendChild = brokerServerPortNum + 500 + getChildId(childIndex);
			Socket sendBrokerChildSocket = new Socket(brokerServerName, serverPortSendChild);
			poutBrokerChild = new PrintStream(sendBrokerChildSocket.getOutputStream());
			return poutBrokerChild;
		} catch (IOException e) {
//			System.err.println(e);
		}
		return poutBrokerChild;
	}
	
/* 	Propagate the publications and subscription to the children */	
	public void sendToChilds(int childIndex, PropagateItemEvent propagateItem, Item item, int numChildren, String tag) {
		for (int sendToChildIndex = 0; sendToChildIndex < numChildren; sendToChildIndex++) {
			if((sendToChildIndex != childIndex)){ 
				propagateItem.propagate(item, getPoutBrokerChild(sendToChildIndex), tag);
			}
		}
	}
	
/* 	Getting the number of children */	
	public int getNumberOfChildren(int branchingFactor) {
		int maxChildrenId = (myId * branchingFactor) + branchingFactor;
		int maxBrokerId = numberOfBrokers - 1;
		int numberOfChildren = 0;
		if(maxChildrenId > maxBrokerId) {
			numberOfChildren = maxBrokerId - ((myId * branchingFactor) + 1) + 1;
		} else {
			numberOfChildren = branchingFactor;
		}
		return numberOfChildren;
	}

/*  Update the current item list with the new item whenever a seller publishes a new item */	
	synchronized void updateCurrentItemList(Item item) {
		if(currentItemList.isEmpty()) {
			currentItemList.add(item);
		} else {
			if(!(currentItemList.contains(item)) ){
				currentItemList.add(item);
			}
		}
	}
	
/*  Remove the items from the currentItemList when the seller logs out */
	synchronized void removeItemCurrentItemList(String sellerId, String tag) {
		Iterator<Item> currentItemItr = currentItemList.iterator();
		ArrayList<String> bidUpdateSubscribersList = new ArrayList<String>();
		while(currentItemItr.hasNext()) {
			 Item currentItem = (Item) currentItemItr.next();
			 String currentItemSellerId = currentItem.getSellerId();
			 if(sellerId.equalsIgnoreCase(currentItemSellerId)) {
				 bidUpdateSubscribersList = currentItem.getBidUpdateSubscribers();
				 if(bidUpdateSubscribersList.size() > 0) {
					 sendBidUpdateToSubscribedBuyers(bidUpdateSubscribersList, currentItem, tag);
				 }
				 currentItemItr.remove();
			 }
		}
	}
	
/*  Update the status of the item to be sold and subscribers list as empty after sale finalized */
	synchronized void updateSaleCurrentItemList(String  itemId, String status) {
		Iterator<Item> currentItemItr = currentItemList.iterator();
		ArrayList<String> bidUpdateSubscribersList = new ArrayList<String>();
		while(currentItemItr.hasNext()) {
			 Item currentItem = (Item) currentItemItr.next();
			 String currentItemId = currentItem.getItemId();
			 if(itemId.equalsIgnoreCase(currentItemId)) {
				 currentItem.setItemStatus(status);
				 currentItem.setBidUpdateSubscribers(bidUpdateSubscribersList);
			 }
		}
	}
	
/*  Remove the buyerId from the subscribers list and reset the buyerId if that buyer is the highest bidder */
	synchronized void removeSubscriberCurrentItemList(String buyerId, String tag) {
		Iterator<Item> currentItemItr = currentItemList.iterator();
		ArrayList<String> bidUpdateSubscribersList = new ArrayList<String>();
		while(currentItemItr.hasNext()) {
			 Item currentItem = (Item) currentItemItr.next();
			 String currentItemBuyerId = currentItem.getBuyerId();
			 bidUpdateSubscribersList = currentItem.getBidUpdateSubscribers();
			 String clientId = currentItem.getSellerId();
			 String currentItemStatus = currentItem.getItemStatus();
			 if(!(currentItemStatus.equalsIgnoreCase("Sold"))) {
				 if(buyerId.equalsIgnoreCase(currentItemBuyerId)) {
					 currentItem.setBuyerId("NA");
					 int currentBid = Integer.parseInt(currentItem.getMinBid()) - 1;
					 currentItem.setCurrentBid("" + currentBid);
					 currentItem.setItemStatus("New");
					 
					 if(bidUpdateSubscribersList.contains(buyerId)) {
						 bidUpdateSubscribersList.remove(buyerId);
						 sendBidUpdateToSubscribedBuyers(bidUpdateSubscribersList, currentItem, tag);
						 bidUpdateSubscribersList.clear();
						 bidUpdateSubscribersList = new ArrayList<String>();
						 currentItem.setBidUpdateSubscribers(bidUpdateSubscribersList);
						 if(connectedClientList.contains(clientId)) {
								sendToClient(clientId, currentItem, tag);
						}
					 }
				 } else {
					 if(bidUpdateSubscribersList.contains(buyerId)) {
						 bidUpdateSubscribersList.remove(buyerId);
						 if(connectedClientList.contains(clientId)) {
							sendToClient(clientId, currentItem, tag);
						}
					 }
				 }
			 }
		}
	}

/*  Update the current interest list with the new item whenever a buyer publishes a new bid */
	synchronized void updateCurrentInterestList(Item item) {
		if(currentInterestList.isEmpty()) {
			currentInterestList.add(item);
		} else {
			if(!(currentInterestList.contains(item)) ){
				currentInterestList.add(item);
			}
		}
	}
	
/*  Remove the interest of the buyer from the interest list when the buyer logs out */
	synchronized void removeSubscriberCurrentInterestList(String buyerId) {
		Iterator<Item> currentInterestListItr = currentInterestList.iterator();
		while(currentInterestListItr.hasNext()) {
			 Item currentInterestItem = (Item) currentInterestListItr.next();
			 String currentInterestBuyerId = currentInterestItem.getBuyerId();
			 if(buyerId.equalsIgnoreCase(currentInterestBuyerId)) {
				 currentInterestListItr.remove();
			 }
		}
	}

/*  Update the existing item with the bid from the buyer if the bid is greater then equal to the present bid on the item */
	public boolean updateItemWithCurrentBid(String buyerId, String itemId, String currentBid, String tag) {
		Iterator<Item> currentItemItr = currentItemList.iterator();
		int bidReceived = Integer.parseInt(currentBid);
		boolean updated = false;
		ArrayList<String> bidSubscribers = new ArrayList<String>();
		while(currentItemItr.hasNext()) {
			Item currentItem = (Item) currentItemItr.next(); 
			int bidPresent = Integer.parseInt(currentItem.getCurrentBid());
			int minBid = Integer.parseInt(currentItem.getMinBid());
			
			if((currentItem.getItemId().equalsIgnoreCase(itemId)) && (bidReceived > bidPresent)
					&& !(currentItem.getItemStatus().equalsIgnoreCase("Sold"))) {
				currentItem.setBuyerId(buyerId);
				currentItem.setCurrentBid(""+bidReceived);
				currentItem.setItemStatus("Active");
				bidSubscribers = currentItem.getBidUpdateSubscribers();
				if(bidSubscribers.size() == 0) {
					bidSubscribers.add(buyerId);
					currentItem.setBidUpdateSubscribers(bidSubscribers);
				 } else {
					 bidSubscribers = currentItem.getBidUpdateSubscribers();
					if(!(bidSubscribers.contains(buyerId)) ){
						bidSubscribers.add(buyerId);
						currentItem.setBidUpdateSubscribers(bidSubscribers);
					}
				 }
				updated = true;
				String clientId = currentItem.getSellerId();
				if(connectedClientList.contains(clientId)) {
					sendToClient(clientId, currentItem, tag);
				}
			} else if((currentItem.getItemId().equalsIgnoreCase(itemId)) && (bidReceived >= minBid) 
					&& !(currentItem.getItemStatus().equalsIgnoreCase("Sold"))) {
				bidSubscribers = currentItem.getBidUpdateSubscribers();
				if(bidSubscribers.size() == 0) {
					bidSubscribers.add(buyerId);
					currentItem.setBidUpdateSubscribers(bidSubscribers);
				 } else {
					 bidSubscribers = currentItem.getBidUpdateSubscribers();
					if(!(bidSubscribers.contains(buyerId)) ){
						bidSubscribers.add(buyerId);
						currentItem.setBidUpdateSubscribers(bidSubscribers);
					}
				 }
				updated = true;
				String clientId = currentItem.getSellerId();
				if(connectedClientList.contains(clientId)) {
					sendToClient(clientId, currentItem, tag);
				}
			}
		}
		return updated;
	}
	
/*  Send the bid update notification to the seller and then the bid update notification, sale closed, item no longer 
 *  available for sale to the buyer */	
	public void sendToClient(String clientId, Item currentItem, String tag) {
		StringTokenizer st = new StringTokenizer(clientId, "_");
		String retValue = "";
		String flag = "notme";
		String clientType = st.nextToken();
		String clientReceivePort = st.nextToken();
		int clientSendPort = Integer.parseInt(clientReceivePort) + 1000;
		if((tag.equals("bidEvent")) && (clientType.equalsIgnoreCase("seller"))) {
			retValue = "BidUpdated:" + currentItem.getItemId() + ":" + currentItem.getBuyerId() + ":" + currentItem.getCurrentBid()
					+ ":" + currentItem.getItemStatus() + ":" + currentItem.getBidUpdateSubscribers().toString() 
					+ ":" + currentItem.getBidUpdateSubscribers().size();
		} else if((tag.equals("bidUpdateEvent")) && (clientType.equalsIgnoreCase("buyer"))) {
			if(currentItem.getBuyerId().equalsIgnoreCase(clientId)) {
				flag = "me";
			}
			retValue = "BidUpdate:" + currentItem.getSellerId() + " :" + currentItem.getItemId() + ":" + currentItem.getItemName()+ ":" 
					+ currentItem.getItemAttribute() + ":" + currentItem.getCurrentBid() + ":" + currentItem.getItemStatus() + ":" + flag;
		} else if((tag.equals("saleFinalizedEvent")) && (clientType.equalsIgnoreCase("buyer"))) {
			if(currentItem.getBuyerId().equalsIgnoreCase(clientId)) {
				retValue = "SaleFinalize:" + "You won the bid for the item Id :" + currentItem.getItemId();
			} else {
				retValue = "SaleFinalize:" + "Sale closed for the item Id :" + currentItem.getItemId();
			}
		} else if((tag.equals("logOutSeller")) && (clientType.equalsIgnoreCase("buyer"))) {
			if(!(currentItem.getItemStatus().equalsIgnoreCase("Sold"))) {
				retValue = "SellerLoggedOut:" + currentItem.getItemId();
			}
		} else if((tag.equals("logOutBuyer")) && (clientType.equalsIgnoreCase("buyer"))) {
			retValue = "BuyerLoggedOut:" + currentItem.getItemId();
		} else if((tag.equals("logOutBuyer")) && (clientType.equalsIgnoreCase("seller"))) {
			retValue = "UpdateSubscriber:" + currentItem.getItemId() + ":" + currentItem.getBuyerId() + ":" + currentItem.getCurrentBid()
					+ ":" + currentItem.getItemStatus() + ":" + currentItem.getBidUpdateSubscribers().toString() 
					+ ":" + currentItem.getBidUpdateSubscribers().size();
		}
		
		getPoutClient(clientSendPort).println(retValue);	
	}
	
/*  Get the print stream to send information to the clients (buyer/seller) */	
	public PrintStream getPoutClient(int serverPortSendClient) {
		PrintStream poutClient = null;
		boolean flag = false;
		while(!flag) {
			try {
			Socket sendClientSocket = new Socket(brokerServerName, serverPortSendClient);
			poutClient = new PrintStream(sendClientSocket.getOutputStream());
			if(poutClient != null) {
				flag = true;
			} 
			} catch (IOException e) {
//				System.err.println(e);
			}
		}
		return poutClient;
	}
	
/*  Broadcast the msg received from the parent to the child and the msg received from the 
 *  child to the parent */
	public void broadcastItem(PropagateItemEvent propagateItem, Item item, String tag) {
		int numChildren = getNumberOfChildren(branchingFactor);
		//recieved the msg from my parent send to child
		if(childIndex == branchingFactor) {
			sendToChilds(childIndex, propagateItem, item, numChildren, tag);
		} else { //received the msg from my child send to parent
			if(!is_root) {
	        	getParent();
	        	propagateItem.propagate(item, poutParent, tag);
	        }
			sendToChilds(childIndex, propagateItem, item, numChildren, tag);
		}
	}
	
/* 	Construct an arrayList of bid subscribers */	
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
	
/* Send the updated bid to the buyers */
	public void sendBidUpdateToSubscribedBuyers(ArrayList<String> bidUpdateSubscribersList, Item item, String tag){
		Iterator<String> bidUpdateSubscribersItr = bidUpdateSubscribersList.iterator();
		while(bidUpdateSubscribersItr.hasNext()) {
			String bidSubscriber = (String) bidUpdateSubscribersItr.next(); 
			 if(connectedClientList.contains(bidSubscriber)) {
					sendToClient(bidSubscriber, item, tag);
			}
		}
	}
	
	synchronized void handleserver(Socket theServer) {
		try {				
			BufferedReader din = new BufferedReader(new InputStreamReader(theServer.getInputStream()));
			PrintWriter pout = new PrintWriter(theServer.getOutputStream());
			String retValue = "";
			String sellerId = "";
			String buyerId = "";
			String itemId = "";
			String name = "";
			String itemAttribute = "";
			String minBid = "";
			String currentBid = "";
			String status = "";
			String bidUpdateSubscribers = "";
			String bidUpdateSubscribersSize = "";
			ArrayList<String> bidUpdateSubscribersList = new ArrayList<String>();
			
			String getline = din.readLine();
			StringTokenizer st = new StringTokenizer(getline, ":");
			String tag = st.nextToken();
			if(tag.equals("childId")) {
				String s = st.nextToken();	
				int id = Integer.parseInt(s);
				int parentId =  getParentId(id);
				retValue = "parentId "+ parentId;
				getPoutBrokerChild(childIndex).println(retValue);				
			} else if(tag.equals("availableItemEvent")) {
				Item item = new Item();
				PropagateItemEvent propagateItem= new PropagateItemEvent();
				
				//Tokenize the string recieved
				sellerId = st.nextToken();
				buyerId = st.nextToken();
				itemId = st.nextToken();
				name = st.nextToken();	
				itemAttribute = st.nextToken();
				minBid = st.nextToken();
				currentBid = st.nextToken();
				status = st.nextToken();
				bidUpdateSubscribers = st.nextToken();
				bidUpdateSubscribersSize = st.nextToken();
				bidUpdateSubscribersList = getBidSubscribers(bidUpdateSubscribersSize, bidUpdateSubscribers);
				
				//Set the item object with the received information
				item.setSellerId(sellerId);
				item.setItemId(itemId);
		        item.setItemName(name);
		        item.setItemAttribute(itemAttribute);
		        item.setMinBid(minBid);
		        item.setCurrentBid(currentBid);
		        item.setItemStatus(status);
				item.setBidUpdateSubscribers(bidUpdateSubscribersList);
				
				updateCurrentItemList(item);
				
				broadcastItem(propagateItem, item, tag);
				
				retValue = "Item Received is " + sellerId + " " + itemId + " " + name + " " + itemAttribute
						+ " " + minBid + " " + currentBid + " " + status;
			} else if(tag.equals("declareInterestEvent")) {
				Item item = new Item();
				PropagateItemEvent propagateItem= new PropagateItemEvent();
				//Tokenize the string received
				sellerId = st.nextToken();
				buyerId = st.nextToken();
				itemId = st.nextToken();
				name = st.nextToken();	
				itemAttribute = st.nextToken();
				minBid = st.nextToken();
				currentBid = st.nextToken();
				status = st.nextToken();
				bidUpdateSubscribers = st.nextToken();
				bidUpdateSubscribersSize = st.nextToken();
				bidUpdateSubscribersList = getBidSubscribers(bidUpdateSubscribersSize, bidUpdateSubscribers);
				
				//Set the item object with the received information
				item.setBuyerId(buyerId);
		        item.setItemName(name);
		        item.setItemAttribute(itemAttribute);
		        item.setMinBid(minBid);
		        item.setBidUpdateSubscribers(bidUpdateSubscribersList);
				
				updateCurrentInterestList(item);
				 
				broadcastItem(propagateItem, item, tag);
				
				retValue = "Interest Received is " + buyerId + " " + name + " " + itemAttribute
						+ " " + minBid;
			} else if(tag.equals("bidEvent")) {
				Item item = new Item();
				PropagateItemEvent propagateItem= new PropagateItemEvent();
				
				//Tokenize the string received
				sellerId = st.nextToken();
				buyerId = st.nextToken();
				itemId = st.nextToken();
				name = st.nextToken();	
				itemAttribute = st.nextToken();
				minBid = st.nextToken();
				currentBid = st.nextToken();
				status = st.nextToken();
				bidUpdateSubscribers = st.nextToken();
				bidUpdateSubscribersSize = st.nextToken();
				bidUpdateSubscribersList = getBidSubscribers(bidUpdateSubscribersSize, bidUpdateSubscribers);
				
				//Set the item object with the received information
				item.setBuyerId(buyerId);
				item.setItemId(itemId);
				item.setCurrentBid(currentBid);
				item.setBidUpdateSubscribers(bidUpdateSubscribersList);
				
				retValue = "Item Bid Received is " + buyerId + " " + itemId + " " + currentBid ;
				
				boolean updated = updateItemWithCurrentBid(buyerId, itemId, currentBid, tag);
				if (updated) {
					broadcastItem(propagateItem, item, tag);
				}
			} else if(tag.equals("bidUpdateEvent")) {
				Item item = new Item();
				PropagateItemEvent propagateItem= new PropagateItemEvent();
				
				//Tokenize the string received
				sellerId = st.nextToken();
				buyerId = st.nextToken();
				itemId = st.nextToken();
				name = st.nextToken();	
				itemAttribute = st.nextToken();
				minBid = st.nextToken();
				currentBid = st.nextToken();
				status = st.nextToken();
				bidUpdateSubscribers = st.nextToken();
				bidUpdateSubscribersSize = st.nextToken();
				bidUpdateSubscribersList = getBidSubscribers(bidUpdateSubscribersSize, bidUpdateSubscribers);
				
				//Set the item object with the received information
				item.setSellerId(sellerId);
				item.setBuyerId(buyerId);
				item.setItemId(itemId);
				item.setItemName(name);
		        item.setItemAttribute(itemAttribute);
		        item.setMinBid(minBid);
		        item.setCurrentBid(currentBid);
		        item.setItemStatus(status);
		        item.setBidUpdateSubscribers(bidUpdateSubscribersList);
		        
		        sendBidUpdateToSubscribedBuyers(bidUpdateSubscribersList, item, tag);
		        
		        broadcastItem(propagateItem, item, tag);
				retValue = "Item Bid Update Received is " + buyerId + " " + itemId + " " + minBid + " " + currentBid ;
			} else if(tag.equals("saleFinalizedEvent")) {
				Item item = new Item();
				PropagateItemEvent propagateItem= new PropagateItemEvent();
				
				//Tokenize the string received
				sellerId = st.nextToken();
				buyerId = st.nextToken();
				itemId = st.nextToken();
				name = st.nextToken();	
				itemAttribute = st.nextToken();
				minBid = st.nextToken();
				currentBid = st.nextToken();
				status = st.nextToken();
				bidUpdateSubscribers = st.nextToken();
				bidUpdateSubscribersSize = st.nextToken();
				bidUpdateSubscribersList = getBidSubscribers(bidUpdateSubscribersSize, bidUpdateSubscribers);
				
				//Set the item object with the received information
				item.setSellerId(sellerId);
				item.setBuyerId(buyerId);
				item.setItemId(itemId);
				item.setItemName(name);
		        item.setItemAttribute(itemAttribute);
		        item.setMinBid(minBid);
		        item.setCurrentBid(currentBid);
		        item.setItemStatus(status);
		        item.setBidUpdateSubscribers(bidUpdateSubscribersList);
		        
		        sendBidUpdateToSubscribedBuyers(bidUpdateSubscribersList, item, tag);
		        
		        broadcastItem(propagateItem, item, tag);
		        
		        updateSaleCurrentItemList(itemId, status);
		        
		        retValue = "Sale Finalize Received is " + buyerId + " " + itemId + " " + minBid+ " " + currentBid + " " + status;
			} else if(tag.equals("logOutSeller")) {
				Item item = new Item();
				PropagateItemEvent propagateItem= new PropagateItemEvent();
				
				//Tokenize the string received
				sellerId = st.nextToken();
				
				//Set the item object with the recieved information
				item.setSellerId(sellerId);
				item.setBuyerId(buyerId);
				item.setItemId(itemId);
				item.setItemName(name);
		        item.setItemAttribute(itemAttribute);
		        item.setMinBid(minBid);
		        item.setCurrentBid(currentBid);
		        item.setItemStatus(status);
		        item.setBidUpdateSubscribers(bidUpdateSubscribersList);
				
				removeItemCurrentItemList(sellerId, tag);
				
				broadcastItem(propagateItem, item, tag);
//				pout.println("Success:" + "You have sucessfully exited the market. Good Bye!");
			} else if(tag.equals("logOutBuyer")) {
				Item item = new Item();
				PropagateItemEvent propagateItem= new PropagateItemEvent();
				
				//Tokenize the string received
				buyerId = st.nextToken();
				
				//Set the item object with the received information
				item.setSellerId(sellerId);
				item.setBuyerId(buyerId);
				item.setItemId(itemId);
				item.setItemName(name);
		        item.setItemAttribute(itemAttribute);
		        item.setMinBid(minBid);
		        item.setCurrentBid(currentBid);
		        item.setItemStatus(status);
		        item.setBidUpdateSubscribers(bidUpdateSubscribersList);
				
				removeSubscriberCurrentItemList(buyerId, tag);
				
				removeSubscriberCurrentInterestList(buyerId);
				
				broadcastItem(propagateItem, item, tag);
//				pout.println("Success:" + "You have sucessfully exited the market. Good Bye!");
			}
			pout.flush();
		} catch (IOException e) {
//			System.err.println(e);
		}		
	}

}

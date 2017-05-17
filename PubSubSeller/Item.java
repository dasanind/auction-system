import java.io.PrintStream;
import java.util.ArrayList;

public class Item {
	
	String itemId;
	String itemName;
	String itemAttribute;
	String minBid;
	String currentBid;
	String buyerId;
	String sellerId;
	ArrayList<String> bidUpdateSubscribers;
	String itemStatus;
	
	public String getItemId() {
		return itemId;
	}
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}
	public String getItemName() {
		return itemName;
	}
	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	public String getItemAttribute() {
		return itemAttribute;
	}
	public void setItemAttribute(String itemAttribute) {
		this.itemAttribute = itemAttribute;
	}
	public String getMinBid() {
		return minBid;
	}
	public void setMinBid(String minBid) {
		this.minBid = minBid;
	}
	public String getCurrentBid() {
		return currentBid;
	}
	public void setCurrentBid(String currentBid) {
		this.currentBid = currentBid;
	}	
	public String getBuyerId() {
		return buyerId;
	}
	public void setBuyerId(String buyerId) {
		this.buyerId = buyerId;
	}
	public String getSellerId() {
		return sellerId;
	}
	public void setSellerId(String sellerId) {
		this.sellerId = sellerId;
	}
	public ArrayList<String> getBidUpdateSubscribers() {
		return bidUpdateSubscribers;
	}
	public void setBidUpdateSubscribers(ArrayList<String> bidUpdateSubscribers) {
		this.bidUpdateSubscribers = bidUpdateSubscribers;
	}
	public String getItemStatus() {
		return itemStatus;
	}
	public void setItemStatus(String itemStatus) {
		this.itemStatus = itemStatus;
	}
	public void sendItem(PrintStream pout, String eventType) {
		pout.println(eventType + ":" + sellerId + ":" + buyerId + ":" + itemId + ":" + itemName + ":" + itemAttribute + ":" + minBid +  ":" + currentBid
				+ ":" + itemStatus + ":" + bidUpdateSubscribers.toString() + ":" + bidUpdateSubscribers.size());
        pout.flush();
	}
}

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class PubSubBroker {
	
	int myId;
	int brokerServerPortNum;
    String brokerServerName;
    int numberOfBrokers;
    int serverPort;
    int branchingFactor;
    boolean is_root;
    int brokerCapacity;
    static boolean[] hasChild;
    ArrayList<Item> currentItemList = new ArrayList<Item>();
    ArrayList<Item> currentInterestList = new ArrayList<Item>();
    ArrayList<Item> matchedItemList = new ArrayList<Item>();
    static ArrayList<String> connectedClientList = new ArrayList<String>();
    
    BufferedReader din;
    PrintStream pout;    
    
/*  Constructor for the PubSubBroker class */ 
	public PubSubBroker(int id, int brokerServerPortNum, String brokerServerName, int numberOfBrokers, 
			int branchingFactor, boolean is_root, int brokerCapacity) {
		this.myId = id;
		this.brokerServerPortNum = brokerServerPortNum;
    	this.brokerServerName = brokerServerName;
    	this.numberOfBrokers = numberOfBrokers;
    	this.branchingFactor = branchingFactor;
    	this.is_root = is_root;
    	this.brokerCapacity = brokerCapacity;
    	
/*    	creating the threads to listen to the child and the parent */
    	if (is_root) {
    		for (int childIndex = 0; childIndex < branchingFactor; childIndex++) {
            	int serverPortChild = brokerServerPortNum +  700 + getChildId(childIndex);
            	new ConnectingThread(myId, numberOfBrokers, serverPortChild, brokerServerName, brokerServerPortNum, branchingFactor, 
            			childIndex, is_root, currentItemList, currentInterestList, connectedClientList).start();
            } 
    	} else {
    		for (int childIndex = 0; childIndex <= branchingFactor; childIndex++) { 
    			if(childIndex == branchingFactor) {
    				int serverPortParent = brokerServerPortNum +  myId + 500;
    				new ConnectingThread(myId, numberOfBrokers, serverPortParent, brokerServerName, brokerServerPortNum, branchingFactor, 
    						childIndex, is_root, currentItemList, currentInterestList, connectedClientList).start();
    			} else {
    				int serverPortChild = brokerServerPortNum +  700 + getChildId(childIndex);
    				new ConnectingThread(myId, numberOfBrokers, serverPortChild, brokerServerName, brokerServerPortNum, branchingFactor, 
    						childIndex, is_root, currentItemList, currentInterestList, connectedClientList).start();
    			}
    		}
    	}
    	
/*    	creating the threads for handling the buyer and seller at each broker */
    	for (int clientIndex = 0; clientIndex < brokerCapacity; clientIndex++) {
    		int serverPortClient = getClientPort(clientIndex); 
    		new ClientThread(myId, serverPortClient, brokerServerName, brokerServerPortNum, brokerCapacity, is_root, clientIndex, 
    				branchingFactor, numberOfBrokers, currentItemList, currentInterestList, connectedClientList).start();
    	}
	}
	
/* calculating the child id based on the child index*/	
	public int getChildId(int childIndex) {
		int childId = (myId * branchingFactor)  + childIndex + 1;
		return childId;
	}

/* calculating the client Port based on the client index*/		
	public int getClientPort(int clientIndex) {
		int serverPortClient = brokerServerPortNum + (myId * brokerCapacity) + clientIndex + 1000;
		return serverPortClient;
	}
	
/* 	getting the socket to connect to the parent*/	
	public void getParent() {
	  	serverPort = myId + 700 + brokerServerPortNum;
	  	try {
	  		Socket serverParentSocket = new Socket(brokerServerName, serverPort);
	  		din = new BufferedReader(new InputStreamReader(serverParentSocket.getInputStream()));
	  		pout = new PrintStream(serverParentSocket.getOutputStream());
	  	} catch (IOException e1) {
//	  		System.err.println("Unable to talk to server: ");
	  		getParent();
	  	}
	}
	
/* 	Connect to the parent*/		
	 public void connectToParent(int id)
	            throws IOException {
		 	getParent();     
	        pout.println("childId:" + id);
	        pout.flush();   
    }
	 
	public static void main(String[] args) {
		 
		int brokerServerPortNum = 0;
    	String brokerServerName = "";
    	int numberOfBrokers = 0;
    	int branchingFactor = 0;
    	int brokerCapacity = 0;
    	
/*    	Reading the config file to get the portnumber, servername, number of brokers, branching factor
    	of the tree  */
		try{
			FileInputStream fstream = new FileInputStream(args[0]);			  
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;			  
			while ((strLine = br.readLine()) != null)   {
    			StringTokenizer st = new StringTokenizer(strLine);
    			String tag = st.nextToken();  
    			if(tag.equalsIgnoreCase("brokerServerPortNum")) {
    				brokerServerPortNum = Integer.parseInt(st.nextToken());    				
    			} else if(tag.equalsIgnoreCase("brokerServerName")) {
    				brokerServerName = st.nextToken();    				
    			} else if(tag.equalsIgnoreCase("numberOfBrokers")) {
    				numberOfBrokers = Integer.parseInt(st.nextToken());    				
    			} else if(tag.equalsIgnoreCase("branchingFactor")) {
    				branchingFactor = Integer.parseInt(st.nextToken());    				
    			} else if(tag.equalsIgnoreCase("brokerCapacity")) {
    				brokerCapacity = Integer.parseInt(st.nextToken());    				
    			}
			  }
			  //Close the input stream
			  in.close();
			    }catch (Exception e){//Catch exception if any
//			  System.err.println("Error: " + e.getMessage());
			  }
		
		int myId = -1;
		boolean server_found = false;
		boolean is_root = false;
		
		//start the brokers
		try{
			BufferedReader stdinp = new BufferedReader(new InputStreamReader(System.in));
			while(!server_found) {
				System.out.println("Enter the server number between 0 and " + (numberOfBrokers - 1) + "(inclusive): ");
				String echoline = stdinp.readLine();
				try{
					int numserver = Integer.parseInt(echoline);
					if ((numserver >= 0) && (numserver < numberOfBrokers)){
						myId = numserver;
						if(myId == 0) {
							is_root = true;
						} else {
							is_root = false;
						}
		    			System.out.println("PubBroker "+ myId +" started: ");
		    			server_found = true;
					} 
					else  {
		    			System.out.println("Server number should be between 0 and " + (numberOfBrokers - 1) + "(inclusive).");
		    			System.out.println("\nPlease enter your choice");
		    		} 
				}catch (Exception e) {
		                System.err.println("Input not in proper form. Query not processed. Please enter your input.");
		        }
			}
		}
		catch (Exception e) {
                System.err.println("Input not in proper form. Query not processed. Aborting.");
        }
		try {
			PubSubBroker nb = new PubSubBroker(myId, brokerServerPortNum, brokerServerName, numberOfBrokers, branchingFactor, is_root, brokerCapacity);
			if(!is_root) {
				nb.connectToParent(myId);
			}
		}
		catch (Exception e) {
//			System.err.println("Server aborted: "+ e);
		}
	}
}

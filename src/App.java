//Application to detect whether a given topology is a ring topology

/*Payload message type details:
	1 = ring detection
	2 = ring failure
*/

class Application implements Listener
{
	Node myNode;
	NodeID myID;
	
	//Node ids of my neighbors
	NodeID[] neighbors;
	
	//Flag to check if connection to neighbors[i] has been broken
	boolean[] brokenNeighbors;
	
	boolean detectingRing;
	boolean isRing;
	
	//ID of my predecessor in the ring
	NodeID pred;
	
	//flag to indicate that the ring detection is over
	boolean terminating;
	
	//synchronized receive
	//invoked by Node class when it receives a message
	public synchronized void receive(Message message)
	{
		//Extract payload from message
		Payload p = Payload.getPayload(message.data);
		
		if(p.messageType == 1)
		{
			//Messagetype 1 is ring detection
			if(detectingRing)
			{
				//detectingRing flag is only true for node 0
				//If node 0 receives a payload with messagetype 1, then topology is a ring
				//Set isRing to true and wakeup all waits using notifyAll()
				isRing = true;
				detectingRing = false;
				notifyAll();
			}
			else if(neighbors.length != 2)
			{
				//If myid is not 0 and I do not have exactly two neighbors, then topology is not a ring
				//Send message with messagetype 2 back to the node that sent the message
				Payload newp = new Payload(2);
				Message msg = new Message(myID, newp.toBytes());
				myNode.send(msg, message.source);
			}
			else
			{
				//store the id of the node that sent message with messagetype 1 in pred
				pred = message.source;
				
				//Send message with messagetype 1 to newNeighbor
				NodeID newNeighbor = (pred.getID() == neighbors[0].getID()) ? neighbors[1] : neighbors[0];
				Payload newp = new Payload(1);
				Message msg = new Message(myID, newp.toBytes());
				myNode.send(msg, newNeighbor);
			}
		}
		else if(p.messageType == 2)
		{
			//Messagetype 2 implies topology is not a ring
			if(detectingRing)
			{
				//For node 0
				isRing = false;
				detectingRing = false;
				//Wakeup all processes that invoked wait()
				notifyAll();
			}
			else
			{
				//Inform pred node that topology is not a ring
				Payload newp = new Payload(2);
				Message msg = new Message(myID, newp.toBytes());
				myNode.send(msg, pred);
			}
		}
		
	}
	
	//If communication is broken with one neighbor, tear down the node
	public synchronized void broken(NodeID neighbor)
	{
		for(int i = 0; i < neighbors.length; i++)
		{
			if(neighbor.getID() == neighbors[i].getID())
			{
				brokenNeighbors[i] = true;
				notifyAll();
				if(!terminating)
				{
					terminating = true;
					myNode.tearDown();
				}
				return;
			}
		}
	}
	
	//Method called by node 0 to initate ring detection algorithm
	//Synchronized method only releases control on wait or return
	public synchronized boolean detectRing()
	{
		if(neighbors.length != 2)
		{
			//Each node in a ring should only have two neighbors
			return false;
		}
		detectingRing = true;
		
		//Send message with messagetype 1 to first neighbor
		Payload p = new Payload(1);
		Message msg = new Message(myID, p.toBytes());
		myNode.send(msg, neighbors[0]);
		
		while(detectingRing)
		{
			try
			{
				//wait till node 0 receives messagetype 1 or 2
				//1 means success, 2 means failure
				wait();
			}
			catch(InterruptedException ie)
			{
			}
		}
		return isRing;
	}

	String configFile;
	
	//Constructor
	public Application(NodeID identifier, String configFile)
	{
		myID = identifier;
		this.configFile = configFile;
	}
	
	//Synchronized run. Control only transfers to other threads once wait is called
	public synchronized void run()
	{
		//Construct node
		myNode = new Node(myID, configFile, this);
		neighbors = myNode.getNeighbors();
		brokenNeighbors = new boolean[neighbors.length];
		for(int i = 0; i < neighbors.length; i++)
		{
			brokenNeighbors[i] = false;
		}
		detectingRing = false;
		terminating = false;
		
		//Node 0 initiates ring detection algorithm
		if(myID.getID() == 0)
		{
			if(detectRing())
			{
				System.out.println("Topology is a ring");
			}
			else
			{
				System.out.println("Topology is not a ring");
			}
			myNode.tearDown();
		}
		for(int i = 0; i < neighbors.length; i++)
		{
			while(!brokenNeighbors[i])
			{
				try
				{
					//wait till we get a broken reply from each neighbor
					wait();
				}
				catch(InterruptedException ie)
				{
				}
			}
		}
	}
}

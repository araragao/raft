package raft;

public class IdIndexPair {
	
	int serverId; 
	int index;
	
	IdIndexPair(int receivedServerId, int receivedIndex) {
		setServerId(receivedServerId);
		setIndex(receivedIndex);
	}
	
	public void setServerId(int receivedServerId) {
		this.serverId = receivedServerId;
	}
	
	public int getServerId() {
		return this.serverId;
	}
	
	public void setIndex(int receivedIndex) {
		this.index = receivedIndex;
	}

	public int getIndex() {
		return this.index;
	}
}
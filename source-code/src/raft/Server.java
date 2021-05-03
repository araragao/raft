package raft;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.Math;

public class Server {
	
	// // // COMMUNICATION PACKAGES // // // 
	
	UnicastCommunicationPackage unicastCommunication;
	MulticastCommunicationPackage multicastCommunication;
	Message message;
	
	// // // COMMON VARIABLES TO ALL SERVER TYPES // // //
	
	int clientId;
	
	int serverId;
	int leaderId;
	int timeOut;
	int currentRole; // 1 -> Leader // 2 -> Candidate // 3 -> Follower
	int stateMachineValue;
	
	Entry firstEntry;
	List<Entry> log = new ArrayList<Entry>();
	
	// Persistent State Variables
	int currentTerm;
	int votedFor;
	
	// Volatile State Variables
	int commitIndex; 
	int lastApplied;
	
	// Persistent Port Array
	List<Integer> portList = new ArrayList<Integer>();
	
	// // // LEADER ONLY VARIABLES // // // 
	
	int leaderTimeOut; // Unit: ms
	int heartbeatTimer; // Unit: ns
	int multicastTimeOut; // Unit: ms
	long lastHeartbeat;
	
	// Volatile State Variables
	List<IdIndexPair> nextIndex = new ArrayList<IdIndexPair>();
	List<IdIndexPair> matchIndex = new ArrayList<IdIndexPair>();
	
	// // // CANDIDATE AND FOLLOWER VARIABLES // // // 
	
	int electionTimeOut; // electionTimeOut = timeOut
	long lastRequestVote;
	
	// // // CONSTRUCTOR // // // 
	
	public Server(int receivedServerId, int receivedCurrentTerm, int receivedVotedFor, int unicastPort, int multicastPort) {
		setMulticastCommunication(multicastPort);
		setElectionTimeOut(150, 300);
		setTimeOut(electionTimeOut);
		setUnicastCommunication(unicastPort, electionTimeOut);
		setMessage();
		
		setServerId(receivedServerId);
		setCurrentRole(3);
		setStateMachineValue(0);
		firstEntry = new Entry(-1, null); // Set first entry of each server's log to -1 so that we don't have to mess around other functions return value
		setLogEntry(firstEntry);
		
		setCurrentTerm(receivedCurrentTerm);
		setVotedFor(receivedVotedFor);
		
		setCommitIndex(0);
		setLastApplied(0);
		
		setLeaderTimeOut(1);
		setHeartbeatTimer(50000000);
		setMulticastTimeOut(1);
		setLastHeartbeat(0);

		resetNextIndex(getLog().size());
		resetMatchIndex();
		setLastRequestVote(0);
	}
	
	// // // AVAILABLE FUNCTIONS // // // 
	
	public void setUnicastCommunication(int portNumber, int timeOut) {
		this.unicastCommunication = new UnicastCommunicationPackage(portNumber, timeOut);
	}
	
	public UnicastCommunicationPackage getUnicastCommunication() {
		return this.unicastCommunication;
	}
	
	public void setMulticastCommunication(int portNumber) {
		this.multicastCommunication = new MulticastCommunicationPackage(portNumber);
	}
	
	public MulticastCommunicationPackage getMulticastCommunication() {
		return this.multicastCommunication;
	}
	
	public void setMessage() {
		this.message = new Message();
	}
	
	public Message getMessage() {
		return this.message;
	}
	
	public void setClientId(int receivedClientId) {
		this.clientId = receivedClientId;
	}
	
	public int getClientId() {
		return this.clientId;
	}
	
	public void setServerId(int receivedServerId) {
		this.serverId = receivedServerId;
	}
	
	public int getServerId() {
		return this.serverId;
	}
	
	public void setLeaderId(int receivedServerId) {
		this.leaderId = receivedServerId;
	}
	
	public int getLeaderId() {
		return this.leaderId;
	}
	
	public void setTimeOut(int receivedTimeOut) {
		this.timeOut = receivedTimeOut;
	}
	
	public int getTimeOut() {
		return this.timeOut;
	}
	
	public void setCurrentRole(int receivedRole) {
		this.currentRole = receivedRole;
	}
	
	public int getCurrentRole() {
		return this.currentRole;
	}
	
	public void printCurrentRole() {
		switch(currentRole) {
			case 1:
				System.out.println("Server " + serverId + " is the leader");
				break;
			case 2:
				System.out.println("Server " + serverId + " is a candidate");
				break;
			case 3:
				System.out.println("Server " + serverId + " is a follower");
				break;
			default:
				System.out.println("Server " + serverId + " has no current role associated");
				break;
		}
	}
	
	public void setStateMachineValue(int receivedValue) {
		this.stateMachineValue = receivedValue;
	}
	
	public void updateStateMachineValue() {
		int whileIntAuxiliar = getLastApplied();
		while(getCommitIndex() > getLastApplied()) {
			String[] commandFromLog = message.messageSplit(getLogEntry(whileIntAuxiliar+1).getCommand());
			if(commandFromLog[0].equals("add")) {
				setStateMachineValue(getStateMachineValue() + Integer.parseInt(commandFromLog[1]));
			}
			else {
				setStateMachineValue(getStateMachineValue() - Integer.parseInt(commandFromLog[1]));
			}
			setLastApplied(getLastApplied() + 1);
			whileIntAuxiliar = getLastApplied();
		}
	}
	
	public int getStateMachineValue() {
		return this.stateMachineValue;
	}
	
	public void setLogEntry(Entry receivedEntry) {
		this.log.add(receivedEntry);
	}
	
	public Entry getLogEntry(int receivedIndex) {
		return this.log.get(receivedIndex);
	}
	
	public void deleteInconsistentEntries(int receivedIndex) {
	     for(int i = this.log.size() - 1; i >= receivedIndex; i--) {
	    	 this.log.remove(i);
	     }
	}
	
	public List<Entry> getLog() {
		return this.log;
	}
	
	public void printAllLogEntries() {
		Iterator<Entry> iter = this.log.iterator();
	      while (iter.hasNext()) {
	         Entry auxEntry = iter.next();
	         System.out.println(serverId + " Log Term " + auxEntry.getTerm());
	         System.out.println(serverId + " Log Command " + auxEntry.getCommand());
	      }
	}
	
	public int getPrevLogTerm(List<Entry> receivedLog, int receivedPrev) {
		return receivedLog.get(receivedLog.size() - receivedPrev).getTerm();
	}
	
	public int getPrevLogIndex(List<Entry> receivedLog, int receivedPrev) {
		return (receivedLog.size() - receivedPrev);
	}
	
	
	public void setCurrentTerm(int receivedCurrentTerm) {
		this.currentTerm = receivedCurrentTerm;
	}
	
	public int getCurrentTerm() {
		return this.currentTerm;
	}
	
	public void setVotedFor(int receivedVotedFor) {
		this.votedFor = receivedVotedFor;
	}
	
	public int getVotedFor() {
		return this.votedFor;
	}
	
	public void setCommitIndex(int receivedCommitIndex) {
		this.commitIndex = receivedCommitIndex;
	}
	
	public int getCommitIndex() {
		return this.commitIndex;
	}
	
	public int shouldLeaderCommit() {
		int NumberOfEqualMatchIndexes = 0;
		for(int N = maxMatchIndex(); N > 0; N--) {
			if(N <= getCommitIndex()) {
				return 0; // Has nothing to commit
			}
			if(getLogEntry(N).getTerm() == getCurrentTerm()) {
				for(int i = 0; i < getMatchIndex().size(); i++) {
					if(getMatchIndex().get(i).getIndex() >= N) {
						NumberOfEqualMatchIndexes++;
					}
				}
				if((float) (NumberOfEqualMatchIndexes) > 0.5*((float) (getMatchIndex().size()))) {
					return N;
				}
			}
		}
		return 0;
	}
	
	public void setLastApplied(int receivedLastApplied) {
		this.lastApplied = receivedLastApplied;
	}
	
	public int getLastApplied() {
		return this.lastApplied;
	}
	
	public void addPortListPort(int portNumber) {
		this.portList.add(portNumber);
	}
	
	public int getPortListPort(int receivedIndex) {
		return this.portList.get(receivedIndex);
	}
	
	public List<Integer> getPortList() {
		return this.portList;
	}
	
	public void printNetworkInfo() {
	String IPAddress = getUnicastCommunication().getLocalIP().toString();
	int unicastPort = getUnicastCommunication().getUnicastPort();
	int multicastPort = getMulticastCommunication().getMulticastPort();
	System.out.println("IP Address: " + IPAddress);
	System.out.println("unicastPort: " + unicastPort);
	System.out.println("multicastPort: " + multicastPort);
	}
	
	// // // LEADER ONLY FUNCTIONS // // // 
	
	public void setLeaderTimeOut(int receivedLeaderTimeOut) {
		this.leaderTimeOut = receivedLeaderTimeOut;
	}
	
	public int getLeaderTimeOut() {
		return this.leaderTimeOut;
	}
	
	public void setHeartbeatTimer(int receivedHeartbeatTimer) {
		this.heartbeatTimer = receivedHeartbeatTimer;
	}
	
	public int getHeartbeatTimer() {
		return this.heartbeatTimer;
	}
	
	public void setLastHeartbeat(long receivedLastHeartbeat) {
		this.lastHeartbeat = receivedLastHeartbeat;
	}
	
	public long getLastHeartbeat() {
		return this.lastHeartbeat;
	}
	
	public void setNextIndex(int receivedServerId, int receivedIndex) {
		IdIndexPair receivedIdIndexPair = new IdIndexPair(receivedServerId,receivedIndex);
		this.nextIndex.add(receivedIdIndexPair);
	}
	
	public List<IdIndexPair> getNextIndex() {
		return this.nextIndex;
	}
	
	public void resetNextIndex(int receivedValue) {
		for(int i = 0; i < nextIndex.size(); i++) {
			nextIndex.get(i).setIndex(receivedValue);
		}
	}
	
	public void updateNextIndex(int receivedServerId, int receivedIndex) {
		for(int i = 0; i < nextIndex.size(); i++) {
			IdIndexPair auxPair = this.nextIndex.get(i);
			if(auxPair.getServerId() == receivedServerId) {
				if(receivedIndex < 0) {
					nextIndex.get(i).setIndex(0);
				}
				else {
					nextIndex.get(i).setIndex(receivedIndex);	
				}
			}
		}
	}
	
	public int minNextIndex() {
		int minValue = nextIndex.get(0).getIndex();
		int actualValue = 0;
		for(int i = 1; i < nextIndex.size(); i++) {
			actualValue = nextIndex.get(i).getIndex();
			if(actualValue < minValue) {
				minValue = actualValue;
			}
		}
		return minValue;
	}
	
	public IdIndexPair getIdIndexPairFromNextIndex(int receivedServerId) {
		for(int i = 0; i < nextIndex.size(); i++) {
			IdIndexPair auxPair = this.nextIndex.get(i);
			if(auxPair.getServerId() == receivedServerId) {
				return auxPair;
			}
		}
		return null;
	}
	
	public void setMatchIndex(int receivedServerId, int receivedIndex) {
		IdIndexPair receivedIdIndexPair = new IdIndexPair(receivedServerId,receivedIndex);
		this.matchIndex.add(receivedIdIndexPair);
	}
	
	public List<IdIndexPair> getMatchIndex() {
		return this.matchIndex;
	}
	
	public void resetMatchIndex() {
		for(int i = 0; i < matchIndex.size(); i++) {
			matchIndex.get(i).setIndex(0);
		}
	}
	
	public void updateMatchIndex(int receivedServerId, int receivedIndex) {
		for(int i = 0; i < matchIndex.size(); i++) {
			IdIndexPair auxPair = this.matchIndex.get(i);
			if(auxPair.getServerId() == receivedServerId) {
				matchIndex.get(i).setIndex(receivedIndex);
			}
		}
	}
	
	public int maxMatchIndex() {
		int maxValue = matchIndex.get(0).getIndex();
		int actualValue = 0;
		for(int i = 1; i < matchIndex.size(); i++) {
			actualValue = matchIndex.get(i).getIndex();
			if(actualValue > maxValue) {
				maxValue = actualValue;
			}
		}
		return maxValue;
	}
	
	public IdIndexPair getIdIndexPairFromMatchIndex(int receivedServerId) {
		for(int i = 0; i < matchIndex.size(); i++) {
			IdIndexPair auxPair = this.matchIndex.get(i);
			if(auxPair.getServerId() == receivedServerId) {
				return auxPair;
			}
		}
		return null;
	}
	
	public void printIdIndexPair(){
		System.out.println("Lider: "+ serverId + " IndexSize: "+ matchIndex.size());
		for(int i = 0; i<matchIndex.size(); i++) {
			System.out.println("Lider: "+ serverId + " Index: "+ matchIndex.get(i).getIndex() + " ServerId: " + matchIndex.get(i).getServerId());
		}
	}
	
	// // // CANDIDATE AND FOLLOWER FUNCTIONS // // //
	
	public void setElectionTimeOut(int receivedMin, int receivedMax) {
		this.electionTimeOut = (int) (Math.random() * (receivedMax - receivedMin + 1) + receivedMin);
	}
	
	public int getElectionTimeOut() {
		return this.electionTimeOut;
	}
	
	public void setLastRequestVote(long receivedLastRequestVote) {
		this.lastRequestVote = receivedLastRequestVote;
	}
	
	public long getLastRequestVote() {
		return this.lastRequestVote;
	}
	
	public void setMulticastTimeOut(int receivedMulticastTimeOut) {
		this.multicastTimeOut = receivedMulticastTimeOut;
	}
	
	public int getMulticastTimeOut() {
		return this.multicastTimeOut;
	}
	
	public void clearMulticastBuffer() {
		this.multicastCommunication.receiveMulticastMessage();
	}
}

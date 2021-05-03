package raft;

import java.util.List;
import java.lang.Math;

public class Message {
	
	public Message() {
	}
	
	// MESSAGE BUILDER TYPES
	// 0 -> Client Message
	// 1 -> Append Entry
	// 2 -> Acknowledge for Append Entry with Entry
	// 3 -> Acknowledge for Append Entry without Entry (heartbeat)
	// 4 -> Request Vote RPC
	// 5 -> Acknowledge for Vote RPC
	
	// Message type: 0 -> Client Message
	// Generates a random value between a minimum and a maximum value and a random operation, either addition or subtraction
	// and formats it with the desired structure
	public String clientMessageBuilder(int clientId, int minValue, int maxValue) {
		int randomValue = (int) (Math.random() * (maxValue - minValue + 1) + minValue);
		int addOrSub = (int) (Math.random() * (1 - 0 + 1) + 0);
		String stringToSend = String.valueOf(0) + " " + String.valueOf(clientId) + " ";
		if(addOrSub == 1) {
			stringToSend = stringToSend + "add" + " " + String.valueOf(randomValue);
		}
		
		else {
			stringToSend = stringToSend + "sub" + " " + String.valueOf(randomValue);
		}
		return stringToSend;
	}
	
	// Message type: 1 -> Append Entry
	// Formats the message with the desired structure
	public String appendEntryMessageBuilder(int leaderId, int leaderCommit, int leaderTerm, int prevLogTerm, int prevLogIndex, List<Entry> receivedLog) {
		String stringToSend = String.valueOf(1) + " " + String.valueOf(leaderId) + " " +  String.valueOf(leaderCommit) + " " +  String.valueOf(leaderTerm) + " " + String.valueOf(prevLogTerm) + " " + String.valueOf(prevLogIndex);
		if(receivedLog == null) { // Hearbeat
		}
		else { // Default appendEntry
			for(int i=0; i < receivedLog.size(); i++) {
				Entry auxLogEntry = receivedLog.get(i);
				stringToSend = stringToSend + " " + String.valueOf(auxLogEntry.getTerm()) + " " + auxLogEntry.getCommand();
			}
		}
		return stringToSend;
	}
	
	// Message type: 2, 3 or 5 -> Acknowledge
	// Formats the message with correct operation mode, given that this Message Builder is used for 3 different Message types, and other necessary values
	// Introduces a delay
	public String acknowledgeBuilder(int operationMode, int serverId, int term, boolean success) {
		String stringToSend = String.valueOf(operationMode) + " " + String.valueOf(serverId) + " " + String.valueOf(term) + " ";
		if(success == true) {
			stringToSend = stringToSend + "1";
		}
		else {
			stringToSend = stringToSend + "0";
		}
		messageDelay(0, 2); // Introduces delay between 0 and 2ms
		return stringToSend;
	}
	
	// Message type: 4 -> Request Vote RPC
	// Formats the message with the desired structure
	public String requestVoteRPCBuilder(int candidateID, int candidateTerm, int lastLogTerm, int lastLogIndex) {
		String stringToSend = String.valueOf(4) + " " + String.valueOf(candidateID) + " " + String.valueOf(candidateTerm) + " " + String.valueOf(lastLogTerm) + " " + String.valueOf(lastLogIndex);
		return stringToSend;
	}
    		
    public boolean processVote(Server serverNode, String[] receivedMessageSplit) {
    	int serverVotedFor = serverNode.getVotedFor();
    	int receivedCandidateId = Integer.parseInt(receivedMessageSplit[1]);
    	int serverTerm = serverNode.getCurrentTerm();
    	int receivedTerm = Integer.parseInt(receivedMessageSplit[2]);
    	int serverLastLogTerm = serverNode.getPrevLogTerm(serverNode.getLog(), 1);
    	int receivedLastLogTerm = Integer.parseInt(receivedMessageSplit[3]);
    	int serverLastLogIndex = serverNode.getPrevLogIndex(serverNode.getLog(), 1);
    	int receivedLastLogIndex = Integer.parseInt(receivedMessageSplit[4]);
    	
    	boolean acknowledgeBool;
    	
    	if(serverTerm > receivedTerm) {
    		acknowledgeBool = false;
    	}
    	else if(serverTerm == receivedTerm) {
    		if(serverVotedFor == 0 || (serverVotedFor == receivedCandidateId)) {
    			acknowledgeBool = true;
    		}
    		else {
    			acknowledgeBool = false;
    		}
    	}
    	else { // serverTerm < receivedTerm
    		serverNode.setCurrentTerm(receivedTerm);
    		if((serverLastLogTerm <= receivedLastLogTerm) && (serverLastLogIndex <= receivedLastLogIndex)) {
    			acknowledgeBool = true;
    			serverNode.setVotedFor(receivedCandidateId);
    		}
    		else {
    			acknowledgeBool = false;
    			serverNode.setVotedFor(0);
    		}
    	}
    	return acknowledgeBool;	
    }
    
	public boolean processMessageFollower(Server serverNode , String[] receivedMessageSplit) {
		int numberOfRows = receivedMessageSplit.length;
		int receivedLeaderId = Integer.parseInt(receivedMessageSplit[1]);
		int receivedLeaderCommit = Integer.parseInt(receivedMessageSplit[2]);
		int receivedLeaderTerm = Integer.parseInt(receivedMessageSplit[3]);
		int receivedPrevLogTerm = Integer.parseInt(receivedMessageSplit[4]);
		int receivedPrevLogIndex = Integer.parseInt(receivedMessageSplit[5]);
		
		boolean evaluateLogConsistancy = false;
		boolean acknowledgeBool = true;
		boolean evaluateTermConsistancy = serverNode.getCurrentTerm() <= receivedLeaderTerm;
		
		if (receivedPrevLogIndex < serverNode.getLog().size()) { // Same size or Follower's log is bigger
			
			evaluateLogConsistancy = serverNode.getLogEntry(receivedPrevLogIndex).getTerm() == receivedPrevLogTerm;	
			
			if(!evaluateTermConsistancy || !evaluateLogConsistancy) { // Different
				acknowledgeBool = false;
				if(!evaluateLogConsistancy) { // Follower has more entries than leader
					serverNode.deleteInconsistentEntries(receivedPrevLogIndex); 
				} 	
			}
			else { // Equal
				acknowledgeBool = true;
				serverNode.setCurrentTerm(receivedLeaderTerm);
				serverNode.setLeaderId(receivedLeaderId);
				if(receivedLeaderCommit > serverNode.getCommitIndex()) {
					serverNode.setCommitIndex(Math.min(receivedLeaderCommit, serverNode.getLog().size()-1));
				}
			}
			
		}
		else { // prevLogIndex does not exist in Follower
			acknowledgeBool = false; 
		}
		
		if((numberOfRows > 6) && (acknowledgeBool == true)) { // Append new entries  
			int i = 6;
			int receivedEntryTerm = 0;
			int auxPrevLogIndex = receivedPrevLogIndex;
			while(i < numberOfRows) {
				
				if(auxPrevLogIndex >= serverNode.getPrevLogIndex(serverNode.getLog(), 1)) { // Reaches entries that Follower does not have so they must be added
					
					receivedEntryTerm =  Integer.parseInt(receivedMessageSplit[i]);
					String receivedEntryCommand =  receivedMessageSplit[i+1] + " " + receivedMessageSplit[i+2];
					Entry receivedEntry = new Entry(receivedEntryTerm, receivedEntryCommand);
		    		serverNode.setLogEntry(receivedEntry);
				}
				auxPrevLogIndex++;	
				i = i+3; 
			}
		}
		
		return acknowledgeBool;
	}
	
	public String clientAnswerBuilder(int receivedValue) {
	    String stringToSend = String.valueOf(receivedValue);
	    return stringToSend;
	}
	
	public String[] messageSplit(String receivedMessage) {
		String[] messageSplited = receivedMessage.split("\\s+");
		return messageSplited;
	}
	
	public void messageDelay(int receivedMinValue, int receivedMaxValue) {
		try {
			Thread.sleep((long)(Math.random() * (receivedMaxValue - receivedMinValue + 1) + 0));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

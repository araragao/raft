package raft;

import java.util.ArrayList;
import java.util.List;

public class ServerThread extends Thread {
	
	Server serverNode;
	ServerGUI serverGUI;
	
	int unicastPort;
	int multicastPort;
	
	public ServerThread(int unicastPort, int multicastPort) {
		serverNode = new Server(unicastPort, 0, 0, unicastPort, multicastPort);
		this.unicastPort = unicastPort;
		this.multicastPort = multicastPort;
	}
	
	public void run() {
		// GUI startup
		serverGUI = new ServerGUI(serverNode.getServerId(),serverNode.getElectionTimeOut(),serverNode.getCurrentRole(),serverNode.getCurrentTerm(),serverNode.getStateMachineValue(), serverNode.getLog());
	    
		threadSleep(2000);
		threadSleep(2000);

		// Acquiring all other server ports using Multicast
		serverNode.getMulticastCommunication().sendMulticastMessage(String.valueOf(this.unicastPort));
		
		String receivedServerIdInString = null;
		for(int j = 0 ; j < 7; j++) {
			receivedServerIdInString = serverNode.getMulticastCommunication().receiveMulticastMessage();
			int receivedServerIdInInt = Integer.parseInt(receivedServerIdInString);
			if(receivedServerIdInInt != serverNode.getServerId()) {
				serverNode.addPortListPort(receivedServerIdInInt);
				serverNode.setNextIndex(receivedServerIdInInt, 0); // Adds ServerId to the List for future index updates of each server
				serverNode.setMatchIndex(receivedServerIdInInt, 0); // Adds ServerId to the List for future index updates of each server
			}
		}
		
		// Loop
		String receivedMessageNotParsed;
		String[] receivedMessageSplit;
		
		String receivedCommandNotParsed;
		String[] receivedCommandSplit;
		
		serverNode.getMulticastCommunication().setMulticastSocketTimeOut(serverNode.getMulticastTimeOut());
		
		int numberOfRequestVoteAcknowledgesReceived = 1;
		List<Entry> auxLog = new ArrayList<Entry>();
		
		while(true) {
			// GUI update
			serverGUI.updateLogTable(serverNode.getLog());
			serverGUI.updateLabels(serverNode.getServerId(), serverNode.getElectionTimeOut(), serverNode.getCurrentRole(), serverNode.getCurrentTerm(), serverNode.getStateMachineValue());
			
		    long currentTime;
		    int currentRole = serverNode.getCurrentRole();
		    switch(currentRole) {
			    case 1: // Leader
			    	int leaderTimeOut = serverNode.getLeaderTimeOut();
			    	if(serverNode.getTimeOut() != leaderTimeOut) {
			    		serverNode.setTimeOut(leaderTimeOut);
			    		serverNode.getUnicastCommunication().setUnicastSocketTimeOut(serverNode.getTimeOut());
			    	}
			    	
			    	receivedCommandNotParsed = serverNode.getMulticastCommunication().receiveMulticastMessage(); // Receiving command from Client
			    	if(receivedCommandNotParsed != "TIMEOUT") {
			    		receivedCommandSplit = serverNode.getMessage().messageSplit(receivedCommandNotParsed);
			    		int receivedMessageType = Integer.parseInt(receivedCommandSplit[0]);
			    		if(receivedMessageType == 0) { // Client message
			    			
			    			int receivedClientId = Integer.parseInt(receivedCommandSplit[1]);
				    		String receivedCommand = receivedCommandSplit[2] + " " + receivedCommandSplit[3];
				    		Entry receivedEntry = new Entry(serverNode.getCurrentTerm(), receivedCommand);	
				    		serverNode.setClientId(receivedClientId);
				    		serverNode.setLogEntry(receivedEntry);
			    		}
			    	}
			    
			    	currentTime = System.nanoTime();
			    	
			    	if((currentTime - serverNode.getLastHeartbeat()) > serverNode.getHeartbeatTimer()) {
			    		String appendEntryMessage = null;

			    		if(serverNode.minNextIndex() <= (serverNode.getLog().size() -1)) { // Normal appendEntry message
			    			for(int i = serverNode.minNextIndex(); i < serverNode.getLog().size(); i++) {
			    				Entry lastServerEntry = serverNode.getLogEntry(i);
				    			auxLog.add(lastServerEntry);
			    			}
			    			appendEntryMessage = serverNode.getMessage().appendEntryMessageBuilder(serverNode.getServerId(), serverNode.getCommitIndex(), serverNode.getCurrentTerm(), 
			    					serverNode.getLogEntry(serverNode.minNextIndex() - 1).getTerm(), serverNode.minNextIndex() - 1, auxLog);
			    			clearLog(auxLog);
			    		}
			    		
			    		else { // Heartbeat appendEntry message
			    			appendEntryMessage = serverNode.getMessage().appendEntryMessageBuilder(serverNode.getServerId(), serverNode.getCommitIndex(), serverNode.getCurrentTerm(), 
			    					serverNode.getLogEntry(serverNode.minNextIndex() - 1).getTerm(), serverNode.minNextIndex() - 1, null);
			    		}
			    		
			    		serverNode.getUnicastCommunication().broadcastMessage(appendEntryMessage, serverNode.getUnicastCommunication().localIP, serverNode.getPortList());
			    		serverNode.setLastHeartbeat(currentTime);
			    	}
			    	
			    	for(int k = 0; k < serverNode.getPortList().size(); k++) { // appendEntry acknowledge wait...
			    		
			    		receivedMessageNotParsed = serverNode.getUnicastCommunication().receiveUnicastMessage();
			    		
			    		if(receivedMessageNotParsed != "TIMEOUT") {
			    			receivedMessageSplit = serverNode.getMessage().messageSplit(receivedMessageNotParsed);
			    			int receivedMessageType = Integer.parseInt(receivedMessageSplit[0]);
			    			int receivedServerId = 0;
			    			int receivedTerm = 0;
			    			int receivedBooleanValue = 0;
			    			
			    			if ((receivedMessageType == 2) || (receivedMessageType == 3)) {
			    				receivedBooleanValue = Integer.parseInt(receivedMessageSplit[3]);
				    			receivedServerId = Integer.parseInt(receivedMessageSplit[1]);
				    			receivedTerm = Integer.parseInt(receivedMessageSplit[2]);
			    			}
			    			
			    			if((receivedMessageType == 2) && (receivedBooleanValue == 1)) { // Received True RPC Acknowledge
			    				serverNode.updateMatchIndex(receivedServerId, serverNode.getLog().size()-1);
			    				
			    				serverNode.updateNextIndex(receivedServerId, serverNode.getLog().size());
			    			}
			    			else if((receivedMessageType == 3) && (receivedBooleanValue == 1)) { // Hearbeat appendEntry message

			    			}
			    			else if(((receivedMessageType == 2) || (receivedMessageType == 3)) && (receivedBooleanValue == 0)) { // Received False RPC Acknowledge
			    				if(serverNode.getCurrentTerm() < receivedTerm) { // Leader -> Follower
			    					serverNode.setCurrentTerm(receivedTerm); // Updates itself with the new term value
			    					serverNode.setCurrentRole(3); // Follower
			    					
			    				} else { // Log replication
			    					serverNode.updateNextIndex(receivedServerId, Math.max( 1 ,serverNode.getIdIndexPairFromNextIndex(receivedServerId).getIndex()-1)); // Decrementation of server Node's log receivedServerId index
			    				}
			    			}
			    			else if(receivedMessageType == 4) { // Received request Vote message
			    				boolean acknowledgeBool = serverNode.getMessage().processVote(serverNode, receivedMessageSplit);
			    				int receivedCandidate = Integer.parseInt(receivedMessageSplit[1]);

			    				String stringToSend = serverNode.getMessage().acknowledgeBuilder(5, serverNode.getServerId(), serverNode.getCurrentTerm(), acknowledgeBool);
			    				serverNode.getUnicastCommunication().sendUnicastMessage(stringToSend, serverNode.getUnicastCommunication().localIP, receivedCandidate);
			    				
			    				if(acknowledgeBool == true) { // Converts to Follower
			    					serverNode.setCurrentRole(3); 
			    				}
			  
			    			}
			    			else if(receivedMessageType == 1){ // Received heartbeat from new leader
			    				int receivedLeaderTerm = Integer.parseInt(receivedMessageSplit[3]);
			    				int receivedLeaderId = Integer.parseInt(receivedMessageSplit[1]);
			    			
			    				if(receivedLeaderTerm > serverNode.getCurrentTerm()) { // Change to follower
			    					serverNode.setCurrentRole(3);
			    				}
			    				
			    				boolean acknowledge = serverNode.getMessage().processMessageFollower(serverNode, receivedMessageSplit);
		    					
			    				String acknowledgeMessage = serverNode.getMessage().acknowledgeBuilder(3, serverNode.getServerId(), serverNode.getCurrentTerm(), acknowledge);	
								serverNode.getUnicastCommunication().sendUnicastMessage(acknowledgeMessage, serverNode.getUnicastCommunication().localIP, receivedLeaderId);
			    			}
			    		}
			    	}
			    	
		    		int indexToCommit = serverNode.shouldLeaderCommit();
			    	if(indexToCommit > 0) {
			    		serverNode.setCommitIndex(indexToCommit);
			    		serverNode.updateStateMachineValue();
			    		String clientAnswer = serverNode.getMessage().clientAnswerBuilder(serverNode.getStateMachineValue());
			    		serverNode.getUnicastCommunication().sendUnicastMessage(clientAnswer, serverNode.getUnicastCommunication().localIP, serverNode.getClientId());
			    	}

					break;
				case 2: // Candidate
					serverNode.clearMulticastBuffer();
					
					int candidateElectionTimeOut = serverNode.getElectionTimeOut();
			    	
			    	if(serverNode.getTimeOut() != candidateElectionTimeOut) {
			    		serverNode.setTimeOut(candidateElectionTimeOut);
			    		serverNode.getUnicastCommunication().setUnicastSocketTimeOut(serverNode.getTimeOut());
			    	}
			    	
					currentTime = System.nanoTime();
			    	if((currentTime - serverNode.getLastRequestVote()) > (serverNode.getElectionTimeOut()*1000000)) { // Request Vote message

			    		serverNode.setLastRequestVote(currentTime);
			    		numberOfRequestVoteAcknowledgesReceived = 1; // Reset Value
			    		serverNode.setCurrentTerm(serverNode.getCurrentTerm() + 1);
			    		serverNode.setVotedFor(serverNode.getServerId()); // Say that voted for itself
			    		
			    		String stringToSend = serverNode.getMessage().requestVoteRPCBuilder(serverNode.getServerId(), 
			    							  serverNode.getCurrentTerm(), serverNode.getPrevLogTerm(serverNode.getLog(), 1), 
			    							  serverNode.getPrevLogIndex(serverNode.getLog(), 1));
			    							  serverNode.getUnicastCommunication().broadcastMessage(stringToSend, serverNode.getUnicastCommunication().localIP, serverNode.getPortList());
			    	}
			    	
			    	for(int k = 0; k < serverNode.getPortList().size(); k++) { // Waiting to receive all request vote acknowledge...
			    		receivedMessageNotParsed = serverNode.getUnicastCommunication().receiveUnicastMessage();
			    		if(receivedMessageNotParsed != "TIMEOUT") {
			    			receivedMessageSplit = serverNode.getMessage().messageSplit(receivedMessageNotParsed);
			    			int receivedMessageType = Integer.parseInt(receivedMessageSplit[0]);
			    			int receivedBooleanValue = Integer.parseInt(receivedMessageSplit[3]);
			    			
			    			if((receivedMessageType == 5) && (receivedBooleanValue == 1)) {
			    				numberOfRequestVoteAcknowledgesReceived++;
			    			}
			    			else if(receivedMessageType == 4) { // Received request Vote message
			    				int receivedCandidate = Integer.parseInt(receivedMessageSplit[1]); //candidate ID
			    				boolean acknowledgeBool = serverNode.getMessage().processVote(serverNode, receivedMessageSplit);

			    				if(acknowledgeBool == true) {
			    					serverNode.setCurrentRole(3); // Follower
			    				}
			    				String stringToSend = serverNode.getMessage().acknowledgeBuilder(5, serverNode.getServerId(), serverNode.getCurrentTerm(), acknowledgeBool);
			    				serverNode.getUnicastCommunication().sendUnicastMessage(stringToSend, serverNode.getUnicastCommunication().localIP, receivedCandidate);
			    			}
			    			
			    			else if(receivedMessageType == 1) { // Received Append Entries
			    				int numberOfRows =  receivedMessageSplit.length;
			    				int receivedLeaderId = Integer.parseInt(receivedMessageSplit[1]);
			    				boolean evaluateTermConsistancy = serverNode.getCurrentTerm() <= Integer.parseInt(receivedMessageSplit[3]);
			    				if (evaluateTermConsistancy) {
			    					int flag = 0;
			    					boolean acknowledge = serverNode.getMessage().processMessageFollower(serverNode, receivedMessageSplit);
									serverNode.updateStateMachineValue();
									if(numberOfRows > 6) {
										flag = 2; //append entry
									}
									else {
										flag = 3; //heartbeat
									}
									String acknowledgeMessage = serverNode.getMessage().acknowledgeBuilder(flag, serverNode.getServerId(), serverNode.getCurrentTerm(), acknowledge);	
									serverNode.getUnicastCommunication().sendUnicastMessage(acknowledgeMessage, serverNode.getUnicastCommunication().localIP, receivedLeaderId);
									serverNode.setCurrentRole(3);
			    				}
			    			}
			    		}
			    	}
			    	
			    	if((serverNode.getCurrentRole() == 2) || (serverNode.getCurrentRole() == 1)) {
					    if(numberOfRequestVoteAcknowledgesReceived > 0.5*(float)(serverNode.getPortList().size())) { // If received at least 50% of requestVote acknowledges, candidate changes to leader
					    	serverNode.setCurrentRole(1); // Candidate is now Leader
					    	numberOfRequestVoteAcknowledgesReceived = 1; // Reset Value
					    	serverNode.resetMatchIndex();
					    	serverNode.resetNextIndex(serverNode.getLog().size());
					    }
			    	}
			    	
					break;
				case 3: // Follower
					serverNode.clearMulticastBuffer();
					
					int followerElectionTimeOut = serverNode.getElectionTimeOut();	
			    	
			    	if(serverNode.getTimeOut() != followerElectionTimeOut) {
			    		serverNode.setTimeOut(followerElectionTimeOut);
			    		serverNode.getUnicastCommunication().setUnicastSocketTimeOut(serverNode.getTimeOut());
			    	}
			    	
					receivedMessageNotParsed = serverNode.getUnicastCommunication().receiveUnicastMessage();
					
					if(receivedMessageNotParsed != "TIMEOUT") {
						receivedMessageSplit = serverNode.getMessage().messageSplit(receivedMessageNotParsed);
						int receivedMessageType = Integer.parseInt(receivedMessageSplit[0]);
						int numberOfRows =  receivedMessageSplit.length;
						if(receivedMessageType == 1) { // received appendEntry
							int receivedLeaderId = Integer.parseInt(receivedMessageSplit[1]);
							boolean acknowledge = serverNode.getMessage().processMessageFollower(serverNode, receivedMessageSplit);
							int flag = 0;
							serverNode.updateStateMachineValue();
							if(numberOfRows > 6) {
								flag = 2; //append entry
							}
							else {
								flag = 3; //heartbeat
							}
							String acknowledgeMessage = serverNode.getMessage().acknowledgeBuilder(flag, serverNode.getServerId(), serverNode.getCurrentTerm(), acknowledge);	
							serverNode.getUnicastCommunication().sendUnicastMessage(acknowledgeMessage, serverNode.getUnicastCommunication().localIP, receivedLeaderId);
						}
						else if(receivedMessageType == 4) { // Received request Vote message
							int receivedCandidate = Integer.parseInt(receivedMessageSplit[1]);
		    				boolean acknowledgeBool = serverNode.getMessage().processVote(serverNode, receivedMessageSplit);
		    				
		    				String stringToSend = serverNode.getMessage().acknowledgeBuilder(5, serverNode.getServerId(), serverNode.getCurrentTerm(), acknowledgeBool);
		    				serverNode.getUnicastCommunication().sendUnicastMessage(stringToSend, serverNode.getUnicastCommunication().localIP, receivedCandidate);
		    			}
					}
					else { // TIMEOUT And need to start vote
						serverNode.setCurrentRole(2);
					}
					break;
				default:
					break;
			}
		}
	}
	
	public void threadSleep(int milis) {
		try {
			Thread.sleep(milis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void clearLog(List<Entry> log) { 
		for(int i = log.size() - 1; i >= 0; i--) {
	    	 log.remove(i);
	     }
	}
}

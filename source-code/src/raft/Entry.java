package raft;

public class Entry {

	int term; 
	String command;
	
	Entry(int receivedTerm, String receivedCommand) {
		setTerm(receivedTerm);
		setCommand(receivedCommand);
	}
	
	public void setTerm(int receivedTerm) {
		this.term = receivedTerm;
	}
	
	public int getTerm() {
		return this.term;
	}
	
	public void setCommand(String receivedCommand) {
		this.command = receivedCommand;
	}

	public String getCommand() {
		return this.command;
	}	
}

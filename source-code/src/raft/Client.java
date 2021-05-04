package raft;

import java.util.List;
import java.util.ArrayList;


public class Client {
	
	// // // COMMUNICATION PACKAGES // // // 
	
	UnicastCommunicationPackage unicastCommunication;
	MulticastCommunicationPackage multicastCommunication;
	Message message;
	
	// // // CLIENT VARIABLES // // //
	
	int clientId;
	int receivedValue;
	
	List<String> command = new ArrayList<String>();
	
	int localValue;
	
	int maxValue;
	int minValue;
	int timeOut;
	
	// // // CONSTRUCTOR // // // 

	public Client(int clientId, int unicastPort, int multicastPort, int maxValue, int minValue) {
		setMulticastCommunication(multicastPort);
		setTimeOut(15000);
		setUnicastCommunication(unicastPort, timeOut);
		setMessage();
		
		setClientId(clientId);
		setRangeValue(maxValue, minValue);
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
	
	public void setReceivedValue(int receivedValue) {
		this.receivedValue = receivedValue;
	}
	
	public int getReceivedValue() {
		return this.receivedValue;
	}
	
	public void setCommand () {
		String commandAux = getMessage().clientMessageBuilder(getClientId(), getMinValue(), getMaxValue()); 
		this.command.add(commandAux);
		String[] parsedCommand = getMessage().messageSplit(commandAux);
		changeLocalValue(parsedCommand[2], Integer.parseInt(parsedCommand[3]));
	}
	
	public String getCommand() {
		return this.command.get(this.command.size()-1);
	}
	
	public List<String> getCommandList() {
		return this.command;
	}
	
	public void changeLocalValue(String receivedCommand, int receivedValue) {
		if(receivedCommand.equals("add")) {
			this.localValue = this.localValue + receivedValue;
		}
		else {
			this.localValue = this.localValue - receivedValue;
		}
	}
	
	public int getLocalValue() {
		return this.localValue;
	}
	
	public void setRangeValue(int receivedMaxValue, int receivedMinValue) {
		this.maxValue = receivedMaxValue;
		this.minValue = receivedMinValue;
	}
	
	public int getMinValue() {
		return this.minValue;
	}
	
	public int getMaxValue() {
		return this.maxValue;
	}
	
	public void setTimeOut(int receivedTimeOut) {
		this.timeOut = receivedTimeOut;
	}
	
	public int getTimeOut() {
		return this.timeOut;
	}
}

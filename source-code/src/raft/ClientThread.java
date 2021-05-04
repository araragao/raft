package raft;

public class ClientThread extends Thread {
	
	Client clientNode;
	ClientGUI clientGUI;

	int unicastPort;
	int multicastPort;

	public ClientThread(int unicastPort, int multicastPort, int maxValue, int minValue) {
		clientNode = new Client(unicastPort, unicastPort, multicastPort, maxValue, minValue);
		this.unicastPort = unicastPort;
		this.multicastPort = multicastPort;
	}
	
	public void run() {
		// GUI startup
		clientGUI = new ClientGUI(clientNode.getClientId(), clientNode.getLocalValue(), clientNode.getReceivedValue(), clientNode.getCommandList());
		
		for(int i = 0 ; i < 7; i++) {
			clientNode.getMulticastCommunication().receiveMulticastMessage();
		}

		threadSleep(2000);
		
		String receivedMessageNotParsed;
		while(true) {	
			
			threadSleep(2000);
			// Send new command
			clientNode.setCommand();
			clientNode.getMulticastCommunication().sendMulticastMessage(clientNode.getCommand());
			
			// GUI update (in the end to update the Log Command right after sending the new command
			clientGUI.updateLogTable(clientNode.getCommandList());
			clientGUI.updateLabels(clientNode.getClientId(), clientNode.getLocalValue(), clientNode.getReceivedValue());
			
			receivedMessageNotParsed = clientNode.getUnicastCommunication().receiveUnicastMessage();
			if(receivedMessageNotParsed != "TIMEOUT") {
				clientNode.setReceivedValue(Integer.parseInt(receivedMessageNotParsed));
				
				clientGUI.updateLabels(clientNode.getClientId(), clientNode.getLocalValue(), clientNode.getReceivedValue());
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
}

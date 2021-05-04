package raft;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class UnicastCommunicationPackage {
	private static final LogManager logManager = LogManager.getLogManager();
	private static final Logger LOGGER = Logger.getLogger(UnicastCommunicationPackage.class.getName());
	
	int unicastPort;
	InetAddress localIP;
	DatagramSocket unicastSocket;
	
	public UnicastCommunicationPackage(int portNumber, int timeOut) {
		try {
	      logManager.readConfiguration(new FileInputStream("C:/Users/andre/Downloads/Telegram Desktop/raft/communicationpackageslogger.properties"));
	    } catch (IOException exception) {
	      LOGGER.log(Level.SEVERE, "Error in loading configuration", exception);
	    }
		
		setUnicastPort(portNumber);
		setLocalIP("localhost");
		setUnicastSocket();
		setUnicastSocketTimeOut(timeOut);
	}
	
	public void setUnicastPort(int portNumber) {
		this.unicastPort = portNumber;
	}
	
	public int getUnicastPort() {
		return this.unicastPort;
	}
	
	public void setLocalIP(String hostName) {
		try {
			this.localIP = InetAddress.getByName(hostName);
		} catch (UnknownHostException exception) {
			LOGGER.log(Level.SEVERE, "Error setting the IP", exception);
		}
	}
	
	public InetAddress getLocalIP() {
		return this.localIP;
	}
	
	public void setUnicastSocket() {
	    try {
	      this.unicastSocket = new DatagramSocket(unicastPort, localIP);
	    } catch (SocketException exception) {
	      LOGGER.log(Level.SEVERE, "Error setting up unicast socket", exception);
	    }
	  }
	
	public DatagramSocket getUnicastSocket() {
		return this.unicastSocket;
	}
	
	public void setUnicastSocketTimeOut(int newTimeOut) {
		try {
			this.unicastSocket.setSoTimeout(newTimeOut);
		} catch (SocketException exception) {
			LOGGER.log(Level.SEVERE, "Error setting up unicast socket timeout", exception);
		}
	}
	
	public void sendUnicastMessage(String stringMessage, InetAddress addressToSend, int portToSend) {
		byte[] byteMessage = stringMessage.getBytes();
		DatagramPacket packetToSend = new DatagramPacket(byteMessage, byteMessage.length, addressToSend, portToSend);
		try {
			this.unicastSocket.send(packetToSend);
		} catch (IOException exception) {
			LOGGER.log(Level.SEVERE, "Error sending unicast message", exception);
		}
	}
	
	public void broadcastMessage(String stringMessage, InetAddress addressToSend, List<Integer> portList) {
		byte[] byteMessage = stringMessage.getBytes();
		Iterator<Integer> iter = portList.iterator();
	     while (iter.hasNext()) {
	    	 Integer portValue = iter.next();
	    	 if(((double)(Math.random() * 10) > 0.0)) { // Random simulation of communication failure
	    		 DatagramPacket packetToSend = new DatagramPacket(byteMessage, byteMessage.length, addressToSend, portValue.intValue());
				 try {
					this.unicastSocket.send(packetToSend);
				} catch (IOException exception) {
					LOGGER.log(Level.SEVERE, "Error sending unicast broadcast message", exception);
				}
	    	 }
	     }
	}

	public String receiveUnicastMessage() {
		byte[] messageToReceive = new byte[65535];
		String returnMessage;
		DatagramPacket emptyPacket = new DatagramPacket(messageToReceive, messageToReceive.length);
		try {
			this.unicastSocket.receive(emptyPacket);
			returnMessage = bufferToString(messageToReceive).toString();
		} catch (IOException exception) {
			returnMessage = "TIMEOUT";
			LOGGER.log(Level.SEVERE, "Error receiving unicast message", exception);
		}
		return returnMessage;
	}
	
	// bufferToString is used to retrieve string data form a buffer
	public static StringBuilder bufferToString(byte[] a) { 
        if (a == null) 
            return null; 
        StringBuilder ret = new StringBuilder(); 
        int i = 0; 
        while (a[i] != 0) 
        { 
            ret.append((char) a[i]); 
            i++; 
        } 
        return ret; 
    }
}

package raft;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MulticastCommunicationPackage {
	private static final LogManager logManager = LogManager.getLogManager();
	private static final Logger LOGGER = Logger.getLogger(MulticastCommunicationPackage.class.getName());
	
	int multicastPort;
	InetAddress multicastGroup;
	MulticastSocket multicastSocket;
	
	public MulticastCommunicationPackage(int portNumber) {
		try {
		  logManager.readConfiguration(new FileInputStream("C:/Users/andre/Downloads/Telegram Desktop/raft/communicationpackageslogger.properties"));
	    } catch (IOException exception) {
	      LOGGER.log(Level.SEVERE, "Error in loading configuration", exception);
	    }
		setMulticastPort(portNumber);
		try {
			setMulticastGroup();
		} catch (UnknownHostException exception) {
			LOGGER.log(Level.SEVERE, "Error setting up multicast group", exception);
		}
		try {
			setMulticastSocket();
		} catch (Exception exception) {
			LOGGER.log(Level.SEVERE, "Error setting up multicast socket", exception);
		}
		try {
			joinMulticastGroup();
		} catch (IOException exception) {
			LOGGER.log(Level.SEVERE, "Error joining multicast group", exception);
		}
	}
	
	public void setMulticastPort(int portNumber)  {
		this.multicastPort = portNumber;
	}
	
	public int getMulticastPort() {
		return this.multicastPort;
	}
	
	public void setMulticastGroup() throws UnknownHostException {
		this.multicastGroup = InetAddress.getByName("228.5.5.5");
	}
	
	public InetAddress getMulticastGroup() {
		return this.multicastGroup;
	}
	
	public void setMulticastSocket() {
		try {
			this.multicastSocket = new MulticastSocket(this.multicastPort);
		} catch (IOException exception) {
			LOGGER.log(Level.SEVERE, "Error setting up multicast socket", exception);
		}
	}
	
	public void setMulticastSocketTimeOut(int receivedTimeOut) {
		try {
			this.multicastSocket.setSoTimeout(receivedTimeOut);
		} catch (SocketException exception) {
			LOGGER.log(Level.SEVERE, "Error setting up multicast socket timeout", exception);
		}
	}
	
	public MulticastSocket getMulticastSocket() {
		return this.multicastSocket;
	}
	
	@SuppressWarnings("deprecation")
	public void joinMulticastGroup() throws IOException {
		this.multicastSocket.joinGroup(this.multicastGroup);
	}
	
	public void sendMulticastMessage(String stringMessage) {
		byte[] byteMessage = stringMessage.getBytes();
		DatagramPacket packetToSend = new DatagramPacket(byteMessage, byteMessage.length, this.multicastGroup, this.multicastPort);
		try {
			multicastSocket.send(packetToSend);
		} catch (IOException exception) {
			LOGGER.log(Level.SEVERE, "Error sending multicast message", exception);
		}
	}
	
	public String receiveMulticastMessage() {
		byte[] messageToReceive = new byte[65535];
		String returnMessage;
		DatagramPacket emptyPacket = new DatagramPacket(messageToReceive, messageToReceive.length);
		try {
			multicastSocket.receive(emptyPacket);
			returnMessage = bufferToString(messageToReceive).toString();
		} catch (IOException exception) {
			LOGGER.log(Level.SEVERE, "Error receiving multicast message", exception);
			returnMessage = "TIMEOUT";
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

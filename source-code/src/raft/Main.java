package raft;

public class Main {
	
	public static void main(String[] args) {
		ServerThread thread1 = new ServerThread(44600, 44601);
		ServerThread thread2 = new ServerThread(44602, 44601);
		ServerThread thread3 = new ServerThread(44604, 44601);
		ServerThread thread4 = new ServerThread(44608, 44601);
		ServerThread thread5 = new ServerThread(44610, 44601);
		ServerThread thread6 = new ServerThread(44612, 44601);
		ServerThread thread7 = new ServerThread(44614, 44601);
		ClientThread thread8 = new ClientThread(44606, 44601, 5, 0);
		
		thread1.start();
		thread2.start();
		thread3.start();
		thread4.start();
		thread5.start();
		thread6.start();
		thread7.start();
		thread8.start();
	}
}

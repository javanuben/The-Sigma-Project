package cc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {

	public final static String VERSION = "dev0";
	public final static String PROGRAM_NAME = "The Sigma Project";
	public final static int STANDARD_PORT = 1337;
	public final static int STANDARD_FILE_PORT = 1340;
	public final static int STANDARD_FILE_SIZE_PORT = 1341;
	public final static int STANDARD_MESSAGE_PORT = 1338;
	public final static String FILE_LIST_PATH = "file_list.txt";
	public final static String OTHER_FILE_LIST_PATH = "temp//file_list.txt";
	public final static String TEMP_PATH = "temp";
	
	private String ip;
	private List<FileElement> files;
	private static BufferedReader reader;
	private List<Executor> executors;
	private List<Executor> executorsWaitingForUserInput;
	private String userInput;
	
	public static void main(String[] args) {
		InetAddress thisIp = null;
		try {
			thisIp = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			System.out.println("ERROR: " + e1);
		}
		System.out.println("Welcome to " + PROGRAM_NAME + "! This is version " + VERSION + ". Your IP-address is: " + thisIp.getHostAddress());
		System.out.println("Starting..");
		Main main = new Main();
		// Make an Executor listen for commands from other PC's
		Thread thread = new Thread(new Executor("listen " + STANDARD_MESSAGE_PORT, main));
		thread.start();
		reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// Do nothing
		}
		System.out.println("Please set your other computer's IP-address using the set_ip command, or wait for incomming connections.");
		try {
			String input;
			// Accept commands from user
			while(true) {
				input = reader.readLine().trim();
				if(input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
					System.out.println("Exiting..");
					break;
				}
				// Check if any Executors are waiting for input
				Executor waiter;
				if((waiter = main.getNextUserInputWaiter()) != null) {
					// Send input to executor
					main.setUserInput(input);
					Thread t;
					synchronized (t = waiter.getThread()) {
						t.notify();
					}
				} else {
					// Send input to a new executor
					Executor e = new Executor(input, main);
					main.addExecutor(e);
					Thread t = new Thread(e);
					e.setThread(t);
					t.start();
				}
			}
		} catch (IOException e) {
			System.out.println("Ooop! Error: " + e);
		}
		System.out.println("Bye!");
		System.exit(0);
	}
	
	public Main() {
		executors = new ArrayList<Executor>();
		executorsWaitingForUserInput = new ArrayList<Executor>();
	}
	
	public void setFiles(List<FileElement> files) {
		this.files = files;
	}
	
	public List<FileElement> getFiles() {
		return this.files;
	}
	
	public void waitForUserInput(Executor e) {
		executorsWaitingForUserInput.add(e);
		try {
			Thread t;
			synchronized (t = e.getThread()) {
				t.wait();
			}
		} catch (InterruptedException e1) {
			System.out.println("ERROR: " + e1);
		}
	}
	
	public Executor getNextUserInputWaiter() {
		if(!executorsWaitingForUserInput.isEmpty()) {
			return executorsWaitingForUserInput.remove(0);
		} else {
			return null;
		}
	}
	
	public void setUserInput(String input) {
		this.userInput = input;
	}
	
	public String getUserInput() {
		return userInput;
	}
	
	public void addExecutor(Executor e) {
		executors.add(e);
	}

	public void removeExecutor(Executor e) {
		executors.remove(e);
	}
	
	public void synchronize() {
		// Load file_list
		// Synchronize file_lists
		// Load file_log
		// Synchronize file_logs
		// ...
	}
	
	public String getIp() {
		return this.ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}
}

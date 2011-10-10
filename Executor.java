package cc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Executor implements Runnable {

	public static final int SKIP = 1;
	public static final int KEEP_BOTH = 2;
	public static final int OVERWRITE = 3;
	
	private String task;
	private Main main;
	private Thread me;
	
	public Executor(String task, Main main) {
		this.task = task;
		this.main = main;
	}
	
	@Override
	public void run() {
		// Replace #IP with this computer's IP address
		if(task.contains("#IP")) {
			InetAddress thisIp = null;
			try {
				thisIp = InetAddress.getLocalHost();
			} catch (UnknownHostException e1) {
				System.out.println("ERROR: " + e1);
			}
			task.replaceAll("#IP", thisIp.getHostAddress());
//			String temp[] = task.split("#IP");
//			if(temp.length == 1) {
//				task = temp[0] + thisIp.getHostAddress();
//			} else if(temp.length == 2) {
//				task = temp[0] + thisIp.getHostAddress() + temp[1];
//			}
		}
		System.out.println("Executor started with task: " + task);
		if(task.startsWith("send file_list")) {
			String[] temp = task.split(" ");
			if(temp[temp.length - 1].equalsIgnoreCase("false")) {
				sendFile_list(false);
			} else {
				sendFile_list(true);
			}
		} else if(task.equalsIgnoreCase("receive file_list")) {
			receiveFile_list();
		} else if(task.equalsIgnoreCase("merge file_list")) {
			mergeFile_lists();
		} else if(task.startsWith("send_command")) {
			String[] input = task.split(" ");
			String command = "";
			for (int i = 1; i < input.length; i++) {
				command += input[i] + " ";
			}
			sendText(command, main.getIp(), Main.STANDARD_MESSAGE_PORT);
		} else if(task.startsWith("listen")) {
			try {
				listen(Integer.parseInt(task.split(" ")[1]));
			} catch(NumberFormatException e) {
				System.out.println("A valid port must be followed by the listen command");
			}
		} else if(task.startsWith("set_ip")) {
			setIp(task.split(" ")[1]);
		} else if(task.equalsIgnoreCase("print ip")) {
			String ip = main.getIp();
			if(ip != null) {
				System.out.println("IP: " + ip);
			}
		} else if(task.equalsIgnoreCase("sync file_list") || task.equalsIgnoreCase("synchronize file_list")) {
			synchronizeFile_lists();
		} else if(task.equalsIgnoreCase("load files")) {
			loadFilesToSynchronize();
		} else if(task.equalsIgnoreCase("sync files") || task.equalsIgnoreCase("synchronize files")) {
			synchronizeFiles();
		} else if(task.startsWith("send file ")) {
			try {
				sendFile(Integer.parseInt(task.split(" ")[2].trim()));
			} catch(NumberFormatException e) {
				System.out.println("'send file' must be followed by a valid file id, separated with a space.");
			}
		}
		main.removeExecutor(this);
		System.out.println("Executor stopped.");
	}
	
	/**
	 * This method will send the file, corresponding to the given id, to the linked program.
	 *  
	 * @param id	The file id.
	 */
	private void sendFile(int id) {
		List<FileElement> files = loadFiles(Main.FILE_LIST_PATH);
		// Find the file corresponding to the received id.
		for (FileElement fe : files) {
			if(fe.getId() == id) {
				System.out.println("Sending " + fe.getName() + "..");
				FileSender fs = new FileSender(Main.STANDARD_FILE_SIZE_PORT, Main.STANDARD_FILE_PORT, main.getIp(), fe.getPath());
				fs.run();
				break;
			}
		}
	}
	
	
	/**
	 * This method will tell the linked program to send all the files in main.files, and attempt to receive them.
	 */
	private void synchronizeFiles() {
		System.out.println("Starting file synchronization.");
		List<FileElement> files = main.getFiles();
		for (FileElement fe : files) {
			// Tell the linked program to send the file
			sendText("send file " + fe.getId(), main.getIp(), Main.STANDARD_MESSAGE_PORT);
			// Receive the file
			FileReceiver fr = new FileReceiver(Main.STANDARD_FILE_SIZE_PORT, Main.STANDARD_FILE_PORT, fe.getPath());
			fr.run();
		}
		System.out.println("Done synchronizing files.");
	}
	
	/**
	 * This method will check what files on the file_list that needs to be updated (synchronized),
	 * and pass them to main's setFiles method.
	 */
	private void loadFilesToSynchronize() {
		List<FileElement> files = new ArrayList<FileElement>();
		// Load the files
		files = loadFiles(Main.FILE_LIST_PATH);
		// Remove files that hasn't been updated since last synchronize
		for (FileElement fe : files) {
			// If the file was synchronized after it was last modified on the other PC..
			if(fe.getLastSynchronized() > fe.getLastModified2()) {
				// we remove it from the list.
				files.remove(fe);
			}
		}
		main.setFiles(files);
	}
	
	/**
	 * Sets this executor's thread to the given thread.
	 * 
	 * @param t	The Thread.
	 */
	public void setThread(Thread t) {
		this.me = t;
	}
	
	/**
	 * @return	This executors thread, if available. Null if not.
	 */
	public Thread getThread() {
		return this.me;
	}
	
	/**
	 * This methods makes this executor request user input from main.
	 * 
	 * @return	The user input.
	 */
	private String requestUserInput() {
		main.waitForUserInput(this);
		String input = main.getUserInput(); 
		return input;
	}
	
	/**
	 * This methods set main's ip field to the given ip.
	 * @param ip	The IP-address
	 */
	private void setIp(String ip) {
		main.setIp(ip);
		System.out.println("IP set to: " + ip);
	}
	
	/**
	 * This method makes the executor listen to the given port, and starts a new executor with the received
	 * text as task.
	 * 
	 * @param port	The port the method should listen to.
	 */
	private void listen(int port) {
		ServerSocket servSock = null;
		Socket sock = null;
		try {
			while(true) {
				servSock = new ServerSocket(port);
				System.out.println("Listening on port " + port + "..");
				// Accept connections
				sock = servSock.accept();
				System.out.println("Connection accepted: " + sock);
				BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				// Read received text.
				String msg = in.readLine().trim();
				System.out.println("Command received: " + msg);
				// Start new executor with msg as it's task.
				Executor e = new Executor(msg, main);
				// Add the executor to main, so the program knows about the new executor.
				main.addExecutor(e);
				Thread t = new Thread(e);
				// Close connection
				servSock.close();
				sock.close();
				System.out.println("Connection closed");
				t.start();
				System.out.println("Executor started");
			}
		} catch (IOException e) {
			System.out.println("ListenError: " + e);
		} finally {
			try {
				servSock.close();
			} catch (Exception e) {
				// Do nothing
			}
			try {
				sock.close();
			} catch (Exception e) {
				// Do nothing
			}
		}
		System.out.println("Stopped listening to " + port + ".");
	}
	
	/**
	 * This method calls updateLastModiefied, and tells the other program to receive the file_list,
	 * before sending the file_list located at FILE_LIST_PATH
	 * 
	 * @param shouldSendReceiveCommand Determines if the method should send the 'receive file_list' command to the
	 * linked program.
	 */
	private void sendFile_list(boolean shouldSendReceiveCommand) {
		// Update file_list before sending.
		updateLastModified(Main.FILE_LIST_PATH);
		if(shouldSendReceiveCommand) {
			// Let the other program know that we're sending the file_list.
			System.out.println("Requesting file transfer..");
			Executor.sendText("receive file_list", main.getIp(), Main.STANDARD_MESSAGE_PORT);
		}
		// Start sending the file.
		System.out.println("Attempting file transfer..");
		FileSender fs = new FileSender(Main.STANDARD_FILE_SIZE_PORT, Main.STANDARD_FILE_PORT, main.getIp(), Main.FILE_LIST_PATH);
		fs.run();
		if(fs.ok()) {
			System.out.println("File transfer done.");
		} else {
			System.out.println("File transfer failed.");
		}
	}
	
	/**
	 * This method updates the lastModified attribute for every file entry in the file at file_listPath.
	 * 
	 * @param file_listPath	The file_list to update.
	 */
	public void updateLastModified(String file_listPath) {
		System.out.println("Updating file_list..");
		// Load the file entries at file_listPath.
		List<FileElement> files = loadFiles(file_listPath);
		// For every file entry in file_listPath..
		for (FileElement fe : files) {
			// Update the lastModified field.
			File f = new File(fe.getPath());
			if(f.exists()) {
				fe.setLastModified(f.lastModified());
			}
		}
		// Save the updated file.
		saveFiles(files, file_listPath);
		System.out.println("Update done.");
	}
	
	/**
	 * This method receives the file_list at the standard ports, and saves the file at OTHER_FILE_LIST_PATH.
	 * 
	 * @return True if the file was received successfully. False if not.
	 */
	private boolean receiveFile_list() {
		// Start receiving the file_list, and save it at OTHER_FILE_LIST_PATH
		System.out.println("Receiving file_list.txt..");
		FileReceiver fr = new FileReceiver(Main.STANDARD_FILE_SIZE_PORT, Main.STANDARD_FILE_PORT, Main.OTHER_FILE_LIST_PATH);
		fr.run();
		if(fr.ok()) {
			System.out.println("Received file_list.txt");
		} else {
			System.out.println("Failed to receive file_list.");
		}
		return fr.ok();
	}
	
	/**
	 * This method sends a text-based message to another program.
	 *  
	 * @param msg	The message that will be sent.
	 * @param ip	The IP the message will be sent to.
	 * @param port	The port that should be used for the message sending.
	 */
	public static void sendText(String msg, String ip, int port) {
		Socket sock = null;
		try {
			// Create a socket for sending the message.
			System.out.println("Connecting to " + ip + ':' + port + "..");
			sock = new Socket(ip, port);
			System.out.println("Connected.");
			System.out.println("Sending command: " + msg + ".");
			PrintWriter send = new PrintWriter(sock.getOutputStream());
			// Send the message.
			send.println(msg);
			send.flush();
			System.out.println("Message sent.");
			send.close();
			sock.close();
		} catch (IOException e) {
			System.out.println("Error: " + e);
		} catch (NumberFormatException e) {
			System.out.println("The port must be an integer!");
		} finally {
			try {
				sock.close();
			} catch (Exception e) {
				// Do nothing
			}
		}
		System.out.println("Connection closed.");
	}

	/**
	 * This method will read the file at the given path, and return the 
	 * lines in the file as a List.
	 * 
	 * @param path	The file to read.
	 * @return		A list containing the lines in the file.
	 */
	public static List<String> readFile(String path) {
		FileReader fr = null;
		BufferedReader br = null;
		// Create a new list to store the lines in.
		List<String> list = new ArrayList<String>();
		try {
			fr = new FileReader(path);
			br = new BufferedReader(fr);
			String s = null;
			// Read the lines..
			while((s = br.readLine()) != null) {
				// and add them to the list
				list.add(s);
			}
		} catch(IOException e) {
			// Do nothing
		} finally {
			try {
				fr.close();
			} catch(IOException e) {
				// Do nothing
			}
			try {
				br.close();
			} catch(IOException e) {
				// Do nothing
			}
		}
		// Return the list containing the lines.
		return list;
	}
	
	/**
	 * This method writes the given lines to the file at the given path.
	 * 
	 * @param lines	The lines to write.
	 * @param path	The file that should be written to.
	 */
	public static void writeFile(List<String> lines, String path) {
		FileWriter fr = null;
		BufferedWriter br = null;
		try {
			fr = new FileWriter(path);
			br = new BufferedWriter(fr);
			// For every line..
			for (String line : lines) {
				// write it to the file.
				br.write(line);
				br.newLine();
			}
		} catch (IOException e) {
			// Do nothing
		} finally {
			try {
				br.close();
			} catch (Exception e) {
				// Do nothing
			}
			try {
				fr.close();
			} catch (Exception e) {
				// Do nothing
			}
		}
	}
	
	/**
	 * This method will save the FileElements given as argument to the file at the given path.
	 * 
	 * @param files	The FileElements that should be saved.
	 * @param path	The path to where the FileElements should be saved.
	 */
	public static void saveFiles(List<FileElement> files, String path) {
		List<String> lines = new ArrayList<String>();
		for (FileElement fe : files) {
			lines.add(fe.toString());
		}
		writeFile(lines, path);
	}
	
	/**
	 * This method loads the files at the given file_list's path and returns them.
	 * Remember to call updateLastModified before using this method!
	 * 
	 * @param path	The file to read from.
	 * @return		A List containing the FileElements represented in the file.
	 */
	private List<FileElement> loadFiles(String path) {
//		// Update lastModified.
//		updateLastModified(path);
		// Read the file.
		List<String> input = readFile(path);
		List<FileElement> files = new ArrayList<FileElement>();
		for (String s : input) {
			// Skip empty lines
			if(s.equals("")) {
				continue;
			}
			// Split the line..
			String[] temp = s.split("<");
			// and create a new FileElement to represent it.
			files.add(new FileElement(Integer.parseInt(temp[0]), temp[1], Long.parseLong(temp[2]), Long.parseLong(temp[3]), Long.parseLong(temp[4])));
		}
		return files;
	}
	
	/**
	 * This method will merge the file_lists, and prompt the user (via requestPathsFromUser and reportConflictToUser)
	 * when a new file or conflict is discovered.
	 * It will also update the lastModified2 property.
	 * 
	 * Note: receiveFile_list or corresponding method should be called first. (This method uses the "other" file_list
	 * at Main.OTHER_FILE_LIST_PATH.)
	 */
	private void mergeFile_lists() {
		System.out.println("Note: receiveFile_list or corresponding method should be called first. (This method uses the 'other' file_list"
	  + "at " + Main.OTHER_FILE_LIST_PATH + ".) If you haven't done this, you might end up loosing some work.");
		// Load this programs file_list.
		List<FileElement> mine = loadFiles(Main.FILE_LIST_PATH);
		// Load the received file_list.
		List<FileElement> other = loadFiles(Main.OTHER_FILE_LIST_PATH);
		// Create a list to store new files in.
		List<FileElement> news = new ArrayList<FileElement>();
		// Create a list to store conflicts in.
		List<FileElement> updates = new ArrayList<FileElement>();
		// Find new files in other that should be synchronized
		// Find the files that have been updated on other after last synchronization
		for (FileElement ofe : other) {
			boolean exists = false;
			for (FileElement mfe : mine) {
				if(mfe.getId() == ofe.getId()) {
					exists = true;
					// Update the lastModified2 property
					mfe.setLastModified2(ofe.getLastModified());
					// If both this file_list's file and the other file_list's file was modified, the file is a conflict.
					if(ofe.getLastModified() > ofe.getLastSynchronized() && mfe.getLastModified() > mfe.getLastSynchronized()) {
						updates.add(ofe);
					}
				}
			}
			if(!exists) {
				news.add(ofe);
			}
		}
		// Request new paths from user for new files
		news = requestPathsFromUser(news);
		mine.addAll(news);
		System.out.println("New paths set.");
		// Handle conflicts
		for (FileElement conflict : updates) {
			int todo = reportConflictToUser(conflict);
			if(todo == SKIP) {
				// If the user wants to skip the file, we do nothing.
				continue;
			} else if(todo == KEEP_BOTH) {
				// If the user wants to keep both files, we save it with the same name, but followed by (conflict).
				conflict.setPath(conflict.getPath() + "(conflict)");
				// And we adds the conflict to this program's file_list.
				mine.add(conflict);
			} else if(todo == OVERWRITE) {
				// If the user wants to overwrite the old file, we simply add the new file (and hope that this works out!)
				mine.add(conflict);
			}
		}
		// Save the new synchronized file_list
		saveFiles(mine, Main.FILE_LIST_PATH);
		System.out.println("File_lists merged and saved!");
		for (FileElement fe : mine) {
			System.out.println(fe);
		}
	}

	/**
	 * This method prompts the user, via the command line, for paths to the new files.
	 * 
	 * @param news	The new files received.
	 * @return		The new files received, with the new paths given read from the user.
	 */
	private List<FileElement> requestPathsFromUser(List<FileElement> news) {
		if(!news.isEmpty()) {
			System.out.println("Trying to synchronize new files. Please enter the paths to save them to.");
			for (FileElement fe : news) {
				System.out.print(fe.getName() + ": ");
				fe.setPath(this.requestUserInput().trim() + "/" + fe.getName());
			}
		}
		return news;
	}
	
	/**
	 * This method will request the user to choose how the given FileElement that is a conflict should be handled:
	 * 1. Skipped.
	 * 2. Keep both.
	 * 3. Overwrite.
	 * 
	 * @param conflict	The FileElement that is a conflict.
	 * @return			An integer representing the user's choice, see comment above for what the different integers represents.
	 */
	private int reportConflictToUser(FileElement conflict) {
		// Print some information to the user.
		System.out.println("Conflict discovered. Please choose what to do with it.");
		System.out.println("Conflicted file: " + conflict.getName());
		System.out.println("1. Don't synchronize this file (skip).");
		System.out.println("2. Synchronize, but save new file with (conflict) appended to it's name (keep both).");
		System.out.println("3. Overwrite this system's file, I know what I'm doing (overwrite).");
		while(true) {
			// Wait for user input
			this.requestUserInput();
			try {
				int input = Integer.parseInt(main.getUserInput());
				if(input < 1 || input > 3) {
					throw new NumberFormatException("The integer input must be greater than or equal to 1 and less than or equal to 3");
				}
				return input;
			} catch(NumberFormatException e) {
				System.out.println("The input must be an integer with value greater than or equal to 1 and less than or equal to 3");
			}
		}
	}

	/**
	 * Synchronizes this program's file_list and the linked program's file_list by:
	 * 1. Updating this program's information about when the files where last modified.
	 * 2. Sends a "set_ip #IP" command to the linked program.
	 * 3. Sends a "send file_list false" command to the linked program.
	 * 4. Receives the file_list
	 * 5. Attempting to merge the file_lists.
	 */
	private void synchronizeFile_lists(){
		// Update the information about when the files where last modified.
		updateLastModified(Main.FILE_LIST_PATH);
		// Tell the linked program what IP-address we have
		sendText("set_ip #IP", main.getIp(), Main.STANDARD_MESSAGE_PORT);
		// Tell the linked program to send the file_list, but don't send the receive file_list command.
		sendText("send file_list false", main.getIp(), Main.STANDARD_MESSAGE_PORT);
		// Receive the file_list
		boolean ok = receiveFile_list();
		// If the file_lise was received successfully..
		if(ok) {
			// Merge the file_lists.
			mergeFile_lists();
			System.out.println("File_lists synchronized successfully.");
		} else {
			System.out.println("Could not synchronize file_lists!");
		}
	}
}

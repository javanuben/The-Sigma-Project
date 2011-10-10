package cc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class FileSender implements Runnable{

	private File file;
	private String ip;
	private int fileSizePort = 1336;
	private int filePort = 1337;
	private boolean ok = false;

	@Override
	public void run() {
		sendFileSize();
		if(ok) {
			sendFile();
		}
	}
	
	public FileSender(int fileSizePort, int filePort, String ip, String path) {
		this.fileSizePort = fileSizePort;
		this.filePort = filePort;
		this.file = new File(path);
		this.ip = ip;
	}
	
	public boolean ok() {
		return ok;
	}
	
	public boolean sendFileSize() {
		Socket sock = null;
		try {
			System.out.println("Connecting to " + ip + ':' + fileSizePort + "..");
			sock = new Socket(ip, fileSizePort);
			System.out.println("Connected.");
			System.out.println("Sending file size..");
			// Send file size
			PrintWriter send = new PrintWriter(sock.getOutputStream());
			send.println(file.length());
			send.flush();
			System.out.println("File size: " + file.length());
			System.out.println("File size sent.");
			send.close();
			sock.close();
			System.out.println("Connection closed.");
			ok = true;
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
		return true;
	}
	
	public boolean sendFile() {
		ok = false;
		Socket sock = null;
		try {
			System.out.println("Connecting to " + ip + ':' + filePort + "..");
			sock = new Socket(ip, filePort);
			System.out.println("Connected.");
			System.out.println("Sending file..");
			// Send file
			byte [] mybytearray  = new byte [(int)file.length()];
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
			bis.read(mybytearray, 0, mybytearray.length);
			OutputStream os = sock.getOutputStream();
			os.write(mybytearray,0,mybytearray.length);
			os.flush();
			System.out.println("File sent.");
			sock.close();
			System.out.println("Connection closed.");
			ok = true;
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
		return true;
	}
}

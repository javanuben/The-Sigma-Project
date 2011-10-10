package cc;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class FileReceiver implements Runnable {

	private int fileSizePort;
	private int filePort;
	private String path;
	private long fileSize;
	private boolean ok = false;

	public FileReceiver(int fileSizePort, int filePort, String path) {
		this.fileSizePort = fileSizePort;
		this.filePort = filePort;
		this.path = path;
	}
	
	public boolean ok() {
		return ok;
	}
	
	private boolean getFileSize() {
		ServerSocket servSock = null;
		Socket sock = null;
		try {
			System.out.println("Attempting to listen on port " + fileSizePort);
			servSock = new ServerSocket(fileSizePort);
			System.out.println("Listening on port " + fileSizePort + " for file size");
			sock = servSock.accept();
			System.out.println("Connection accepted: " + sock);

			// Receive file size
			InputStreamReader isz = new InputStreamReader(sock.getInputStream());
			BufferedReader rec = new BufferedReader(isz);
			fileSize = Integer.parseInt(rec.readLine().trim());
			System.out.println("File size: " + fileSize);
			rec.close();
			isz.close();

			sock.close();
			System.out.println("Connection closed");
			ok = true;
		} catch (IOException e) {
			System.out.println("Error: " + e);
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
		return true;
	}
	
	private boolean getFile() {
		ok = false;
		ServerSocket servSock = null;
		Socket sock = null;
		try {
			System.out.println("Attempting to listen on port " + filePort);
			servSock = new ServerSocket(filePort);
			System.out.println("Listening on port " + filePort + " for file");
			sock = servSock.accept();
			System.out.println("Connection accepted: " + sock);
			// Receive file
			System.out.println("Receiving file..");
			long start = System.currentTimeMillis();
			int bytesRead;
			int current = 0;
			byte [] mybytearray  = new byte [(int) fileSize];
			InputStream is = sock.getInputStream();
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
			bytesRead = is.read(mybytearray,0,mybytearray.length);
			current = bytesRead;

			do {
				bytesRead = is.read(mybytearray, current, (mybytearray.length-current));
				if(bytesRead >= 0) current += bytesRead;
			} while(bytesRead > 0);
			System.out.println("File received.\nSaving file..");
			bos.write(mybytearray, 0 , current);
			bos.flush();
			System.out.println("File saved.");
			long end = System.currentTimeMillis();
			System.out.println("Time: " + (end-start) + "ms.");
			bos.close();
			sock.close();
			System.out.println("Connection closed");
			ok = true;
		} catch (IOException e) {
			System.out.println("Error: " + e);
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
		return true;
	}
	
	@Override
	public void run() {
		getFileSize();
		if(ok) {
			getFile();
		}
	}
}

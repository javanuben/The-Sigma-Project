package cc;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
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
	
	private Socket listen(int port) throws IOException {
		Socket sock = null;
		ServerSocket servSock = new ServerSocket(port);
		System.out.println("Attempting to listen on port " + fileSizePort);
		System.out.println("Listening on port " + fileSizePort + " for file size");
		sock = servSock.accept();
		servSock.close();
		System.out.println("Connection accepted: " + sock);
		return sock;
	}
	
//	public static void close(Closeable c) {
//		try {
//			if(c instanceof Closeable) {
//				c.close();
//			} else {
//				System.out.println("Oops! Something weird happened when closing a socket! You might have to restart your computer to make the port accessable again.");
//			}
//		} catch (Exception e) {
//			// Do nothing
//		}
//		System.out.println("Connection closed");
//	}
	
	private void getFileSize() {
		ok = false;
		Socket sock = null;
		try {
			sock = listen(fileSizePort);
			// Receive file size
			InputStreamReader isz = new InputStreamReader(sock.getInputStream());
			BufferedReader rec = new BufferedReader(isz);
			fileSize = Integer.parseInt(rec.readLine().trim());
			System.out.println("File size: " + fileSize);
			rec.close();
			isz.close();
			ok = true;
		} catch (IOException e) {
			System.out.println("Error: " + e);
		} finally {
//			close(sock);
//			close(servSock);
			try {
				sock.close();
			} catch(Exception e) {}
		}
	}
	
	private void getFile() {
		ok = false;
		Socket sock = null;
		try {
			sock = listen(filePort);
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
			ok = true;
		} catch (IOException e) {
			System.out.println("Error: " + e);
		} finally {
//			close(sock);
//			close(servSock);
			try {
				sock.close();
			} catch(Exception e) {}
		}
	}
	
	@Override
	public void run() {
		getFileSize();
		if(ok) {
			getFile();
		}
	}
}

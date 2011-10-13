package cc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class FileSender implements Runnable{

	private File file;
	private String ip;
	private int fileSizePort;
	private int filePort;
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
	
	private void close(Socket sock) {
		try {
			sock.close();
		} catch (Exception e) {
			// Do nothing
		}
	}
	
	public void sendFileSize() {
		ok = Executor.sendText(Long.toString(file.length()), ip, fileSizePort);
	}
	
	public void sendFile() {
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
			close(sock);
		}
	}
}

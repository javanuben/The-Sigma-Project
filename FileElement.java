package cc;

public class FileElement {

	private int id;
	private String path;
	private long lastSynchronized;
	private long lastModified;	// On this computer
	private long lastModified2;	// On other computer
	
	public FileElement(int id, String path, long lastSynchronized, long lastModified, long lastModified2) {
		this.id = id;
		this.path = path;
		this.lastSynchronized = lastSynchronized;
		this.lastModified = lastModified;
		this.lastModified2 = lastModified2;
	}

	@Override
	public String toString() {
		return getId() + "<" + getPath() + "<" + getLastSynchronized() + "<" + getLastModified() + "<" + getLastModified2();
	}
	
	public String getName() {
		String path = getPath();
		int pos = 0;
		for(int i = path.length() - 1; i >= 0; i--) {
			if(path.charAt(i) == '/' || path.charAt(i) == 92) {
				pos = i + 1;
				break;
			}
		}
		String s = path.substring(pos); 
		return s;
	}
	
	public int getId() {
		return id;
	}

	public String getPath() {
		return path;
	}

	public long getLastSynchronized() {
		return lastSynchronized;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setLastSynchronized(long lastSynchronized) {
		this.lastSynchronized = lastSynchronized;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public long getLastModified2() {
		return lastModified2;
	}

	public void setLastModified2(long lastModified2) {
		this.lastModified2 = lastModified2;
	}
	
}

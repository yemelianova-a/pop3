import java.io.File;


public class FileInCache {
	
	private File myFile;
	private long fileSize;
	
	public FileInCache(File file, long l) {
		myFile = file;
		fileSize = l;
	}

	public File getFile() {
		return myFile;
	}
	
	public long getFileSize() {
		return fileSize;
	}
	
	public String getFileName(){
		return myFile.getName();
	}

	public void removeFile() {
		myFile.delete();
	}
}

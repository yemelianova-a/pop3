import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class Cache {
	
	private int cacheMaxSize;
	private int cacheSize;
	private List<String> queries;
	private List<FileInCache> files;
	
	public Cache(int size) {
		cacheMaxSize = size;
		queries = new LinkedList<String>();
		files = new LinkedList<FileInCache>();
	}

	public File getFile(String query){
		int index = queries.indexOf(query);
		if(index<0)
			return null;
		return files.get(index).getFile();
	}
	
	public int addFile(String query, File link){
		long size = link.length();
		if(size>cacheMaxSize){
			Logger.LogCacheAction(LogEvents.CACHE_FILE_IS_TOO_LARGE,link.getName(),size);
			return -2;
		}
		
		while(cacheSize+size>cacheMaxSize){
			remove();
		}
		
		queries.add(query);
		files.add(new FileInCache(link, size));
		cacheSize +=size;
		Logger.LogCacheAction(LogEvents.CACHE_FILE_ADDED,link.getName(),size);
		return 0;
	}
	
	private void remove(){
		queries.remove(0);
		FileInCache tmp = files.remove(0);
		tmp.removeFile();
		Logger.LogCacheAction(LogEvents.CACHE_FILE_REMOVED,tmp.getFileName(),tmp.getFileSize());
		cacheSize -=  tmp.getFileSize();
	}
	
	public boolean isExist(String query){
		return queries.contains(query);
	}
	
	public void changeSize(int newSize){
		while(newSize<cacheSize){
			remove();
		}
		cacheMaxSize = newSize;
	}
	
	public int getSize(){
		return cacheSize;
	}
}

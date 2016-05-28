import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;


public final class Logger {
	private static File logFile = new File(Config.LogFileName);

	private Logger() {

	}
	
	public static void clean() {
		try (PrintWriter writer = new PrintWriter(logFile)) {
			writer.print("");
    		writer.close();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}		
	}
	
	public static void LogCacheAction(LogEvents action, String name, long size){
		synchronized (logFile) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z");
			try (
				FileWriter fw = new FileWriter(logFile, true);
				PrintWriter out = new PrintWriter(new BufferedWriter(fw));
			) {
				StringBuilder logRecord = new StringBuilder("[").append(dateFormat.format(new Date())).append("]: ");
				switch (action){
				case CACHE_FILE_ADDED:
					logRecord.append("Cache event. Added file: ");
					break;
				case CACHE_FILE_REMOVED:
					logRecord.append("Cache event. Removed file: ");
					break;
				case CACHE_FILE_IS_TOO_LARGE:
					logRecord.append("Cache event. Cannot add to cache file (too large): ");
					break;
				default:
					break;
				}
				logRecord.append(name+" Size: "+String.valueOf(size)+"bytes");
				out.println(logRecord.toString());
			} catch (IOException e) {
				//do nothing
			}
		}
	}
	
	public static void LogConnection(String client, String direction, String text){
		synchronized (logFile) {
			try (
				FileWriter fw = new FileWriter(logFile, true);
				PrintWriter out = new PrintWriter(new BufferedWriter(fw));
			) {
				StringBuilder logRecord = new StringBuilder("[").append("Client "+client+" thread]: "+direction+text);
				out.println(logRecord.toString());
			} catch (IOException e) {
				//do nothing
			}
		}
	}
	
	public static void Log(String action) {
		synchronized (logFile) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z");
			try (
				FileWriter fw = new FileWriter(logFile, true);
				PrintWriter out = new PrintWriter(new BufferedWriter(fw));
			) {
				StringBuilder logRecord = new StringBuilder("[").append(dateFormat.format(new Date())).append("]: ");
				logRecord.append(action);
				out.println(logRecord.toString());
			} catch (IOException e) {
				//do nothing
			}
		}
	}
	
	public static void LogAction(LogEvents action, String clientAddress) {
		synchronized (logFile) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z");
			try (
				FileWriter fw = new FileWriter(logFile, true);
				PrintWriter out = new PrintWriter(new BufferedWriter(fw));
			) {
				StringBuilder logRecord = new StringBuilder("[").append(dateFormat.format(new Date())).append("]: ");
				switch (action) {
				case CANNOT_CREATE_SERVERSOCKET:
					logRecord.append("Unable to create serversocket.");
					break;
				case CLIENT_CONNECTED:
				case CLIENT_DISCONNECTED:
					logRecord.append("Client ")
							.append(clientAddress)
							.append(action == LogEvents.CLIENT_CONNECTED ? " connected" : " disconnected");
					break;
				case SERVER_STARTED:
				case SERVER_FINISHED:
					logRecord.append("Server ")
						.append(action == LogEvents.SERVER_STARTED ? "started"	: "finished");
					break;
				default:
					break;
				}

				out.println(logRecord.toString());
			} catch (IOException e) {
				//do nothing
			}
		}
	}
}

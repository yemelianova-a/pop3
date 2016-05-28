import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements Runnable {
	
	private int port;
	private int cacheSize;
	
	
	private ArrayList<ClientProcessor> clients = new ArrayList<ClientProcessor>();
	
	private volatile boolean running = true;
	public volatile boolean connectionOpened = true;
	
    public void terminate() {
    	running = false;      
    }
		
	
	public Server() {
		port = Config.DefaultBindPort;
		cacheSize = Config.DefaultCacheSize;
	}
		
	public Server(int bindPort, int cache) {
		port = bindPort;
		cacheSize = cache;
	}

	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(5000);
		} catch (IOException e) {
			if(Config.Debug) System.err.println("Unable to create server socket");
			Logger.LogAction(LogEvents.CANNOT_CREATE_SERVERSOCKET, null);
			return;
		}		
		if(Config.Debug) System.out.println("Server is started");
		Logger.LogAction(LogEvents.SERVER_STARTED, null);
		Logger.Log("[Parametres]: "+port+" port, "+cacheSize/(1024*1024)+" Mbytes of cache.");
		
		
		
		while (running) {
			try {
        	    Socket controlClient = serverSocket.accept();
        		ClientProcessor clientProcessor = new ClientProcessor(controlClient, cacheSize);
        		clients.add(clientProcessor);
        		clientProcessor.start();
			} catch (IOException e) {}
        }
        	
        for (ClientProcessor client : clients) {
        	client.shutDown();
    	}
        try {serverSocket.close();} catch (IOException e) {}
		
        connectionOpened = false;
		if(Config.Debug) System.out.println("Server is stopped");
		Logger.LogAction(LogEvents.SERVER_FINISHED, null);
	}
}

import java.net.*;
import java.util.Arrays;
import java.io.*;

import org.apache.commons.io.FileUtils;

public class ClientProcessor extends Thread {

    Socket skControlClient, skControlServer;
    BufferedReader brClient, brServer;
    PrintStream psClient, osServer;

    ServerSocket ssDataClient, ssDataServer;
    Socket skDataClient;

    //IP of interface facing client and server respectively
    private String clientIP;
    private String serverIP;

    private DataConnect dcData;
    private boolean userLoggedIn = false;
    private boolean connectionClosed = false;

    public Cache cache;
    
    //constants for debug output
    final static PrintStream pwDebug = System.out;
    final static String server2proxy = "S->P: ";
    final static String proxy2server = "S<-P: ";
    final static String proxy2client = "P->C: ";
    final static String client2proxy = "P<-C: ";
    final static String server2client = "S->C: ";
    final static String client2server = "S<-C: ";

    public static String CRLF = "\r\n";


    public ClientProcessor(Socket skControlClient, int cacheSize) {
        this.skControlClient = skControlClient;  
        cache = new Cache(cacheSize);
    }
    
    
    public void shutDown(){
    	connectionClosed = true;
    }
    
    private void proxyToClient(String toClient){
    	psClient.print(toClient + CRLF);
        psClient.flush();
        if (Config.Debug) pwDebug.println(proxy2client + toClient);
        Logger.LogConnection(clientIP, proxy2client, toClient);
    }
    
    private void proxyToServer(String toServer){
    	osServer.print(toServer + CRLF);
        osServer.flush();
        if (Config.Debug) pwDebug.println(proxy2server + toServer);
        Logger.LogConnection(clientIP, proxy2server, toServer);
    }

    public void run() {
        try {
            brClient = new BufferedReader(new InputStreamReader(skControlClient.getInputStream()));
            psClient = new PrintStream(skControlClient.getOutputStream());

            clientIP = skControlClient.getLocalAddress().getHostAddress().replace('.', ',');
		
            String username = null;
            String hostname = null;
            int serverport = 21;

            String toClient = MsgText.msgConnect;
            proxyToClient(toClient);
                
            //the username is read from the client
            String fromClient = brClient.readLine(); 
            if (Config.Debug) pwDebug.println(client2proxy + fromClient); 
            Logger.LogConnection(clientIP, client2proxy, fromClient);
                
            String userString = fromClient.substring(5);
                
            int a = userString.indexOf('@');
            int c = userString.lastIndexOf(':');
    
            if (a == -1) {
            	username = userString;
            	hostname = null;
            	serverport = Config.CommandsPortNumber;
            } else if (c == -1) {
            	username = userString.substring(0, a);
            	hostname = userString.substring(a + 1);
            } else {
            	username = userString.substring(0, a);
            	hostname = userString.substring(a + 1, c);
            	serverport = Integer.parseInt(userString.substring(c + 1));	
            }
            
                
            //if don't know which host to connect to
            if (hostname == null) {
                toClient = MsgText.msgIncorrectSyntax;
                proxyToClient(toClient);
                skControlClient.close();
                return;
            }

            InetAddress serverAddress = InetAddress.getByName(hostname);
            
            if (Config.Debug) pwDebug.println("Connecting to " + hostname + " on port " + serverport);
            Logger.Log("Connecting to " + hostname + " on port " + serverport);
                                       
            try {
                skControlServer = new Socket(serverAddress, serverport);
            } catch (ConnectException e) {
                toClient = MsgText.msgConnectionRefused;
                proxyToClient(toClient);
                return;
            }
        
            brServer = new BufferedReader(new InputStreamReader(skControlServer.getInputStream()));
            osServer = new PrintStream(skControlServer.getOutputStream(), true);
            serverIP = skControlServer.getLocalAddress().getHostAddress().replace('.' ,',');

 
            String fromServer = readResponseFromServer(false);
                
            if (fromServer.startsWith("421")) {
            	proxyToClient(fromServer);
            	return;
            }
            
            proxyToServer("USER " + username);

            readResponseFromServer(true);

            for (;;) {
                String s = brClient.readLine();
                if (s == null) {
                    break;
                }
                readCommandFromClient(s);
                //exit if connection closed (response == 221,421,530)
                if (connectionClosed) {
                    break;
                }
            }

        } catch (Exception e) {
            String toClient = MsgText.msgInternalError;
            if (Config.Debug) {
            	proxyToClient(toClient+" Stack trace: "+e.toString());
                e.printStackTrace(pwDebug);
            }else{
            	proxyToClient(toClient);
            }
            

        } finally {
            if (ssDataClient != null) {
                try {ssDataClient.close();} catch (IOException ioe) {}
            }
            if (ssDataServer != null ) {
                try {ssDataServer.close();} catch (IOException ioe) {}
            }
            if (skDataClient != null) try {skDataClient.close();} catch (IOException ioe) {}
            if (psClient != null) psClient.close();
            if (osServer != null) osServer.close();
            if (dcData != null) dcData.close();
        }
    }

    private void readCommandFromClient(String fromClient) throws IOException, InterruptedException {
        String cmd = fromClient.toUpperCase();
 
        if (!userLoggedIn && (cmd.startsWith("PASV") || cmd.startsWith("PORT"))) {
            //do not process PASV if user has not logged in yet.
            psClient.print(MsgText.msgNotLoggedIn + CRLF);
            psClient.flush();
            return;
        }

        if (cmd.startsWith("PASV")) {
            if (Config.Debug) pwDebug.println(client2proxy + fromClient);

            if (ssDataClient != null) {
                try { ssDataClient.close(); } catch (IOException ioe) {}
            }
            if (skDataClient != null) try { skDataClient.close(); } catch (IOException ioe) {}
            if (dcData != null) dcData.close();

            ssDataClient = new ServerSocket(0, 1, skControlClient.getLocalAddress());

            if (ssDataClient != null) {
                int port = ssDataClient.getLocalPort();

                String toClient = "227 Entering Passive Mode (" + clientIP + "," + 
                        (int) (port / 256) + "," + (port % 256) + ")";
                proxyToClient(toClient);
                    
                setupServerConnection(ssDataClient);

            } else {
                proxyToClient(MsgText.msgCannotAllocateLocalPort);
            }

        } else if (cmd.startsWith("PORT")) {
            int port = parsePort(fromClient);

            if (ssDataClient != null) {
                try {ssDataClient.close();} catch (IOException ioe) {}
                ssDataClient = null;
            }
            if (skDataClient != null) try {skDataClient.close();} catch (IOException ioe) {}
            if (dcData != null) dcData.close();
            

            if (Config.Debug) pwDebug.println(client2proxy + fromClient);

            try {
                skDataClient = new Socket(skControlClient.getInetAddress(), port);
                proxyToClient(MsgText.msgPortSuccess);
                setupServerConnection(skDataClient);

            } catch (IOException e) {
                proxyToClient(MsgText.msgPortFailed);
                return;
            }

             
        } else {
            if (Config.Debug) {
                pwDebug.print(client2server);
                if (cmd.startsWith("PASS")) {
                    pwDebug.println("PASS *******");
                }else {
                    pwDebug.println(fromClient);
                }
            }
            
            if(cmd.startsWith("RETR")){
            	//here's some problem with DataConnect threads, it need to be fixed. Instead of delay
            	dcData.sleep(50); 
            	if(!cache.isExist(fromClient)){
            		proxyToServer(fromClient);
            		dcData.request = fromClient;
            		dcData.isDownloading = true;
            		readResponseFromServer(true);
            		pwDebug.println("Receiving "+fromClient); 
            		Logger.Log("Sending file from ftp-server: "+fromClient);
            	}else{
            		dcData.isFromCache = true;
            		dcData.isDownloading = false;
            		dcData.request = fromClient;
            		pwDebug.println("Sending file to client from proxy: "+fromClient); 
            		Logger.Log("Sending file to client from proxy: "+fromClient);
            		osServer.print(fromClient + CRLF);
                    osServer.flush();
                    readResponseFromServer(true);
            	}
            }else{
            	proxyToServer(fromClient);
                readResponseFromServer(true);
            }
        }
    }

    private String readResponseFromServer(boolean forwardToClient) throws IOException {
        String fromServer = brServer.readLine();
        String firstLine = fromServer;
        
        int response = Integer.parseInt(fromServer.substring(0, 3));
        if (fromServer.charAt(3) == '-') {
            String multiLine = fromServer.substring(0, 3) + ' ';
            while (!fromServer.startsWith(multiLine)) {
                if (forwardToClient) {
                    psClient.print(fromServer + CRLF);
                    psClient.flush();
                }
                if (Config.Debug) pwDebug.println((forwardToClient ? server2client : server2proxy) + fromServer);
                Logger.LogConnection(clientIP, (forwardToClient ? server2client : server2proxy), fromServer);

                fromServer = brServer.readLine();
            }
        }
        
        //check for successful login
        if (response == 230) {
            userLoggedIn = true;
        } else if (response == 221 || response == 421 || response == 530) {
            if (userLoggedIn) {
        	connectionClosed = true;
            }
            userLoggedIn = false;
        }
        
        if (forwardToClient || response == 110) {
            psClient.print(fromServer + CRLF);
            psClient.flush();
        }
        if (Config.Debug) pwDebug.println((forwardToClient ? server2client : server2proxy) + fromServer);
        Logger.LogConnection(clientIP, (forwardToClient ? server2client : server2proxy), fromServer);
        
        if (response >= 100 && response <= 199) {
            firstLine = readResponseFromServer(true);
        }

        return firstLine;
    }

    private void setupServerConnection(Object s) throws IOException {
	    if (ssDataServer != null) {
		try {ssDataServer.close();} catch (IOException ioe) {}
	    }
	    
            ssDataServer = new ServerSocket(0, 1, skControlServer.getLocalAddress());
    
            if (ssDataServer != null) {
                int port = ssDataServer.getLocalPort();        
                proxyToServer("PORT " + serverIP + ',' + (int) (port / 256) + ',' + (port % 256));
        
                readResponseFromServer(false); 
    
                (dcData = new DataConnect(s, ssDataServer)).start();
                
            } else {
            	proxyToClient(MsgText.msgCannotAllocateLocalPort);
            }

    }


    public static int parsePort(String s) throws IOException {
        int port;
        try {
            int i = s.lastIndexOf('(');
            int j = s.lastIndexOf(')');
            if ((i != -1) && (j != -1) && (i < j)) {
                s = s.substring(i + 1, j);
            }
            
            i = s.lastIndexOf(',');
            j = s.lastIndexOf(',', i - 1);
            
            port = Integer.parseInt(s.substring(i + 1));
            port += 256 * Integer.parseInt(s.substring(j + 1, i));
        } catch (Exception e) {
            throw new IOException();
        }
        return port;
    }

    public class DataConnect extends Thread {
        private byte buffer[] = new byte[Config.DataBufferSize];
        private final Socket[] sockets = new Socket[2];
        private boolean isInitialized;
        private final Object[] o;
        private boolean validDataConnection;
        public boolean isDownloading = false;
        public String request;
        public boolean isFromCache = false;
        
        private Object mutex = new Object();

        //each argument may be either a Socket or a ServerSocket
        public DataConnect (Object o1, Object o2) {
            this.o = new Object[] {o1, o2};
        }

        public void run() {
        	        	
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            FileInputStream is = null;
        	File file = null;
            validDataConnection = false;
            
            try {
                // n = 0 - Thread Copy socket 0 to socket 1
                // n = 1 - Thread Copy socket 1 to socket 0
                int n = isInitialized ? 1 : 0;
                if (!isInitialized) {
                    for (int i = 0; i < 2; i++) {
                        if (o[i] instanceof ServerSocket) {
                            ServerSocket ss = (ServerSocket) o[i];
                            sockets[i] = ss.accept();
                            if (ss == ssDataServer  ||
                                ss == ssDataClient) {

                                ss.close();
                            }
                        } else {
                            sockets[i] = (Socket) o[i];
                        }
                        //check to see if DataConnection is from same IP address
                        //as the ControlConnection
                        if (skControlClient.getInetAddress().getHostAddress().
                            compareTo(sockets[i].getInetAddress().getHostAddress()) == 0) {
                        
                            validDataConnection = true;
                        }                        
                    }
                    //check to see if Data InetAddress == Control InetAddress, otherwise
                    //somebody else opened a connection!  Close all the connections
                    if (!validDataConnection) {
                        pwDebug.println("Invalid DataConnection - wrong client");
                        Logger.Log("Invalid DataConnection - wrong client");
                        throw new SocketException("Invalid DataConnection - wrong client");
                    }
                    
                    isInitialized = true;
                    
                    
                    //in some cases thread socket[0] -> socket[1] thread can
               	    //finish before socket[1] -> socket[0] has a chance to start,
               	    //so synchronize on a semaphore
                    synchronized(mutex) {
                        new Thread(this).start();
                        try {
                            mutex.wait();
                        } catch (InterruptedException e) {
                            //Never occur
                        }
                    }
                    
                }
                
                bis = new BufferedInputStream(sockets[n].getInputStream());
                bos = new BufferedOutputStream(sockets[1 - n].getOutputStream());
				
                synchronized(mutex) {
                   mutex.notify();
                }

                for (;;) {
                	if(isFromCache){
                		if(isDownloading) continue;
                		pwDebug.println("From cache");
                    	isDownloading = false;
                    	file = cache.getFile(request);
                    	is = new FileInputStream(file);
                    	for (int i; (i = is.read(buffer, 0, Config.DataBufferSize)) != -1; ) {
                            bos.write(buffer, 0, i);
                        }
                    	is.close();
                    	isFromCache = false;
                	}else if(isDownloading){
                		if(isFromCache) continue;
                		pwDebug.println("DL");
                		file = new File(Config.DefaultDir + "\\" +request.substring(4));
                		if(file.length()>0){
                			file.delete();
                		}
                		file = new File(Config.DefaultDir + "\\" +request.substring(4));
                		file.createNewFile();
                	
                		for (int i; (i = bis.read(buffer, 0, Config.DataBufferSize)) != -1; ) {
                			bos.write(buffer, 0, i);
                			FileUtils.writeByteArrayToFile(file, Arrays.copyOf(buffer, i), true); 
                		}
                    	if(cache.addFile(request, file)==-2){
                    		file.delete(); 
                    	}
                    }else{
                    	pwDebug.println("Oth");
                    	for (int i; (i = bis.read(buffer, 0, Config.DataBufferSize)) != -1; ) {
                            bos.write(buffer, 0, i);
                        }
                    }
                	isFromCache = false;
                    break;
                }
               bos.flush();
            } catch (SocketException e) {
                //socket closed
            } catch (IOException e) {
                if (Config.Debug) e.printStackTrace(pwDebug);
            }
            close();
        }
    
        public void close() {
            try { sockets[0].close(); } catch (Exception e) {}
            try { sockets[1].close(); } catch (Exception e) {}
        }
    }
}
         


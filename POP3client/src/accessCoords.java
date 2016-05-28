
public class accessCoords {

	private String host;
	private String userName;
	private String password;
	private int port;
	
	public accessCoords(String hostName, int portNumber, String uName, String userPass) {
		host = hostName;
		userName = uName;
		password = userPass;
		port = portNumber;
	}
	public String getHostName(){
		return host;
	}
	public String getUserName(){
		return userName;
	}
	public String getPassword(){
		return password;
	}
	public int getPort(){
		return port;
	}
}

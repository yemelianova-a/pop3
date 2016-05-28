import java.io.*;
import java.net.*;
import java.util.Vector;
import javax.net.ssl.SSLSocketFactory;

public class POP3client
{
  // MODE
  private final static boolean debug = true;

  // CONSTANTS
  private final String EOL = "";
  private final String S_OK = "+OK";
  private final String S_ERR = "-ERR";

  // Class fields
  private POPCommand command = null;
  private POPResponse response = null;
  private Socket socket = null;
  private BufferedReader ir = null;
  private PrintWriter ow = null;
  private PrintWriter log = null;
  public Vector<String> messages = null;
  public Vector<String> headers = null;
  private boolean logEnabled = false;
  
  public POP3client()
  {
    command = this.new POPCommand();
    response = this.new POPResponse();
    messages = new Vector<String>();
    headers = new Vector<String>();
  }

  public POP3client(String logName)
  {
    this();
    logEnabled = true;
    try
    {
       log = new  PrintWriter(new FileOutputStream(logName, true), true);
    }
    catch(IOException e)
    {
      // Problem with creating protocol file
      logEnabled = false;
    }
  }

  // If user forgot to disconnect
  protected void finalize() throws Throwable
  {
	  reset();
	  disconnect();
  }

  // Set connection
  public boolean connect(String hostName, int portNumber) throws POP3Exceptions
  {
    try
    {
      logText("Creating a socket...");
      SSLSocketFactory fac = (SSLSocketFactory) SSLSocketFactory.getDefault();
	  socket = fac.createSocket(hostName, portNumber);
      
      logText("Creating an input stream...");
      ir = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      logText("Creating an output stream...");
      ow = new PrintWriter(new DataOutputStream(socket.getOutputStream()), true);

      command.readCommandResponse();
      return response.isSucceed();
    }
    catch(UnknownHostException e)
    {
      logText("Host unknown");
      throw new POP3Exceptions(POP3Exceptions.HOST_UNKNOWN);
    }
    catch(IOException e)
    {
      logText("Creating an I/O channel failed");
      throw new POP3Exceptions(POP3Exceptions.SOCKET_ERROR);
    }
  }


  public void disconnect()
  {
    try
    {
    	
      logText("Disconnecting... ");
      // Send request for disconnection
      command.quit();
      if(ir != null)
      {
        ir.close();
        ir = null;
      }
      logText("[Input stream closed]");
      if(ow != null)
      {
        ow.close();
        ow = null;
      }
      logText("[Output stream closed]");
      if(socket != null)
      {
        socket.close();
        socket = null;
      }
      logText("[Socket closed]");
      logText("[Disconnected]");
      if(log != null)
      {
        log.close();
        log = null;
      }
    }
    catch(Exception e)
    { logText("Disconnection failed!");}
  }
  
  public void reset() throws POP3Exceptions
  {
	  logText("Cancelling current transaction...");
  	  command.rset();
  	  downloadHeaders();
  }

  public void logText(String text)
  {
    if(logEnabled) log.println(text);
    if(debug) System.out.println(text);
  }

  public void login(String name, String password) throws POP3Exceptions
  {
    logText("Sending the user name...");
    if( !command.user(name) )
        throw new POP3Exceptions(POP3Exceptions.BAD_NAME);
    logText("Sending the password...");
    if(password != null)
      if( !command.pass(password) )
        throw new POP3Exceptions(POP3Exceptions.BAD_PASSWORD);
  }
  
  //Getting mailbox state
  public String getStat() throws POP3Exceptions
  {
	  String statResult = command.stat();
	  String tmp[] = statResult.split(" ");
	  int countOfMessages = Integer.parseInt(tmp[1]);
	  int sizeOfAllMessages = Integer.parseInt(tmp[2]);
	  return "Messages in mailbox: "+countOfMessages+" Total length: "+sizeOfAllMessages+" bytes";
  }

  /**
   * Downloading all headers, saves in into headers array
   * */
  public void downloadHeaders() throws POP3Exceptions
  {
	  headers.clear();
	  int tmpCnt=0;
	  int i;
	  for(i=1; tmpCnt<4; i++)
	  {
		  if(!command.checkList(i))
			  tmpCnt++;
		  else if(command.top(i))
		  {
			  int offset = response.buff.indexOf(EOL);
			  headers.addElement(response.buff.substring(offset + EOL.length())); //cut off status
			  logText("Received header:\n"+response.buff.substring(offset + EOL.length())); 
		  }
		  else throw new POP3Exceptions(POP3Exceptions.RETR_ERROR);
    }
  }
  
  /**
   * Take (from headers array) and (trying to) parse header. 
   * Before calling it you should run downloadHeaders() method 
   * @return String in format: From: (e-mail address) Subject: (subject)
   * @param number - number of message to get header (number>0)
   * */
  public String getHeader(int number)
  {
	  if(headers.size()<1 || headers.size()<number)
	  {
		  logText("Wrong number of letter to download: "+number);
		  return "Error";
	  }
	  String tmp = (String)headers.elementAt(number-1);
	  tmp = tmp.substring(tmp.indexOf("From: ")+1);
	  tmp = tmp.substring(tmp.indexOf("<")+1);
	  String result = "From: "+tmp.substring(0,tmp.indexOf(">"));
	  tmp = tmp.substring(tmp.indexOf("Subject: ")+9);
	  tmp = tmp.substring(0,tmp.indexOf("To: "));
	  return result+" Subject: "+tmp;
	  
  }

  /**
   * Download from server and (trying to) parse header. 
   * Should show html-version (with tags, yep =_=")
   * I swear, it works with current mail.ru and gmail.com versions of letters
   * @return String in format: From: (e-mail address) Subject: (subject)
   * @param number - number of message to get header (number>0)
   * */
  public String getMessage(int number) throws POP3Exceptions
  {
	  String tmp="";
	  if(command.retr(number)){
		  int offset = response.buff.indexOf(EOL);
    	  tmp = response.buff.substring(offset + EOL.length());
    	  tmp = tmp.substring(tmp.indexOf("Content-Type: text/html; ")+25);
    	  tmp = tmp.substring(tmp.indexOf("<"));
    	  tmp = tmp.substring(0, tmp.lastIndexOf("--"));
    	  tmp = tmp.substring(0, tmp.lastIndexOf("--"));
    		       
        messages.addElement(tmp);
        logText(tmp);   
	  }
	  return tmp;
  }

  public boolean deleteMessage(int number) throws POP3Exceptions
  {
    if(command.dele(number))
    {
    	downloadHeaders();
    	return true;
    }
    return false;
  }


  class POPResponse
  {
    private String buff = "";

    /*
     * True if last command executed successful
     * False if not
     * Throws exception if server return smth strange
     * */
    public boolean isSucceed() throws POP3Exceptions
    {
      boolean result = true;

      if(!buff.startsWith(S_OK))
      {
        if(!buff.startsWith(S_ERR))
        {
          throw new POP3Exceptions(POP3Exceptions.BAD_RESPONSE);
        }
        result = false;
      }
      return result;
    }

    //Save data in temporary buffer
    protected void setBuff(String s)
    {
      buff = s;
    }

    protected String getBuff()
    {
      return buff;
    }

    protected String cutOffStatus()
    {
      int offset = buff.indexOf(' ');
      if(offset != -1)
      {
        String tmpStr = buff.substring(offset);
        return tmpStr.trim();
      }
      return null;
    }

    public String getServerComment()
    {
      String tmpStr = null;

      tmpStr = cutOffStatus();
      int offset = tmpStr.indexOf(EOL);
      if(offset != -1)
        return tmpStr.substring(0, offset);
      return null;
    }
  }


  class POPCommand
  {
    private void sendCommand(String command) throws POP3Exceptions
    {
      logText("Sending command... ");
      ow.println(command);
    }

    private void readCommandResponse() throws POP3Exceptions
    {
      logText("Reading response...");
      StringBuffer tmpBuff = new StringBuffer();

      try { tmpBuff.append(ir.readLine()); }
      catch(IOException e)
      {
        throw new POP3Exceptions(POP3Exceptions.IO_ERROR);
      }
      response.setBuff(tmpBuff.toString());
    }

    private void readMessage() throws POP3Exceptions
    {
      logText("Reading message...");
      String tmpStr = new String("");
      StringBuffer tmpBuff = new StringBuffer();

      try
      {
    	  //Read strings until terminator is found
    	  while(!(tmpStr = ir.readLine()).equals("."))
          tmpBuff.append(tmpStr + "");
      }
      catch(IOException e)
      {
        throw new POP3Exceptions(POP3Exceptions.IO_ERROR);
      }
      tmpStr = tmpBuff.toString();
      response.setBuff(tmpStr);
    }

    // Do typical command
    private boolean transactCommand(String command) throws POP3Exceptions
    {
      sendCommand(command);
      readCommandResponse();
      return response.isSucceed();
    }
    

    /*
     * MAIN COMMANDS OF POP3 PROTOCOL
     * */
    
    // Sends user information to server
    public boolean user(String name) throws POP3Exceptions
    {
      return transactCommand("USER " + name);
    }

    // Sends password
    public boolean pass(String password) throws POP3Exceptions
    {
      return transactCommand("PASS " + password);
    }
    
    // Getting mailbox state
    public String stat() throws POP3Exceptions
    {
    	sendCommand("STAT");
        readCommandResponse();
        return response.buff;
    }
    
    // Get list
    public String list() throws POP3Exceptions
    {
    	sendCommand("LIST");
        readCommandResponse();
        return response.buff;
    }
    
    // Read message
    public boolean retr(int number) throws POP3Exceptions
    {
      if(number != 0)
      {
        sendCommand("RETR " + Integer.toString(number));
        readMessage();
        return response.isSucceed();
      }
      else return false;
    }
    
    // Select message to delete
    public boolean dele(int number) throws POP3Exceptions
    {
      if(number != 0)
        return transactCommand("DELE " + Integer.toString(number));
      else return false;
    }
    
	// Read few lines of message
    public boolean top(int number) throws POP3Exceptions
    {
      if(number != 0)
      {
        sendCommand("TOP " + Integer.toString(number)+" 2");
        readMessage();
        return response.isSucceed();
      }
      else return false;
    }
    
    // Read message identifier
    public boolean uidl(int number) throws POP3Exceptions
    {
      if(number != 0)
      {
    	return transactCommand("UIDL " + Integer.toString(number));
      }
      else return false;
    }
    
    // Cancel transaction
    public boolean rset() throws POP3Exceptions
    {
      return transactCommand("RSET");
    }

    // Delete all selected messages and quit
    public boolean quit() throws POP3Exceptions
    {
      return transactCommand("QUIT");
    }

    // Check
    public boolean checkList(int number) throws POP3Exceptions
    {
      if(number != 0)
        return transactCommand("LIST " + Integer.toString(number));
      else return false;
    }
       
    
  }
}
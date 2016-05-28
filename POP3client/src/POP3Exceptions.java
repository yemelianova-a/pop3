public class POP3Exceptions extends Exception {
	 
	
	// Have no idea what is it, Eclipse suggested it
	private static final long serialVersionUID = 6942766943501579458L;
	// Types of situations
	  public static final int NOT_AVAILABLE = 0;
	  public static final int BAD_RESPONSE = 1;
	  public static final int BAD_NAME = 2;
	  public static final int BAD_PASSWORD = 3;
	  public static final int SOCKET_ERROR = 4;
	  public static final int HOST_UNKNOWN = 5;
	  public static final int IO_ERROR = 6;
	  public static final int RETR_ERROR = 7;
	  
	  //Reason of exception
	  private static int why = NOT_AVAILABLE;

	  public POP3Exceptions()
	  {
	    super();
	  }

	  public POP3Exceptions(String message)
	  {
	    super(message);
	  }


	  public POP3Exceptions(int reason)
	  {
	    super(POP3Exceptions.assignMessage(reason));
	  }

	  //Choose string to return err message
	  private static String assignMessage(int reason)
	  {
	    why = reason;
	    
	    return returnText();
	  }
	  
	  public static String returnText()
	  {
		  int reason = why;
		  switch(reason)
		    {
		      case BAD_RESPONSE:
		        return new String("Bad response from the mail server");
		      case SOCKET_ERROR:
		        return new String("Socket error occurred");
		      case BAD_NAME:
		        return new String("User with this name not found");
		      case BAD_PASSWORD:
		        return new String("Invalid usename or password");
		      case HOST_UNKNOWN:
		        return new String("Hostname is unknown");
		      case IO_ERROR:
		        return new String("I/O error");
		      case RETR_ERROR:
		        return new String("Fatal error occured during message reading");
		      default:
		        return new String("Unknown failure. Sorry. We really tried");
		    }
	  }

	  public int why()
	  {
	    return why;
	  }
}

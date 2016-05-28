import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class ProxySettings {

	protected Shell shlProxysettings;
	private Text port;
	private Text cacheSize;
	private Server server = null;
	private Thread serverThread;
	
	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ProxySettings window = new ProxySettings();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	ProxySettings(){
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shlProxysettings.open();
		shlProxysettings.layout();
		while (!shlProxysettings.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	private int strToNum(String str){
		if(str=="")
			return -1;
		int result;
		try{
			result = Integer.parseInt(str);
		}
		catch(NumberFormatException e1)
		{
			result=-1;
		}
		if(result>65533){
			result = -1;
		}
		return result;
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shlProxysettings = new Shell();
		shlProxysettings.setSize(214, 157);
		shlProxysettings.setText("ProxySettings");
		
		port = new Text(shlProxysettings, SWT.BORDER);
		
		port.setText(String.valueOf(Config.DefaultBindPort));
		port.setBounds(76, 47, 76, 21);
		
		cacheSize = new Text(shlProxysettings, SWT.BORDER);
		cacheSize.setText(String.valueOf(Config.DefaultCacheSize/(1024*1024)));
		cacheSize.setBounds(76, 82, 76, 21);
		
		Label lblNewLabel = new Label(shlProxysettings, SWT.NONE);
		lblNewLabel.setBounds(10, 50, 55, 15);
		lblNewLabel.setText("Port");
		
		Label lblNewLabel_1 = new Label(shlProxysettings, SWT.NONE);
		lblNewLabel_1.setBounds(10, 85, 55, 15);
		lblNewLabel_1.setText("Cache size");
		
		Label letLabel = new Label(shlProxysettings, SWT.NONE);
		letLabel.setBounds(158, 85, 27, 15);
		letLabel.setText("Mb");
		
		Button startupButton = new Button(shlProxysettings, SWT.NONE);
		Button shutdownButton = new Button(shlProxysettings, SWT.NONE);
		shutdownButton.setVisible(false);
		
		startupButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				if (server == null) {
					port.setEditable(false);
					cacheSize.setEditable(false);
					startupButton.setVisible(false);
					shutdownButton.setVisible(true);
					
					int portNum = strToNum(port.getText());
					int cache = strToNum(cacheSize.getText());
					if(portNum<=0 || cache<=0)
						server = new Server();
					else
						server = new Server(portNum, cache*1024*1024);
					serverThread = new Thread(server);
					serverThread.start();			
				}
			}
		});
		startupButton.setBounds(10, 10, 75, 25);
		startupButton.setText("Start proxy");
		
		
		shutdownButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shutdownButton.setVisible(false);
				if (server != null) {
					server.terminate();
				}
				while(server.connectionOpened);
				server = null;
				port.setEditable(true);
				cacheSize.setEditable(true);
				startupButton.setVisible(true);
			}
		});
		shutdownButton.setBounds(91, 10, 75, 25);
		shutdownButton.setText("Stop proxy");

	}
}

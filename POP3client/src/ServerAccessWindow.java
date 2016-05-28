import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;


public class ServerAccessWindow extends Dialog {

	protected accessCoords result;
	protected Shell shell;
	private Text ipField;
	private Text domenNameField;
	private Text userNameField;
	private Text passwordField;
	private Text PortField;


	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ServerAccessWindow(Shell parent, int style) {
		super(parent, style);
		setText("SWT Dialog");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), getStyle());
		shell.setSize(447, 331);
		
		//Labels that won't be changed.
		
		Label lblIpAddr = new Label(shell, SWT.NONE);
		lblIpAddr.setBounds(77, 53, 47, 15);
		lblIpAddr.setText("IP-\u0430\u0434\u0440\u0435\u0441");
		
		Label lblDName = new Label(shell, SWT.NONE);
		lblDName.setBounds(38, 80, 85, 15);
		lblDName.setText("\u0414\u043E\u043C\u0435\u043D\u043D\u043E\u0435 \u0438\u043C\u044F");
		
		Label lblUName = new Label(shell, SWT.NONE);
		lblUName.setBounds(22, 176, 102, 15);
		lblUName.setText("\u0418\u043C\u044F \u043F\u043E\u043B\u044C\u0437\u043E\u0432\u0430\u0442\u0435\u043B\u044F");
		
		Label lblPass = new Label(shell, SWT.NONE);
		lblPass.setBounds(77, 224, 47, 15);
		lblPass.setText("\u041F\u0430\u0440\u043E\u043B\u044C");

		Label lblPort = new Label(shell, SWT.NONE);
		lblPort.setBounds(50, 128, 74, 15);
		lblPort.setText("\u041D\u043E\u043C\u0435\u0440 \u043F\u043E\u0440\u0442\u0430");
		
		//Labels to show errors
		Color myRed = SWTResourceManager.getColor(220,20,60);
		Label lblErrHost = new Label(shell, SWT.NONE);
		lblErrHost.setForeground(myRed);
		lblErrHost.setBounds(141, 29, 247, 15);
		
		Label lblErrUName = new Label(shell, SWT.NONE);
		lblErrUName.setForeground(myRed);
		lblErrUName.setBounds(141, 152, 247, 15);
		
		Label lblErrPass = new Label(shell, SWT.NONE);
		lblErrPass.setForeground(myRed);
		lblErrPass.setBounds(141, 200, 247, 15);

		Label lblErrPort = new Label(shell, SWT.NONE);
		lblErrPort.setForeground(myRed);
		lblErrPort.setBounds(141, 104, 247, 15);
		
		//Text fields
		
		ipField = new Text(shell, SWT.BORDER);
		ipField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				lblErrHost.setText("");
			}
		});
		ipField.setBounds(141, 50, 247, 21);
		
		domenNameField = new Text(shell, SWT.BORDER);
		domenNameField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				lblErrHost.setText("");
			}
		});
		domenNameField.setBounds(141, 77, 247, 21);
		
		PortField = new Text(shell, SWT.BORDER);
		PortField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				lblErrPort.setText("");
			}
		});
		PortField.setBounds(141, 125, 247, 21);
		
		userNameField = new Text(shell, SWT.BORDER);
		userNameField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				lblErrUName.setText("");
			}
		});
		userNameField.setBounds(141, 173, 247, 21);
		
		passwordField = new Text(shell, SWT.BORDER | SWT.PASSWORD);
		passwordField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				lblErrPass.setText("");
			}
		});
		passwordField.setBounds(141, 221, 247, 21);
		
		Button button = new Button(shell, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String hostIp = ipField.getText().toString();
				String hostName = domenNameField.getText();
				int port = 0;
				String userName = userNameField.getText();
				String userPass = passwordField.getText();
				try{port = Integer.parseInt(PortField.getText());}
				catch(NumberFormatException e1)
				{
					lblErrPort.setText("Please, enter correct port number");
					port=0;
				}
				if(hostIp=="" & hostName==""){
					lblErrHost.setText("Please, enter IP or domen name");
				}else				
				if(userName==""){
					lblErrUName.setText("Please, enter username");
				}else
				if(userPass==""){
					lblErrPass.setText("Please, enter password");
				}else if(port==0){}
				else{
					if(hostName=="")
						result = new accessCoords(hostIp, port, userName, userPass);
					else
						result = new accessCoords(hostName, port, userName, userPass);
				
					//closing the window
					shell.dispose();
				}
			}
		});
		button.setBounds(294, 248, 94, 25);
		button.setText("\u041F\u043E\u0434\u043A\u043B\u044E\u0447\u0438\u0442\u044C\u0441\u044F");
		
	}

}

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.custom.StyledText;


public class POP3GUI {

	protected Shell shell;
	private POP3client client;
	private int selectedMsg=0;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			POP3GUI window = new POP3GUI();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	
	protected void createContents() {
		
		shell = new Shell();
		shell.setSize(675, 543);
		shell.setText("\u041F\u043E\u0447\u0442\u043E\u0432\u044B\u0439 \u043A\u043B\u0438\u0435\u043D\u0442");
		
		//LABELS
		Label lblState = new Label(shell, SWT.NONE);
		lblState.setBounds(10, 41, 639, 15);
		Label lblError = new Label(shell, SWT.NONE);
		lblError.setForeground(SWTResourceManager.getColor(220, 20, 60));
		lblError.setBounds(10, 41, 639, 15);
		lblError.setText("");
		
		//BUTTONS
		Button connectButton = new Button(shell, SWT.NONE);
		Button cancelButton = new Button(shell, SWT.NONE);
		Button deleteButton = new Button(shell, SWT.NONE);
		Button downloadButton = new Button(shell, SWT.NONE);
		Button returnButton = new Button(shell, SWT.NONE);
		Button updButton = new Button(shell, SWT.NONE);
		Button disconnectButton = new Button(shell, SWT.NONE);
		//Buttons properties
		cancelButton.setBounds(272, 10, 60, 25);
		cancelButton.setText("\u0421\u0431\u0440\u043E\u0441");
		connectButton.setBounds(10, 10, 94, 25);
		connectButton.setText("\u041F\u043E\u0434\u043A\u043B\u044E\u0447\u0438\u0442\u044C\u0441\u044F");
		deleteButton.setBounds(338, 10, 70, 25);
		deleteButton.setText("\u0423\u0434\u0430\u043B\u0438\u0442\u044C");
		disconnectButton.setBounds(576, 10, 60, 25);
		disconnectButton.setText("\u0412\u044B\u0439\u0442\u0438");
		downloadButton.setBounds(414, 10, 75, 25);
		downloadButton.setText("\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C");
		returnButton.setBounds(414, 10, 75, 25);
		returnButton.setText("\u0412\u0435\u0440\u043D\u0443\u0442\u044C\u0441\u044F");
		updButton.setBounds(495, 10, 75, 25);
		updButton.setText("\u041E\u0431\u043D\u043E\u0432\u0438\u0442\u044C");
				
		//LIST OF MESSAGES
		List list = new List(shell, SWT.BORDER);
		list.setBounds(10, 63, 639, 432);
		
		//BLOCK TO SHOW MESSAGE
		StyledText styledText = new StyledText(shell, SWT.BORDER);
		styledText.setAlwaysShowScrollBars(false);
		styledText.setVisible(false);
		styledText.setWordWrap(true);
		styledText.setBounds(10, 63, 639, 432);
		
		//DEFAULT VISIBLITY OF ELEMENTS
		cancelButton.setVisible(false);
		updButton.setVisible(false);
		downloadButton.setVisible(false);
		returnButton.setVisible(false);
		deleteButton.setVisible(false);
		list.setVisible(false);
		disconnectButton.setVisible(false);
		
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					client.reset();
				} catch (POP3Exceptions e1) {
					lblState.setVisible(false);
					lblError.setVisible(true);
					lblError.setText("[Error]: "+e1.returnText());
					return;
				}
			}
		});
		
		updButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				lblError.setVisible(false);
				lblError.setVisible(false);
				try {
					lblState.setText(client.getStat());
					lblState.setVisible(true);
					client.downloadHeaders();
					
					list.removeAll();
					for(int i = 1; i<=client.headers.size(); i++){
						  list.add(client.getHeader(i));
					  }
				} catch (POP3Exceptions e1) {
					lblError.setVisible(true);
					lblError.setText("[Error]: "+e1.returnText());
					return;
				}
				
			}
		});
		
		deleteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedMsg = list.getSelectionIndex()+1;
				if(selectedMsg>0){
					try {
						client.deleteMessage(selectedMsg);
						lblState.setText(client.getStat());
						lblState.setVisible(true);
						client.downloadHeaders();
						list.removeAll();
						for(int i = 1; i<=client.headers.size(); i++){
							  list.add(client.getHeader(i));
						}
					} catch (POP3Exceptions e1) {
						lblState.setVisible(false);
						lblError.setVisible(true);
						lblError.setText("[Error]: "+e1.returnText());
						return;
					}
				}
			}
		});
		
		disconnectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				client.disconnect();
				connectButton.setVisible(true);
				disconnectButton.setVisible(false);
				updButton.setVisible(false);
				list.removeAll();
				list.setVisible(false);
				lblState.setVisible(false);
				downloadButton.setVisible(false);
				cancelButton.setVisible(false);
				deleteButton.setVisible(false);
				styledText.setVisible(false);
				returnButton.setVisible(false);
			}
		});
				
		connectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				client = new POP3client("logPOP3.txt");
				ServerAccessWindow dialogue = new ServerAccessWindow(shell, 0);
				dialogue.open();
				//here we have some result from our dialogue
				
				lblError.setVisible(false);
				lblState.setVisible(false);
				//setting connection
				try {
					
					client.connect(dialogue.result.getHostName(), dialogue.result.getPort());					
					client.login(dialogue.result.getUserName(), dialogue.result.getPassword());
					
					lblState.setText(client.getStat());
					lblState.setVisible(true);
					client.downloadHeaders();

					for(int i = 1; i<=client.headers.size() && i<28; i++){
						  list.add(client.getHeader(i));
					  }
					
				} catch (POP3Exceptions e1) {
					lblError.setVisible(true);
					lblError.setText("[Error]: "+e1.returnText());
					return;
				}
				list.setVisible(true);
				connectButton.setVisible(false);
				disconnectButton.setVisible(true);
				updButton.setVisible(true);
				downloadButton.setVisible(true);
				deleteButton.setVisible(true);
				cancelButton.setVisible(true);
				
			}
		});
		
		downloadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedMsg = list.getSelectionIndex()+1;
				if(selectedMsg>0){
					try {
						list.setVisible(false);
						styledText.setVisible(true);
						styledText.setText(client.getMessage(selectedMsg));
						downloadButton.setVisible(false);
						returnButton.setVisible(true);
						deleteButton.setVisible(false);
						cancelButton.setVisible(false);
					} catch (POP3Exceptions e1) {
						lblState.setVisible(false);
						lblError.setVisible(true);
						lblError.setText("[Error]: "+e1.returnText());
						return;
					}
				}
			}
		});
		
		returnButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				returnButton.setVisible(false);
				downloadButton.setVisible(true);
				styledText.setVisible(false);
				list.setVisible(true);
				deleteButton.setVisible(true);
				cancelButton.setVisible(true);
			}
		});
	}
}

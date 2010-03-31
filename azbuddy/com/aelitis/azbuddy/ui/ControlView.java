package com.aelitis.azbuddy.ui;

import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.aelitis.azbuddy.BuddyPlugin;
import com.aelitis.azbuddy.buddy.Buddy;

class ControlView extends Tab
{
	String		host	= "";
	int			port	= 0;

	UILogger logMan;

	ControlView (TabFolder folder, int tab_property)
	{
		super(folder,tab_property,"DHT Ping");
	}

	String buddyMagnet;

	void initialize(Composite parent)
	{
		GridLayout layout;
		GridData data;

		layout = new GridLayout();
		layout.numColumns = 3;
		parent.setLayout(layout);




		Label lbl = new Label(parent,SWT.NULL);
		lbl.setText("Buddy Magnet:");
		data = new GridData();
		lbl.setLayoutData(data);

		final Text txtBuddy = new Text(parent,SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);     
		txtBuddy.setLayoutData(data);

		Button btnBuddy = new Button(parent,SWT.NULL);
		data = new GridData(GridData.END);
		data.widthHint = 100;
		btnBuddy.setLayoutData(data);
		btnBuddy.setText("Add Buddy");
		btnBuddy.addListener(SWT.Selection,new Listener() {
			public void handleEvent(Event arg0)
			{
				BuddyPlugin.getSingleton().getBuddyMan().addBuddyByMagnet(txtBuddy.getText());
			}
		});

		StyledText  log = new StyledText(parent,SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 3;
		log.setLayoutData(data);
		logMan = UILogger.getSingleton(log);

		lbl = new Label(parent,SWT.NULL);
		lbl.setText("Send Command:");
		data = new GridData();
		lbl.setLayoutData(data);

		final Text txtCommand = new Text(parent,SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);     
		txtCommand.setLayoutData(data);
		txtCommand.addListener(SWT.KeyUp, new Listener() {
			public void handleEvent(Event event) {
				if(event.keyCode == 13) {
					String cmd = txtCommand.getText();
					txtCommand.setText("");
					executeCommand(cmd);
				}
			}
		});

		Button btnCommand = new Button(parent,SWT.NULL);
		data = new GridData(GridData.END);
		data.widthHint = 100;
		btnCommand.setLayoutData(data);
		btnCommand.setText("Execute");
		btnCommand.addListener(SWT.Selection,new Listener() {
			public void handleEvent(Event arg0)
			{
				String cmd = txtCommand.getText();
				txtCommand.setText("");
				executeCommand(cmd);
			}
		});

	}

	private void executeCommand(String command) {
		if(command.equals("help")) {
			logMan.log("Available commands :");
			logMan.log("list\tlists your buddys");
			logMan.log("send <nick> <msg>\tsends a message to a buddy");
			logMan.log("setNick <pubKey> <nick>\tsets the nick of a buddy");
			logMan.log("nick <nick>\tsets our nickname");
		}

		if(command.equals("list")) {
			List<Buddy> buddies = BuddyPlugin.getSingleton().getBuddyMan().getBuddies();
			for(Buddy buddy : buddies) {
				logMan.log(buddy.getPermaNick() + " : " + buddy.getCurrentNick() + " , " + buddy.getSerializedPublicKey());
			}
		}

		if(command.startsWith("send ")) {
			StringTokenizer st = new StringTokenizer(command," ");
			if(st.countTokens() >= 3) {
				st.nextElement();
				String id = st.nextToken();
				StringBuffer message = new StringBuffer();
				String separator = "";
				while(st.hasMoreElements()) {
					message.append(separator);
					message.append(st.nextToken());
					separator = " ";
				}
				List<Buddy> buddies = BuddyPlugin.getSingleton().getBuddyMan().getBuddies();
				for(Buddy buddy : buddies) {
					String nick = buddy.getPermaNick();
					if(nick.equals(id)) {
						buddy.sendChatMessage(message.toString(),false);
					}
				}
			}
		}

		if(command.startsWith("setNick ")) {
			StringTokenizer st = new StringTokenizer(command," ");
			if(st.countTokens() == 3) {
				st.nextElement();
				String id = st.nextToken();
				String nick = st.nextToken();
				List<Buddy> buddies = BuddyPlugin.getSingleton().getBuddyMan().getBuddies();
				for(Buddy buddy : buddies) {
					String pubKey = buddy.getSerializedPublicKey();
					if(pubKey.equals(id)) {
						buddy.setPermaNick(nick);
						logMan.log("changed nick of " + id + " to " + nick);
					}
				}
			}
		}

		if(command.startsWith("nick ")) {
			StringTokenizer st = new StringTokenizer(command," ");
			if(st.countTokens() == 2) {
				st.nextElement();
				String nick = st.nextToken();
				BuddyPlugin.getSingleton().getBuddyMan().setLocalNick(nick);
			}
		}
	
	}

	void refresh()
	{
		logMan.refresh();
	}

	void destroy()
	{
		logMan.destroy();
	}
}
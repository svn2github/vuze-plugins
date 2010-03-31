/**
 * 
 */
package com.aelitis.azureus.plugins.chat.ui;

import java.util.HashMap;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

import com.aelitis.azureus.plugins.chat.ChatPlugin;

/**
 * @author TuxPaper
 * @created Mar 26, 2006
 *
 */
public class ChatPanelsManager implements UISWTViewEventListener {
	private HashMap panels = new HashMap(2);

	private final ChatPlugin chatPlugin;
	
	/**
	 * 
	 */
	public ChatPanelsManager(ChatPlugin chatPlugin) {
		this.chatPlugin = chatPlugin;
		// TODO Auto-generated constructor stub
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				UISWTView view = event.getView();
				
				Download newChannelDL = null;
				boolean bCreate = view.getViewID().equals("CreateChat");
				if (bCreate) {
					Display display = chatPlugin.getSWTUI().getDisplay();
					String sChannel = new ShellCreateChannel().open(display,
							"Views.plugins.CreateChat.title", "Views.plugins.CreateChat.prompt",
							chatPlugin.getLocaleUtils());
					if (sChannel == null)
						return false;
					newChannelDL = chatPlugin.addChannel(sChannel);
				}

				panels.put(view, new ChatPanel(chatPlugin, view, newChannelDL));
				break;

			case UISWTViewEvent.TYPE_INITIALIZE: {
				ChatPanel panel = (ChatPanel) panels.get(event.getView());
				Composite c = (Composite)event.getData();
				if (panel != null && c != null && !c.isDisposed()) {
					Download download = (Download)event.getView().getDataSource();

					panel.initialize(c, download);
				}
				break;
			}
				
			case UISWTViewEvent.TYPE_LANGUAGEUPDATE: {
				ChatPanel panel = (ChatPanel) panels.get(event.getView());
				if (panel != null) {
					panel.updateLanguage();
				}
				
				break;
			}

			case UISWTViewEvent.TYPE_REFRESH: {
				ChatPanel panel = (ChatPanel) panels.get(event.getView());
				if (panel != null) {
					panel.refresh();
				}
				
				break;
			}

			case UISWTViewEvent.TYPE_DESTROY: {
				ChatPanel panel = (ChatPanel) panels.get(event.getView());
				if (panel != null) {
					panel.delete();
				}
				
				break;
			}
		}

		return true;
	}

}

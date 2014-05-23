package org.tfdn.azureus2.plugins.atelnetui;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Vector;

import org.gudy.azureus2.plugins.PluginInterface;

public class TelnetListener extends Thread {
	
	private ServerSocket _server;
	private boolean _active=true;
	private PluginInterface _iface;
	private Vector<TelnetConnection> _connections;
	
	public TelnetListener(PluginInterface iface) throws IOException {
		_server = new ServerSocket(iface.getPluginconfig().getPluginIntParameter("port"));
		_iface = iface;
		_connections = new Vector<TelnetConnection>();
		setDaemon(true);
	}
	
	public void run() {
		while (_active) {
			try {
				TelnetConnection t = new TelnetConnection(_server.accept(),_iface);
				_connections.add(t);
			} catch (IOException e) {
			}
		}
	}
	
	public void quit() {
		_active = false;
		for(TelnetConnection c : _connections) {
			c.quit();
		}
		try {
			_server.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

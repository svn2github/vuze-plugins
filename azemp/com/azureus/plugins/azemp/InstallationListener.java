package com.azureus.plugins.azemp;

public interface InstallationListener {

	public void reportPercentDone(int percent);
	
	public void reportComplete();
	
	public void reportError();
}

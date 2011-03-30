package com.vuze.plugin.btapp;

public interface BtApp
{
	static final long PRIV_LOCAL = 0x0;

	static final long PRIV_READALL = 0x1;

	static final long PRIV_WRITEALL = 0x2;

	String getAppId();

	void executeJS(String js);

	void insertAjaxProxy();
	
	void log(String s);

	void insertBtAppJS();
}

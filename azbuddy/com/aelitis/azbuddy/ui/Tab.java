package com.aelitis.azbuddy.ui;

import org.eclipse.swt.SWT;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

abstract class Tab
{
	TabItem tab;
	TabFolder folder;
	Composite container;
	
	Tab(TabFolder folder, int tabProperty, String tabName)
	{
		this.folder = folder;
		tab = new TabItem(folder, tabProperty);
		tab.setText(tabName);
	}
		
	abstract void refresh();
	abstract void destroy();
	abstract void initialize(Composite parent);
	
	void initControl()
	{
		container = new Composite(folder,SWT.NULL);
		tab.setControl(container);
		initialize(container);
	}
	
	void destroyControl()
	{
		container = null;
		destroy();
	}
	
	void refreshControl()
	{
		refresh();
	}
}
/*
 * Created on May 24, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.vuze.plugins.mlab.ui;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

public class 
MLabWizardStart
	extends AbstractWizardPanel<MLabWizard> 
{
	protected
	MLabWizardStart(
		MLabWizard		wizard )
	{
		super( wizard, null );
	}

	public void 
	show() 
	{
		wizard.setTitle(MessageText.getString( "mlab.wizard.intro.title" ));
        wizard.setCurrentInfo( "" );
        wizard.setPreviousEnabled(false);
        wizard.setFinishEnabled(false);

        Composite rootPanel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		rootPanel.setLayout(layout);
		
		Image img = wizard.getImage( "com/vuze/plugins/mlab/ui/resources/mlab.png" );
		
		Label l = new Label( rootPanel, SWT.NULL );
        GridData gridData = new GridData(GridData.FILL_BOTH);
		l.setLayoutData(gridData);

		
		l.setImage( img );
	}
	
	public boolean 
	isNextEnabled()
	{
		return( true );
	}
	
	public IWizardPanel 
	getNextPanel() 
	{
		return( new MLabWizardNDT( wizard, this ));
	}
}

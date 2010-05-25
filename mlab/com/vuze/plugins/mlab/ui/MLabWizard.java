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

import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

import com.vuze.plugins.mlab.MLabPlugin;

public class 
MLabWizard
	extends Wizard
{
	public
	MLabWizard(
		MLabPlugin		_plugin,
		IPCInterface	_callback )
	{
		super( "mlab.wizard.title" );
		
		MLabWizardStart panel = new MLabWizardStart( this );
		
		setFirstPanel( panel );
	}

	public void 
	onClose()
	{
		super.onClose();
	}
}

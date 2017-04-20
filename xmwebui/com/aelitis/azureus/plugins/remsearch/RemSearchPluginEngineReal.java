/*
 * Created on Jan 11, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


package com.aelitis.azureus.plugins.remsearch;


import com.aelitis.azureus.core.metasearch.Engine;

public class 
RemSearchPluginEngineReal 
	extends RemSearchPluginEngine
{
	private Engine		engine;

	
	protected
	RemSearchPluginEngineReal(
		Engine		_engine )
	{
		engine	= _engine;
	}
	
	public Engine
	getEngine()
	{
		return( engine );
	}
	
	public String
	getName()
	{
		return( engine.getName());
	}
	
	public String
	getUID()
	{
		return( engine.getUID());
	}
	
	public String
	getIcon()
	{
		return( engine.getIcon());
	}
	
	public String
	getDownloadLinkCSS()
	{
		return( engine.getDownloadLinkCSS());
	}
	
	public int
	getSelectionState()
	{
		return( engine.getSelectionState());
	}
	
	public  int
	getSource()
	{
		return( engine.getSource());
	}
}

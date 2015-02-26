/*
 * Created on Apr 15, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.plugins.rcmplugin;

import org.gudy.azureus2.plugins.download.Download;

import com.aelitis.azureus.ui.UserPrompterResultListener;

public interface 
RelatedContentUI 
{
	public void
	showFTUX(
		UserPrompterResultListener		listener );
	
	public void
	setUIEnabled(
		boolean		enabled );
	
	public void
	addSearch(
		Download		download );
	
	public void
	addSearch(
		long			size,
		String[]		networks );
	
	public void
	addSearch(
		byte[]			hash,
		String[]		networks,
		String			name );

	public void
	addSearch(
		String			expression,
		String[]		networks );
	
	public void
	destroy();
}

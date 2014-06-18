/******************************************************************************
Cubit distribution
Copyright (C) 2008 Bernard Wong

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

The copyright owner can be contacted by e-mail at bwong@cs.cornell.edu
*******************************************************************************/

package org.cornell.hyper.overlay;

import java.util.Map;

public interface RemoteHyperInterface {
	public void init(HyperNode hyperNode);
	public String echo(String inString);
	public Object[] getAllPeers();
	public Object[] getRandomNodes();
	public Object[] getRandomWords();
	public Object[] getKClosestNodes(int val, String keyword);
	public Object[] getKClosestEntries(int val, Object[] keywords);
	@SuppressWarnings("unchecked")
	//public Map getCloserEntries(String keyword);
	public Object[] getCloserKeys(String keyword);
	@SuppressWarnings("unchecked")
	public Map getEntries(String keyword);
	public Boolean insertKeyEntry(String key, Object[] entry);
	public Boolean addNodes(Object[] inNode);
	@SuppressWarnings("unchecked")
	public Map getNode();	
	public Object[] getDistBoundNodes(String keyword, int distBound);
	public Object[] getDistKNodes(String keyword, int distBound, int kVal);
	public Object[] getRingInfo();
}
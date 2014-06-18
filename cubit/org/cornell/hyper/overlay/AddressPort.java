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

public class AddressPort {
	private String addr;
	private int port;
	
	public AddressPort(String addr, int port){
		this.addr = addr;
		this.port = port;
	}
	
	public String getAddr() { return addr; }	
	public int getPort() 	{ return port; }
	
	public boolean equals(Object o){
		if (!(o instanceof AddressPort)) {
			return false;		
		}
		AddressPort otherAP = (AddressPort)o;
		if (getAddr().equals(otherAP.getAddr()) && 
			getPort() == otherAP.getPort()) {
			return true;
		}
		return false;
	}
	
	public int hashCode() {
		return getAddr().hashCode() ^ getPort();			
	}
}

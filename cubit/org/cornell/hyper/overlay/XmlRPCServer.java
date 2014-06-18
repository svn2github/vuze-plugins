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

import java.io.IOException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import org.cornell.hyper.overlay.RemoteHyperImpl;

public class XmlRPCServer {
	public static WebServer startXmlRPCServer(HyperNode hyperNode, int port) 
			throws IOException, XmlRpcException {
		WebServer webServer = new WebServer(port);
		
		XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
		PropertyHandlerMapping phm = new PropertyHandlerMapping();
		HyperProcessorFactory factory = new HyperProcessorFactory(hyperNode);
		phm.setRequestProcessorFactoryFactory(factory);        		
		
		String shortName = "RemoteHyperInterface";
		String fullName = "org.cornell.hyper.overlay.RemoteHyperInterface";        
        phm.addHandler(shortName, RemoteHyperImpl.class);
        phm.addHandler(fullName, RemoteHyperImpl.class);
        		
		xmlRpcServer.setHandlerMapping(phm);        
		XmlRpcServerConfigImpl serverConfig =
			(XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
		//serverConfig.setEnabledForExtensions(true);
		serverConfig.setContentLengthOptional(false);		
		webServer.start();
		return webServer;
    }		
}

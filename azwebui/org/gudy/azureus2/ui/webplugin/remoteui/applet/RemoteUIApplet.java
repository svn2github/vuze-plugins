/*
 * File    : WebPluginApplet.java
 * Created : 27-Jan-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
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

package org.gudy.azureus2.ui.webplugin.remoteui.applet;

/**
 * @author parg
 *
 */

import java.util.*;
import java.util.zip.*;

import java.net.*;
import java.io.*;

import java.applet.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;
import javax.net.ssl.*;
import org.gudy.azureus2.ui.swing.*;

import org.gudy.azureus2.core3.security.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.Semaphore;
import org.gudy.azureus2.ui.webplugin.util.*;

import org.gudy.azureus2.pluginsimpl.remote.*;

import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.ui.webplugin.remoteui.applet.view.*;

public class 
RemoteUIApplet
	extends 	Applet
	implements 	RPRequestDispatcher
{
	public static final int REQUEST_RETRY_LIMIT	= 5;
	
	protected RPPluginInterface		plugin_interface;
	
	protected RemoteUIMainPanel		panel;
	
	protected WUJarReader			jar_reader;
	
	protected Semaphore	dialog_sem			= new Semaphore(1);
	protected ArrayList	outstanding_dialogs	= new ArrayList();
	
	protected boolean	application;
	protected URL		dispatch_url;
	protected JFrame	application_frame;
	
	public
	RemoteUIApplet()
	{	
		this( null );
	}
	
	protected
	RemoteUIApplet(
		URL		_dispatch_url )
	{
		if ( _dispatch_url == null ){
			
			// can't do this here, defer it... dispatch_url = getDocumentBase();
			
			application		= false;
			
		}else{
		
			dispatch_url	= _dispatch_url;
			
			application		= true;
		}
		
		try{
			
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			
		}catch( Exception e ){
			
			e.printStackTrace();
		}	
	}
	
	public void
	init()
	{
			// set up dummy configuration parameters as file-based default read will fail as 
			// we are an applet!
		
		COConfigurationManager.initialiseFromMap( new HashMap());
	}
	
	public void
	start()
	{
		if ( !application ){
			
			dispatch_url = getDocumentBase();
		}
		
		try{
			plugin_interface = RPFactory.getPlugin( this );
			
			//System.out.println( "got pi:" + pi );
			//Properties props = pi.getPluginProperties();
			//System.out.println( "props = " + props );
			
			final DownloadManager		download_manager	= plugin_interface.getDownloadManager();
				
			panel = new RemoteUIMainPanel( 
						plugin_interface, 
						download_manager,
						new RemoteUIMainPanelAdaptor()
						{
							public InputStream
							getResource(
								String	name )
							{
								return( getIconResource( name ));
							}
							
							public void
							refresh()
							{							
							}
							
							public void
							error(
								Throwable 		e )
							{
								showError( e );
							}
						});
			
			JPanel	outer_panel = new JPanel( new GridBagLayout());
			
			outer_panel.setBorder( BorderFactory.createLineBorder(Color.black));
			
			outer_panel.add(
				panel,
				new VWGridBagConstraints(
					0, 0, 1, 1, 1.0, 1.0,
					GridBagConstraints.WEST,
					GridBagConstraints.BOTH, 
					new Insets(2, 2, 2, 2), 0, 0 ));
			
			construct( outer_panel );
			
		}catch( Throwable e ){
			
			showError( e );
		}
	}
	
	protected void
	construct(
		JPanel		outer_panel )
	{
		if ( application ){
			
			application_frame = new JFrame( "Azureus Swing UI" );

			application_frame.addWindowListener(
				new WindowAdapter()
				{
					public void
					windowClosing(
						WindowEvent	ev )
					{
						System.exit(0);
					}
				});
			
			application_frame.setSize(600,300);

			Container cont = application_frame.getContentPane();
			
			cont.setLayout(new BorderLayout());
			
			cont.add(outer_panel, BorderLayout.CENTER );
			
			application_frame.setVisible(true);
		}else{
			
			setLayout(new BorderLayout());
		
			add(outer_panel, BorderLayout.CENTER );
		
			validate();
		}
	}
	
	protected Component
	getDialogOwner()
	{
		if ( application ){
			
			return( application_frame );
			
		}else{
		
			return( this );
		}
	}
	
	protected InputStream
	getIconResource(
		String	name )
	{
		if ( application ){
	
			return( UISwingImageRepository.getImageAsStream( name ));
			
		}else{

			if ( jar_reader == null ){
				
				jar_reader	= new WUJarReader( "remuiicons.jar");
			}
			
			return( jar_reader.getResource( "org/gudy/azureus2/ui/icons/" + name ));
		}
	}
	
	protected void
	showError(
		final Throwable e )
	{
		new Thread()
		{
			public void
			run()
			{
				showErrorSupport(e);
			}
		}.start();
	}
	
	protected void
	showErrorSupport(
		Throwable e )
	{
		e.printStackTrace();
		
		String	message_chain = "";
		
		Throwable	temp = e;
		
		while( temp != null ){
			
			String	this_message = temp.getMessage();
			
			if ( this_message != null ){
				
				message_chain += (message_chain.length()==0?"":"\n") + this_message;
			}
			
			temp = temp.getCause();
		}
					
		final String	message = message_chain.length()==0?e.toString():message_chain;
			
		synchronized( outstanding_dialogs ){
				
			if ( outstanding_dialogs.contains( message )){
					
				return;
			}
			
			outstanding_dialogs.add( message );
		}
			
		dialog_sem.reserve();
		
		SwingUtilities.invokeLater(
				new Runnable()
				{
					public void
					run()
					{
						try{
							JOptionPane.showMessageDialog( 
									getDialogOwner(), 
									message,
									"Error Occurred",  
									JOptionPane.ERROR_MESSAGE );
							
							}finally{
								
								synchronized( outstanding_dialogs ){
									
									outstanding_dialogs.remove( message );
								}
								
								dialog_sem.release();
							}
					}
				});
	}
	
	public RPPluginInterface
	getPlugin()
	{
		return( plugin_interface );
	}
	
	protected URL
	getDispatchURL()
	{
		return( dispatch_url );
	}
	
	public RPReply
	dispatch(
		RPRequest	request )
	
		throws RPException
	{
		Throwable 	last_error = null;
		
		for (int i=0;i<REQUEST_RETRY_LIMIT;i++){
			
			try{
				if ( panel != null ){
					
					panel.logMessage("Request" + (i>0?"{retry #"+i+"}":"") + ":" + request.getObject()._getName()+ "::" + request.getMethod());
				}
				
				RPReply	reply = dispatchSupport( request );
			
				return( reply );
				
			}catch( Throwable e ){
				
				last_error	= e;
				
				Throwable cause = e.getCause();
				
				if ( cause != null ){
					
					String m = cause.getMessage();
					
					if ( m != null && m.indexOf( "Connection refused" ) != -1 ){
						
						break;
					}
				}
			}
		}
		
		if ( last_error instanceof RPException ){
			
			throw((RPException)last_error);
		}
		
		throw( new RPException( "RemoteUIApplet::dispatch failed", last_error ));
	}
	
	protected RPReply
	dispatchSupport(
		RPRequest	request )
	
		throws RPException
	{
		try{
			URL	url = getDispatchURL();
		
		    url = new URL( 	url.getProtocol() + "://" +
		    				url.getHost() + 
							(url.getPort()==-1?"":(":" + url.getPort())) + "/process.cgi" );
			
			// System.out.println( "doc base = " + url );
			
			HttpURLConnection con;
			
			if ( url.getProtocol().equalsIgnoreCase("https")){
				
				// see ConfigurationChecker for SSL client defaults
				
				URLConnection url_con = url.openConnection();
				
					// Opera doesn't return a javax class
				
				if ( url_con.getClass().getName().startsWith( "javax")){
									
					HttpsURLConnection ssl_con = (HttpsURLConnection)url_con;
					
					// allow for certs that contain IP addresses rather than dns names
					
					ssl_con.setHostnameVerifier(
							new HostnameVerifier()
							{
								public boolean
								verify(
										String		host,
										SSLSession	session )
								{
									return( true );
								}
							});	
				
					con = ssl_con;
				}else{
					
					con = (HttpURLConnection)url_con;
				}
			}else{
				
				con = (HttpURLConnection) url.openConnection();
			}

			con.setRequestProperty("Connection", "close" );
			
			con.setRequestMethod( "POST" );
			
			con.setAllowUserInteraction( true );
			
			con.setDoInput( true );
			
			con.setDoOutput( true );
						
			con.connect();
		
			ObjectOutputStream dos = null;
			
			try{
				dos = new ObjectOutputStream(new GZIPOutputStream(con.getOutputStream()));
			
				dos.writeObject( request );
				
				dos.flush();
				
			}finally{
			
				if ( dos != null ){
					
					dos.close();
				}
			}
			
			InputStream is = null;
			
			try{
				
				is = con.getInputStream();
				
				int content_length = con.getContentLength();
				
				byte[] data = new byte[1024];
				
				int	num_read = 0;
				
				ByteArrayOutputStream	baos = new ByteArrayOutputStream();
				
				while ( num_read < content_length ){
					
					try{
						int	len = is.read(data);
						
						if ( len > 0 ){
							
							baos.write(data, 0, len);
															
							num_read += len;
							
						}else if ( len == 0 ){
							
							Thread.sleep(20);
							
						}else{
							
							break;
						}
						
					}catch (Exception e){
						
						e.printStackTrace();
						
						break;
					}
				}
				
				ObjectInputStream	ois = new ObjectInputStream(new GZIPInputStream( new ByteArrayInputStream( baos.toByteArray())));
				
				try{
					return((RPReply)ois.readObject());
					
				}finally{
					
					ois.close();
				}
			}finally{
				
				if ( is != null ){
					
					is.close();
				}
			}
		}catch( Throwable e ){		
		
			throw( new RPException( "RequestDispatch fails", e ));
		}
	}
	
	public void
	destroy()
	{
		panel.destroy();
		
		super.destroy();
	}
	
	public static void
	main(
		String[]		args )
	{
		if ( args.length != 1 ){
			
			System.err.println( "Usage: RemoteUIApplet <url of webui server>");
			System.err.println( "    For example RemoteUIAppler http://fred.com:6883/" );
			System.err.println( "    If you need user/password encode in url. e.g. http://paul:secret@fred.com:6883/" );
			System.err.println( "    For https you'll need to set up the ketstore yourself" );
			
			System.exit(1);
		}
		
		try{
			final URL	target = new URL( args[0] );
			
			String	user_info = target.getUserInfo();
			
			if ( user_info != null ){
				
				int	pos = user_info.indexOf( ":" );
				
				if ( pos == -1 ){
					
					System.err.println( "Invalid user info in URL" );
					
					System.exit(1);
				}
				
				final String	user 		= user_info.substring(0,pos);
				final String	password	= user_info.substring(pos+1);
				
				SESecurityManager.initialise();
				
				SESecurityManager.addPasswordListener(
						new SEPasswordListener()
						{
							public PasswordAuthentication
							getAuthentication(
								String		realm,
								URL			tracker )
							{
								if ( target.getHost().equals( tracker.getHost())){
									
									return( new PasswordAuthentication( user, password.toCharArray()));
								}
								
								return( null );
							}
							
							public void
							setAuthenticationOutcome(
								String		realm,
								URL			tracker,
								boolean		success )
							{
								
							}
						});
			}
			
			RemoteUIApplet	applet = new RemoteUIApplet(target);
			
			applet.init();
			
			applet.start();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
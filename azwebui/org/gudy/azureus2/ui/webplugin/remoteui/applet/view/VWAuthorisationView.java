/*
 * Created on 04-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.webplugin.remoteui.applet.view;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.*;

import javax.swing.*;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.ui.webplugin.remoteui.applet.*;

public class 
VWAuthorisationView 
{
	public
	VWAuthorisationView(
		final RemoteUIMainPanel	main_panel,
		final URL				url )
	{
		final JFrame	frame = new JFrame( "Authorisation Required" );
			
		Container	cont = frame.getContentPane();
			
		cont.setLayout( new GridBagLayout());
			
		int	row = 0;
		
		cont.add( 	new JLabel( url.toString()),
					new VWGridBagConstraints(
						0, row, 2, 1, 0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE, 
						new Insets(8, 8, 8, 8), 0, 0 ));
		
		row++;
		
		cont.add( 	new JLabel( "User name:" ),
					new VWGridBagConstraints(
						0, row, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST,
						GridBagConstraints.NONE, 
						new Insets(8, 8, 8, 8), 0, 0 ));
	
		final JTextField	user_name = new JTextField();
		
		user_name.setColumns( 16 );
		
		cont.add( 	user_name,
					new VWGridBagConstraints(
						1, row, 1, 1, 1.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.HORIZONTAL, 
						new Insets(8, 16, 8, 8), 0, 0 ));
		
		row++;
		
		cont.add( 	new JLabel( "Password:" ),
					new VWGridBagConstraints(
						0, row, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST,
						GridBagConstraints.NONE, 
						new Insets(0, 8, 8, 8), 0, 0 ));
	
		final JPasswordField	password = new JPasswordField();
		
		password.setColumns(16);
		
		cont.add( 	password,
					new VWGridBagConstraints(
						1, row, 1, 1, 1.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.HORIZONTAL, 
						new Insets(0, 16, 8, 8), 0, 0 ));
		
		row++;
		
		JPanel	but_pan = new JPanel( new GridBagLayout());
		
		but_pan.add( 	
				new JPanel(),
				new VWGridBagConstraints(
					0, 0, 1, 1, 1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL, 
					new Insets(0, 0, 0, 0), 0, 0 ));
		
		JPanel	but_subpan = new JPanel(new GridLayout(1,2));
		
		JButton ok_but	= new JButton( "OK" );

		AbstractAction ok_action = 
			new AbstractAction()
			{
				public void
				actionPerformed(
					ActionEvent	ev )
				{
					frame.dispose();
					
					new Thread()
					{
						public void
						run()
						{
							main_panel.openTorrent( url, user_name.getText(), new String(password.getPassword()));
						}
					}.start();
				}
			};

		ok_but.registerKeyboardAction(
				ok_action,
				KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 ),
				JComponent.WHEN_IN_FOCUSED_WINDOW );	
		
		ok_but.addActionListener( ok_action );
		
		frame.getRootPane().setDefaultButton( ok_but );
		
		JPanel	ok_but_pan = new JPanel(new GridLayout(1,1));
		
		ok_but_pan.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		ok_but_pan.add( ok_but );
		
		but_subpan.add( ok_but_pan );
		
		JButton cancel_but =  new JButton( "Cancel" );

		AbstractAction cancel_action = 
			new AbstractAction()
			{
				public void
				actionPerformed(
					ActionEvent	ev )
				{
					frame.dispose();
				}
			};

		cancel_but.registerKeyboardAction(
				cancel_action,
				KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ),
				JComponent.WHEN_IN_FOCUSED_WINDOW );		
		
		cancel_but.addActionListener( cancel_action );
		
		JPanel	cancel_but_pan = new JPanel(new GridLayout(1,1));
		
		cancel_but_pan.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		cancel_but_pan.add( cancel_but );

		but_subpan.add( cancel_but_pan );
	
	
		but_pan.add( 	
				but_subpan,
				new VWGridBagConstraints(
					1, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE, 
					new Insets(0, 0, 0, 0), 0, 0 ));
		
		cont.add( 	but_pan,
					new VWGridBagConstraints(
						0, row, 2, 1, 1.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.HORIZONTAL, 
						new Insets(8, 8, 8, 8), 0, 0 ));		
		
		frame.pack();
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			
		frame.setLocation( (dim.width - frame.getSize().width)/2,
						   (dim.height - frame.getSize().height)/2 );
		
		frame.setVisible(true);
	}
	
	public static void
	main(
		String[]	args )
	{
		try{
			new VWAuthorisationView(null,new URL( "http://a.vb.c:6969/"));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
			
}

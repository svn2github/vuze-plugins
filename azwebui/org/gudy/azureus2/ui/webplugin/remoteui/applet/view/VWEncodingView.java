/*
 * Created on 27-Jul-2004
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
import java.awt.event.*;

import java.net.URL;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;


import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.webplugin.remoteui.applet.RemoteUIMainPanel;

/**
 * @author parg
 *
 */
public class 
VWEncodingView 
{
	public
	VWEncodingView(
		final RemoteUIMainPanel		main_panel,
		final String[]				encodings,
		final String[]				names,
		final URL					url,
		final String				user,
		final String				password )
	{
		final JFrame	frame = new JFrame( "Select Encoding" );
			
		Container	cont = frame.getContentPane();
			
		cont.setLayout( new GridBagLayout());
			
		int	row = 0;
		
		String[][] table_data = new String[encodings.length][2];
		
		for (int i=0;i<encodings.length;i++){
			
			table_data[i][0] = encodings[i];
			
			table_data[i][1] = names[i];
		}
		
		TableModel	model = 
			new DefaultTableModel( table_data, new String[]{"Encoding", "Decoded Torrent Name"} )
			{
			   	public boolean 
			   	isCellEditable(int row, int column) 
			   	{
			   		return( false );
			   	}		
			};
			
		final JTable	table = new JTable( model );
		
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		table.setAutoscrolls( true );
		
		table.setShowHorizontalLines( true );
		
		table.setShowVerticalLines( true );
				
		TableColumnModel	tcm = table.getColumnModel();
		
		TableCellRenderer	cell_renderer = 
			new DefaultTableCellRenderer()
			{
				public Component 
				getTableCellRendererComponent(
					JTable		table,
					Object 		o_value,
					boolean 	isSelected,
					boolean 	hasFocus,
					int 		row,
					int 		column )
				{
					JLabel	res = (JLabel)super.getTableCellRendererComponent( table, o_value, isSelected, hasFocus, row, column );
					
					res.setBorder( noFocusBorder );
					
					return( res );
				}
			};
			
		tcm.getColumn(0).setCellRenderer(cell_renderer);
		tcm.getColumn(1).setCellRenderer(cell_renderer);
			
		table.getSelectionModel().setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		
		table.getSelectionModel().setSelectionInterval(0,0);

	
		cont.add( 	new JScrollPane(table),
					new VWGridBagConstraints(
						0, row, 1, 1, 1.0, 1.0,
						GridBagConstraints.WEST,
						GridBagConstraints.BOTH, 
						new Insets(8, 8, 8, 8), 0, 0 ));
		
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
		
		final JButton ok_but	= new JButton( "OK" );

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
							String	encoding = encodings[table.getSelectedRow()];
														
							main_panel.openTorrent( url, user, password, encoding );
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
		
		table.getSelectionModel().addListSelectionListener(
				new ListSelectionListener()
				{
					public void
					valueChanged(
						ListSelectionEvent	ev )
					{
						ok_but.setEnabled( table.getSelectedRow() != -1 );
					}
				});
		
		table.addMouseListener(
				new MouseAdapter()
				{
					public void
					mouseClicked(
						MouseEvent	ev )
					{
						if ( ev.getClickCount() == 2 && table.getSelectedRow() != -1 ){
			
							ok_but.doClick();
							
						}
					}
				});
			
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
						0, row, 1, 1, 1.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.HORIZONTAL, 
						new Insets(8, 8, 8, 8), 0, 0 ));		
		
		
		frame.setSize(400,300);
			
		frame.validate();
		
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
			new VWEncodingView(
					null,
					new String[]{ "a", "b"}, 
					new String[]{"sdsdsdsdsd!","grtrotprtoprto"},
					null, null, null );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
}

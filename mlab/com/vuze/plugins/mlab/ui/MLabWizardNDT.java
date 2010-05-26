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

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.ui.swt.TextViewerWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

import com.vuze.plugins.mlab.MLabPlugin;
import com.vuze.plugins.mlab.MLabPlugin.*;

public class 
MLabWizardNDT 
	extends AbstractWizardPanel<MLabWizard> 
{
	private StyledText			log;
	private MLabPlugin.ToolRun	runner;
	
	private Composite			root_panel;
	
	private TimerEventPeriodic	prog_timer;
	private StackLayout 		stack_layout;
	private Composite 			progress_panel;
	private Composite 			status_panel;
	private Label				result_label;
	private Button 				retest_button;
	
	private int	prog_value = 0;

	private StringBuffer details = new StringBuffer();
	

	
	protected
	MLabWizardNDT(
		MLabWizard							wizard,
		AbstractWizardPanel<MLabWizard>		prev )
	{
		super( wizard, prev );
	}

	public void 
	show() 
	{
		wizard.setTitle(MessageText.getString( "mlab.wizard.ndt.title" ));
        wizard.setCurrentInfo( MessageText.getString( "mlab.wizard.ndt.info" ));
        wizard.setPreviousEnabled(false);
        wizard.setFinishEnabled(false);

        root_panel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		root_panel.setLayout(layout);

	   	log = new StyledText(root_panel,SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
    	GridData gridData = new GridData(GridData.FILL_BOTH);
    	gridData.horizontalSpan = 1;
    	log.setLayoutData(gridData);

    	
        Composite controlPanel = new Composite( root_panel, SWT.NULL );
    	gridData = new GridData(GridData.FILL_HORIZONTAL );
    	gridData.horizontalSpan = 1;
    	controlPanel.setLayoutData(gridData);

        stack_layout = new StackLayout();
        
        controlPanel.setLayout( stack_layout );
        
		progress_panel 	= new Composite( controlPanel, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 1;
		progress_panel.setLayout(layout);

		final ProgressBar prog = new ProgressBar( progress_panel, SWT.HORIZONTAL );
	   	gridData = new GridData(GridData.FILL_HORIZONTAL );
    	gridData.horizontalSpan = 1;
    	prog.setLayoutData(gridData);

		prog.setMinimum(0);
		prog.setMaximum(100);	
		
		
		status_panel 	= new Composite( controlPanel, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		status_panel.setLayout(layout);

		result_label	= new Label( status_panel, SWT.NULL );
	   	gridData = new GridData(GridData.FILL_HORIZONTAL );
	   	result_label.setLayoutData(gridData);

	   	Button details_button = new Button( status_panel, SWT.NULL );
	   	details_button.setText( MessageText.getString( "mlab.wizard.details" ));
		
	   	details_button.addListener(
			SWT.Selection,
			new Listener()
			{
				public void 
				handleEvent(
					Event arg0 )
				{
					new TextViewerWindow( "sdsd", "sdsd", details.toString());
				}
			});
	   	
		retest_button = new Button( status_panel, SWT.NULL );
		retest_button.setText( MessageText.getString( "mlab.wizard.retest" ));
		retest_button.setEnabled( false );
		
		retest_button.addListener(
			SWT.Selection,
			new Listener()
			{
				public void 
				handleEvent(
					Event arg0 )
				{
					retest_button.setEnabled( false );
					
					stack_layout.topControl = progress_panel;
			    	
					prog_value = 0;
					
					log.setText( "" );
					
					root_panel.layout( true, true );
					
					runTest();
				}
			});
		
		stack_layout.topControl = progress_panel;
    	
		root_panel.layout( true );
		
		prog_timer = 
			SimpleTimer.addPeriodicEvent(
				"ProgressUpdater",
				250,
				new TimerEventPerformer()
				{					
					public void 
					perform(
						TimerEvent event ) 
					{
						if ( progress_panel.isDisposed()){
							
							prog_timer.cancel();
							
						}else if ( stack_layout.topControl == progress_panel ){
							
							Utils.execSWTThread(
									new Runnable()
									{
										public void
										run()
										{
											if ( !prog.isDisposed()){
											
												prog.setSelection( ( prog_value++ )%100 );
											}
										}
									});
						}
					}
				});
		
		runTest();
	}
	
	private void
	runTest()
	{
		wizard.setFinishEnabled( false );
		
		if ( wizard.pauseDownloads()){
			
			log.append( "Pausing downloads before performing test." );
		
			new AEThread2( "waiter" )
			{
				public void
				run()
				{
					try{
						for (int i=0;i<50;i++){
							
							final int f_i = i;
							
							Utils.execSWTThread(
								new Runnable()
								{
									public void
									run()
									{
										log.append( "." );
										
										if ( f_i == 49 ){
											
											log.append( "\n" );
										}
									}
								});
							
							try{
								Thread.sleep(100);
								
							}catch( Throwable e ){
								
							}
						}
					}finally{
						
						runTestSupport();
					}
				}
			}.start();
			
		}else{
		
			runTestSupport();
		}
	}
	
	private void
	runTestSupport()
	{
	   	runner = wizard.getPlugin().
			runNDT(
				new ToolListener()
				{
					{
						details.setLength(0);
					}
					
					public void
					reportSummary(
						final String		str )
					{
						Utils.execSWTThread(
							new Runnable()
							{
								public void
								run()
								{
									if ( !log.isDisposed()){
									
										log.append( str + "\n" );
																			
										log.setSelection( log.getText().length());
										
										if ( stack_layout.topControl != progress_panel ){
										
											stack_layout.topControl = progress_panel;
	
											root_panel.layout( true, true );
										}
									}
								}
							});
					}
					
					public void
					reportDetail(
						String		str )
					{
						details.append( str );
						details.append( "\n" );
					}
					
					public void
					complete(
						final Map<String,Object>	results )
					{
						Utils.execSWTThread(
								new Runnable()
								{
									public void
									run()
									{
										try{
											if ( !root_panel.isDisposed()){
											
												Long	up 		= (Long)results.get( "up" );
												Long	down 	= (Long)results.get( "down" );
												
												if ( up == null || up == 0 ){
													
													result_label.setText( MessageText.getString( "mlab.wizard.noresults" ) );
													
												}else{
													
													result_label.setText( 
														MessageText.getString( "mlab.wizard.results",
														new String[]{
																DisplayFormatters.formatByteCountToKiBEtcPerSec( up ) + " (" + 
																DisplayFormatters.formatByteCountToBitsPerSec( up ) + ")" }));
													
													wizard.setRates( up, down==null?0:down );
													
													wizard.setFinishEnabled( true );
												}
												
												retest_button.setEnabled( true );
												
												stack_layout.topControl = status_panel;
		
												root_panel.layout( true, true );
												
											}
										}finally{ 
											
											runner = null;
										}
									}
								});
	
					}
				});
	}
	
	public boolean 
	isNextEnabled()
	{
		return( false );
	}
	
	public IWizardPanel 
	getNextPanel() 
	{
		return( null );
	}
	
	public boolean 
	isPreviousEnabled() 
	{
		return( false );
	}

	public void
	cancelled()
	{
		if ( runner != null ){
		
			runner.cancel();
		}
	}
	
	public IWizardPanel 
	getFinishPanel() 
	{
		return( this );
	}
	
	public void
	finish()
	{
		wizard.finish();
	}
}
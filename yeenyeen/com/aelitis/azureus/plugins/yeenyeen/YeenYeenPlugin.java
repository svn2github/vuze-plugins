/**
 * 
 */
package com.aelitis.azureus.plugins.yeenyeen;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.ui.swt.plugins.*;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * @author allan
 *
 */
public class YeenYeenPlugin implements Plugin, SchedulerController, UIManagerListener {
	
	private static int[][] RGB_COLOURS = new int[][] {
		{0, 168, 0}, // Dark Green
		{141, 206, 141}, // Light Green
		{255, 192, 192}, // Pink
		{255, 255, 255}, // White
		{254, 154, 4}, // Orange
		{193, 0, 193}, // Purple
		{234, 0, 0}, // Red
		{255, 255, 0}, // Light Yellow
	};
	
	private static String[] LABELS = new String[] {
		"Full speed", "Limited", "Turn off", "Seeding only",
		"Orangey", "Purpley", "Red", "Yellowish"
	};
	
	private UISWTInstance ui_instance = null;

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.Plugin#initialize(org.gudy.azureus2.plugins.PluginInterface)
	 */
	public void initialize(final PluginInterface plugin_interface)	throws PluginException {
		plugin_interface.getUIManager().addUIListener(this);
		
		final BasicPluginConfigModel model = plugin_interface.getUIManager().createBasicPluginConfigModel("yeenyeen");
		
		final SchedulerWidget widget = new SchedulerWidget(this, RGB_COLOURS);

		ParameterListener enable_listener = new ParameterListener() {
			public void parameterChanged(Parameter p) {
				widget.setEnabled(((BooleanParameter)p).getValue());
			}
		};

		BooleanParameter enable_param = model.addBooleanParameter2("enabled", "yeenyeen.enabled", true);
		enable_param.addListener(enable_listener);
		enable_listener.parameterChanged(enable_param);
		
		model.addUIParameter2(new UISWTParameterContext() {
			public void create(Composite parent) {
				Group group = new Group(parent, SWT.NONE);
				group.setText("Scheduler");
				widget.prepare(parent);
				widget.createScheduler(group, new int[7][24]);
			}
		}, "_blank");
		model.addUIParameter2(new UISWTParameterContext() {
			public void create(Composite parent) {
				Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
				group.setText("Available Modes");
				widget.createLegend(group, LABELS);
			}
		}, "_blank");
		
		model.addUIParameter2(new UISWTParameterContext() {
			public void create(Composite parent) {
				GridLayout gl = new GridLayout();
				parent.setLayout(gl);
				Table table = new Table(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
				GridData gridData = new GridData();//GridData.FILL_HORIZONTAL);
				gridData.heightHint = 100;
				table.setLayoutData(gridData);
				table.setLinesVisible (false);
				table.setHeaderVisible (true);
				String[] titles = {" ", "C", "!", "Description", "Resource", "In Folder", "Location"};
				for (int i=0; i<titles.length; i++) {
					TableColumn column = new TableColumn (table, SWT.NONE);
					column.setText (titles [i]);
				}	
				int count = 128;
				for (int i=0; i<count; i++) {
					TableItem item = new TableItem (table, SWT.NONE);
					item.setText (0, "x");
					item.setText (1, "y");
					item.setText (2, "!");
					item.setText (3, "this stuff behaves the way I expect");
					item.setText (4, "almost everywhere");
					item.setText (5, "some.folder");
					item.setText (6, "line " + i + " in nowhere");
				}
				for (int i=0; i<titles.length; i++) {
					table.getColumn (i).pack ();
				}
				table.pack();
				table.layout();
			}
		}, "_blank");
		
	}
	
	public void UIAttached(UIInstance ui) {
		if (!(ui instanceof UISWTInstance)) {return;}
		this.ui_instance = (UISWTInstance)ui;
	}
	
	public void UIDetached(UIInstance ui) {
		if (this.ui_instance == ui) {this.ui_instance = null;}
	}
	
	public String describeBlock(int day, int hour) {
		   String[] alt_days = new java.text.DateFormatSymbols().getWeekdays();
		   String[] days = new String[7];
		   days[6] = alt_days[1];
		   System.arraycopy(alt_days, 2, days, 0, 6);
		   return days[day] + ", " + hour + ":00 - " + ((hour+1) % 24) + ":00";
	}
	
	public void modifyBlock(int day, int hour) {
		/*
		if (this.ui_instance == null) {return;}
		
		UISWTInputReceiver ir = (UISWTInputReceiver)this.ui_instance.getInputReceiver();
		ir.setLocalisedTitle("Modify start time");
		ir.setLocalisedMessage(describeBlock(day, hour));
		
		String[] choices = new String[] {
			
		};
		
		ir.prompt();
		*/
	}
	
}
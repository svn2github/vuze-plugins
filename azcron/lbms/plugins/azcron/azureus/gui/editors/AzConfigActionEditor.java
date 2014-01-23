package lbms.plugins.azcron.azureus.gui.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.actions.ConfigAction;
import lbms.plugins.azcron.actions.Parameter;
import lbms.plugins.azcron.actions.Parameter.Type;
import lbms.plugins.azcron.azureus.AzCronPlugin;
import lbms.plugins.azcron.azureus.actions.AzConfigAction;
import lbms.plugins.azcron.main.ColorUtilities;
import lbms.plugins.azcron.main.SWTSafeRunnable;
import lbms.plugins.azcron.main.editors.ConfigActionEditor;
import lbms.plugins.azcron.main.editors.InvalidActionException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.plugins.PluginConfig;

public class AzConfigActionEditor extends ConfigActionEditor{

	private Text keyText, valueText;
	private Button choose;
	private String[][] cfgValues;
	private String[][] filteredCfgValues;

	public AzConfigActionEditor(Composite parent) {
		super(parent);
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.azureus.gui.ConfigActionEditor#getAction()
	 */
	@Override
	public Action getAction() throws InvalidActionException {
/*		switch(typeCombo.getSelectionIndex()){

		//Boolean
		case 0:
			return new AzConfigAction(new Parameter(configKey, configValue, Type.Boolean));

			//String
		case 1:
			return new AzConfigAction(new Parameter(configKey, configValue, Type.String));

			//Integer
		case 2:
			return new AzConfigAction(new Parameter(configKey, configValue, Type.Integer));

			//Float
		case 3:
			return new AzConfigAction(new Parameter(configKey, configValue, Type.Float));

			//Long
		case 4:
			return new AzConfigAction(new Parameter(configKey, configValue, Type.Long));


			//COConfigurator does not have a Double for the type
		}
		*/
		return new AzConfigAction(new Parameter(configKey, configValue, Type.String));
		//throw new InvalidActionException();
	}


	@Override
	public void setAction(Action a) throws InvalidActionException {
		if(a instanceof ConfigAction){
			Parameter p = ((ConfigAction) a).getParameter();
			/*switch(p.getType()){
			case Boolean:
				typeCombo.select(0);
				break;
			case String:
				typeCombo.select(1);
				break;
			case Integer:
				typeCombo.select(2);
				break;
			case Float:
				typeCombo.select(3);
				break;
			case Long:
				typeCombo.select(4);
				break;
			}
*/			//TODO set the Inputs to the correct values p.getKey(); p.getValue();
			if(keyText != null || !keyText.isDisposed())
				keyText.setText(p.getName());
			if(valueText != null || !valueText.isDisposed())
				valueText.setText(p.getValue());

		}else{
			throw new InvalidActionException();
		}

	}


	@Override
	public void drawValueArea(){
		Label label = new Label(this,SWT.NONE);
		label.setText("Config Action");
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		Composite keyComp = new Composite(this,SWT.BORDER);
		keyComp.setLayout(new GridLayout(3,false));
		keyComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		final Display display = keyComp.getDisplay();

		Label keyLabel = new Label(keyComp, SWT.NULL);
		keyLabel.setText("Config Key:");
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		keyLabel.setLayoutData(gd);

		keyText = new Text(keyComp, SWT.BORDER | SWT.READ_ONLY);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		keyText.setLayoutData(gd);
		keyText.setText("No Key Chosen Yet");

		choose = new Button(keyComp, SWT.PUSH);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		choose.setLayoutData(gd);

		choose.setText("Choose");
		choose.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event arg0) {
				choose.setEnabled(false);
				chooseKey(display);
			}
		});

		Label valueLabel = new Label(keyComp, SWT.NULL);
		valueLabel.setText("Config Value:");
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		valueLabel.setLayoutData(gd);

		valueText = new Text(keyComp, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		valueText.setLayoutData(gd);
		valueText.setText("No Key Chosen Yet");
		valueText.addListener(SWT.Modify, new Listener(){
			public void handleEvent(Event arg0) {
				configValue = valueText.getText();
			}
		});

		Label warning = new Label(keyComp, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		warning.setLayoutData(gd);
		warning.setText("Note:" +
				"\nAzureus does not know the 'type' of value here" +
				"\nso everything is represented by a string" +
				"\nBe sure to adhere to the following:" +
				"\nBoolean - 0 or 1" +
				"\nint - is just the whole value" +
				"\nfloat - string value of the float" +
				"\nString - is just the String");

	}

	private void chooseKey(final Display display){

		if(display != null || !display.isDisposed()){
			display.asyncExec(new SWTSafeRunnable(){
				@Override
				public void runSafe() {
					PluginConfig pc = AzCronPlugin.getPluginInterface().getPluginconfig();
					int size = pc.getUnsafeParameterList().size();

					cfgValues = new String[size][];
					{
						Map<String,Object> map = pc.getUnsafeParameterList();
						String[] keyStrings = (String[])map.keySet().toArray(new String[map.size()]);
						Object[] valueObjects = (Object[])map.values().toArray(new Object[map.size()]);
						for (int i=0;i<size;i++) {
							cfgValues[i] = new String[] {keyStrings[i],String.valueOf(valueObjects[i])};
						}
					}
					Arrays.sort(cfgValues, new Comparator<String[]>() {
						/* (non-Javadoc)
						 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
						 */
						public int compare(String[] o1, String[] o2) {
							return o1[0].compareTo(o2[0]);
						}
					});
					filteredCfgValues = cfgValues;

					final Shell shell = new Shell(display);
					shell.setLayout(new GridLayout(1,false));
					shell.setText("Azureus Core Settings");

					Label filterLabel = new Label(shell, SWT.NULL);
					filterLabel.setText("Filter");

					Text filter = new Text(shell,SWT.BORDER);
					filter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

					Label label = new Label(shell, SWT.NULL);
					label.setText("Double Click Key to Select It");

					final Table table = new Table(shell, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.V_SCROLL);
					table.setHeaderVisible(true);
					GridData gd = new GridData(GridData.FILL_BOTH);
					gd.heightHint = 300;
					gd.widthHint = 500;
					table.setLayoutData(gd);

					TableColumn keyName = new TableColumn(table, SWT.LEFT);
					keyName.setText("Key Name");
					keyName.setWidth(300);

					TableColumn keyValue = new TableColumn(table, SWT.LEFT);
					keyValue.setText("Current Value");
					keyValue.setWidth(200);

					table.addListener(SWT.SetData, new Listener(){
						public void handleEvent(Event arg0) {
							TableItem item = (TableItem) arg0.item;
							int index = table.indexOf(item);
							item.setText(filteredCfgValues[index]);
							if (index%2==0)
								item.setBackground(ColorUtilities.getBackgroundColor(display));;

						}
					});

					table.addListener(SWT.MouseDoubleClick, new Listener(){

						public void handleEvent(Event arg0) {
							TableItem[] items = table.getSelection();
							if(items.length == 1){
								keyText.setText(items[0].getText(0));
								configKey = items[0].getText(0);
								valueText.setText(items[0].getText(1));
								configValue = items[0].getText(1);
								shell.close();
								choose.setEnabled(true);
							}

						}

					});

					filter.addModifyListener(new ModifyListener() {
						/* (non-Javadoc)
						 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
						 */
						public void modifyText(ModifyEvent e) {
							String filterStr = ((Text)e.widget).getText().toLowerCase();
							if (filterStr.length()>0) {
								List<String[]> list = new ArrayList<String[]>();
								for (int i=0;i<cfgValues.length;i++) {
									if (cfgValues[i][0].toLowerCase().contains(filterStr)) {
										list.add(cfgValues[i]);
									}
								}
								filteredCfgValues = list.toArray(new String[list.size()][]);
							} else {
								filteredCfgValues = cfgValues;
							}
							table.clearAll();
							table.setItemCount(filteredCfgValues.length);
						}
					});


					shell.pack();

					//Center Shell
					Monitor primary = display.getPrimaryMonitor ();
					Rectangle bounds = primary.getBounds ();
					Rectangle rect = shell.getBounds ();
					int x = bounds.x + (bounds.width - rect.width) / 2;
					int y = bounds.y +(bounds.height - rect.height) / 2;
					shell.setLocation (x, y);

					//open shell
					shell.open();

					table.clearAll();
					table.setItemCount(filteredCfgValues.length);
				}
			});
		}





	};
}

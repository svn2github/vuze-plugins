package lbms.plugins.azcron.service;

import java.io.IOException;
import java.util.ArrayList;

import lbms.plugins.azcron.actions.IPCAction;
import lbms.plugins.azcron.actions.Parameter;
import lbms.plugins.azcron.actions.RestartAction;
import lbms.plugins.azcron.actions.StartStopAction;
import lbms.plugins.azcron.azureus.actions.AzConfigAction;
import lbms.plugins.azcron.azureus.service.AzTask;
import lbms.plugins.azcron.azureus.service.AzTaskService;

import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;


/**
 * @author Damokles
 *
 */
public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AzTaskService service = new AzTaskService();
		AzTask t1 = new AzTask("Task 1", "*", "*", "*", "*", "*");
		t1.addAction(new AzConfigAction(new Parameter("P1","V1",Parameter.Type.String)) {
			/* (non-Javadoc)
			 * @see lbms.plugins.azcron.actions.ConfigAction#run()
			 */
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("T1 Action 1; "+parameter);
			}
		});
		AzTask t2 = new AzTask("Task 2", "*/2", "*", "*", "*", "*");
		ArrayList<Parameter> params = new ArrayList<Parameter>();
		params.add(new Parameter("Param","Value",Parameter.Type.Double));
		params.add(new Parameter("Param2","Value2",Parameter.Type.Long));
		params.add(new Parameter("Param3","Value3",Parameter.Type.Boolean));
		t2.addAction(new IPCAction("ScaneRSS", "publicTestMethod", params) {

			/* (non-Javadoc)
			 * @see lbms.plugins.azcron.actions.IPCAction#run()
			 */
			@Override
			public void run() {
				try {
					Thread.sleep(15000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("T2 Action 1");
			}
		});
		AzTask t3 = new AzTask("Task 3", "*/3", "*", "*", "*", "*");
		t3.addAction(new RestartAction() {
			/* (non-Javadoc)
			 * @see lbms.plugins.azcron.actions.RestartAction#run()
			 */
			@Override
			public void run() {
				System.out.println("T3 RestartAction");
			}
		});
		t3.addAction(new StartStopAction(false) {
			/* (non-Javadoc)
			 * @see lbms.plugins.azcron.actions.StartStopAction#run()
			 */
			@Override
			public void run() {
				System.out.println("T3 StartStopAction");
			}
		});

		service.addTask(t1);
		service.addTask(t2);
		service.addTask(t3);

	/*	try {
			new XMLOutputter(Format.getPrettyFormat()).output(service.toElement(), System.out);		//Response
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

		System.out.println();
		System.out.println();

		service.start();
		try {
			Thread.sleep(300000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

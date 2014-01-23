package lbms.plugins.azcron.azureus.service;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.jdom.Element;

import lbms.plugins.azcron.actions.ActionSyntaxException;
import lbms.plugins.azcron.azureus.AzCronPlugin;
import lbms.plugins.azcron.azureus.actions.AzConfigAction;
import lbms.plugins.azcron.azureus.actions.AzIPCAction;
import lbms.plugins.azcron.azureus.actions.AzJythonAction;
import lbms.plugins.azcron.azureus.actions.AzPauseResumeAction;
import lbms.plugins.azcron.azureus.actions.AzRestartAction;
import lbms.plugins.azcron.azureus.actions.AzSpeedAction;
import lbms.plugins.azcron.azureus.actions.AzStartStopAction;
import lbms.plugins.azcron.service.Task;

/**
 * @author Damokles
 *
 */
public class AzTask extends Task implements Runnable, Comparable<AzTask> {


	protected boolean debug = true;

	private boolean[] bMinutes	= new boolean[60];
	private boolean[] bHours	= new boolean[24];
	private boolean[] bMonths	= new boolean[12];
	private boolean[] bDaysOfWeek	= new boolean[7];
	private boolean[] bDaysOfMonth	= new boolean[31];

	private long nextExecutionTime;

	/**
	 * @param id
	 * @param name
	 * @param deamon
	 * @param minutes
	 * @param hours
	 * @param months
	 * @param daysOfWeek
	 * @param daysOfMonth
	 */
	public AzTask(String name, 	 String minutes,
			String hours, String months, String daysOfWeek, String daysOfMonth) {
		super(name, minutes, hours, months, daysOfWeek, daysOfMonth);

		init();
	}

	/**
	 * @param e
	 */
	protected AzTask(Element e) {
		super(e);
		actions.clear();
		List<Element> elems = e.getChildren();
		for (Element ele:elems) {
			try {
				if (ele.getName().equals("ConfigAction")) {
					actions.add(AzConfigAction.createFromElement(ele));
				} else if (ele.getName().equals("RestartAction")) {
					actions.add(AzRestartAction.createFromElement(ele));
				} else if (ele.getName().equals("StartStopAction")) {
					actions.add(AzStartStopAction.createFromElement(ele));
				} else if (ele.getName().equals("PauseResumeAction")) {
					actions.add(AzPauseResumeAction.createFromElement(ele));
				} else if (ele.getName().equals("IPCAction")) {
					actions.add(AzIPCAction.createFromElement(ele));
				} else if (ele.getName().equals("JythonAction")) {
					actions.add(AzJythonAction.createFromElement(ele));
				} else if (ele.getName().equals("SpeedAction")) {
					actions.add(AzSpeedAction.createFromElement(ele));
				}
			} catch (ActionSyntaxException e1) {
				e1.printStackTrace();
			}
		}
		init();
	}

	private void init () {
		try {
			parseToken (minutes, bMinutes, false);
			parseToken (hours, bHours, false);
			parseToken (months, bMonths, true);
			parseToken (daysOfWeek, bDaysOfWeek, false);
			parseToken (daysOfMonth, bDaysOfMonth, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.service.Task#run()
	 */
	public void run() {

		if(isDisabled()) return;

		for (int i=0;i<actions.size();i++)
			actions.get(i).run();

		if (isPopupNotify()) {
			AzCronPlugin.getLogChannel().logAlertRepeatable(LoggerChannel.LT_INFORMATION,
					"Executed task: "+getTaskName());
		}

		if (isRunOnce())
			setDisabled(true);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(AzTask o) {
		long diff = getNextExecutionTime()-o.getNextExecutionTime();
		if ( diff > 0 )
			return 1;
		else if ( diff < 0 )
			return -1;
		else
			return 0;
	}

	public long getTimeUntilNextExecution() {
		return nextExecutionTime-System.currentTimeMillis();
	}

	public long getNextExecutionTime() {
		return nextExecutionTime;
	}


	//---adapted from jcrontab---

	/**
	 * Parses a token and fills the array of booleans that represents this
	 * CrontabEntryBean
	 * @param token String to parser usually smth like [ * , 2-3 , 2,3,4 ,4/5 ]
	 * @param arrayBool this array is the most efficient way to compare entries
	 * @param bBeginInOne says if the array begins in 0 or in 1
	 * @throws CrontabEntryException Error parsing the string
	 */

	public void parseToken(String token, boolean[] arrayBool, boolean bBeginInOne) throws Exception {

		int index;
		int each=1;
		try {

			// Look for step first
			index = token.indexOf("/");
			if(index > 0) {
				each = Integer.parseInt(token.substring(index + 1));
				if (each == 0) {
							throw new Exception(
						 "Never use expressions like */0 ");
				}
				token=token.substring(0,index);
			}

			if(token.equals("*")) {
				for(int i=0; i<arrayBool.length; i+=each) {
					arrayBool[i] = true;
				}
				return;
			}

			index = token.indexOf(",");
			if(index > 0) {
				StringTokenizer tokenizer = new StringTokenizer(token, ",");
				while (tokenizer.hasMoreElements()) {
					parseToken(tokenizer.nextToken(), arrayBool, bBeginInOne);
				}
				return;
			}

			index = token.indexOf("-");
			if(index > 0) {
				int start = Integer.parseInt(token.substring(0, index));
				int end = Integer.parseInt(token.substring(index + 1));

				if(bBeginInOne) {
					start--;
					end--;
				}
				for(int j=start; j<=end; j+=each)
					arrayBool[j] = true;
				return;
			}

				int iValue = Integer.parseInt(token);
				if(bBeginInOne) {
					iValue--;
				}
				arrayBool[iValue] = true;
				return;
		} catch (Exception e) {
			throw new Exception( "Smth was wrong with " + token, e);
		}
	}

	protected long calculateNextExcecution() {
		if (nextExecutionTime <= System.currentTimeMillis()) {
			nextExecutionTime = calculateNextExcecution(System.currentTimeMillis()+10000);
		}
		return nextExecutionTime;
	}

	/**This method builds a Date from a CrontabEntryBean and from a starting
	 * Date
	 * @return Date
	 * @param ceb CrontabEntryBean
	 * @param afterDate Date
	 */
	private long calculateNextExcecution(long afterDate) {

		Calendar after = Calendar.getInstance();
		after.setTimeInMillis(afterDate);


		int minute = getNextIndex(bMinutes, after.get(Calendar.MINUTE)+1);
		if (minute == -1) {
			minute = getNextIndex(bMinutes, 0);
			after.add(Calendar.HOUR_OF_DAY, 1);
		}

		int hour = getNextIndex(bHours, after.get(Calendar.HOUR_OF_DAY));
		if (hour == -1) {
			minute = getNextIndex(bMinutes, 0);
			hour = getNextIndex(bHours, 0);
			after.add(Calendar.DAY_OF_MONTH, 1);
		}

		int dayOfMonth = getNextIndex(bDaysOfMonth, after.get(Calendar.DAY_OF_MONTH));
		if (dayOfMonth == -1) {
			minute = getNextIndex(bMinutes, 0);
			hour = getNextIndex(bHours, 0);
			dayOfMonth = getNextIndex(bDaysOfMonth, 0);
			after.add(Calendar.MONTH, 1);
		}

		boolean dayMatchRealDate = false;
		while (!dayMatchRealDate) {
			if (checkDayValidInMonth(dayOfMonth + 1, after.get(Calendar.MONTH),
							after.get(Calendar.YEAR))) {
				dayMatchRealDate = true;
			} else {
				after.add(Calendar.MONTH, 1);
			}
		}

		int month = getNextIndex(bMonths, after.get(Calendar.MONTH));
		if (month == -1) {
			minute = getNextIndex(bMinutes, 0);
			hour = getNextIndex(bHours, 0);
			dayOfMonth = getNextIndex(bDaysOfMonth, 0);
			month = getNextIndex(bMonths, 0);
			after.add(Calendar.YEAR, 1);
		}

		Calendar calendar = Calendar.getInstance();
		calendar.set(after.get(Calendar.YEAR), month, dayOfMonth, hour, minute, 0);


		 if (bDaysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1]) {
			 if (debug) {
				 System.out.println(taskName+" next Execution on "+new Date(calendar.getTimeInMillis()) + " now: "+new Date());
			 }
			 return calendar.getTimeInMillis();
		 } else {
			 calendar.add(Calendar.DAY_OF_YEAR, 1);
			 return calculateNextExcecution(calendar.getTimeInMillis());
		 }
	}

	/**
	 * This method says wich is next index of this array
	 * @param array the list of booleans to check
	 * @param start int the id where starts the search
	 * @return index int
	 */
	private int getNextIndex(boolean[] array, int start) {
		for (int i = start; i < array.length; i++) {
			if (array[i]) return i;
		}
		return -1;
	}

	/**
	 * This says if this month has this day or not, basically this problem
	 * occurrs with 31 days in months with less days.
	 * @thanks to Javier Pardo :-)
	 * @param day int the day so see if exists or not
	 * @param month int the month to see it has this day or not.
	 * @param year to see if valid ... to work with 366 days years and February
	 * :-)
	 */
	private boolean checkDayValidInMonth(int day, int month, int year) {
		try {
			Calendar cl = Calendar.getInstance();
			cl.setLenient(false);
			cl.set(Calendar.DAY_OF_MONTH, day);
			cl.set(Calendar.MONTH, month);
			cl.set(Calendar.YEAR, year);
			cl.getTime();
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.service.Task#setDaysOfMonth(java.lang.String)
	 */
	@Override
	public void setDaysOfMonth(String daysOfMonth) throws Exception {
		super.setDaysOfMonth(daysOfMonth);
		Arrays.fill(bDaysOfMonth, false);
		parseToken (daysOfMonth, bDaysOfMonth, true);
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.service.Task#setDaysOfWeek(java.lang.String)
	 */
	@Override
	public void setDaysOfWeek(String daysOfWeek) throws Exception {
		super.setDaysOfWeek(daysOfWeek);
		Arrays.fill(bDaysOfWeek, false);
		parseToken (daysOfWeek, bDaysOfWeek, false);
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.service.Task#setHours(java.lang.String)
	 */
	@Override
	public void setHours(String hours) throws Exception {
		super.setHours(hours);
		Arrays.fill(bHours, false);
		parseToken (hours, bHours, false);
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.service.Task#setMinutes(java.lang.String)
	 */
	@Override
	public void setMinutes(String minutes) throws Exception {
		super.setMinutes(minutes);
		Arrays.fill(bMinutes, false);
		parseToken (minutes, bMinutes, false);
	}

	/* (non-Javadoc)
	 * @see lbms.plugins.azcron.service.Task#setMonths(java.lang.String)
	 */
	@Override
	public void setMonths(String months) throws Exception {
		super.setMonths(months);
		Arrays.fill(bMonths, false);
		parseToken (months, bMonths, true);
	}


}

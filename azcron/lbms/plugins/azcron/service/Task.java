package lbms.plugins.azcron.service;

import java.util.ArrayList;
import java.util.List;

import lbms.plugins.azcron.actions.Action;
import lbms.plugins.azcron.actions.ActionSyntaxException;
import lbms.plugins.azcron.actions.ConfigAction;
import lbms.plugins.azcron.actions.IPCAction;
import lbms.plugins.azcron.actions.JythonAction;
import lbms.plugins.azcron.actions.PauseResumeAction;
import lbms.plugins.azcron.actions.RestartAction;
import lbms.plugins.azcron.actions.SpeedAction;
import lbms.plugins.azcron.actions.StartStopAction;

import org.jdom.Element;

/**
 * This Class represents a Task to be performed.
 * Some of this code was adapted from http://sourceforge.net/projects/jcrontab/
 * 
 * @author Damokles
 *
 */
public class Task {

	public static final Task[] EMPTY_ARRAY = new Task[0];

	protected String taskName;
	protected List<Action> actions = new ArrayList<Action>();

	protected String minutes		= "0";
	protected String hours			= "*";
	protected String months			= "*";
	protected String daysOfWeek		= "*";
	protected String daysOfMonth	= "*";
	private EditMode editMode		= EditMode.None;
	private boolean disabled, runOnce, popupNotify;

	public Task(String name, String minutes, String hours, String months, String daysOfWeek, String daysOfMonth) {
		this.taskName = name;
		this.minutes = minutes;
		this.hours = hours;
		this.months = months;
		this.daysOfWeek = daysOfWeek;
		this.daysOfMonth = daysOfMonth;
		this.disabled = false;
		this.runOnce = false;
		this.popupNotify = false;
	}

	public Task (Element e) {
		this.taskName = e.getAttributeValue("taskName");
		this.minutes = e.getAttributeValue("minutes");
		this.hours = e.getAttributeValue("hours");
		this.months = e.getAttributeValue("months");
		this.daysOfWeek = e.getAttributeValue("daysOfWeek");
		this.daysOfMonth = e.getAttributeValue("daysOfMonth");
		this.editMode = EditMode.valueOf(e.getAttributeValue("editMode"));
		this.disabled = Boolean.parseBoolean(e.getAttributeValue("disabled"));
		this.runOnce = Boolean.parseBoolean(e.getAttributeValue("runOnce"));;
		this.popupNotify = Boolean.parseBoolean(e.getAttributeValue("popupNotify"));;
	}

	public Task (Element e, boolean withActions) {
		this(e);
		if (withActions) {
			List<Element> elems = e.getChildren();
			for (Element ele:elems) {
				try {
					if (ele.getName().equals("ConfigAction")) {
						actions.add(ConfigAction.createFromElement(ele));
					} else if (ele.getName().equals("RestartAction")) {
						actions.add(RestartAction.createFromElement(ele));
					} else if (ele.getName().equals("StartStopAction")) {
						actions.add(StartStopAction.createFromElement(ele));
					} else if (ele.getName().equals("PauseResumeAction")) {
						actions.add(PauseResumeAction.createFromElement(ele));
					} else if (ele.getName().equals("IPCAction")) {
						actions.add(IPCAction.createFromElement(ele));
					} else if (ele.getName().equals("JythonAction")) {
						actions.add(JythonAction.createFromElement(ele));
					} else if (ele.getName().equals("SpeedAction")) {
						actions.add(SpeedAction.createFromElement(ele));
					}
				} catch (ActionSyntaxException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public Element toElement() {
		Element e = new Element ("Task");
		e.setAttribute("taskName", taskName);
		e.setAttribute("minutes", minutes);
		e.setAttribute("hours", hours);
		e.setAttribute("months", months);
		e.setAttribute("daysOfWeek", daysOfWeek);
		e.setAttribute("daysOfMonth", daysOfMonth);
		e.setAttribute("editMode",editMode.name());
		if (disabled)
			e.setAttribute("disabled", Boolean.toString(disabled));
		if (runOnce)
			e.setAttribute("runOnce", Boolean.toString(runOnce));
		if (popupNotify)
			e.setAttribute("popupNotify", Boolean.toString(popupNotify));
		for (int i=0;i<actions.size();i++)
			e.addContent(actions.get(i).toElement());
		return e;
	}

	public void addAction (Action a) {
		actions.add(a);
	}

	public void removeAction (Action a) {
		actions.remove(a);
	}

	public Action[] getActions () {
		return actions.toArray(Action.EMPTY_ARRAY);
	}

	/**
	 * @return the daysOfMonth
	 */
	public String getDaysOfMonth() {
		return daysOfMonth;
	}

	/**
	 * @param daysOfMonth the daysOfMonth to set
	 */
	public void setDaysOfMonth(String daysOfMonth) throws Exception {
		this.daysOfMonth = daysOfMonth;
	}

	/**
	 * @return the daysOfWeek
	 */
	public String getDaysOfWeek() {
		return daysOfWeek;
	}

	/**
	 * @param daysOfWeek the daysOfWeek to set
	 */
	public void setDaysOfWeek(String daysOfWeek) throws Exception {
		this.daysOfWeek = daysOfWeek;
	}

	/**
	 * @return the hours
	 */
	public String getHours() {
		return hours;
	}

	/**
	 * @param hours the hours to set
	 */
	public void setHours(String hours) throws Exception {
		this.hours = hours;
	}

	/**
	 * @return the minutes
	 */
	public String getMinutes() {
		return minutes;
	}

	/**
	 * @param minutes the minutes to set
	 */
	public void setMinutes(String minutes) throws Exception {
		this.minutes = minutes;
	}

	/**
	 * @return the months
	 */
	public String getMonths() {
		return months;
	}

	/**
	 * @param months the months to set
	 */
	public void setMonths(String months) throws Exception {
		this.months = months;
	}

	/**
	 * @return the taskName
	 */
	public String getTaskName() {
		return taskName;
	}

	/**
	 * @param taskName the taskName to set
	 */
	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	/**
	 * @return the editMode
	 */
	public EditMode getEditMode() {
		return editMode;
	}

	/**
	 * @param editMode the editMode to set
	 */
	public void setEditMode(EditMode editMode) {
		this.editMode = editMode;
	}


	/**
	 * @return the disabled
	 */
	public boolean isDisabled() {
		return disabled;
	}

	/**
	 * @param disabled the disabled to set
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * @return the runOnce
	 */
	public boolean isRunOnce() {
		return runOnce;
	}

	/**
	 * @param runOnce the runOnce to set
	 */
	public void setRunOnce(boolean runOnce) {
		this.runOnce = runOnce;
	}

	/**
	 * @return the popupNotify
	 */
	public boolean isPopupNotify() {
		return popupNotify;
	}

	/**
	 * @param popupNotify the popupNotify to set
	 */
	public void setPopupNotify(boolean popupNotify) {
		this.popupNotify = popupNotify;
	}

	public enum EditMode {
		None, Expert, Basic_Hour, Basic_Day, Basic_Week, Basic_Month
	}
}

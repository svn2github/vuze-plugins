package lbms.plugins.azcron.main.editors;

import lbms.plugins.azcron.actions.Action;

public interface ActionEditor {
	public Action getAction() throws InvalidActionException;
	public void setAction(Action a) throws InvalidActionException;
}

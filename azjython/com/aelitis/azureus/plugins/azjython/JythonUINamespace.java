/**
 * 
 */
package com.aelitis.azureus.plugins.azjython;

/**
 * @author Allan Crooks
 *
 */
public class JythonUINamespace {
	
	// This is used to store selected items from the context menu.
	private Object[] selected = new Object[0];

	public void setSelectedItems(Object[] o) {
		this.selected = o;
	}
	
	public Object[] getSelectedItems() {
		return selected;
	}

}

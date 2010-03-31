/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.utils;

/**
 * @author Allan Crooks
 *
 */
public abstract class SimpleEnum {
	private String text;
	protected SimpleEnum(String text) {this.text = text;}
	public String toString() {
		String class_name = this.getClass().getName(); 
		return class_name.substring(class_name.lastIndexOf(".")+1) + "::" + text;
	}
}

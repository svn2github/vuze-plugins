/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

/**
 * @author Allan Crooks
 *
 */
public class OutputContextString {
	public String text;
	public ConsoleOutputDescriptor type;
	
	public OutputContextString(String text, ConsoleOutputDescriptor type) {
		if (type == null) {throw new NullPointerException("type is null");}
		this.text = text;
		this.type = type;
	}
}

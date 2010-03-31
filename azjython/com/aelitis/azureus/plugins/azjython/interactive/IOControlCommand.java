/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;
import com.aelitis.azureus.plugins.azjython.utils.SimpleEnum;

/**
 * @author Allan Crooks
 *
 */
public class IOControlCommand extends SimpleEnum {
	private IOControlCommand(String command_name) {super(command_name);}
	public static final IOControlCommand ENSURE_NEW_LINE = new IOControlCommand("ENSURE_NEW_LINE");
}

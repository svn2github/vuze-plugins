/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;
import com.aelitis.azureus.plugins.azjython.utils.SimpleEnum;

/**
 * @author Allan Crooks
 *
 */
public class ConsoleOutputDescriptor extends SimpleEnum {
	private ConsoleOutputDescriptor(String command_name) {super(command_name);}
	public static ConsoleOutputDescriptor STDIN = new ConsoleOutputDescriptor("STDIN");
	public static ConsoleOutputDescriptor STDOUT = new ConsoleOutputDescriptor("STDOUT");
	public static ConsoleOutputDescriptor STDERR = new ConsoleOutputDescriptor("STDERR");
	public static ConsoleOutputDescriptor STDOUT_UNKNOWN = new ConsoleOutputDescriptor("STDOUT");
	public static ConsoleOutputDescriptor STDERR_UNKNOWN = new ConsoleOutputDescriptor("STDERR");
	public static ConsoleOutputDescriptor CONSOLE_INFO = new ConsoleOutputDescriptor("CONSOLE_INFO");
	public static ConsoleOutputDescriptor PROMPT = new ConsoleOutputDescriptor("PROMPT");
	public static ConsoleOutputDescriptor UNPRINTED_STDIN = new ConsoleOutputDescriptor("UNPRINTED_STDIN");
	
	public static ConsoleOutputDescriptor asUnknown(ConsoleOutputDescriptor cd) {
		if (cd == null) {throw new NullPointerException();}
		if (cd == STDOUT) {return STDOUT_UNKNOWN;}
		if (cd == STDERR) {return STDERR_UNKNOWN;}
		return cd;
	}
	
}

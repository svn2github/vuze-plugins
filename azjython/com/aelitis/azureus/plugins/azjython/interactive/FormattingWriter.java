/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Allan Crooks
 *
 */
public abstract class FormattingWriter extends Writer {
	
	private static class DefaultOutputFormatter implements OutputFormatter {
		public Object format(String result) {return result;}
	}
	
	protected abstract void handleFormattedObject(Object o);
	private OutputFormatter of;	
	
	public FormattingWriter(OutputFormatter cof) {
		this.of = (cof == null) ? new DefaultOutputFormatter() : cof;
	}

	public void close() {}
	public void flush() throws IOException {}
	public void write(char[] arg0, int arg1, int arg2) {
		Object result = of.format(new String(arg0, arg1, arg2));
		if (result != null) {
			this.handleFormattedObject(result);
		}
	}

}

/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

import com.aelitis.azureus.plugins.azjython.utils.DataSink;
import com.aelitis.azureus.plugins.azjython.utils.FilteringDataSink;

/**
 *
 */
public class IdentifiedIODataSink extends FilteringDataSink {
	
	private BasicThreadedConsole console;
	
	public IdentifiedIODataSink(DataSink sink, BasicThreadedConsole console) {
		super(sink);
		this.console = console;
	}

	public Object filter(Object o) {
		
		Thread[] console_threads = this.console.getConsoleThreads(); 
		if (console_threads[0] == null) {
			throw new RuntimeException("already filtering, but console thread not initialised yet");
		}
		
		IdentifiedIO iio = (IdentifiedIO)o;
		
		// This object belongs to this console.
		if (iio.thread == console_threads[0]) {return iio.object;}
		
		// This object belongs to a different console, ignore it.
		if (iio.thread instanceof BasicThreadedConsole.ConsoleThread) {return null;}
		
		// This object doesn't indicate where it came from. In that case,
		// alter the descriptor (if it has one) to indicate that it is unknown.
		//
		// The receiving interpreter can decide what to do with it.
		if (iio.object instanceof OutputContextString) {
			OutputContextString orig_ocs = (OutputContextString)iio.object;
			OutputContextString new_ocs = new OutputContextString(orig_ocs.text, ConsoleOutputDescriptor.asUnknown(orig_ocs.type)); 
			return new_ocs;
		}
		
		// Doesn't have a descriptor, just return the orignal object.
		return iio.object;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof IdentifiedIODataSink)) {return false;}
		IdentifiedIODataSink other = (IdentifiedIODataSink)o;
		if (this.console != other.console) {return false;}
		if (this.sink != other.sink) {return false;}
		return true;
	}

}

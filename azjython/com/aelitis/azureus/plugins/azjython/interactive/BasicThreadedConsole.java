/**
 * 
 */
package com.aelitis.azureus.plugins.azjython.interactive;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.aelitis.azureus.plugins.azjython.JythonPluginCore;
import com.aelitis.azureus.plugins.azjython.utils.DataSink;
import com.aelitis.azureus.plugins.azjython.utils.MultisourceDataSink;
import com.aelitis.azureus.plugins.azjython.utils.ProxyDataSink;
import com.aelitis.azureus.plugins.azjython.utils.WorkQueue;
import org.python.core.PyObject;
import org.python.core.Py;
import org.python.core.PyClass;
import org.python.core.PyException;
import org.python.core.PyFile;
import org.python.core.PySyntaxError;
import org.python.core.PyTuple;
import org.python.core.ThreadState;
import org.python.util.InteractiveConsole;

/**
 * @author Allan Crooks
 *
 */
public class BasicThreadedConsole extends InteractiveConsole {

	// Default constructors.
	private JythonPluginCore core;
	private String name;
	private ProxyDataSink outgoing;
	
	// PrintWriter objects.
	public PrintWriter pw_stdout = null;
	public PrintWriter pw_stderr = null;
	
	public BasicThreadedConsole(JythonPluginCore core, String name) {
		this.core = core;
		this.name = name;
		this.outgoing = new ProxyDataSink();
		this.setupOutputStreams();
	}
	
	// The threaded queues we work with containing data from "stdin".
	private WorkQueue incoming = new WorkQueue();
	
	// Call this upon exit.
	private Runnable terminate_hook = null;
	
	private boolean has_started = false;
	private boolean has_finished = false;
	public boolean hasStarted() {return has_started;}
	public boolean hasFinished() {return has_finished;}
	
	public void setDataSink(DataSink sink) {
		this.outgoing.setDelegate(sink);
	}
	
	public void setTerminateHook(Runnable r) {
		this.terminate_hook = r; 
	}

	/**
	 * This expects a line not terminated by a newline.
	 * @param s
	 */
	public void putInput(String s) {
		if (s != null) { 
			outgoing.put(STD_IN_FMT.format(s + "\n")); // We want to display the output.
		}
		incoming.put(s);
	}
	
	/**
	 * This expects lines not terminated by a newline.
	 * @return
	 */
	public void putDelayedInput(String[] s) {
		synchronized (incoming) {
			for (int i=0; i<s.length; i++) {
				incoming.put(DELAYED_STD_IN_FMT.format(s[i]));
			}
		}
	}
	
	// Private methods for pushing and pulling information.
	private Object getInput() {
		while (true) {
			try {return incoming.get();}
			catch (InterruptedException ie) {}
		}
	}

	
	// These are the threads that we create.
	private Thread main_thread = null; 
		
	public Thread[] getConsoleThreads() {
		return new Thread[] {main_thread};
	}
	
	public String raw_input(PyObject prompt) {
		this.outgoing.put(IOControlCommand.ENSURE_NEW_LINE);
		this.outgoing.put(PROMPT_FMT.format(prompt.__str__().toString()));
		Object o_result = this.getInput();
		String s_result;
		if (o_result instanceof OutputContextString) {
			OutputContextString ocs = (OutputContextString)o_result;
			s_result = ocs.text;
			if (ocs.type == ConsoleOutputDescriptor.UNPRINTED_STDIN) {
				this.outgoing.put(STD_IN_FMT.format(ocs.text + "\n"));
			}
		}
		else {
			s_result = (String)o_result;
		}
		
		if (s_result == null) {
			this.consume_newline_hack = true;
			throw Py.EOFError("no more input");
		}

		return s_result;
	}
	
	public static String getDefaultBanner(JythonPluginCore core) {
		String result = InteractiveConsole.getDefaultBanner();
		String version = core.plugin_interface.getPluginVersion();
		if (version != null) {
			result += " [AzJython " + version + "]";
		}
		return result;
	}
	
	private static ConsoleOutputFormatter CON_INFO_FMT = new ConsoleOutputFormatter(ConsoleOutputDescriptor.CONSOLE_INFO);
	private static ConsoleOutputFormatter PROMPT_FMT = new ConsoleOutputFormatter(ConsoleOutputDescriptor.PROMPT);
	private static ConsoleOutputFormatter STD_IN_FMT = new ConsoleOutputFormatter(ConsoleOutputDescriptor.STDIN);
	private static ConsoleOutputFormatter DELAYED_STD_IN_FMT = new ConsoleOutputFormatter(ConsoleOutputDescriptor.UNPRINTED_STDIN);
	private static ConsoleOutputFormatter STD_OUT_FMT = new ConsoleOutputFormatter(ConsoleOutputDescriptor.STDOUT);
	private static ConsoleOutputFormatter STD_ERR_FMT = new ConsoleOutputFormatter(ConsoleOutputDescriptor.STDERR);

	// Specialised output streams.
	public static PrintWriter shared_std_out = null;
	public static PrintWriter shared_std_err = null;
	
	private static boolean output_streams_initialised = false;
	private static MultisourceDataSink multi_sink = null;
	private void setupOutputStreams() {
		
		/**
		 * We append this sink to the "multisink" attached to the shared
		 * PrintWriter. We must make sure that we unwrap all identified IO.
		 */
		DataSink out_sink = new IdentifiedIODataSink(this.outgoing, this);
		
		/**
		 * We share a reference to the raw streams (in PrintWriter form) to
		 * allow them to be available on the environment object.	
		 */
		this.pw_stdout = new PrintWriter(new FormattedConsoleWriter(STD_OUT_FMT, this.outgoing));
		this.pw_stderr = new PrintWriter(new FormattedConsoleWriter(STD_ERR_FMT, this.outgoing));
		
		// We only set up the shared print writer once.
		if (output_streams_initialised) {
			multi_sink.addSink(out_sink);
			return;
		}
		
		multi_sink = new MultisourceDataSink();
		multi_sink.addSink(out_sink);
		
		FallbackIdentifiedDataSink jython_sink = new FallbackIdentifiedDataSink();
		jython_sink.setDelegate(multi_sink);
		output_streams_initialised = true;

		// Set up "stdout" and "stderr".
		FormattedConsoleWriter stdout, stderr;
		stdout = new FormattedConsoleWriter(STD_OUT_FMT, jython_sink);
		stderr = new FormattedConsoleWriter(STD_ERR_FMT, jython_sink);
		shared_std_out = new PrintWriter(stdout);
		shared_std_err = new PrintWriter(stderr);
		this.setOut(shared_std_out);
		this.setErr(shared_std_err);
	}
	
	public void startInteracting() {startInteracting(InteractiveConsole.getDefaultBanner());}
	public void startInteracting(final String banner) {
		this.main_thread = new ConsoleThread(banner);
		this.main_thread.start();
		core.logger.log(name + " started.");
	}
	
	public void exit() {
		this.incoming.destroy();
		this.interruptExec();
	}
	
	public void interruptExec() {
		ThreadState ts = new ThreadState(this.main_thread, this.systemState);
		this.interrupt(ts);
	}
	
	public void showexception(PyException exc)  {
		StringWriter sw = new StringWriter(); 
		Py.displayException(exc.type, exc.value, exc.traceback, new PyFile(sw));
		outgoing.put(STD_ERR_FMT.format(fixNewLines(sw.toString())));
	}
	
	private List startup_scripts = new ArrayList();
	
	public void runScriptOnStartup(InputStream script_content, String name, String pre_init, String post_init) {
		StartupScriptData ssd = new StartupScriptData();
		ssd.script_content = script_content;
		ssd.script_name = name;
		ssd.pre_init = pre_init;
		ssd.post_init = post_init;
		startup_scripts.add(ssd);
	}
	
	private boolean consume_newline_hack = false;
	public void write(String data) {
		if (this.consume_newline_hack && "\n".equals(data)) {
			this.consume_newline_hack = false;
			return;
		}
		super.write(data);
	}
	
	private void initialiseConsole(String banner) {

		// Put banner there.
		outgoing.put(CON_INFO_FMT.format(banner));
		
		// Startup script initialisation.
		Iterator itr = startup_scripts.iterator();
		while (itr.hasNext()) {
			this.execScript((StartupScriptData)itr.next());
		}
	}
	
	private void execScript(StartupScriptData ssd) {
		boolean init_ok = true;
		if (ssd.pre_init != null) {
			outgoing.put(CON_INFO_FMT.format(ssd.pre_init));
			outgoing.put(IOControlCommand.ENSURE_NEW_LINE);
		}
		try {execfile(ssd.script_content, ssd.script_name);}
		catch (PyException pe) {
			/**
			 * Working around a problem that I can't quite figure out at the
			 * moment - SyntaxErrors that occur here won't get rendered correctly.
			 * 
			 * I think it is because the SyntaxError has a value of a tuple here,
			 * but the Py.displayException code wants an instance of SyntaxError.
			 * 
			 * We'll basically fake the error here.
			 */ 
			if (pe instanceof PySyntaxError) {
				if (pe.value instanceof PyTuple) {
					// Nice and hacky. :)
					set("_original_exception_data", pe.value);
					exec("_new_exception_data = SyntaxError(*_original_exception_data)");
					pe = new PyException(pe.type, get("_new_exception_data"), pe.traceback);
					exec("del _new_exception_data");
				}
			}
			outgoing.put(CON_INFO_FMT.format(core.locale_utils.getLocalisedMessageText("azjython.interpreter.script_init_error")));
			outgoing.put(IOControlCommand.ENSURE_NEW_LINE);
			showexception(pe);
			init_ok = false;
		}
		if (ssd.post_init != null && init_ok) {
			outgoing.put(CON_INFO_FMT.format(ssd.post_init));
		}
		outgoing.put(IOControlCommand.ENSURE_NEW_LINE);
		try {ssd.script_content.close();}
		catch (IOException ioe) {
			// We don't care too much if we can't close the stream.
		}		
	}
	
	private static String fixNewLines(String s) {
		String good_nl = "\n";
		String bad_nl = System.getProperty("line.separator");
		if (bad_nl.equals(good_nl)) {return s;}
		StringBuffer sb = new StringBuffer(s);
		int last_newline_pos = sb.lastIndexOf(bad_nl);
		while (last_newline_pos != -1) {
			sb.replace(last_newline_pos, last_newline_pos+bad_nl.length(), good_nl);
			last_newline_pos = sb.lastIndexOf(bad_nl, last_newline_pos);
		}
		return sb.toString();
	}

	private static class ConsoleOutputFormatter implements OutputFormatter {
		private ConsoleOutputDescriptor desc;
		public ConsoleOutputFormatter(ConsoleOutputDescriptor desc) {this.desc = desc;}
		public Object format(String s) {return new OutputContextString(s, desc);}
	}
	
	private class FormattedConsoleWriter extends FormattingWriter {
		private DataSink sink;
		public FormattedConsoleWriter(OutputFormatter cof, DataSink sink) {
			super(cof);
			this.sink = sink;
		}
		protected void handleFormattedObject(Object o) {this.sink.put(o);}
	}
	
	private class StartupScriptData {
		public InputStream script_content;
		public String script_name;
		public String pre_init;
		public String post_init;
	}
	
	public class ConsoleThread extends Thread {
		public BasicThreadedConsole console;
		private String banner;
		
		public ConsoleThread(String banner) {
			super();
			this.console = BasicThreadedConsole.this;
			this.banner = banner;
			this.setName("jython-console thread for " + console.name);
			this.setDaemon(true);
		}
		
		public void run() {
			has_started = true;
			
			BasicThreadedConsole.this.initialiseConsole(banner);
			
			String con_name = BasicThreadedConsole.this.name;
			try {
				BasicThreadedConsole.this.interact(null); // XXX: fix
				core.logger.log(con_name + " exited normally.");
			}
			catch (PyException pe) {
				boolean is_sys_exit = pe.type instanceof PyClass && 
					((PyClass)pe.type).__name__.equals("SystemExit");
				
				if (is_sys_exit) {
					core.logger.log(con_name + " terminated via SystemExit: " + pe.value.__str__().toString());
				}
				else {
					core.logger.log("Unexpected termination of " + con_name, pe);
					throw pe;
				}
				
			}
			finally {

				has_finished = true;
				
				// Call termination hook.
				if (BasicThreadedConsole.this.terminate_hook != null) {
					BasicThreadedConsole.this.terminate_hook.run();
				}
				
				// Does this cause problems with multiple interpreters?
				BasicThreadedConsole.this.cleanup();

			} // end finally
		} // end method
	} // end class
	
	public void cleanup() {
		super.cleanup();
		multi_sink.delSink(new IdentifiedIODataSink(this.outgoing, this));
	}

}

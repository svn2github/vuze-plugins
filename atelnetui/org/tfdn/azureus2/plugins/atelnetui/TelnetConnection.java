package org.tfdn.azureus2.plugins.atelnetui;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Vector;

import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.utils.LocaleUtilities;

/**
 * This is the most important class of the plugin. This class interacts with the
 * remote client
 * 
 * @author Francois-Xavier Payet
 */
public class TelnetConnection extends Thread {

	private Socket _sock;

	private PluginInterface _iface;

	private boolean _active = true;

	private PluginConfig _config;

	private LocaleUtilities _local;

	private DownloadManager _manager;

	/**
	 * Creates the communication
	 * 
	 * @param sock
	 *            The socket of the communication
	 * @param iface
	 *            The interface with Azureus
	 */
	public TelnetConnection(Socket sock, PluginInterface iface) {
		_sock = sock;
		_iface = iface;
		_config = iface.getPluginconfig();
		_local = _iface.getUtilities().getLocaleUtilities();
		_manager = _iface.getDownloadManager();
		setDaemon(true);
		start();
	}

	/**
	 * @see Thread#run()
	 */
	public void run() {
		Scanner scan = null;
		PrintWriter out = null;

		// Authentification phase
		try {
			out = new PrintWriter(_sock.getOutputStream());
			scan = new Scanner(_sock.getInputStream());

			out.print(getLocalizedMessage("atelnetui.tln.enter_username"));
			out.flush();
			String username = scan.nextLine();
			EraserThread et = new EraserThread(out,
					getLocalizedMessage("atelnetui.tln.enter_password"));
			et.start();
			String password = scan.nextLine();
			et.stopMasking();
			if ((!username.equals(_config.getPluginStringParameter("username")))
					|| (!password.equals(new String(_config
							.getPluginByteParameter("password",
									(new String("")).getBytes()))))) {
				// Wrong login
				_active = false;
				out.println(getParam("wrong_password"));
				out.flush();
			} else {
				// Login OK
				out.println(getParam("welcome_message"));
				out.flush();
			}
		} catch (IOException e) {
			_active = false;
			e.printStackTrace();
		}

		// Active part
		while (_active) {
			out.print(getParam("prompt") + " ");
			out.flush();
			try {
				out.println(processCommand(scan.nextLine()));
			} catch (NoSuchElementException nsee) {
			}
			out.flush();
		}

		scan.close();
		try {
			_sock.close();
		} catch (IOException e) {
		}
	}

	private String processCommand(String command) {
		String ret = "";
		Scanner scan = new Scanner(command);
		String cmd = scan.next();
		Vector<String> opts = new Vector<String>();
		while (scan.hasNext()) {
			opts.add(scan.next());
		}
		if (cmd.equals("quit")) {
			ret = getParam("goodbye");
			_active = false;
		} else if (cmd.equals("show")) {
			Download[] down = _manager.getDownloads();
			if (opts.size() > 0)
				return getLocalizedMessage("atelnetui.tln.show.usage");
			for (Download d : down) {
				ret += d.getIndex() + "(";
				if (d.isComplete()) {
					ret += getLocalizedMessage("atelnetui.tln.show.finished");
				}
				if (d.getState() == Download.ST_STOPPED) {
					ret += " "
							+ getLocalizedMessage("atelnetui.tln.show.stopped");
				}
				ret += ") -- " + d.getName() + "\n";
			}
			ret += "\010";
		} else if (cmd.equals("details")) {
			Download[] downloads = _manager.getDownloads();
			if (opts.size() != 1) {
				return getLocalizedMessage("atelnetui.tln.details.usage");
			}
			int torrent = -1;
			try {
				torrent = (new Integer(opts.get(0))).intValue();
			} catch (NumberFormatException nfe) {
				return getLocalizedMessage("atelnetui.tln.details.usage");
			}
			if (torrent < 0 || torrent >= downloads.length)
				return getLocalizedMessage("atelnetui.tln.nosuchtorrent");
			Download myTorrent = downloads[torrent];
			ret += "\t" + getLocalizedMessage("atelnetui.tln.details.name")
					+ " : " + myTorrent.getName() + "\n";
			ret += "\t" + getLocalizedMessage("atelnetui.tln.details.status")
					+ ": " + myTorrent.getStats().getStatus() + "\n";
			ret += "\t"
					+ getLocalizedMessage("atelnetui.tln.details.availability")
					+ " : " + myTorrent.getStats().getAvailability() + "\n";
			ret += "\t"
					+ getLocalizedMessage("atelnetui.tln.details.percentage")
					+ " : "
					+ (float) (myTorrent.getStats().getCompleted() / 10)
					+ "%\n";
			ret += "\t" + getLocalizedMessage("atelnetui.tln.details.elapsed")
					+ " : " + myTorrent.getStats().getElapsedTime() + "\n";
			ret += "\t" + getLocalizedMessage("atelnetui.tln.details.eta")
					+ " : " + myTorrent.getStats().getETA() + "\n";
			ret += "\t"
					+ getLocalizedMessage("atelnetui.tln.details.shareratio")
					+ " : "
					+ (float) (myTorrent.getStats().getShareRatio() / 1000);
		} else if (cmd.equals("stop")) {
			Download[] downloads = _manager.getDownloads();
			if (opts.size() != 1) {
				return getLocalizedMessage("atelnetui.tln.stop.usage");
			}
			int torrent = -1;
			try {
				torrent = (new Integer(opts.get(0))).intValue();
			} catch (NumberFormatException nfe) {
				return getLocalizedMessage("atelnetui.tln.stop.usage");
			}
			if (torrent < 0 || torrent >= downloads.length) {
				return getLocalizedMessage("atelnetui.tln.nosuchtorrent");
			}
			if (downloads[torrent].getState() == Download.ST_STOPPED)
				return getLocalizedMessage("atelnetui.tln.stop.alreadystopped");
			try {
				downloads[torrent].stop();
				ret = getLocalizedMessage("atelnetui.tln.stop.stopped1") + " "
						+ downloads[torrent].getName() + " "
						+ getLocalizedMessage("atelnetui.tln.stop.stopped2");
			} catch (DownloadException e) {
				ret = getLocalizedMessage("atelnetui.tln.stop.error") + " "
						+ downloads[torrent].getName();
			}
		} else if (cmd.equals("start")) {
			Download[] downloads = _manager.getDownloads();
			if (opts.size() != 1) {
				return getLocalizedMessage("atelnetui.tln.start.usage");
			}
			int torrent = -1;
			try {
				torrent = (new Integer(opts.get(0))).intValue();
			} catch (NumberFormatException nfe) {
				return getLocalizedMessage("atelnetui.tln.start.usage");
			}
			if (torrent < 0 || torrent >= downloads.length) {
				return getLocalizedMessage("atelnetui.tln.nosuchtorrent");
			}
			int state = downloads[torrent].getState();
			switch (state) {
			case Download.ST_ERROR:
				return getLocalizedMessage("atelnetui.tln.start.torrenthaserror");
			case Download.ST_DOWNLOADING:
			case Download.ST_PREPARING:
			case Download.ST_QUEUED:
			case Download.ST_SEEDING:
			case Download.ST_STOPPING:
				return getLocalizedMessage("atelnetui.tln.start.cantstartthisstate");
			}
			try {
				downloads[torrent].restart();
				ret = getLocalizedMessage("atelnetui.tln.start.started1") + " "
						+ downloads[torrent].getName() + " "
						+ getLocalizedMessage("atelnetui.tln.start.started2");
			} catch (DownloadException e) {
				ret = getLocalizedMessage("atelnetui.tln.start.error")
						+ downloads[torrent].getName();
				e.printStackTrace();
			}
		} else if (cmd.equals("remove")) {
			Download[] downloads = _manager.getDownloads();
			boolean removeData = false;
			boolean removeTorrent = false;
			boolean torrentDefined = false;
			int torrent = -1;
			for (String opt : opts) {
				if (opt.equals("-t") && !removeTorrent) {
					removeTorrent = true;
				} else if (opt.equals("-d") && !removeData) {
					removeData = true;
				} else if ((opt.equals("-td") || opt.equals("-dt"))
						&& !removeData && !removeTorrent) {
					removeData = true;
					removeTorrent = true;
				} else if (!torrentDefined) {
					try {
						torrent = (new Integer(opt)).intValue();
						torrentDefined = true;
					} catch (NumberFormatException nfe) {
						return getLocalizedMessage("atelnetui.tln.remove.usage");
					}
				} else {
					return getLocalizedMessage("atelnetui.tln.remove.usage");
				}
			}
			if (!torrentDefined)
				return getLocalizedMessage("atelnetui.tln.remove.usage");
			if (torrent < 0 || torrent >= downloads.length)
				return getLocalizedMessage("atelnetui.tln.nosuchtorrent");
			if (downloads[torrent].getState() != Download.ST_STOPPED)
				return getLocalizedMessage("atelnetui.tln.remove.muststop");
			try {
				downloads[torrent].remove(removeTorrent, removeData);
				ret = getLocalizedMessage("atelnetui.tln.remove.removed1") + " " + downloads[torrent].getName() + " " + getLocalizedMessage("atelnetui.tln.remove.removed2");
			} catch (DownloadException e) {
				ret = getLocalizedMessage("atelnetui.tln.remove.error") + " "
						+ downloads[torrent].getName();
				e.printStackTrace();
			} catch (DownloadRemovalVetoException e) {
				ret = getLocalizedMessage("atelnetui.tln.remove.error") + " "
						+ downloads[torrent].getName() + " !!!";
				e.printStackTrace();
			}
		} else if (cmd.equals("help")) {
			if (opts.size() == 0) {
				ret = getLocalizedMessage("atelnetui.tln.help.basichelp1") + "\n";
				ret += getLocalizedMessage("atelnetui.tln.help.basichelp2");
			} else if (opts.size() == 1) {
				String helpCommand = opts.get(0);
				if (helpCommand.equals("help")) {
					ret = getLocalizedMessage("atelnetui.tln.help.usage") + "\n";
					ret += getLocalizedMessage("atelentui.tln.help.help");
				} else if (helpCommand.equals("show")) {
					ret = getLocalizedMessage("atelnetui.tln.show.usage") + "\n";
					ret += getLocalizedMessage("atelnetui.tln.show.help");
				} else if (helpCommand.equals("details")) {
					ret = getLocalizedMessage("atelnetui.tln.details.usage") + "\n";
					ret += getLocalizedMessage("atelnetui.tln.details.help");
				} else if (helpCommand.equals("stop")) {
					ret = getLocalizedMessage("atelnetui.tln.stop.usage") + "\n";
					ret += getLocalizedMessage("atelnetui.tln.stop.help");
				} else if (helpCommand.equals("start")) {
					ret = getLocalizedMessage("atelnetui.tln.start.usage") + "\n";
					ret += getLocalizedMessage("atelnetui.tln.start.help");
				} else if (helpCommand.equals("remove")) {
					ret = getLocalizedMessage("atelnetui.tln.remove.usage") + "\n";
					ret += getLocalizedMessage("atelnetui.tln.remove.help");
				} else {
					ret = getLocalizedMessage("atelnetui.tln.help.unknowncommand");
				}
			} else {
				ret = getLocalizedMessage("atelnetui.tln.help.usage");
			}
		} else {
			ret = getLocalizedMessage("atelnetui.tln.help.unknowncommand");
		}
		return ret;
	}

	/**
	 * Close the connection with the client
	 */
	public void quit() {
		_active = false;
		try {
			_sock.close();
		} catch (IOException e) {
		}
	}

	/**
	 * Return the localized version of a text who is not personnalizable
	 * 
	 * @param key
	 *            Key representing the text to translate
	 * @return Translated text
	 */
	private String getLocalizedMessage(String key) {
		return _local.getLocalisedMessageText(key);
	}

	/**
	 * Return rhe text from the preferences
	 * 
	 * @param key
	 *            The text to get
	 */
	private String getParam(String key) {
		return _config.getPluginStringParameter(key);
	}
}

/**
 * This class is used to mask the characters entered for the password. There's a
 * lot of work to do on this one...
 * 
 * @author Francois-Xavier Payet
 */
class EraserThread extends Thread {
	private boolean stop;

	private PrintWriter _out;

	/**
	 * @param out
	 *            The PrintWriter for the password prompt
	 * @parma prompt Text to show as prompt
	 */
	public EraserThread(PrintWriter out, String prompt) {
		setDaemon(true);
		_out = out;
		out.print(prompt);
		out.flush();
	}

	/**
	 * Begin masking...display asterisks (*)
	 */
	public void run() {
		stop = true;
		int priority = Thread.currentThread().getPriority();
		Thread.currentThread().setPriority(MAX_PRIORITY);
		try {
			stop = true;
			while (stop) {
				_out.print("\010*");
				_out.flush();
				Thread.sleep(1);
			}
		} catch (InterruptedException e) {
		} finally {
			Thread.currentThread().setPriority(priority);
		}
	}

	/**
	 * Instruct the thread to stop masking
	 */
	public void stopMasking() {
		this.stop = false;
	}
}

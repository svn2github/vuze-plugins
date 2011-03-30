package com.vuze.plugin.btapp;

import java.io.*;
import java.net.*;
import java.util.List;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;

import com.aelitis.azureus.core.util.HTTPUtils;

/**
 * Creates a socket, listens and responds to HTTP GET
 * 
 * @note This is mostly copied from {@link MagnetURIHandlerImpl}
 */
public class BtAppWebServ
	implements Runnable
{
	protected static final boolean DEBUG = true;

	protected static final String NL = "\015\012";

	private static final String PREFIX_AJAXPROXY = "ajaxProxy/";

	private ServerSocket socket;

	private final PluginInterface pi;

	private final File root;

	private final BtApp app;

	private int port;

	private boolean added;

	public BtAppWebServ(PluginInterface pi, File root, BtApp app)
			throws Exception {
		this.pi = pi;
		this.root = root;
		this.app = app;
		for (int i = 8000; i <= 9000; i++) {
			try {
				socket = new ServerSocket(i, 50, InetAddress.getByName("127.0.0.1"));
				port = i;
				break;
			} catch (Throwable e) {
			}
		}

		if (socket == null) {
			throw new Exception("No Sockets");
		}

		log("Listening on Port " + socket.getLocalPort());

		pi.getUtilities().createThread("btapp.webserve", this);
	}

	private void log(String string) {
		if (app != null) {
			app.log("WebServ:" + port + "] " + string);
		}
	}

	public void run() {

		int errors = 0;
		int ok = 0;

		while (socket != null) {

			try {

				synchronized (NL) {
					if (socket == null) {
						break;
					}
				}

				final Socket sck = socket.accept();

				ok++;

				errors = 0;

				pi.getUtilities().createThread("btapp.webserving", new Runnable() {
					public void run() {
						readSocket(sck);
					}
				});

			} catch (Throwable e) {
				if (socket == null) {
					return;
				}

				Debug.printStackTrace(e);

				errors++;

				if (errors > 100) {
					log("bailing out, too many socket errors");
				}
			}
		}
	}

	protected void readSocket(Socket sck) {

		boolean close_socket = true;

		try {
			String address = sck.getInetAddress().getHostAddress();

			if (address.equals("localhost") || address.equals("127.0.0.1")) {

				BufferedReader br = new BufferedReader(new InputStreamReader(
						sck.getInputStream(), Constants.DEFAULT_ENCODING));

				String line = br.readLine();

				//if (DEBUG) {
				//	log(line + "] =====");
				//	//log("Traffic Class: " + sck.getTrafficClass());
				//	//log("OS: " + sck.getOutputStream());
				//	log(line + "] isBound? " + sck.isBound() + "; isClosed=" + sck.isClosed()
				//			+ "; isConn=" + sck.isConnected() + ";isIShutD "
				//			+ sck.isInputShutdown() + ";isOShutD " + sck.isOutputShutdown());
				//	log(line + "] - - - -");
				//	log(line);
				//}

				String xLocation = null;

				while (br.ready()) {
					String extraline = br.readLine();
					if (extraline.toLowerCase().startsWith("x-location: ")) {
						xLocation = extraline.substring(12);
						log("Got x-location of " + xLocation);
					}
					//if (DEBUG) {
					//	log(line + "] " + extraline);
					//}
				}

//				if (DEBUG) {
//					log(line + "] =====");
//				}

				if (line != null) {

					if (line.toUpperCase().startsWith("GET ")) {

						//log("processing '" + line + "'");

						line = line.substring(4);

						int pos = line.lastIndexOf(' ');

						line = line.substring(0, pos);

						close_socket = process(line, xLocation, sck.getOutputStream());

					} else {

						log("invalid command - '" + line + "'");
					}
				} else {

					log("connect from " + "'" + address + "': no data read");

				}

			} else {

				log("connect from " + "invalid address '" + address + "'");
			}
		} catch (Throwable e) {

			if (!(e instanceof IOException || e instanceof SocketException)) {

				Debug.printStackTrace(e);
			}
		} finally {

			try {
				// leave client to close socket if not requested

				if (close_socket) {

					sck.close();
				}

			} catch (Throwable e) {
			}
		}
	}

	private boolean process(String line, String xLocation,
			OutputStream outputStream) {
		String filename = line.replaceAll("^[\\s./]+", "");

		if (filename.startsWith(PREFIX_AJAXPROXY)) {
			
			// X-Location is used by btapp SDK's xhr.js.. so use that if we can
			String ajax = xLocation == null || xLocation.contains("callback=?&_=")
					? UrlUtils.decode(line.substring(PREFIX_AJAXPROXY.length() + 1))
					: xLocation;
			if (ajax.startsWith("http://")) {
				try {
					log("AJAX PROXY] Reading " + ajax);
					ResourceDownloader rd = pi.getUtilities().getResourceDownloaderFactory().create(
							new URL(ajax));
					InputStream is = rd.download();
					String contentType = null;
					Object contentTypeObject = rd.getProperty("URL_Content-Type");
					if (contentTypeObject instanceof List) {
						List list = (List) contentTypeObject;
						if (list.size() > 0) {
							contentType = list.get(0).toString();
							log("AJAX PROXY] result is " + contentType);
						}
					}
					byte[] b = FileUtil.readInputStreamAsByteArray(is);
					writeReply(outputStream, contentType, b);
					log("AJAX PROXY] Done proxying " + b.length + " bytes from " + ajax);
					return true;
				} catch (Exception e) {
					log("AJAX PROXY] " + e.toString());
				}
			} else {
				log("AJAX PROXY] Invalid " + ajax);
			}
		}

		filename = filename.replaceAll("/", "\\" + File.separator);
		filename = filename.replaceAll("[#?].*$", "");
		
		if (filename.length() == 0) {
			return true;
		}

		File file = new File(root, filename);
		try {
			if (!file.exists()) {
				writeNotFound(outputStream);
				log("Not Found -- " + line);
			} else {
				String ext = FileUtil.getExtension(filename).replaceAll("^\\.", "");
				String contentType = HTTPUtils.guessContentTypeFromFileType(ext);
				log("Serving Content Type " + contentType + " for " + filename);
				writeReply(outputStream, contentType,
						FileUtil.readFileAsByteArray(file));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (!added) {
			added = true;
			app.insertBtAppJS();
		}


		if (filename.toLowerCase().contains("jquery")) {
			app.insertAjaxProxy();
		}
		return true;
	}

	protected void writeReply(OutputStream os, String content_type, byte[] content)
			throws IOException {
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));

		pw.print("HTTP/1.1 200 OK" + NL);
		pw.print("Cache-Control: no-cache" + NL);
		pw.print("Pragma: no-cache" + NL);
		if (content_type != null) {
			pw.print("Content-Type: " + content_type + NL);
		}
		pw.print("Content-Length: " + content.length + NL + NL);

		pw.flush();

		os.write(content);

	}

	protected void writeNotFound(OutputStream os)

			throws IOException {
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));

		pw.print("HTTP/1.0 404 Not Found" + NL + NL);

		pw.flush();

	}

	public void delete() {
		synchronized (NL) {
			if (socket != null) {
				log("Closing socket " + socket.getLocalPort());
				try {
					socket.close();
				} catch (IOException e) {
				}
				socket = null;
			}
		}
	}

	public int getPort() {
		synchronized (NL) {
			if (socket == null) {
				return 0;
			}
			return socket.getLocalPort();
		}
	}
}

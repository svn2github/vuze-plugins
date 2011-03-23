package com.vuze.plugin.btapp;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;

public class Plugin
	implements UnloadablePlugin
{
	private static UISWTInstance swtUI;

	private static File pluginUserDir;

	private static LoggerChannel logger;

	private static PluginInterface pi;

	public void initialize(final PluginInterface pi)
			throws PluginException {

		this.pi = pi;

		pi.getUIManager().addUIListener(new UIManagerListener() {

			public void UIDetached(UIInstance instance) {
				if (instance instanceof UISWTInstance) {
				}
			}

			public void UIAttached(UIInstance instance) {
				if (instance instanceof UISWTInstance) {
					swtUI = (UISWTInstance) instance;

					swtUI.addView(UISWTInstance.VIEW_MAIN, "btapplist",
							BtAppListView.class, null);
					swtUI.openView(UISWTInstance.VIEW_MAIN, "btapplist", null, false);

					// tux test window
					File testDir = new File("/Volumes/Workspace/workspace/plugins-public/btapp/test");
					if (testDir.exists()) {
  					swtUI.addView(UISWTInstance.VIEW_MAIN, "btapp.test",
  							BtAppView.class, testDir);
					}
					
					String[] appIDs = getAppIDs();
					for (String id : appIDs) {
						swtUI.addView(UISWTInstance.VIEW_MAIN, "btapp." + id,
								BtAppView.class, new File(pluginUserDir, id));
					}

				}
			}
		});

		logger = pi.getLogger().getTimeStampedChannel("btapp");

		pi.getUIManager().createLoggingViewModel(logger, true);
		pluginUserDir = pi.getPluginconfig().getPluginUserFile("nan").getParentFile();
	}

	public void unload()
			throws PluginException {
		if (swtUI != null) {
			UISWTView[] openViews = swtUI.getOpenViews(UISWTInstance.VIEW_MAIN);
			for (UISWTView view : openViews) {
				if (view.getViewID().startsWith("btapp")) {
					swtUI.removeViews(UISWTInstance.VIEW_MAIN, view.getViewID());
				}
			}
		}
	}

	private String[] getAppIDs() {
		File[] listFiles = pluginUserDir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory() && new File(pathname, "btapp").isFile();
			}
		});
		String[] s = new String[listFiles.length];
		for (int i = 0; i < listFiles.length; i++) {
			s[i] = listFiles[i].getName();
		}
		return s;
	}

	public static String jsTextify(String s) {
		// this looks silly and incomplete, but it works so far
		return s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"").replaceAll(
				"[\r\n]+", "");
	}

	public static void log(String s) {
		System.out.println("LOGGER: " + s);
		if (logger == null) {
			return;
		}
		logger.log(s);
	}

	public static boolean isAppInstalled(String ourAppId) {
		File appDir = new File(pluginUserDir, ourAppId);
		return appDir.isDirectory() && new File(appDir, "btapp").isFile();
	}

	public static void installApp(final String ourAppId, String url) {
		try {
			ResourceDownloader rd = pi.getUtilities().getResourceDownloaderFactory().create(
					new URL(url));

			rd.addListener(new ResourceDownloaderListener() {
				public void reportPercentComplete(ResourceDownloader downloader,
						int percentage) {
				}

				public void reportAmountComplete(ResourceDownloader downloader,
						long amount) {
				}

				public void reportActivity(ResourceDownloader downloader,
						String activity) {
				}

				public void failed(ResourceDownloader downloader,
						ResourceDownloaderException e) {
				}

				public boolean completed(ResourceDownloader downloader, InputStream is) {
					final int BUFFER = 2048;
					File destDir = new File(pluginUserDir, ourAppId);
					destDir.delete();
					destDir.mkdirs();
					ZipInputStream zis = new ZipInputStream(is);
					ZipEntry entry;
					try {
						while ((entry = zis.getNextEntry()) != null) {
							int count;
							byte data[] = new byte[BUFFER];
							File destFile = new File(destDir, entry.getName());
							if (entry.isDirectory()) {
								// Could make directories, but we don't need to since we
								// ensure creation before writing any files
								//destFile.mkdirs();
							} else {
  							destFile.getParentFile().mkdirs();
  							FileOutputStream fos = new FileOutputStream(destFile);
  							BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
  							while ((count = zis.read(data, 0, BUFFER)) != -1) {
  								dest.write(data, 0, count);
  							}
  							dest.flush();
  							dest.close();
							}
						}
						zis.close();

						String viewID = "btapp."
								+ Integer.toHexString(destDir.getName().hashCode());
						swtUI.addView(UISWTInstance.VIEW_MAIN, viewID, BtAppView.class,
								destDir);
						swtUI.openView(UISWTInstance.VIEW_MAIN, viewID, null, true);

					} catch (Exception e) {
						log(e.toString());
					}
					return true;
				}
			});
			rd.asyncDownload();
		} catch (MalformedURLException e) {
			log(e.toString());
		}
	}

	public static void uninstallApp(String ourAppId) {
		if (swtUI != null) {
			swtUI.removeViews(UISWTInstance.VIEW_MAIN, "btapp." + ourAppId);
		}
		File dir = new File(pluginUserDir, ourAppId);
		FileUtil.recursiveDeleteNoCheck(dir);
	}

	public static void openApp(String ourAppId) {
		if (swtUI != null) {
			swtUI.openView(UISWTInstance.VIEW_MAIN, "btapp." + ourAppId, null, true);
		}
	}
}

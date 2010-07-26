package com.vuze.plugins.mlab.ui;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.speedmanager.SpeedManagerLimitEstimate;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.VuzeMessageBox;
import com.aelitis.azureus.ui.swt.views.skin.VuzeMessageBoxListener;
import com.vuze.plugins.mlab.MLabPlugin;
import com.vuze.plugins.mlab.MLabPlugin.ToolListener;

public class MLabVzWizard
{
	protected static final String PATH_SKIN_DEFS = "com/vuze/plugins/mlab/ui/resources/";
	
	private final int BUTTON_OK = 0;

	private volatile MLabPlugin.ToolRun runner;

	private volatile boolean cancelled;

	StringBuffer lg = new StringBuffer();

	private final MLabPlugin mLabPlugin;

	private boolean downloads_paused;

	private StringBuffer summary = new StringBuffer();

	private StringBuffer details = new StringBuffer();

	protected Long up_rate;

	protected Long down_rate;

	protected SWTSkinObjectTextbox soDetails;

	private VuzeMessageBox boxTest;

	private int maxActiveTorrents;

	private int maxDownloads;

	private long uploadLimit;

	private final IPCInterface callback;

	private boolean limitsApplied;

	public MLabVzWizard(MLabPlugin mLabPlugin, IPCInterface callback) {
		this.mLabPlugin = mLabPlugin;
		this.callback = callback;
	}

	public void open() {
		openWelcome();
	}

	private void openWelcome() {
		String title = MessageText.getString("mlab.vzwiz.welcome.subtitle");
		final VuzeMessageBox box = new VuzeMessageBox(title, title, new String[] {
			MessageText.getString("Button.continue"),
			MessageText.getString("Button.cancel"),
		}, 0);
		box.setSubTitle("");

		box.setButtonVals(new Integer[] {
			BUTTON_OK,
			SWT.CANCEL
		});

		box.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				addResourceBundle(skin, PATH_SKIN_DEFS, "skin_speedtest");

				box.setIconResource("image.mlab.header");

				skin.createSkinObject("speedtest.welcome", "speedtest.welcome", soExtra);

			}
		});

		box.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {

				if (result == 0) {
					openTest();
				} else {
					cleanup();
				}
			}
		});
	}

	protected void openTest() {
		String title = MessageText.getString("mlab.vzwiz.welcome.subtitle");
		boxTest = new VuzeMessageBox(title, title, new String[] {
			MessageText.getString("Button.apply"),
			MessageText.getString("Button.cancel"),
		}, 0);
		boxTest.setSubTitle("");

		boxTest.setButtonVals(new Integer[] {
			BUTTON_OK,
			SWT.CANCEL
		});
		boxTest.setButtonEnableStates(new boolean[] {
			true,
			false
		});

		boxTest.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				final SWTSkin skin = soExtra.getSkin();
				addResourceBundle(skin, PATH_SKIN_DEFS, "skin_speedtest");

				boxTest.setIconResource("image.mlab.header");

				SWTSkinObject so = skin.createSkinObject("speedtest.run",
						"speedtest.run", soExtra);

				SWTSkinObject soProgressA = skin.getSkinObject("speedtest-progress", so);
				if (soProgressA instanceof SWTSkinObjectContainer) {
					SWTSkinObjectContainer soProgress = (SWTSkinObjectContainer) soProgressA;
					ProgressBar pb = new ProgressBar(soProgress.getComposite(),
							SWT.INDETERMINATE);
					pb.setLayoutData(Utils.getFilledFormData());
				}

				soDetails = (SWTSkinObjectTextbox) skin.getSkinObject(
						"details-textbox", so);

				final SWTSkinObjectImage soTwist = (SWTSkinObjectImage) skin.getSkinObject(
						"twist", so);

				SWTSkinObject soDetailsHeader = skin.getSkinObject("details-header-text");
				if (soDetailsHeader != null) {
					MouseListener mouseListener = new MouseListener() {

						public void mouseUp(MouseEvent e) {
							if (soDetails != null && !soDetails.isDisposed()) {
								boolean newVisiblility = !soDetails.isVisible();
								if (soTwist != null && !soTwist.isDisposed()) {
									soTwist.setImageByID("image.mlab."
											+ (newVisiblility ? "expanded" : "collapsed"), null);
								}
								soDetails.setVisible(newVisiblility);
								skin.layout();
							}
						}

						public void mouseDown(MouseEvent e) {
						}

						public void mouseDoubleClick(MouseEvent e) {
						}
					};
					soDetailsHeader.getControl().addMouseListener(mouseListener);
					if (soTwist != null) {
						soTwist.getControl().addMouseListener(mouseListener);
					}
				}

				if (soDetails != null) {
					String details = "";
					soDetails.setText(details == null ? "" : details);
					soDetails.setVisible(false);
				}

				pauseAndRun(new AERunnable() {
					public void runSupport() {
						runNDT();
					}
				});

			}
		});

		boxTest.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {

				if (result == 0) {
					if (uploadLimit == 0) {
						openUnavailable();
					} else {
						openDone();
					}
				} else {
					cancelled = true;
					cleanup();
				}
			}
		});
	}

	public void cleanup() {
		if (downloads_paused) {

			mLabPlugin.getPluginInterface().getDownloadManager().resumeDownloads();
		}

		try {
			if (cancelled) {

				callback.invoke("cancelled", new Object[] {});

			} else {

				Map<String, Object> args = new HashMap<String, Object>();

				args.put("up", up_rate);
				args.put("down", down_rate);
				args.put("maxActiveTorrents", maxActiveTorrents);
				args.put("maxDownloads", maxDownloads);
				args.put("uploadLimit", uploadLimit);
				args.put("limitsApplied", limitsApplied);

				callback.invoke("results", new Object[] {
					args
				});

			}
		} catch (IPCException e) {
			Debug.out(e);
		}
	}

	public static void addResourceBundle(SWTSkin skin, String path, String name) {
		String sFile = path + name;
		ClassLoader loader = MLabVzWizard.class.getClassLoader();
		SWTSkinProperties skinProperties = skin.getSkinProperties();
		try {
			ResourceBundle subBundle = ResourceBundle.getBundle(sFile,
					Locale.getDefault(), loader);
			skinProperties.addResourceBundle(subBundle, path, loader);
		} catch (MissingResourceException mre) {
			Debug.out(mre);
		}

		// Images stored in plugin must be loaded using our classloader, so
		// we put the image ref in their own properties file and load them in a
		// new SkinProperties
		//SWTSkinPropertiesImpl imageProps = new SWTSkinPropertiesImpl(
		//		loader, BurnGlobals.PATH_SKIN_DEFS,
		//		BurnGlobals.FILE_SKINIMAGES_DEFS);
		//skin.getImageLoader(skinProperties).addSkinProperties(imageProps);
	}

	private void pauseAndRun(final AERunnable run) {
		final int waitCycles = pauseDownloads() ? 50 : 0;
		if (waitCycles > 0) {

			appendLog("Pausing downloads before performing test.");
		}

		new AEThread2("waiter") {
			public void run() {
				try {
					for (int i = 0; i < waitCycles && !cancelled; i++) {

						appendLog(".");

						if (i == waitCycles - 1) {

							appendLog("\n");
						}

						try {
							Thread.sleep(100);

						} catch (Throwable e) {

						}
					}
				} finally {

					run.run();
				}
			}
		}.start();

	}

	private void runNDT() {
		if (cancelled) {

			return;
		}

		runner = mLabPlugin.runNDT(new ToolListener() {
			{
				summary.setLength(0);
				details.setLength(0);
			}

			public void reportSummary(final String str) {
				summary.append(str);
				summary.append("\n");

				appendLog(str + "\n");
			}

			public void reportDetail(String str) {
				details.append(str);
				details.append("\n");
			}

			public void complete(final Map<String, Object> results) {
				up_rate = (Long) results.get("up");
				down_rate = (Long) results.get("down");

				calculateLimits();
				
				if (shouldRunShaperProbe()) {
					pauseAndRun(new AERunnable() {
						public void runSupport() {
							runShaperProbe();
						}
					});
				} else {
					boxTest.closeWithButtonVal(BUTTON_OK);
				}

				runner = null;

			}
		});

		if (cancelled) {

			runner.cancel();
		}
	}

	protected void openDone() {

		String title = MessageText.getString("mlab.vzwiz.result.subtitle");
		boxTest = new VuzeMessageBox(title, title, new String[] {
			MessageText.getString("Button.apply"),
			MessageText.getString("Button.cancel"),
		}, 0);
		boxTest.setSubTitle("");

		boxTest.setButtonVals(new Integer[] {
			BUTTON_OK,
			SWT.CANCEL
		});

		boxTest.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				final SWTSkin skin = soExtra.getSkin();
				addResourceBundle(skin, PATH_SKIN_DEFS, "skin_speedtest");

				boxTest.setIconResource("image.mlab.header");

				SWTSkinObject so = skin.createSkinObject("speedtest.result",
						"speedtest.result", soExtra);

				String ul = DisplayFormatters.formatByteCountToKiBEtcPerSec(up_rate)
						+ " (" + DisplayFormatters.formatByteCountToBitsPerSec(up_rate)
						+ ")";
				SWTSkinObjectText soUL = (SWTSkinObjectText) skin.getSkinObject("speedtest-ul");
				soUL.setText(ul);

				String result = DisplayFormatters.formatByteCountToKiBEtcPerSec(uploadLimit);
				SWTSkinObjectText soResult = (SWTSkinObjectText) skin.getSkinObject("speedtest-result");
				soResult.setText(result);

				soDetails = (SWTSkinObjectTextbox) skin.getSkinObject(
						"details-textbox", so);

				final SWTSkinObjectImage soTwist = (SWTSkinObjectImage) skin.getSkinObject(
						"twist", so);

				SWTSkinObject soDetailsHeader = skin.getSkinObject("details-header-text");
				if (soDetailsHeader != null) {
					MouseListener mouseListener = new MouseListener() {

						public void mouseUp(MouseEvent e) {
							if (soDetails != null && !soDetails.isDisposed()) {
								boolean newVisiblility = !soDetails.isVisible();
								if (soTwist != null && !soTwist.isDisposed()) {
									soTwist.setImageByID("image.mlab."
											+ (newVisiblility ? "expanded" : "collapsed"), null);
								}
								soDetails.setVisible(newVisiblility);
								skin.layout();
							}
						}

						public void mouseDown(MouseEvent e) {
						}

						public void mouseDoubleClick(MouseEvent e) {
						}
					};
					soDetailsHeader.getControl().addMouseListener(mouseListener);
					if (soTwist != null) {
						soTwist.getControl().addMouseListener(mouseListener);
					}
				}

				if (soDetails != null) {
					soDetails.setText(lg.toString());
					soDetails.setVisible(false);
				}
			}
		});

		boxTest.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				cancelled = result != 0;
				if (!cancelled) {
					applyLimits();
				}
				cleanup();
			}
		});
	}

	protected void openUnavailable() {

		String title = MessageText.getString("mlab.vzwiz.result.subtitle");
		boxTest = new VuzeMessageBox(title, title, new String[] {
			MessageText.getString("Button.ok"),
		}, 0);
		boxTest.setSubTitle("");

		boxTest.setListener(new VuzeMessageBoxListener() {
			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				final SWTSkin skin = soExtra.getSkin();
				addResourceBundle(skin, PATH_SKIN_DEFS, "skin_speedtest");

				boxTest.setIconResource("image.mlab.header");

				SWTSkinObject so = skin.createSkinObject("speedtest.noresult",
						"speedtest.noresult", soExtra);

				soDetails = (SWTSkinObjectTextbox) skin.getSkinObject(
						"details-textbox", so);

				final SWTSkinObjectImage soTwist = (SWTSkinObjectImage) skin.getSkinObject(
						"twist", so);

				SWTSkinObject soDetailsHeader = skin.getSkinObject("details-header-text");
				if (soDetailsHeader != null) {
					MouseListener mouseListener = new MouseListener() {

						public void mouseUp(MouseEvent e) {
							if (soDetails != null && !soDetails.isDisposed()) {
								boolean newVisiblility = !soDetails.isVisible();
								if (soTwist != null && !soTwist.isDisposed()) {
									soTwist.setImageByID("image.mlab."
											+ (newVisiblility ? "expanded" : "collapsed"), null);
								}
								soDetails.setVisible(newVisiblility);
								skin.layout();
							}
						}

						public void mouseDown(MouseEvent e) {
						}

						public void mouseDoubleClick(MouseEvent e) {
						}
					};
					soDetailsHeader.getControl().addMouseListener(mouseListener);
					if (soTwist != null) {
						soTwist.getControl().addMouseListener(mouseListener);
					}
				}

				if (soDetails != null) {
					soDetails.setText(lg.toString());
					soDetails.setVisible(false);
				}
			}
		});

		boxTest.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				cleanup();
			}
		});
	}

	protected void calculateLimits() {

		if (up_rate == 0) {
			return;
		}

		uploadLimit = (up_rate / 5) * 4;

		uploadLimit = (uploadLimit / 1024) * 1024;

		if (uploadLimit < 5 * 1024) {

			uploadLimit = 5 * 1024;
		}

		int nbMaxActive = (int) (Math.pow(uploadLimit / 1024, 0.34) * 0.92);
		int nbMaxUploads = (int) (Math.pow(uploadLimit / 1024, 0.25) * 1.68);
		int nbMaxDownloads = (nbMaxActive * 4) / 5;

		if (nbMaxDownloads == 0) {
			nbMaxDownloads = 1;
		}

		if (nbMaxUploads > 50) {
			nbMaxUploads = 50;
		}

		maxActiveTorrents = nbMaxActive;
		maxDownloads = nbMaxDownloads;
	}

	private void applyLimits() {
		if (uploadLimit <= 0) {
			return;
		}
		COConfigurationManager.setParameter(
				TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY, false);
		COConfigurationManager.setParameter(
				TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY, false);
		COConfigurationManager.setParameter("Max Upload Speed KBs",
				uploadLimit / 1024);
		COConfigurationManager.setParameter("enable.seedingonly.upload.rate", false);
		COConfigurationManager.setParameter("max active torrents",
				maxActiveTorrents);
		COConfigurationManager.setParameter("max downloads", maxDownloads);

		try {
			SpeedManager sm = AzureusCoreFactory.getSingleton().getSpeedManager();

			sm.setEstimatedUploadCapacityBytesPerSec((int) uploadLimit,
					SpeedManagerLimitEstimate.TYPE_MEASURED);

		} catch (Throwable e) {

			Debug.out(e);
		}

		// toggle to ensure listeners get the message that they should recalc things

		COConfigurationManager.setParameter("Auto Adjust Transfer Defaults", false);
		COConfigurationManager.setParameter("Auto Adjust Transfer Defaults", true);

		limitsApplied = true;
	}

	private void runShaperProbe() {
		if (cancelled) {

			return;
		}

		runner = mLabPlugin.runShaperProbe(new ToolListener() {
			public void reportSummary(final String str) {
				appendLog(str + "\n");
			}

			public void reportDetail(String str) {
			}

			public void complete(final Map<String, Object> results) {
				//Long	up 			= (Long)results.get( "up" );
				//Long	down 		= (Long)results.get( "down" );
				Long shape_up = (Long) results.get("shape_up");
				//Long	shape_down 	= (Long)results.get( "shape_down" );

				if (shape_up == null || shape_up == 0) {

					// no results, use existing

				} else if (shape_up >= up_rate) {

					// no interference, use existing up

				} else {

					// interference, use shape up
					up_rate = shape_up;

				}

				boxTest.closeWithButtonVal(BUTTON_OK);
			}
		});

		if (cancelled) {

			runner.cancel();
		}
	}

	protected void appendLog(String string) {
		lg.append(string);
		soDetails.setText(lg.toString());
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Text text = soDetails.getTextControl();
				if (text != null && !text.isDisposed()) {
					text.setSelection(text.getText().length());
					text.showSelection();
				}
			}
		});
	}

	protected boolean pauseDownloads() {
		if (downloads_paused) {

			return (false);
		}

		DownloadManager download_manager = mLabPlugin.getPluginInterface().getDownloadManager();

		Download[] downloads = download_manager.getDownloads();

		boolean active = false;

		for (Download d : downloads) {

			int state = d.getState();

			if (state != Download.ST_ERROR && state != Download.ST_STOPPED) {

				active = true;

				break;
			}
		}

		if (active) {

			downloads_paused = true;

			download_manager.pauseDownloads();

			return (true);
		} else {

			return (false);
		}
	}

	private boolean shouldRunShaperProbe() {
		return true;
	}
}

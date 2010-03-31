/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.azureus.plugins.azemp.ui.swt.emp;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.core3.util.Constants;

import com.azureus.swt.OpenGLLayoutData;
import com.azureus.swt.OpenGLRenderPanel;
import com.azureus.swt.OpenGLVideo;

/**
 * @author TuxPaper
 * @created Sep 25, 2007
 *
 */
public class EmbeddedMPlayerOSXUtils
{
	public static String initMPlayerOSX(EmbeddedMediaPlayer emp,
			Composite composite, boolean useSWT2) throws Throwable {
		if (!Constants.isOSX) {
			return null;
		}

		if (useSWT2) {
			return initMPlayerOSX_new(emp, composite);
		} else {
			initMPlayerOSX_old(composite);
		}
		return null;
	}

	private static String initMPlayerOSX_new(final EmbeddedMediaPlayer emp,
			final Composite composite) throws Throwable {
		try {
			composite.setLayout(new FillLayout());
			final OpenGLRenderPanel panel = new OpenGLRenderPanel(composite);
			final OpenGLVideo video = new OpenGLVideo(panel.getRootComposite());
			OpenGLLayoutData data;
			data = new OpenGLLayoutData();
			data.x = data.y = 0;
			data.width = 1f;
			data.height = 1f;
			
			emp.addListener(new EmpListenerFileChanged() {
				public void empFileChanged(String nowPlaying) {
					panel.clearNumSkippedFrames();
				}
			});
			emp.addListener(new EmpListenerSemiClose() {
				public void empSemiClose() {
					emp.setExtraDropFrames(panel.getNumSkippedFrames());
				}
			});

			return video.getMplayerVOCode().replaceAll("swt:", "swt2:");
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * @param control
	 * @throws Throwable 
	 *
	 * @since 3.0.3.3
	 */
	private static void initMPlayerOSX_old(Composite composite) throws Throwable {
		try {

			// Check if already inited
			Control[] children = composite.getChildren();
			for (int i = 0; i < children.length; i++) {
				Control control = children[i];

				if (control instanceof Browser) {
					return;
				}
			}

			ClassLoader cl = composite.getClass().getClassLoader();
			Field fldWebBrowser = Browser.class.getField("webBrowser");

			// NSRect class reflection setup
			Class claNSRect = cl.loadClass("org.eclipse.swt.internal.cocoa.NSRect");
			final Field fldX = claNSRect.getField("x");
			final Field fldY = claNSRect.getField("y");
			final Field fldWidth = claNSRect.getField("width");
			final Field fldHeight = claNSRect.getField("height");

			// Cocoa class reflection setup
			Class claCocoa = cl.loadClass("org.eclipse.swt.internal.cocoa.Cocoa");

			//Cocoa.objc_msgSend(arg0, arg1)
			Method methCocoaObjc_msgSend2 = claCocoa.getMethod("objc_msgSend",
					new Class[] {
						int.class,
						int.class
					});
			Method methCocoaObjc_msgSend3 = claCocoa.getMethod("objc_msgSend",
					new Class[] {
						int.class,
						int.class,
						int.class,
					});
			final Method methCocoaObjc_msgSend3r = claCocoa.getMethod("objc_msgSend",
					new Class[] {
						int.class,
						int.class,
						claNSRect,
					});
			Method methCocoaObjc_msgSend4 = claCocoa.getMethod("objc_msgSend",
					new Class[] {
						int.class,
						int.class,
						claNSRect,
						int.class
					});
			Method methHIWebViewGetWebView = claCocoa.getMethod(
					"HIWebViewGetWebView", new Class[] {
						int.class
					});
			Field fldC_VideoOpenGLView = claCocoa.getField("C_VideoOpenGLView");
			Field fldS_defaultPixelFormat = claCocoa.getField("S_defaultPixelFormat");
			Field fldS_alloc = claCocoa.getField("S_alloc");
			Field fldS_initWithFramepixelFormat = claCocoa.getField("S_initWithFramepixelFormat");
			Field fldS_awakeFromNib = claCocoa.getField("S_awakeFromNib");
			Field fldS_addSubview = claCocoa.getField("S_addSubview");
			final Field fldS_setFrame = claCocoa.getField("S_setFrame");
			final Field fldS_setBounds = claCocoa.getField("S_setBounds");

			composite.setLayout(new FillLayout());
			Browser browser = new Browser(composite, SWT.NONE);

			Object defaultPixelFormat = methCocoaObjc_msgSend2.invoke(null,
					new Object[] {
						fldC_VideoOpenGLView.get(null),
						fldS_defaultPixelFormat.get(null)
					});

			final Constructor consNSRect = claNSRect.getConstructor(new Class[] {});
			Object rect = consNSRect.newInstance(new Object[] {});
			fldX.set(rect, new Float(0));
			fldY.set(rect, new Float(0));
			fldWidth.set(rect, new Float(100));
			fldHeight.set(rect, new Float(100));

			final Object videoView = methCocoaObjc_msgSend2.invoke(null,
					new Object[] {
						fldC_VideoOpenGLView.get(null),
						fldS_alloc.get(null)
					});

			methCocoaObjc_msgSend4.invoke(null, new Object[] {
				videoView,
				fldS_initWithFramepixelFormat.get(null),
				rect,
				defaultPixelFormat
			});

			methCocoaObjc_msgSend4.invoke(null, new Object[] {
				videoView,
				fldS_awakeFromNib.get(null),
				rect,
				defaultPixelFormat
			});

			Object webBrowser = fldWebBrowser.get(browser);
			Field fldWebViewHandle = webBrowser.getClass().getField("webViewHandle");
			Object webView = methHIWebViewGetWebView.invoke(null, new Object[] {
				fldWebViewHandle.get(webBrowser)
			});

			methCocoaObjc_msgSend3.invoke(null, new Object[] {
				webView,
				fldS_addSubview.get(null),
				videoView
			});

			Listener listener = new Listener() {
				public void handleEvent(Event event) {
					try {
						Control control = (Control) event.widget;
						Point p = control.getSize();

						Object rect = consNSRect.newInstance(new Object[] {});
						fldX.set(rect, new Float(0));
						fldY.set(rect, new Float(0));
						fldWidth.set(rect, new Float(p.x));
						fldHeight.set(rect, new Float(p.y));

						methCocoaObjc_msgSend3r.invoke(null, new Object[] {
							videoView,
							fldS_setFrame.get(null),
							rect
						});
						methCocoaObjc_msgSend3r.invoke(null, new Object[] {
							videoView,
							fldS_setBounds.get(null),
							rect
						});
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			};
			browser.addListener(SWT.Resize, listener);
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}
	}
}

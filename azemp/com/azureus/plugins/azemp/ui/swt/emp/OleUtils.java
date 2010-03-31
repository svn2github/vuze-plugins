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

import org.eclipse.swt.internal.ole.win32.COM;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * @author TuxPaper
 * @created Sep 20, 2007
 *
 */
public class OleUtils
{

	private static void warnIfNotSWTThread() {
		if (!Utils.isThisThreadSWT()) {
			System.err.println("Warning: Not SWT Thread\n"
					+ Debug.getCompressedStackTrace());
		}
	}

	/**
	 * @param automation
	 * @param string
	 * @return
	 */
	public static int getFirstID(OleAutomation automation, String string) {
		if (automation == null) {
			return -1;
		}
		int[] IDs = automation.getIDsOfNames(new String[] {
			string
		});
		if (IDs == null || IDs.length == 0) {
			System.out.println("ID " + string + " invalid "
					+ Debug.getCompressedStackTrace());
			return -1;
		}
		int id = IDs[0];
		//System.out.println(IDs.length + " IDs for " + string + ". 1st: " + id);
		return id;
	}

	/**
	 * Invoke an OLE command in the format of 
	 * OleAutomation.OleAutomation.method
	 *  
	 * @param auto
	 * @param parts
	 * @return null if not sucessfull.  return value of invoke if successfull.
	 *          If invoke returns a null Variant, this function returns a
	 *          Variant of type OLE.VT_EMPTY
	 */
	public static OleAutomation getAutomation(OleAutomation auto, String[] parts) {
		if (auto == null) {
			return null;
		}
		OleAutomation finalAuto = auto;
		for (int i = 0; i < parts.length - 1; i++) {
			int firstID = getFirstID(finalAuto, parts[i]);
			if (firstID >= 0) {
				Variant variant = finalAuto.getProperty(firstID);
				if (variant != null && variant.getType() == COM.VT_DISPATCH) {
					OleAutomation newAuto = variant.getAutomation();
					if (auto != finalAuto) {
						finalAuto.dispose();
					}

					if (newAuto == null) {
						return null;
					}

					finalAuto = newAuto;
				}
			}
		}

		return finalAuto;
	}

	public static Variant invokeOle(OleAutomation auto, String sOLECommand) {
		if (auto == null) {
			return null;
		}
		warnIfNotSWTThread();

		Variant returnVal = null;
		String[] parts = sOLECommand.split("\\.");

		OleAutomation finalAuto = getAutomation(auto, parts);
		if (finalAuto == null)
			return null;

		int firstID = getFirstID(finalAuto, parts[parts.length - 1]);
		if (firstID >= 0) {
			returnVal = finalAuto.invoke(firstID);

			if (returnVal == null) {
				returnVal = new Variant();
			}
		}

		if (auto != finalAuto) {
			finalAuto.dispose();
		}

		return returnVal;
	}

	public static Variant invokeOle(OleAutomation auto, String sOLECommand,
			String param) {
		return invokeOle(auto, sOLECommand, new Variant[] {
			new Variant(param)
		});
	}

	public static Variant invokeOle(OleAutomation auto, String sOLECommand,
			Variant[] params) {
		warnIfNotSWTThread();

		Variant returnVal = null;
		String[] parts = sOLECommand.split("\\.");

		OleAutomation finalAuto = getAutomation(auto, parts);
		if (finalAuto == null)
			return null;

		int firstID = getFirstID(finalAuto, parts[parts.length - 1]);
		if (firstID >= 0) {
			returnVal = finalAuto.invoke(firstID, params);

			if (returnVal == null) {
				returnVal = new Variant();
			}
		}

		if (auto != finalAuto) {
			finalAuto.dispose();
		}

		return returnVal;
	}

	public static boolean setOleProperty(OleAutomation auto, String sOLECommand,
			String property) {
		return setOleProperty(auto, sOLECommand, new Variant(property));
	}

	public static boolean setOleProperty(OleAutomation auto, String sOLECommand,
			int property) {
		return setOleProperty(auto, sOLECommand, new Variant((short) property));
	}

	public static boolean setOleProperty(OleAutomation auto, String sOLECommand,
			Variant property) {
		warnIfNotSWTThread();

		boolean ok = false;
		String[] parts = sOLECommand.split("\\.");

		OleAutomation finalAuto = getAutomation(auto, parts);
		if (finalAuto != null) {
			int firstID = getFirstID(finalAuto, parts[parts.length - 1]);
			if (firstID >= 0) {
				ok = finalAuto.setProperty(firstID, property);
			}

			if (auto != finalAuto) {
				finalAuto.dispose();
			}
		}

		return ok;
	}

	public static String getOleString(OleAutomation auto, String sOLECommand,
			boolean strict, String def) {
		Variant variant = getOleProperty(auto, sOLECommand);
		if (variant == null) {
			return def;
		}
		try {
			if (strict) {
				if (variant.getType() != COM.VT_BSTR) {
					return def;
				}
			}
			return variant.getString();
		} catch (Exception e) {
		}

		return def;
	}

	public static Variant getOleProperty(OleAutomation auto, String sOLECommand) {
		Variant returnVal = null;
		String[] parts = sOLECommand.split("\\.");

		OleAutomation finalAuto = getAutomation(auto, parts);
		if (finalAuto != null) {
			int firstID = getFirstID(finalAuto, parts[parts.length - 1]);
			if (firstID >= 0) {
				returnVal = finalAuto.getProperty(firstID);
			}

			if (auto != finalAuto) {
				finalAuto.dispose();
			}
		}

		return returnVal;
	}

	protected Variant getOleProperty(OleAutomation auto, String sOLECommand,
			String param) {
		return getOleProperty(auto, sOLECommand, new Variant(param));
	}

	protected static Variant getOleProperty(OleAutomation auto,
			String sOLECommand, Variant param) {
		Variant returnVal = null;
		String[] parts = sOLECommand.split("\\.");

		OleAutomation finalAuto = getAutomation(auto, parts);
		if (finalAuto != null) {
			int firstID = getFirstID(finalAuto, parts[parts.length - 1]);
			if (firstID >= 0) {
				returnVal = finalAuto.getProperty(firstID, new Variant[] {
					param
				});
			}

			if (auto != finalAuto) {
				finalAuto.dispose();
			}
		}

		return returnVal;
	}

	public static int getOleID(OleAutomation auto, String sOLECommand) {
		String[] parts = sOLECommand.split("\\.");
		OleAutomation finalAuto = getAutomation(auto, parts);

		int id = getFirstID(finalAuto, parts[parts.length - 1]);

		if (auto != finalAuto) {
			finalAuto.dispose();
		}

		return id;
	}

}

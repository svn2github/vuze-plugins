/*
 * Created on 04.05.2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package de.mbaeker.utilities;

import gnu.regexp.RE;
import gnu.regexp.REException;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author BaekerAdmin
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class REUtilities {
	private static Map storedREs = null;
	private static REUtilities re = new REUtilities();
	/**
	 * @return
	 */
	public static REUtilities getInstance() {
		return re;
	}

	public RE getRE(String regEx,int compilationFlags) throws REException {
		RE rt = null;
		if (storedREs == null) {
			storedREs = new TreeMap();
		}
		if (!storedREs.containsKey(compilationFlags+"_"+regEx)) {
			rt = new RE(regEx,compilationFlags);
			storedREs.put(regEx, rt);
		} else {
			return (RE) storedREs.get(compilationFlags+"_"+regEx);
		}
		return rt;

	}
	
	public RE getRE(String regEx) throws REException {
		return getRE(regEx,0);

	}

	private REUtilities() {

	}

	public static void main(String[] args) {
	}
}

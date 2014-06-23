/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.json;

import java.util.*;

import org.json.simple.JSONValue;

/**
 * This class converts a JsonObject to a Map containing no json classes
 * 
 * @author TuxPaper
 * @created Jan 4, 2007
 *
 */
public class JsonDecoder
{
	/**
	 * decodes JSON formatted text into a map.
	 * 
	 *  If the json text is not a map, a map with the key "value" will be returned.
	 *  the value of "value" will either be an List, String, Number, Boolean, or null
	 */
	public static Map decodeJSON(String json) {
		Object object = JSONValue.parse(json);
		if (object instanceof Map) {
			return (Map) object;
		}
		// could be : ArrayList, String, Number, Boolean
		Map map = new HashMap();
		map.put("value", object);
		return map;
	}
}

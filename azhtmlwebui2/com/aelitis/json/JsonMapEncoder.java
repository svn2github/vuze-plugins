/*
 * Created on 30-Aug-2005
 * Created by Olivier Chalouhi
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.json;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class encodes a Java Map into a JSON String.
 * Supported types are :
 * Map (String keys)                (JSON Objects)
 * List                             (JSON Arrays)
 * String                           (JSON Strings)
 * Integer / Long / Float / Double  (JSON number)
 * Boolean                          (JSON true / false)
 * null                             (JSON null value)
 * 
 * @author Olivier Chalouhi
 *
 */
public class JsonMapEncoder {
  
  public static String encodes(Map data) {
    return encodes(data,true);
  }
  
  public static String encodes(Map data,boolean ident) {
    StringBuffer buffer = new StringBuffer();
    buffer.append(encodesMap(data,ident,""));
    return buffer.toString();
  }
  
  
  private static String encodesUnknown(Object obj,boolean ident,String identation) {
    
    if(obj == null) {
      return encodesNull();
    }
    
    if(obj instanceof Map) {
      return encodesMap((Map)obj,ident,identation);
    }
    if(obj instanceof List) {
      return encodesList((List)obj,ident,identation);
    }
    if(obj instanceof String) {
      return encodesString((String)obj);
    }
    if(obj instanceof Integer) {
      return encodesInteger((Integer)obj);
    }
    if(obj instanceof Long) {
      return encodesLong((Long)obj);
    }
    if(obj instanceof Float) {
      return encodesFloat((Float)obj);
    }
    if(obj instanceof Double) {
      return encodesDouble((Double)obj);
    }
    if(obj instanceof Boolean) {
      return encodesBoolean((Boolean)obj);
    }
    
    return encodesNull();
    
  }
  
  private static String encodesMap(Map data,boolean ident,String identation) {
    String separator = "";
    StringBuffer buffer = new StringBuffer();
    
    buffer.append("{");
    if(ident) {
      separator = "\n" + identation + "   ";
    }
    
    for(Iterator iter = data.keySet().iterator() ; iter.hasNext() ; ) {
      String key = (String) iter.next();
      Object obj = data.get(key);
      
      buffer.append(separator);
      buffer.append(encodesString(key));
      buffer.append(":");
      if(ident) {
        buffer.append(" ");
      }
      buffer.append(encodesUnknown(obj,ident,identation + "   "));
      
      if(ident) {
        separator = ",\n" + identation + "   ";      
      } else {
        separator = ",";
      }
    }
    
    if(ident) {
      buffer.append("\n" + identation);
    }
    buffer.append("}");
    
    return buffer.toString();
  }
  
  private static String encodesList(List data,boolean ident,String identation) {
    String separator = "";
    StringBuffer buffer = new StringBuffer();
    
    buffer.append("[");    
    
    for(Iterator iter = data.iterator() ; iter.hasNext() ; ) {
      Object obj = iter.next();
      buffer.append(separator);
      buffer.append(encodesUnknown(obj,ident,identation));
      if(ident) {
        separator = ", ";
      } else {
        separator = ",";
      }
    }
    
    buffer.append("]");
    
    return buffer.toString();
  } 
  
  private static String encodesString(String data) {
    return "\"" + escape(data) + "\"";
  }
  
  private static String encodesInteger(Integer data) {
    return "" + data.intValue();
  }
  
  private static String encodesLong(Long data) {
    return "" + data.longValue();
  }
  
  private static String encodesFloat(Float data) {
    return "" + data.floatValue();
  }
  
  private static String encodesDouble(Double data) {
    return "" + data.doubleValue();  
  }
  
  private static String encodesBoolean(Boolean data) {
    return "" + data.booleanValue();
  }
  
  private static String encodesNull() {
    return "null";
  }
  
  public static String escape(String string) {
    string = string.replace("\\","\\\\");
    string = string.replace("\"","\\\"");
    string = string.replace("/","\\/");
    string = string.replace("\b","\\b");
    string = string.replace("\f","\\f");
    string = string.replace("\r","\\r");
    string = string.replace("\n","\\n");
    string = string.replace("\t","\\t");
    return string;
  }

}

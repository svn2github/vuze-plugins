/*
 * Created on Apr 17, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.parg.azureus.plugins.networks.i2p.vuzedht;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class 
DHTUtilsI2P 
{
	protected static void
	serialiseLength(
		DataOutputStream	os,
		int					len,
		int					max_length )
	
		throws IOException
	{
		if ( len > max_length ){
			
			throw( new IOException( "Invalid DHT data length: max=" + max_length + ",actual=" + len ));
		}
		
		if ( max_length < 256 ){
			
			os.writeByte( len );
			
		}else if ( max_length < 65536 ){
			
			os.writeShort( len );
			
		}else{
			
			os.writeInt( len );
		}
	}
	
	protected static int
	deserialiseLength(
		DataInputStream	is,
		int				max_length )
	
		throws IOException
	{
		int		len;
		
		if ( max_length < 256 ){
			
			len = is.readByte()&0xff;
			
		}else if ( max_length < 65536 ){
			
			len = is.readShort()&0xffff;
			
		}else{
			
			len = is.readInt();
		}
		
		if ( len > max_length ){
			
			throw( new IOException( "Invalid DHT data length: max=" + max_length + ",actual=" + len ));
		}
		
		return( len );
	}
	
	protected static byte[]
	deserialiseByteArray(
		DataInputStream	is,
		int				max_length )
	
		throws IOException
	{
		int	len = deserialiseLength( is, max_length );
		
		byte[] data	= new byte[len];
		
		is.read(data);
		
		return( data );
	}
	
	protected static void
	serialiseByteArray(
		DataOutputStream	os,
		byte[]				data,
		int					max_length )
	
		throws IOException
	{
		serialiseByteArray( os, data, 0, data.length, max_length );
	}
	
	protected static void
	serialiseByteArray(
		DataOutputStream	os,
		byte[]				data,
		int					start,
		int					length,
		int					max_length )
	
		throws IOException
	{
		serialiseLength( os, length, max_length );
		
		os.write( data, start, length );
	}
}

package com.aelitis.azbuddy.config;

import com.aelitis.azbuddy.utils.Utils;

public class InheritableString extends InheritableConfigNode {
	
	static final String PREFIX = "s";
	
	private byte[] value;
	
	/**
	 * Use this constructor with caution, getInstance should be used to obtain monitors or avoid exceptions
	 * @throws IllegalArgumentException if the parent already contains an identical key
	 */	
	public InheritableString(InheritableConfigContainer parent, String name, boolean startAsMonitor, String initialValue)
	{
		super(parent,name,startAsMonitor);
		value = initialValue.getBytes();
	}
	
	/**
	 * @see {@link InheritableString#InheritableString(InheritableConfigTree, String, boolean, String)} 
	 */	
	public InheritableString(InheritableConfigContainer parent, String name, boolean startAsMonitor, byte[] initialValue)
	{
		super(parent,name,startAsMonitor);
		value = initialValue;
	}
	
	public static InheritableString getInstance(InheritableConfigContainer parent, String name, boolean startAsMonitor)
	{
		InheritableString newInstance = null;
		
		// fetch existing value to preserve singleton status
		if(parent != null)
			newInstance = parent.containsValue(InheritableString.class, name);
		
		// use new one otherwise
		if(newInstance == null)
			newInstance = new InheritableString(parent,name,startAsMonitor,"");
		
		return newInstance;
	}

	public String getString()
	{
		if(isMonitor())
			return ((InheritableString)nearestAncestor).getString();
		return Utils.bytesToString(value);
	}
	
	public void setString(String setTo)
	{
		if(isMonitor())
			throw new IllegalStateException("Value "+name+" currently is in a monitoring state");
		value = Utils.stringToBytes(setTo);
	}
	
	public byte[] getBytes()
	{
		if(isMonitor())
			return ((InheritableString)nearestAncestor).getBytes();
		return value;
	}
	
	public void setBytes(byte[] setTo)
	{
		if(isMonitor())
			throw new IllegalStateException("Value "+name+" currently is in a monitoring state");
		value = setTo;
	}

	String getPrefix() { return PREFIX; }
	Object getContentForSerialization() { return value; }
}

package com.aelitis.azbuddy.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InheritableConfigTree extends InheritableConfigContainer {
	
	static final String PREFIX = "ChldMap:";


	
	
	private final ArrayList<InheritableConfigNode> elements = new ArrayList<InheritableConfigNode>();
	private final ArrayList<InheritableConfigContainerListener> listeners = new ArrayList<InheritableConfigContainerListener>();
	
	private InheritableConfigTree(InheritableConfigContainer parent, String name)
	{
		// start as non monitoring instance
		super(parent, name, false); 
	}
	
	
	public static InheritableConfigTree getInstance(InheritableConfigContainer parent, String name)
	{
		InheritableConfigTree newMap = null;
		
		// fetch existing value to preserve singleton status
		if(parent != null)
			newMap = parent.containsValue(InheritableConfigTree.class, name);
		
		// use new one otherwise
		if(newMap == null)
			newMap = new InheritableConfigTree(parent,name);
		
		return newMap;
	}

	
	<T extends InheritableConfigNode> T containsValue(Class<T> type, String name)
	{
		for(InheritableConfigNode i : elements)
		{
			if(type.isInstance(i) && name.equalsIgnoreCase(i.getName()))
				return (T)i;
		}
		return null;
	}
	

	/** 
	 * Use of this method is discouraged, use the getInstance methods of the individual node objects instead 
	 * @param toAdd
	 * @throws IllegalArgumentException if an identical value is already in the list
	 */
	public void addValueNode(InheritableConfigNode toAdd)
	{
		if(containsValue(toAdd.getClass(), toAdd.getName()) != null)
			throw new IllegalArgumentException("Value node with identical key already added");
		elements.add(toAdd);
		nodeAdded(this, toAdd);
	}
	
	void removeValueNode(InheritableConfigNode toRemove)
	{
		if(elements.remove(toRemove))
			nodeRemoved(this, toRemove);
	}
	
	void registerListener(InheritableConfigContainerListener listener)
	{
		listeners.add(listener);
	}
	
	void unregisterListener(InheritableConfigContainerListener listener)
	{
		listeners.remove(listener);
	}
	
	
	// Node implementations
	
	String getPrefix()
	{
		return PREFIX;
	}
	
	Map getContentForSerialization()
	{
		HashMap map = new HashMap();
		for(InheritableConfigNode i : elements)
		{
			if(i.isMonitor) // ignore non-overriding instances
				continue;
			map.put(i.getPrefix()+i.getName(), i.getContentForSerialization());
		}
		
		return map;
	}
	
	@Override
	protected void nodeAdded(InheritableConfigContainer atParent, InheritableConfigNode node)
	{
		for(InheritableConfigContainerListener i : listeners)
		{
			i.nodeAdded(atParent, node);
		}
	}
	
	@Override
	protected void nodeRemoved(InheritableConfigContainer fromParent, InheritableConfigNode node)
	{
		for(InheritableConfigContainerListener i : listeners)
		{
			i.nodeRemoved(fromParent, node);
		}
	}
}

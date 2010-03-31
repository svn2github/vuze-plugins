package com.aelitis.azbuddy.config;

import java.util.HashMap;
import java.util.Set;

public class UninheritableMap extends InheritableConfigContainer {
	
	static final String PREFIX = "m";
	
	private final HashMap<String, InheritableConfigNode> values = new HashMap<String, InheritableConfigNode>();
	
	UninheritableMap(InheritableConfigContainer parent, String name)
	{
		super(parent, name, false);
	}
	
	public static UninheritableMap getInstance(InheritableConfigContainer parent, String name)
	{
		UninheritableMap newMap = null;
		
		// fetch existing value to preserve singleton status
		if(parent != null)
			newMap = parent.containsValue(UninheritableMap.class, name);
		
		// use new one otherwise
		if(newMap == null)
			newMap = new UninheritableMap(parent,name);
		
		return newMap;
	}

	<T extends InheritableConfigNode> T containsValue(Class<T> type, String name)
	{
		if(type.isInstance(values.get(name)))
			return (T)values.get(name);
		return null;
	}
	
	public void addValueNode(InheritableConfigNode toAdd)
	{
		values.put(toAdd.getName(), toAdd);		
	}
	
	public void put(InheritableConfigNode toAdd)
	{
		addValueNode(toAdd);
	}
	
	void removeValueNode(InheritableConfigNode toRemove)
	{
		values.remove(toRemove.getName());
	}
	
	public void remove(String toRemove)
	{
		values.remove(toRemove);
	}
	
	public <T extends InheritableConfigNode> T get(Class<T> type, String name)
	{
		return containsValue(type, name);
	}
	
	public Set<String> getKeySet()
	{
		return values.keySet();
	}
	
	public void clear()
	{
		values.clear();
	}
	
	// do nothing, as we do not inform items of events
	void registerListener(InheritableConfigContainerListener listener) {}
	void unregisterListener(InheritableConfigContainerListener listener) {}
	
	// do nothing, as we don't inherit	
	protected void nodeAdded(InheritableConfigContainer atParent, InheritableConfigNode node) {}
	protected void nodeRemoved(InheritableConfigContainer fromParent, InheritableConfigNode node) {}
	
	
	String getPrefix() { return PREFIX; }
	HashMap getContentForSerialization()
	{
		HashMap map = new HashMap();
		for(String i : getKeySet())
		{
			map.put(values.get(i).getPrefix()+i, values.get(i).getContentForSerialization()); 
		}
		return map;
	}
	
	
	
	

}

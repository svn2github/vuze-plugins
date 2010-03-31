package com.aelitis.azbuddy.config;

abstract class InheritableConfigContainer extends InheritableConfigNode {

	InheritableConfigContainer(InheritableConfigContainer parent, String name, boolean startAsMonitor)
	{
		super(parent, name, startAsMonitor);
	}
	
	
	@Override
	public boolean isMonitor()
	{
		return isMonitor;
	}
	
	/**
	 * If a {@link InheritableConfigContainer} is set to Monitor state it's parent won't serialize it and all its children and thus has to be serialized seperately  
	 */
	@Override
	public boolean setMonitoringState(boolean setState)
	{
		isMonitor = setState;
		return isMonitor();
	}
	
	// adding the necessary methods for a container
	abstract public void addValueNode(InheritableConfigNode toAdd);
	abstract <T extends InheritableConfigNode> T containsValue(Class<T> type, String name);
	abstract void removeValueNode(InheritableConfigNode toRemove);
	abstract void registerListener(InheritableConfigContainerListener listener);
	abstract void unregisterListener(InheritableConfigContainerListener listener);

}

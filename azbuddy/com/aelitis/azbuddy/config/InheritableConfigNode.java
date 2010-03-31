package com.aelitis.azbuddy.config;

abstract class InheritableConfigNode {

	protected final String name;
	protected final InheritableConfigContainer parent;
	protected boolean isMonitor;
	protected InheritableConfigNode nearestAncestor;
	protected InheritableConfigContainerListener myListener = new InheritableConfigContainerListener() {
		public void nodeAdded(InheritableConfigContainer atParent, InheritableConfigNode node)
		{
			InheritableConfigNode.this.nodeAdded(atParent, node);
		}

		public void nodeRemoved(InheritableConfigContainer fromParent, InheritableConfigNode node)
		{
			InheritableConfigNode.this.nodeRemoved(fromParent, node);
		}
	};

	InheritableConfigNode(InheritableConfigContainer parent, String name, boolean startAsMonitor)
	{
		this.parent = parent;
		this.name = name;
		if(parent != null)
		{
			parent.addValueNode(this);
			parent.registerListener(myListener);
		}


		nearestAncestor = discoverAncestor();

		setMonitoringState(startAsMonitor);
	}

	public String getName()
	{
		return name;		
	}

	public InheritableConfigContainer getParent()
	{
		return parent;		
	}

	boolean isSameKey(InheritableConfigNode otherNode)
	{
		return name.equalsIgnoreCase(otherNode.getName()) && getPrefix().equals(otherNode.getPrefix());
	}

	private InheritableConfigNode discoverAncestor()
	{
		if(parent == null) // no parent -> no ancestor
			return null;
		
		InheritableConfigContainer parentLevel = parent.getParent();
		InheritableConfigNode ancestor = null;
		while(parentLevel != null)
		{
			if((ancestor = parentLevel.containsValue(this.getClass(), name)) != null)
				break;
			parentLevel = parentLevel.getParent();
		}
		return ancestor;
	}

	public boolean setMonitoringState(boolean tryState)
	{
		if(nearestAncestor == null)
			isMonitor = false;
		else
			isMonitor = tryState;
		return isMonitor();
	}

	public boolean isMonitor()
	{
		return isMonitor;
	}

	public boolean hasAncestor()
	{
		return nearestAncestor != null;
	}


	public void unregister()
	{
		if(parent != null)
		{
			parent.unregisterListener(myListener);
			parent.removeValueNode(this);
		}

	}

	protected void nodeAdded(InheritableConfigContainer atParent, InheritableConfigNode node)
	{
		if(isSameKey(node))
		{
			nearestAncestor = discoverAncestor();
			if(nearestAncestor == null)
				throw new IllegalStateException("This should never happen");
		}
	}

	protected void nodeRemoved(InheritableConfigContainer fromParent, InheritableConfigNode node)
	{
		if(isSameKey(node))
		{
			nearestAncestor = discoverAncestor();
			if(nearestAncestor == null)
				setMonitoringState(false);
		}
	}


	abstract String getPrefix();	
	abstract Object getContentForSerialization();
}

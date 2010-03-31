package com.aelitis.azbuddy.config;

interface InheritableConfigContainerListener {
	void nodeAdded(InheritableConfigContainer atParent, InheritableConfigNode node);
	void nodeRemoved(InheritableConfigContainer fromParent, InheritableConfigNode node);
}

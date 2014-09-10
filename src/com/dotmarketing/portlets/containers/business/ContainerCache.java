package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.containers.model.Container;

//This interface should have default package access
public abstract class ContainerCache implements Cachable {
	
	abstract protected Container add(String identifier, Container container);

	abstract protected Container getWorking(String identifier);
	
	abstract protected Container getLive(String identifier);

	abstract public void clearCache();

	abstract public void remove(String identifier);
}
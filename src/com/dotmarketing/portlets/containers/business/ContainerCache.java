package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.containers.model.Container;

//This interface should have default package access
public abstract class ContainerCache implements Cachable {

	abstract protected Container add(String key, Container container);

	abstract protected Container get(String key);

	abstract public void clearCache();

	abstract public void remove(String key);
}
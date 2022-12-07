package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.templates.model.Template;

//This interface should have default package access
public abstract class ContainerCache implements Cachable {
    abstract protected Container add(String key, Container container);

    public abstract Container get(String inode);
    
	abstract public void clearCache();

	abstract public void remove(Container inode);

    public void remove(VersionInfo cvinfo) {
        // TODO Auto-generated method stub
        
    }
}
package com.dotmarketing.portlets.containers.business;

import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.containers.model.Container;

//This interface should have default package access
public abstract class ContainerCache implements Cachable {
	
	public abstract Container add(Container container);
	
    public abstract Container get(String inode);
    
	abstract public void clearCache();

	abstract public void remove(Container inode);

    public void remove(VersionInfo cvinfo) {
        // TODO Auto-generated method stub
        
    }
}
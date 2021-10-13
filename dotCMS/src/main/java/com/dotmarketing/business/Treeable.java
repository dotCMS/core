package com.dotmarketing.business;

import java.util.Date;
import java.util.Map;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

public interface Treeable extends Permissionable{

	public String getInode();

	public String getIdentifier();

	public String getType();

	public Date getModDate();

	public String getName();
	
	public default int getMenuOrder() {
	    return -1;
	}

	public Map<String, Object> getMap() throws DotStateException, DotDataException, DotSecurityException;

}

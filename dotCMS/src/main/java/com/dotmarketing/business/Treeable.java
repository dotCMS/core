package com.dotmarketing.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface Treeable extends Permissionable{

	public String getInode();

	public String getIdentifier();

	public String getType();

	public Date getModDate();

	public String getName();

	public Map<String, Object> getMap() throws DotStateException, DotDataException, DotSecurityException;

}

package com.dotmarketing.portlets.hostvariable.bussiness;

import java.util.List;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;

//This interface should have default package access
public abstract class HostVariablesCache implements Cachable {

	abstract protected List<HostVariable> put(List<HostVariable> variables);

	abstract protected List<HostVariable> getAll();

	abstract public void clearCache();

}
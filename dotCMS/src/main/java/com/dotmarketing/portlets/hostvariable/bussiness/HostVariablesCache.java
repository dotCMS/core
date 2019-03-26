package com.dotmarketing.portlets.hostvariable.bussiness;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import java.util.List;

// This interface should have default package access
public abstract class HostVariablesCache implements Cachable {

  protected abstract List<HostVariable> put(List<HostVariable> variables);

  protected abstract List<HostVariable> getAll();

  public abstract void clearCache();
}

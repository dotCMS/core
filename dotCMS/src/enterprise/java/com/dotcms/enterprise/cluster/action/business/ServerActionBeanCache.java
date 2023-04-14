package com.dotcms.enterprise.cluster.action.business;

import java.util.List;

import com.dotcms.enterprise.cluster.action.model.ServerActionBean;
import com.dotmarketing.business.Cachable;

public interface ServerActionBeanCache extends Cachable {

  List<ServerActionBean> getServerActions();
  
  void clearServerActions();

  void putServerActions(List<ServerActionBean> serverActionBeans);
  
  
}

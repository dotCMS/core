package com.dotcms.datagen;

import java.util.HashMap;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.ejb.PortletManagerFactory;
import com.liferay.portal.model.Portlet;

public class PortletDataGen extends AbstractDataGen<Portlet> {

  private String portletId, companyId, groupId, portletClass, roles, extendsPortletId;
  private Map<String, String> initParams;

  public PortletDataGen() {

  }

  public PortletDataGen portletId(String portletId) {
    this.portletId = portletId;
    return this;
  }

  public PortletDataGen companyId(String companyId) {
    this.companyId = companyId;
    return this;
  }

  public PortletDataGen groupId(String groupId) {
    this.groupId = groupId;
    return this;
  }

  public PortletDataGen portletClass(String portletClass) {
    this.portletClass = portletClass;
    return this;
  }

  public PortletDataGen roles(String roles) {
    this.roles = roles;
    return this;
  }

  public PortletDataGen initParams(Map<String, String> initParams) {
    this.initParams = initParams;
    return this;
  }

  public PortletDataGen extendsPortlet(Portlet extendsPortlet) {

    this.portletClass=extendsPortlet.getPortletClass();
    this.initParams=extendsPortlet.getInitParams();
    return this;
  }


  @Override
  public Portlet next() {

    Portlet portlet = new Portlet((portletId == null) ? UUIDGenerator.generateUuid() : portletId, 
        portletClass == null ? "com.liferay.portlet.JSPPortlet" : portletClass, initParams == null ? new HashMap<>() : initParams);

    return portlet;
  }

  @Override
  public Portlet persist(Portlet portlet) {
    try {
      return PortletManagerFactory.getManager().insertPortlet(portlet);
    } catch (DotDataException e) {
      throw new DotRuntimeException(e);
    }

  }

}

package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.ejb.PortletManagerFactory;
import com.liferay.portal.model.Portlet;
import java.util.HashMap;
import java.util.Map;

public class PortletDataGen extends AbstractDataGen<Portlet> {

  private String portletId, companyId, groupId, portletClass, roles, extendsPortletId;
  private Map<String, String> initParams;

  public PortletDataGen() {

  }

  public PortletDataGen portletId(final String portletId) {
    this.portletId = portletId;
    return this;
  }

  public PortletDataGen companyId(final String companyId) {
    this.companyId = companyId;
    return this;
  }

  public PortletDataGen groupId(final String groupId) {
    this.groupId = groupId;
    return this;
  }

  public PortletDataGen portletClass(final String portletClass) {
    this.portletClass = portletClass;
    return this;
  }

  public PortletDataGen roles(final String roles) {
    this.roles = roles;
    return this;
  }

  public PortletDataGen initParams(final Map<String, String> initParams) {
    this.initParams = initParams;
    return this;
  }

  public PortletDataGen extendsPortlet(final Portlet extendsPortlet) {

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

  @WrapInTransaction
  @Override
  public Portlet persist(final Portlet portlet) {
    try {
      return PortletManagerFactory.getManager().insertPortlet(portlet);
    } catch (DotDataException e) {
      throw new DotRuntimeException(e);
    }

  }

}

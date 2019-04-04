package com.dotcms.rest.api.v1.portlet;

import static com.dotcms.util.CollectionsUtils.map;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.jersey.repackaged.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.portal.DotPortlet;
import com.dotmarketing.business.portal.PortletAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Portlet;

import io.vavr.control.Try;


@Path("/v1/portlet")
@SuppressWarnings("serial")
public class PortletResource implements Serializable {

  private static final String ROLE_ID_SEPARATOR = ",";

  private final WebResource webResource;
  private final PortletAPI portletApi;
  private final SystemMessageEventUtil systemMessageEventUtil =
      SystemMessageEventUtil.getInstance();
  private final LanguageAPI langApi =
      APILocator.getLanguageAPI();
  
  /**
   * Default class constructor.
   */
  public PortletResource() {
    this(new WebResource(new ApiProvider()), APILocator.getPortletAPI());
  }

  @VisibleForTesting
  public PortletResource(WebResource webResource, PortletAPI portletApi) {
    this.webResource = webResource;
    this.portletApi = portletApi;
  }

  @POST
  @Path("/custom")
  @JSONP
  @NoCache
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
  public final Response contentPortlet(@Context final HttpServletRequest request,
                                 @Context final HttpServletResponse response,
                                 final CustomPortletForm formData) {
    InitDataObject init = webResource.init(null, true,request,true, "roles");
    
    if(formData.portletId==null) {
      return ExceptionMapperUtil.createResponse(new DotStateException("Portlet Id is required"), Response.Status.BAD_REQUEST);
    }
    if(formData.portletName==null) {
      return ExceptionMapperUtil.createResponse(new DotStateException("Portlet Nane is required"), Response.Status.BAD_REQUEST);
    }
    
    final String portletId = "c-" + formData.portletId;
    Portlet portlet = portletApi.findPortlet(portletId);
    if(portlet!=null) {
      return ExceptionMapperUtil.createResponse(new DotStateException("PortletId already Exists"), Response.Status.CONFLICT);
    }
    List<String> contentTypes= formData.resolveContentTypes().stream().map(ct->ct.variable()).collect(Collectors.toList());
    List<String> baseTypes= formData.resolveBaseTypes().stream().map(bt->bt.name()).collect(Collectors.toList());
   
    if(contentTypes.size() + baseTypes.size()==0) {
      return ExceptionMapperUtil.createResponse(new DotStateException("You must specify at least one baseType or Content Type"), Response.Status.BAD_REQUEST);
    }
    
    
    final Portlet contentPortlet = portletApi.findPortlet("content");
    Map<String,String> initValues=new HashMap<>();
    
    initValues.putAll(contentPortlet.getInitParams());
    initValues.put("name", formData.portletName);
    initValues.put("baseTypes", String.join(",", baseTypes));
    initValues.put("contentTypes", String.join(",", contentTypes));
    initValues.put("portletSource", "db");
    

    Portlet newPortlet = new DotPortlet(portletId, contentPortlet.getPortletClass(), initValues);
    

    
    APILocator.getPortletAPI().savePortlet(newPortlet);

    Map<String, String> keys = ImmutableMap.of(com.dotcms.repackage.javax.portlet.Portlet.class.getPackage().getName() + ".title."+portletId, formData.portletName);
    try {
      for(Language lang : langApi.getLanguages()) {
        APILocator.getLanguageAPI().saveLanguageKeys(lang, keys, ImmutableMap.of(), ImmutableSet.of());
      }
    } catch (DotDataException e) {
      Logger.warnAndDebug(this.getClass(), e.getMessage(), e);
    }
    
    systemMessageEventUtil.pushSimpleTextEvent(Try.of(()-> LanguageUtil.get(init.getUser(), "custom.content.portlet.created")).getOrElse("Custom Content Created"), init.getUser().getUserId(), "roles");

    return Response.ok(new ResponseEntityView(map("portlet", newPortlet.getPortletId()))).build();
  }

}

package com.dotmarketing.velocity.directive;

import javax.servlet.http.HttpServletRequest;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

public class RenderParams {
  public static final String RENDER_PARAMS_ATTRIBUTE = "com.dotcms.directive.renderparams";
  final boolean live;
  final User user;
  final Language language;
  final Host currentHost;
  final boolean editMode;


  public RenderParams(HttpServletRequest request) {
    this(request, (RenderParams) request.getAttribute(RENDER_PARAMS_ATTRIBUTE));

  }

  RenderParams(HttpServletRequest request, RenderParams params) {
    if (params != null) {
      this.live = params.live;
      this.user = params.user;
      this.language = params.language;
      this.currentHost = params.currentHost;
      this.editMode = params.editMode;
    } else {
      PageMode mode = PageMode.get(request);
      live = mode.showLive;
      user = WebAPILocator.getUserWebAPI().getUser(request);
      language = WebAPILocator.getLanguageWebAPI().getLanguage(request);
      currentHost = WebAPILocator.getHostWebAPI().getHost(request);
      editMode = mode == PageMode.EDIT;
      request.setAttribute(RENDER_PARAMS_ATTRIBUTE, this);
    }
    
  }



}

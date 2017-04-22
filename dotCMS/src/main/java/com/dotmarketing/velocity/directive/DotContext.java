package com.dotmarketing.velocity.directive;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ChainedContext;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.PageRequestModeUtil;
import com.liferay.portal.model.User;

public class DotContext extends ChainedContext implements Context {

  final boolean live;
  final long languageId;
  final User user;



  public DotContext(Context ctx, VelocityEngine velocity, HttpServletRequest request, HttpServletResponse response,
      ServletContext application) {
    super(ctx, velocity, request, response, application);
    live = isLive(request);
    languageId = WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();
    user = WebAPILocator.getUserWebAPI().getUser(request);
  }



  private boolean isLive(HttpServletRequest request) {
    boolean live = true;
    if (request != null) {
      HttpSession session = request.getSession(false);
      if (session != null) {
        if (session != null) {
          live = !PageRequestModeUtil.isAdminMode(session) || !(PageRequestModeUtil.isPreviewMode(session))
              || !(PageRequestModeUtil.isEditMode(session));
        }
      }
    }

    return live;
  }

}

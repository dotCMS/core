package com.dotmarketing.velocity.directive;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.directive.InputBase;

import com.dotmarketing.util.PageRequestModeUtil;
import com.dotmarketing.util.StringUtils;

abstract class DotDirective extends InputBase {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;



  boolean isLive(Context context) {

    /*
     * now use the Runtime resource loader to get the template
     */
    HttpServletRequest request = (HttpServletRequest) context.get("request");



    boolean live = true;
    if (request != null) {
      HttpSession session = request.getSession(false);
      if (session != null) {
        if (session != null) {
          live = !PageRequestModeUtil.isAdminMode(session) 
              || !(PageRequestModeUtil.isPreviewMode(session))
              || !(PageRequestModeUtil.isEditMode(session));
        }
      }
    }

    return live;

  }



}

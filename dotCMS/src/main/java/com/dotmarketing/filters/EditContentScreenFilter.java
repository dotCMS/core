package com.dotmarketing.filters;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;

import javax.servlet.*;
import java.io.IOException;
import java.util.Arrays;

public class EditContentScreenFilter implements Filter {

    private static String CONTENT_LAYOUT_NAME = "Content";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(
            final ServletRequest servletRequest,
            final ServletResponse servletResponse,
            final FilterChain filterChain) throws ServletException {

        try {
            final Layout contentLayout = APILocator.getLayoutAPI().findLayoutByName(CONTENT_LAYOUT_NAME);
            final String[] urlSplit = new String[]{
                    "/c/portal/layout",
                    String.format("?p_l_id=%s", contentLayout.getId()),
                    "&p_p_id=content",
                    "&p_p_action=1",
                    "&p_p_state=maximized",
                    "&p_p_mode=view",
                    "&_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet",
                    String.format("&_content_cmd=edit&inode=%s", servletRequest.getParameter("inode"))
            };

            final String url = String.join("", Arrays.asList(urlSplit));
            servletRequest.getRequestDispatcher(url).forward(servletRequest, servletResponse);
        } catch(Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void destroy() {

    }
}

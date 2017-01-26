package com.dotcms.taglib;

import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * This is basically the same of {@link com.dotcms.repackage.com.liferay.portlet.taglib.NamespaceTag}
 * but normalize (aka remove dashes by underscore)
 *
 * @author jsanca
 */
public class NamespaceTag extends TagSupport {

    public  static final String COM_DOTCMS_REPACKAGE_JAVAX_PORTLET_RESPONSE = "com.dotcms.repackage.javax.portlet.response";
    private static final String SEARCH_STRING = "-";
    private static final String REPLACEMENT   = "_";

    public int doStartTag() throws JspTagException {

        ServletRequest servletRequest = null;
        RenderResponse renderResponse = null;
        String namespace              = null;

        try {

            servletRequest = this.pageContext.getRequest();
            renderResponse = (RenderResponse)servletRequest.getAttribute(COM_DOTCMS_REPACKAGE_JAVAX_PORTLET_RESPONSE);
            namespace      = renderResponse.getNamespace();

            if (null != namespace) {

                namespace = StringUtils.replace(namespace,
                        SEARCH_STRING, REPLACEMENT);
            }

            this.pageContext.getOut().print(namespace);
            return SKIP_BODY;
        } catch (Exception e) {

            throw new JspTagException(e.getMessage());
        }
    } // doStartTag.

} // E:O:F:NamespaceTag.

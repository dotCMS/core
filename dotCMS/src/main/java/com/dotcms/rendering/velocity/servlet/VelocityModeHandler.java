package com.dotcms.rendering.velocity.servlet;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.jersey.repackaged.com.google.common.collect.ImmutableMap;
import com.dotcms.visitor.business.VisitorAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.exception.ParseErrorException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

public abstract class VelocityModeHandler {

    protected static final String CHARSET = Config.getStringProperty("CHARSET", "UTF-8");
    protected static final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    protected static final VisitorAPI visitorAPI = APILocator.getVisitorAPI();

    private static final Map<PageMode, Function> pageModeVelocityMap =ImmutableMap.<PageMode, VelocityModeHandler.Function>builder()
            .put(PageMode.PREVIEW_MODE, VelocityPreviewMode::new)
            .put(PageMode.EDIT_MODE, VelocityEditMode::new)
            .put(PageMode.LIVE, VelocityLiveMode::new)
            .put(PageMode.ADMIN_MODE, VelocityAdminMode::new)
            .put(PageMode.NAVIGATE_EDIT_MODE, VelocityNavigateEditMode::new)
            .build();

    @FunctionalInterface
    private interface Function {
        VelocityModeHandler apply(HttpServletRequest request, HttpServletResponse response, String uri, Host host);
    }


    abstract void serve() throws DotDataException, IOException, DotSecurityException;

    abstract void serve(OutputStream out) throws DotDataException, IOException, DotSecurityException;


    public final String eval() {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(4096)) {
            serve(out);
            return new String(out.toByteArray());
        } catch (DotDataException | IOException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
    
    public static final VelocityModeHandler modeHandler(PageMode mode, HttpServletRequest request, HttpServletResponse response, String uri, Host host) {
        return pageModeVelocityMap.get(mode).apply(request, response, uri, host);
    }
    
    public static final VelocityModeHandler modeHandler(PageMode mode, HttpServletRequest request, HttpServletResponse response) {
        return pageModeVelocityMap.get(mode).apply(request, response, request.getRequestURI(), hostWebAPI.getCurrentHostNoThrow(request));
    }

    public final Template getTemplate(IHTMLPage page, PageMode mode) {

        return VelocityUtil.getEngine().getTemplate(mode.name() + File.separator + page.getIdentifier() + "_"
                + page.getLanguageId() + "." + VelocityType.HTMLPAGE.fileExtension);
    }

    protected void handleParseError (final ParseErrorException e, final String name, final User user) {

        try {

            final SystemMessageBuilder systemMessageBuilder = new SystemMessageBuilder();
            final StringBuilder message                     = new StringBuilder();
            final String errorMessage                       = WordUtils.wrap
                    (e.getMessage(), 15, "<br/>", false);

            message.append("<h3>").append("Parsing Error").append("</h3>")
                    .append("<b>Name: </b>").append("<br/>&nbsp;&nbsp;")
                    .append(name).append("<br/>")
                    .append("<b>Template: </b>").append("<br/>&nbsp;&nbsp;")
                    .append(e.getTemplateName()).append("<br/>")

                    .append("<b>Invalid Syntax: </b>").append("<br/>&nbsp;&nbsp;")
                    .append(e.getInvalidSyntax()).append("<br/>")

                    .append("<b>Column Number: </b>").append("<br/>&nbsp;&nbsp;")
                    .append(e.getColumnNumber()).append("<br/>")

                    .append("<b>Line Number: </b>").append("<br/>&nbsp;&nbsp;")
                    .append(e.getLineNumber()).append("<br/>")

                    .append("<pre>")
                    .append(errorMessage)
                    .append("</pre>");

            systemMessageBuilder.setMessage(message.toString())
                    .setLife(DateUtil.FIVE_SECOND_MILLIS)
                    .setType(MessageType.SIMPLE_MESSAGE)
                    .setSeverity(MessageSeverity.ERROR);

            SystemMessageEventUtil.getInstance().
                    pushMessage(systemMessageBuilder.create(), Arrays.asList(user.getUserId()));
        } catch (Exception ex) {
            Logger.error(this, ex.getMessage(), ex);
        }
    } // handleParseError.
}

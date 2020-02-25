package com.dotcms.rendering.velocity.servlet;

import com.dotcms.rendering.velocity.events.PreviewEditParseErrorException;
import com.dotcms.rendering.velocity.services.PageRenderUtil;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class VelocityPreviewMode extends VelocityModeHandler {

    @Deprecated
    public VelocityPreviewMode(final HttpServletRequest request, final HttpServletResponse response, final String uri, final Host host) {
        this(
                request,
                response,
                VelocityModeHandler.getHtmlPageFromURI(PageMode.get(request), request, uri, host),
                host
        );
    }

    protected VelocityPreviewMode(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final IHTMLPage htmlPage,
            final Host host) {

        super(request, response, htmlPage, host);
        this.setMode(PageMode.PREVIEW_MODE);
    }

    @Override
    public void serve() throws DotDataException, IOException, DotSecurityException {

        serve(response.getOutputStream());

    }

    @Override
    public void serve(final OutputStream out) throws DotDataException, IOException, DotSecurityException {


        // Getting the user to check the permissions
        final  User user = WebAPILocator.getUserWebAPI().getUser(request);

         // creates the context where to place the variables
        response.setContentType(CHARSET);
        Context context = VelocityUtil.getWebContext(request, response);

        context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), user, mode, host, context));


        new PageRenderUtil((HTMLPageAsset) htmlPage, user, PageMode.PREVIEW_MODE).addAll(context);
        context.put("dotPageContent", new ContentMap(((Contentlet) htmlPage), user, mode, host, context));

        request.setAttribute("velocityContext", context);
        try(final Writer outStr = new BufferedWriter(new OutputStreamWriter(out))){
            this.getTemplate(htmlPage, mode).merge(context, outStr);
        } catch (PreviewEditParseErrorException e) {
            this.processException(user, htmlPage.getName(), e);
        }
    }

}

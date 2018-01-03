package com.dotcms.rendering.velocity.servlet;

import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.visitor.business.VisitorAPI;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;

public abstract class VelocityModeHandler {

    protected static final String CHARSET = Config.getStringProperty("CHARSET", "UTF-8");
    protected static final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    protected static final VisitorAPI visitorAPI = APILocator.getVisitorAPI();



    abstract void serve() throws DotDataException, IOException, DotSecurityException;

    abstract void serve(Writer out) throws DotDataException, IOException, DotSecurityException;


    public final String eval() {
        StringWriter out = new StringWriter(4096);

        try {
            serve(out);
        } catch (DotDataException | IOException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }

        return out.toString();
    }

    public static final VelocityModeHandler modeHandler(PageMode mode, HttpServletRequest request, HttpServletResponse response) {
        switch (mode) {
            case PREVIEW_MODE:
                return new VelocityPreviewMode(request, response);
            case EDIT_MODE:
                return new VelocityEditMode(request, response);
            default:
                return new VelocityLiveMode(request, response);

        }
    }

    public final Template getTemplate(IHTMLPage page, PageMode mode) {

        return VelocityUtil.getEngine().getTemplate(mode.name() + File.separator + page.getIdentifier() + "_"
                + page.getLanguageId() + "." + VelocityType.HTMLPAGE.fileExtension);
    }


}

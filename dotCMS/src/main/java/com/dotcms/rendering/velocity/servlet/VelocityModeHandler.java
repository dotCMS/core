package com.dotcms.rendering.velocity.servlet;

import com.dotcms.visitor.business.VisitorAPI;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;

import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class VelocityModeHandler {
    
    protected String CHARSET = Config.getStringProperty("CHARSET", "UTF-8");
    protected static HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    protected static VisitorAPI visitorAPI = APILocator.getVisitorAPI();
    
    
    
    abstract void serve() throws Exception;

    abstract void serve(Writer out) throws Exception;


    public final String eval() {
        StringWriter out = new StringWriter(4096);
        try {
            serve(out);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
        return out.toString();
    }

    public final static VelocityModeHandler modeHandler(PageMode mode, HttpServletRequest request, HttpServletResponse response) {
        switch (mode) {
            case PREVIEW_MODE:
                return new VelocityPreviewMode(request, response);
            case EDIT_MODE:
                return new VelocityEditMode(request, response);
            default:
                return new VelocityLiveMode(request, response);

        }
    }
    
    
    
    
}

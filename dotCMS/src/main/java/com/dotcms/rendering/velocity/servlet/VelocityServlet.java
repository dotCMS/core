package com.dotcms.rendering.velocity.servlet;

import com.dotcms.business.CloseDB;
import com.dotcms.rendering.velocity.viewtools.RequestWrapper;
import com.dotcms.visitor.business.VisitorAPI;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;

import java.io.IOException;
import java.net.URLDecoder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

public class VelocityServlet extends HttpServlet {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    static HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    static VisitorAPI visitorAPI = APILocator.getVisitorAPI();
    String CHARSET = Config.getStringProperty("CHARSET", "UTF-8");


    @CloseDB
    protected final void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        RequestWrapper request = new RequestWrapper(req);
        final String uri = URLDecoder.decode((request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE) != null)
                ? (String) request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE)
                : request.getRequestURI(), "UTF-8");

        if (uri == null) {
            response.sendError(500, "VelocityServlet called without running through the CMS Filter");
            Logger.error(this.getClass(),
                    "You cannot call the VelocityServlet without passing the requested url via a  requestAttribute called  "
                            + Constants.CMS_FILTER_URI_OVERRIDE);
            return;
        }

        request.setRequestUri(uri);

        PageMode mode = PageMode.get(request);



        try {
            switch (mode) {
                case PREVIEW_MODE:
                    new VelocityPreviewMode(request, response).serve();
                    break;
                case EDIT_MODE:
                    new VelocityEditMode(request, response).serve();
                    break;
                default:
                    new VelocityLiveMode(request, response).serve();
                    break;
            }



        } catch (ResourceNotFoundException rnfe) {
            Logger.error(this, "ResourceNotFoundException" + rnfe.toString(), rnfe);
            response.sendError(404);
            return;
        } catch (ParseErrorException pee) {
            Logger.error(this, "Template Parse Exception : " + pee.toString(), pee);
            response.sendError(500, "Template Parse Exception");
        } catch (MethodInvocationException mie) {
            Logger.error(this, "MethodInvocationException" + mie.toString(), mie);
            response.sendError(500, "MethodInvocationException Error on template");
        } catch (Exception e) {
            Logger.error(this, "Exception" + e.toString(), e);
            response.sendError(500, "Exception Error on template");

        } 

    }

    @Override
    public void init(ServletConfig config) throws ServletException {



    }



}

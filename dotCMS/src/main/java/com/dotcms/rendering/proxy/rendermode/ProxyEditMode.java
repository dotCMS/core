package com.dotcms.rendering.proxy.rendermode;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Mock;

import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.mock.response.MockHttpCaptureResponse;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rendering.RenderModeHandler;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

public class ProxyEditMode extends RenderModeHandler {

    protected final HttpServletRequest request;
    protected final HttpServletResponse response;
    private static final PageMode mode = PageMode.EDIT_MODE;
    protected final String uri;
    private final Host host;
    private final User user;

    public ProxyEditMode(HttpServletRequest request, HttpServletResponse response, String uri, Host host) {
        this.request = request;
        this.response = response;
        this.uri = uri;
        this.host = host;
        this.user = WebAPILocator.getUserWebAPI().getUser(request);
    }

    public ProxyEditMode(HttpServletRequest request, HttpServletResponse response) {
        this(request, response, request.getRequestURI(), hostWebAPI.getCurrentHostNoThrow(request));
    }


    public void serve(final OutputStream out) throws DotDataException, IOException, DotSecurityException {


        MockHttpResponse mockRes= new MockHttpResponse(new BaseResponse().response());
        MockHttpRequest mockreq = new MockHttpRequest("localhost", request.getRequestURI());
        
        
        ProxyRequest proxy = new ProxyRequest(request, mockRes.response(), mode);
       
        
        
        
        
        try {

            proxy.service();
            
            out.write(proxy.getResponse().toByteArray());
            
            
            
        } catch (Exception e) {
            Logger.warn(this.getClass(), e.getMessage(),e);
        }
        

    }

    @Override
    public void serve() throws DotDataException, IOException, DotSecurityException {
        serve(response.getOutputStream());
    }

    
    


}

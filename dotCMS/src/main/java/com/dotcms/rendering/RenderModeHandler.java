package com.dotcms.rendering;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;

import com.dotcms.rendering.proxy.rendermode.ProxyRenderModeMapper;
import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
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
import com.dotmarketing.util.PageMode;

public abstract class RenderModeHandler {

    protected static final String CHARSET = Config.getStringProperty("CHARSET", "UTF-8");
    protected static final HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
    protected static final VisitorAPI visitorAPI = APILocator.getVisitorAPI();

    public static final List<RenderModeMapper> renderModeMappers = new CopyOnWriteArrayList<>();
    private static final VelocityModeMap defaultModeMap = new VelocityModeMap();
    
    @FunctionalInterface
    public interface Function {
        RenderModeHandler apply(HttpServletRequest request, HttpServletResponse response, String uri, Host host);
    }
    static {
        renderModeMappers.add(new ProxyRenderModeMapper());
    }

    public abstract void serve() throws DotDataException, IOException, DotSecurityException;

    public abstract void serve(OutputStream out) throws DotDataException, IOException, DotSecurityException;


    public final String eval() {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(4096)) {
            serve(out);
            return new String(out.toByteArray());
        } catch (DotDataException | IOException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
    
    public static final RenderModeHandler modeHandler(PageMode mode, HttpServletRequest request, HttpServletResponse response, String uri, Host host) {
        return resolveMapper(request).getModMop().get(mode).apply(request, response, uri, host);
    }
    
    public static final RenderModeHandler modeHandler(PageMode mode, HttpServletRequest request, HttpServletResponse response) {
        return resolveMapper(request).getModMop().get(mode).apply(request, response, request.getRequestURI(), hostWebAPI.getCurrentHostNoThrow(request));
    }

    public final Template getTemplate(IHTMLPage page, PageMode mode) {

        return VelocityUtil.getEngine().getTemplate(mode.name() + File.separator + page.getIdentifier() + "_"
                + page.getLanguageId() + "." + VelocityType.HTMLPAGE.fileExtension);
    }


    
    private static final RenderModeMapper resolveMapper(HttpServletRequest request) {
        for(RenderModeMapper map : renderModeMappers) {
            if(map.useModes(request)) {
                return map;
            }
        }
        return defaultModeMap;
    }
    
    
    
}

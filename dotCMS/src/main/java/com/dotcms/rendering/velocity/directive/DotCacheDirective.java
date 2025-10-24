package com.dotcms.rendering.velocity.directive;

import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Duration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.BlockDirectiveCache;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.PageMode;
import io.vavr.control.Try;

public class DotCacheDirective extends Directive {

    public static final String DONT_USE_DIRECTIVE_CACHE = "_dotDontUseDirectiveCache";

    private static final long serialVersionUID = 1L;

    public String getName() {
        return "dotcache";
    }

    public int getType() {
        return BLOCK;
    }

    private static final String REFRESH="refresh";

    public boolean render(InternalContextAdapter context, Writer writer, Node node)
                    throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {



        HttpServletRequest request = (HttpServletRequest) context.get("request");
        boolean shouldCache = shouldCache(context, request);
        boolean refreshCache = refreshCache(request);

        int ttl = 0;
        Object ttlObj = node.jjtGetChild(1).value(context);
        if (ttlObj instanceof Integer) {
            ttl = (Integer) node.jjtGetChild(1).value(context);
        } else{
            Logger.debug(this.getClass(), "TTL value for #dotcache must be an Integer and cannot be null. Using ttl = 0");
        }
        if (!shouldCache || ttl <= 0) {
            node.jjtGetChild(2).render(context, writer);
            return true;
        }


        final String key = String.valueOf(node.jjtGetChild(0).value(context));
        if (refreshCache) {
            CacheLocator.getBlockDirectiveCache().remove(key);
        }

        Map<String, Serializable> savedMap = CacheLocator.getBlockDirectiveCache().get(key);

        if (savedMap.containsKey(BlockDirectiveCache.PAGE_CONTENT_KEY)) {
            writer.write((String) savedMap.get(BlockDirectiveCache.PAGE_CONTENT_KEY));
            return true;
        }

        final StringWriter blockContent = new StringWriter();
        node.jjtGetChild(2).render(context, blockContent);
        writer.write(blockContent.toString());
        CacheLocator.getBlockDirectiveCache().add(key, Map.of(BlockDirectiveCache.PAGE_CONTENT_KEY, blockContent.toString()),
                Duration.ofSeconds(ttl));
        return true;


    }

    protected boolean allowExecution() {
        return LicenseManager.getInstance().isEnterprise();
    }


    boolean shouldCache(final Context context, HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        if (VelocityUtil.getDontUseDirectiveCache(context)) {
            return false;
        }
        if ("no".equals(request.getParameter(getName())) || "no".equals(request.getAttribute(getName()))) {
            return false;
        }
        if ("false".equals(request.getParameter(getName())) || "false".equals(request.getAttribute(getName()))) {
            return false;
        }
        if ("true".equals(request.getParameter(getName())) || "true".equals(request.getAttribute(getName()))) {
            return true;
        }
        if (refreshCache(request)) {
            return false;
        }
        
        if (!allowExecution()) {
            return false;
        }

        PageMode mode = PageMode.get(request);
        String url = Try.of(request::getRequestURI).getOrElse("");

        // force cache refresh for edit mode requests (but not velocity endpoint)
        if (mode.isAdmin && !url.contains("/api/vtl/dynamic")) {
            return false;
        }

        return true;


    }

    boolean refreshCache(HttpServletRequest request) {
        if (REFRESH.equals(request.getParameter(getName())) 
            || REFRESH.equals(request.getAttribute(getName()))) {
            return true;
        }
        return false;

    }
    
    
    

}

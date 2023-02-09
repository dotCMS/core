package com.dotcms.rendering.velocity.directive;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import com.dotcms.enterprise.license.LicenseManager;
import com.dotcms.enterprise.velocity.DotCachedInternalContextAdapter;
import com.dotmarketing.business.BlockDirectiveCache;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import io.vavr.Lazy;
import io.vavr.control.Try;

public class DotCacheDirective extends Directive {

    private static final long serialVersionUID = 1L;

    public String getName() {
        return "dotcache";
    }

    public int getType() {
        return BLOCK;
    }


    public boolean render(InternalContextAdapter context, Writer writer, Node node)
                    throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {


        boolean noCache = false;
        boolean refreshCache = false;



        HttpServletRequest request = (HttpServletRequest) context.get("request");

        if (request != null && request.getParameter(getName()) != null) {
            noCache = (request.getParameter(getName()).equals("no"));
            refreshCache = (request.getParameter(getName()).equals("refresh"));
        }


        if (!allowExecution()) {
            noCache = true;
        }
        
        PageMode mode = PageMode.get(request);
        // only force cache refresh for edit mode requests
        if (mode.isEditMode() || mode == PageMode.PREVIEW_MODE) {
            noCache = true;
        }

        if (noCache) {
            node.jjtGetChild(2).render(context, writer);
            return true;
        }


        final String key = String.valueOf(node.jjtGetChild(0).value(context));
        final int ttl = (Integer) node.jjtGetChild(1).value(context);
        if (refreshCache) {
            CacheLocator.getBlockDirectiveCache().remove(key);
        }

        Map<String, Serializable> savedMap = CacheLocator.getBlockDirectiveCache().get(key);

        if (savedMap.isEmpty() || !savedMap.containsKey(BlockDirectiveCache.PAGE_CONTENT_KEY)) {
            savedMap = new HashMap<>(savedMap);
            // read and parse content in
            final StringWriter content = new StringWriter();

            DotCachedInternalContextAdapter localContext = new DotCachedInternalContextAdapter(context, savedMap);

            node.jjtGetChild(2).render(localContext, content);
            if(!DOTCACHE_VELOCITY_CONTEXT.get()) {
                savedMap.clear();
            }
            savedMap.put(BlockDirectiveCache.PAGE_CONTENT_KEY, content.toString());
            
            CacheLocator.getBlockDirectiveCache().add(key, savedMap, ttl);

        } else if(DOTCACHE_VELOCITY_CONTEXT.get()) {
            savedMap.forEach(context::put);
        }
        writer.write((String) savedMap.get(BlockDirectiveCache.PAGE_CONTENT_KEY));


        return true;
    }

    protected boolean allowExecution() {
        return LicenseManager.getInstance().isEnterprise();
    }

    private static final ThreadLocal<StringWriter> STRINGWRITER = ThreadLocal.withInitial(StringWriter::new);


    private static final Lazy<Boolean> DOTCACHE_VELOCITY_CONTEXT = Lazy.of(()->Config.getBooleanProperty("DOTCACHE_VELOCITY_CONTEXT", false));
    
    
}

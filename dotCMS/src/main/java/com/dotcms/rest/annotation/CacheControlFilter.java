package com.dotcms.rest.annotation;

import com.dotcms.repackage.javax.annotation.Priority;
import com.dotcms.repackage.javax.ws.rs.Priorities;
import com.dotcms.repackage.javax.ws.rs.container.ContainerRequestContext;
import com.dotcms.repackage.javax.ws.rs.container.ContainerResponseContext;
import com.dotcms.repackage.javax.ws.rs.container.ContainerResponseFilter;
import com.dotcms.repackage.javax.ws.rs.core.MultivaluedMap;
import com.dotcms.rest.annotation.Cacheable;
import com.dotcms.rest.annotation.NoCache;

import java.io.IOException;
import java.lang.annotation.Annotation;

@Priority(Priorities.HEADER_DECORATOR)
public class CacheControlFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
		for (Annotation a : responseContext.getEntityAnnotations()) {
			if (a.annotationType() == Cacheable.class) {
				String cc = ((Cacheable) a).cc();
				headers.add("Cache-Control", cc);
			}else if (a.annotationType() == NoCache.class) {
				headers.add("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
				headers.add("Pragma", "no-cache"); // HTTP 1.0.
				headers.add("Expires", "Mon, 26 Jul 1997 05:00:00 GMT"); // Proxies.
			}
		}
		
    }
}
 

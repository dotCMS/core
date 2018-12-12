package com.dotcms.rest.api.v1.vtl;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotmarketing.business.APILocator;

public class DynamicGetMethod implements MethodToTest {
    @Override
    public Response execute(final VTLResourceIntegrationTest.HTTPMethodParams params) {
        final VTLResource resource = new VTLResource(APILocator.getHostAPI(), APILocator.getIdentifierAPI(),
                APILocator.getContentletAPI(), params.getWebResource());
        return resource.dynamicGet(params.getRequest(), params.getServletResponse(), params.getUriInfo(),
                params.getPathParam(), params.getBodyMap());
    }
}

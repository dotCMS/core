package com.dotcms.rest.api.v1.vtl;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotmarketing.business.APILocator;

public class GetMethod implements MethodToTest {

    @Override
    public Response execute(final VTLResourceIntegrationTest.HTTPMethodParams params) {
        final VTLResource resource = new VTLResource(APILocator.getHostAPI(), APILocator.getIdentifierAPI(),
                APILocator.getContentletAPI(), params.getWebResource());
        return resource.get(params.getRequest(), params.getServletResponse(), params.getUriInfo(), params.getFolderName(),
                params.getPathParam(), params.getBodyMap());
    }
}
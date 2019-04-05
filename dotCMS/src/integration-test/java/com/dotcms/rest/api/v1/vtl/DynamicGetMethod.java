package com.dotcms.rest.api.v1.vtl;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.api.MultiPartUtils;

public class DynamicGetMethod implements MethodToTest {
    @Override
    public Response execute(final VTLResourceIntegrationTest.HTTPMethodParams params) {
        final VTLResource resource = new VTLResource(params.getWebResource(), new MultiPartUtils());
        return resource.dynamicGet(params.getRequest(), params.getServletResponse(), params.getUriInfo(),
                params.getPathParam(), params.getBodyMapString());
    }
}

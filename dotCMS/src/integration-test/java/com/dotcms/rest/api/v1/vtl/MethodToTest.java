package com.dotcms.rest.api.v1.vtl;

import com.dotcms.repackage.javax.ws.rs.core.Response;

public interface MethodToTest {
  Response execute(final VTLResourceIntegrationTest.HTTPMethodParams params);
}

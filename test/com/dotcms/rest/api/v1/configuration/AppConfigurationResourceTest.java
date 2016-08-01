package com.dotcms.rest.api.v1.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.system.AppConfigurationResource;

/**
 * Unit test for validating the information returned by the
 * {@link AppConfigurationResource} end-point.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 29, 2016
 *
 */
public class AppConfigurationResourceTest {

	protected static final String HOST_NAME = "localhost:8080";

	public void testVerifyConfigurationData() {
		final AppConfigurationResource resource = new AppConfigurationResource();
		final HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRequestURL()).thenReturn(new StringBuffer("http://" + HOST_NAME + "/html/ng/"));
		final Response response = resource.list(request);
		assertNotNull(response);
		assertEquals(response.getStatus(), 200);
		assertNotNull(response.getEntity());
		assertTrue(response.getEntity() instanceof ResponseEntityView);
		assertTrue(ResponseEntityView.class.cast(response.getEntity()).getErrors() == null);
	}

}

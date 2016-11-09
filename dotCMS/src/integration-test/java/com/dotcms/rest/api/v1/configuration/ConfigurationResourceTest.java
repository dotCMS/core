package com.dotcms.rest.api.v1.configuration;

import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_BASEURL;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_ENDPOINTS;
import static com.dotmarketing.util.WebKeys.DOTCMS_WEBSOCKET_PROTOCOL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.system.ConfigurationResource;
import com.dotmarketing.util.IntegrationTestInitService;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for validating the information returned by the
 * {@link ConfigurationResource} end-point.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 26, 2016
 *
 */
public class ConfigurationResourceTest {

	protected static final String HOST_NAME = "localhost:8080";
	
	@BeforeClass
	public static void prepare() throws Exception{
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testVerifyConfigurationData() {
		final ConfigurationResource resource = new ConfigurationResource();
		final HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRequestURL()).thenReturn(new StringBuffer("http://" + HOST_NAME + "/html/ng/"));
		final Response response = resource.list(request);
		assertNotNull(response);
		assertEquals(response.getStatus(), 200);
		assertNotNull(response.getEntity());
		assertTrue(response.getEntity() instanceof ResponseEntityView);
		assertTrue(ResponseEntityView.class.cast(response.getEntity()).getErrors().isEmpty());
		Map<String, Object> responseEntity = (Map<String, Object>) ResponseEntityView.class.cast(response.getEntity()).getEntity();
		assertNotNull(responseEntity.get(DOTCMS_WEBSOCKET_PROTOCOL));
		assertNotNull(responseEntity.get(DOTCMS_WEBSOCKET_BASEURL));
		assertTrue(HOST_NAME.equals(responseEntity.get(DOTCMS_WEBSOCKET_BASEURL)));
		assertNotNull(responseEntity.get(DOTCMS_WEBSOCKET_ENDPOINTS));
	}

}

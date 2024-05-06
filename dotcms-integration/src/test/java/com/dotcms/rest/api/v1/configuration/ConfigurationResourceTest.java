package com.dotcms.rest.api.v1.configuration;


import static com.dotmarketing.util.WebKeys.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.liferay.util.LocaleUtil;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.system.ConfigurationResource;
import com.dotcms.util.IntegrationTestInitService;

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
		when(request.getRequestURL()).thenReturn(new StringBuffer("http://" + HOST_NAME + "/dotAdmin/"));
		when(LocaleUtil.getLocale(request)).thenReturn(new Locale("en","US"));
		final Response response = resource.list(request);
		assertNotNull(response);
		assertEquals(response.getStatus(), 200);
		assertNotNull(response.getEntity());
		assertTrue(response.getEntity() instanceof ResponseEntityView);
		assertTrue(ResponseEntityView.class.cast(response.getEntity()).getErrors().isEmpty());
		Map<String, Object> responseEntity = (Map<String, Object>) ResponseEntityView.class.cast(response.getEntity()).getEntity();

		final Map webSocketConfig = (Map) responseEntity.get(DOTCMS_WEBSOCKET);

		assertEquals(Integer.parseInt(webSocketConfig.get(DOTCMS_WEBSOCKET_TIME_TO_WAIT_TO_RECONNECT).toString()), 15000);
		assertEquals(Boolean.parseBoolean(webSocketConfig.get(DOTCMS_DISABLE_WEBSOCKET_PROTOCOL).toString()), false);
	}

}

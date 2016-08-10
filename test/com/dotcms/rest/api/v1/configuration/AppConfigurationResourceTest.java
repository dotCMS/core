package com.dotcms.rest.api.v1.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.cms.login.LoginService;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.api.v1.system.AppConfigurationHelper;
import com.dotcms.rest.api.v1.system.AppConfigurationResource;
import com.dotcms.util.UserUtilTest;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Map;

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

	@Test
	public void testVerifyConfigurationData() throws DotSecurityException, DotDataException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

		Object menuData = new Object();
		Object configData = new Object();

		HttpServletRequest mockHttpRequest = RestUtilTest.getMockHttpRequest();
		AppConfigurationHelper helper = mock( AppConfigurationHelper.class );
		when(helper.getMenuData( mockHttpRequest )).thenReturn( menuData );
		when(helper.getConfigurationData( mockHttpRequest )).thenReturn( configData );

		LoginService loginService = mock( LoginService.class );

		User user = UserUtilTest.createUser();
		when( loginService.getLogInUser( mockHttpRequest ) ).thenReturn( user );

		final AppConfigurationResource resource = new AppConfigurationResource( helper, loginService );
		Response responseEntityView = resource.list(mockHttpRequest);

		RestUtilTest.verifySuccessResponse( responseEntityView );
		Object entity = ((ResponseEntityView) responseEntityView.getEntity()).getEntity();
		assertTrue(entity instanceof Map);

		Map map = ( Map ) entity;
		assertEquals(menuData, map.get( "menu" ));
		assertEquals(configData, map.get( "config" ));
		assertEquals(user.toMap(), map.get( "user" ));
	}

}

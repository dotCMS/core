package com.dotcms.rest.api.v1.configuration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.UnitTestBase;
import com.dotcms.cms.login.LoginServiceAPI;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.api.v1.menu.Menu;
import com.dotcms.rest.api.v1.menu.MenuResource;
import com.dotcms.rest.api.v1.system.AppConfigurationHelper;
import com.dotcms.rest.api.v1.system.AppContextInitResource;
import com.dotcms.rest.api.v1.system.ConfigurationHelper;
import com.dotcms.util.UserUtilTest;
import com.dotmarketing.business.LoginAsAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

/**
 * Unit test for validating the information returned by the
 * {@link AppContextInitResource} end-point.
 * 
 * @author Jose Castro
 * @version 3.7
 * @since Jul 29, 2016
 *
 */
public class AppContextInitResourceTest extends UnitTestBase {

	@Test
	public void testVerifyConfigurationData() throws DotSecurityException, DotDataException, IllegalAccessException,
			NoSuchMethodException, InvocationTargetException, LanguageException, ClassNotFoundException {

		Collection<Menu> menuData = mock(Collection.class);
		Map<String, Object> configData = mock(Map.class);

		HttpServletRequest mockHttpRequest = RestUtilTest.getMockHttpRequest();
		final HttpServletResponse httpServletResponse  = mock(HttpServletResponse.class);
		LoginAsAPI loginAsAPI = mock( LoginAsAPI.class );

		MenuResource menuResource = mock(MenuResource.class);
		ConfigurationHelper configurationHelper = mock(ConfigurationHelper.class);

		when(menuResource.getMenus( mockHttpRequest, httpServletResponse )).thenReturn(
				Response.ok(new ResponseEntityView<>(menuData)).build() );

		when(configurationHelper.getConfigProperties( mockHttpRequest )).thenReturn(configData);

		LoginServiceAPI loginService = mock( LoginServiceAPI.class );

		User user = UserUtilTest.createUser();
		when( loginService.getLoggedInUser( mockHttpRequest ) ).thenReturn( user );

		AppConfigurationHelper helper = new AppConfigurationHelper(configurationHelper);

		final AppContextInitResource resource = new AppContextInitResource( helper );
		Response responseEntityView = resource.list(mockHttpRequest);

		RestUtilTest.verifySuccessResponse( responseEntityView );
		Object entity = ((ResponseEntityView) responseEntityView.getEntity()).getEntity();
		assertTrue(entity instanceof Map);

		Map map = ( Map ) entity;
		assertEquals(configData, map.get( "config" ));
	}
}

package com.dotcms;

import static org.mockito.Mockito.mock;

import org.junit.BeforeClass;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.BaseMessageResources;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;

public abstract class UnitTestBase extends BaseMessageResources {

	protected static ContentTypeAPI contentTypeAPI = mock(ContentTypeAPI.class);

	public static class MyAPILocator extends APILocator {		
		@Override
		protected ContentTypeAPI getContentTypeAPIImpl(User user, boolean respectFrontendRoles) {
			return contentTypeAPI;
		}
	}

	@BeforeClass
	public static void prepare () throws DotDataException, DotSecurityException, Exception {
		Config.initializeConfig();
		Config.setProperty("API_LOCATOR_IMPLEMENTATION", MyAPILocator.class.getName());
		Config.setProperty("SYSTEM_EXIT_ON_STARTUP_FAILURE", false);
	}
}
package com.dotcms;

import static org.mockito.Mockito.mock;

import org.junit.BeforeClass;

import com.dotcms.contenttype.business.ContentTypeApi;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.BaseMessageResources;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;

public abstract class TestBase extends BaseMessageResources {

	protected static ContentTypeApi contentTypeAPI = mock(ContentTypeApi.class);

	public static class MyAPILocator extends APILocator {		
		@Override
		protected ContentTypeApi getContentTypeAPI2Impl(User user, boolean respectFrontendRoles) {
			return contentTypeAPI;
		}
	}

	@BeforeClass
	public static void prepare () throws DotDataException, DotSecurityException, Exception {
		Config.initializeConfig();
		Config.setProperty("API_LOCATOR_IMPLEMENTATION", MyAPILocator.class.getName());
	}
}
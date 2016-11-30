package com.dotcms;

import org.junit.BeforeClass;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.BaseMessageResources;
import com.dotmarketing.util.Config;

public abstract class TestBase extends BaseMessageResources {

	public static class MyAPILocator extends APILocator {		
	}

	@BeforeClass
	public static void prepare () throws DotDataException, DotSecurityException, Exception {
		Config.initializeConfig();
		Config.setProperty("API_LOCATOR_IMPLEMENTATION", MyAPILocator.class.getName());
	}
}
package com.dotcms.content.elasticsearch.business;

import com.dotcms.repackage.junit_4_8_1.org.junit.BeforeClass;
import com.dotcms.repackage.junit_4_8_1.org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.liferay.portal.model.User;

public class ESIndexSpeedTest extends TestBase {
	
	private static final ContentletAPI contAPI = APILocator.getContentletAPI();
	private static User user=null;
	
	@BeforeClass
	public static void before() throws DotDataException {
		user=APILocator.getUserAPI().getAnonymousUser();
	}
	
	@Test
	public void callCount10k() throws Exception {
		for(int i=1;i<=10000;i++) {
			contAPI.searchIndex("+structureName:webPageContent", 20, 0, "modDate", user, true);
		}
	}
	
	@Test
	public void callIndexSearch10k() throws Exception {
		for(int i=1;i<=10000;i++) {
			contAPI.indexCount("+structureName:webPageContent", user, true);
		}
	}
}

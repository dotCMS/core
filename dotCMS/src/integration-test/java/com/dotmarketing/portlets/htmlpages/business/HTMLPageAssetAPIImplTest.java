package com.dotmarketing.portlets.htmlpages.business;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import org.junit.Test;

public class HTMLPageAssetAPIImplTest {
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Given scenario:
     * - Given a URL-Mapped content
     */

    @Test
    public void test_getHTML_GivenSpanishOnlyUrlMappedContent_shouldReturnURLMapDetailPage() {

    }
}

package com.dotcms.rendering.velocity;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Config;
import com.dotcms.rendering.velocity.util.VelocityUtil;

import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.app.Velocity;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * VelocityUtilTest
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VelocityUtilTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Test the velocity path is the default path
     */
    @Test
    public void test01DefaultVelocityPath() throws Exception {
        String velocityPath = VelocityUtil.getVelocityRootPath();

        Assert.assertNotNull(velocityPath);
        assertThat("Path ends with /WEB-INF/velocity", velocityPath.endsWith("/WEB-INF/velocity"));
    }

    /**
     * Test the velocity path is a custom path
     */
    @Test
    public void test02CustomVelocityPath() throws Exception {
        String originalVelocityPath = Config.getStringProperty("VELOCITY_ROOT", "/WEB-INF/velocity");
        String newVelocityPath = "/WEB-INF/customVelocity";
        Config.setProperty("VELOCITY_ROOT", newVelocityPath);

        String customVelocityPath = VelocityUtil.getVelocityRootPath();

        Assert.assertNotNull(customVelocityPath);
        assertThat("Path ends with /WEB-INF/customVelocity", customVelocityPath.endsWith("/WEB-INF/customVelocity"));

        // restore the default value (used on other tests)
        Config.setProperty("VELOCITY_ROOT", originalVelocityPath);
        String velocityRoot = Config.getStringProperty("VELOCITY_ROOT");
        assertThat("Path velocity root has been successfully restored", originalVelocityPath.equals(velocityRoot));
    }

    @Test
    public void test_getPageCacheKey_returnNull() throws Exception{
        HttpServletRequest request = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequest("localhost", "/").request()
                ).request())
                .request();

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(),false);
        final IHTMLPage page = APILocator.getHTMLPageAssetAPI().
                getPageByPath("/",defaultHost,APILocator.getLanguageAPI().getDefaultLanguage().getId(),true);

        //DotCache set to Refresh must return null (not use cached page)
        request.getSession().setAttribute(VelocityUtil.DOTCACHE, VelocityUtil.REFRESH);
        String pageCacheKey = VelocityUtil.getPageCacheKey(request,page);
        Assert.assertNull(pageCacheKey);

        request.getSession().removeAttribute(VelocityUtil.DOTCACHE);

        //DotCache set to No must return null (not use cached page)
        request.getSession().setAttribute(VelocityUtil.DOTCACHE, VelocityUtil.NO);
        pageCacheKey = VelocityUtil.getPageCacheKey(request,page);
        Assert.assertNull(pageCacheKey);

        request.getSession().removeAttribute(VelocityUtil.DOTCACHE);

        //TTL of a page is set 0 must return null (not use cached page)
        page.setCacheTTL(0);
        pageCacheKey = VelocityUtil.getPageCacheKey(request,page);
        Assert.assertNull(pageCacheKey);

    }

}

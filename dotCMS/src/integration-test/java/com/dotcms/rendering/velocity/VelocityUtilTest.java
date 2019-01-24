package com.dotcms.rendering.velocity;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Config;
import com.liferay.portal.util.WebKeys;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.VelocityRequestWrapper;
import com.dotcms.rendering.velocity.viewtools.VelocitySessionWrapper;
import com.dotcms.repackage.javax.validation.constraints.AssertTrue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.context.Context;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

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
        final HttpServletRequest request = new MockSessionRequest(
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

        page.setCacheTTL(15);

        //Use Cached Page Scenario
        when(request.getMethod()).thenReturn("GET");
        pageCacheKey = VelocityUtil.getPageCacheKey(request,page);
        Assert.assertNotNull(pageCacheKey);

    }
    
    
    @Test
    public void test_unable_to_modify_userid_in_velocity_session() throws Exception{
    
        final HttpServletRequest request = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequest("localhost", "/").request()
                ).request())
                .request();
    
        final HttpServletResponse response = new MockHttpResponse().response();

        
        Context context = VelocityUtil.getWebContext(request, response);
        HttpServletRequest velRequest = (HttpServletRequest) context.get("request");
        HttpSession velSession = velRequest.getSession();
        
        
        Assert.assertTrue(velRequest instanceof VelocityRequestWrapper);
        Assert.assertTrue(velSession instanceof VelocitySessionWrapper);
        
        final String TEST_KEY="TEST_KEY";
        final String TEST_VALUE="TEST_VALUE";
        
        
        // Test setting normal value
        velSession.setAttribute(TEST_KEY, TEST_VALUE);
        assertTrue(TEST_VALUE.equals(velSession.getAttribute(TEST_KEY).toString()));
        
        // Test that it made it to the underlying session
        assertTrue(TEST_VALUE.equals(request.getSession().getAttribute(TEST_KEY).toString()));
        
        // Test setting USER_ID 
        velSession.setAttribute(WebKeys.USER_ID, TEST_VALUE);
        
        // Test that it fails to set
        
        assertTrue(velSession.getAttribute(WebKeys.USER_ID) ==null);
        
        // Test that it fails to set the underlying session
        assertTrue(request.getSession().getAttribute(WebKeys.USER_ID) ==null);
    }
    

}

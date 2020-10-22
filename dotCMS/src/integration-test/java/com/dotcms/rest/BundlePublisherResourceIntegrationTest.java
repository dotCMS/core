package com.dotcms.rest;

import com.dotcms.IntegrationTestBase;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.InvalidLicenseException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;

import java.util.List;

import static org.mockito.Mockito.mock;

public class BundlePublisherResourceIntegrationTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test(expected = InvalidLicenseException.class)
    public void publishShouldReturnLicenseException() throws Exception {

        runNoLicense(()-> {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response= mock(HttpServletResponse.class);

            final BundlePublisherResource bundlePublisherResource = new BundlePublisherResource();

            bundlePublisherResource.publish(null, null, true, request, response);
        });

    }
}

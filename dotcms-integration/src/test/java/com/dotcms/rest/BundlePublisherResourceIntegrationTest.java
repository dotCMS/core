package com.dotcms.rest;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.InvalidLicenseException;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import static org.mockito.Mockito.mock;

public class BundlePublisherResourceIntegrationTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test()
    public void publishShouldReturnLicenseException() throws Exception {

        runNoLicense(()-> {
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response= mock(HttpServletResponse.class);

            final BundlePublisherResource bundlePublisherResource = new BundlePublisherResource();

            try {
                bundlePublisherResource.publish(null, null, true, request, response);
                throw new AssertionError();
            } catch(InvalidLicenseException e) {
                //expected
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }
}
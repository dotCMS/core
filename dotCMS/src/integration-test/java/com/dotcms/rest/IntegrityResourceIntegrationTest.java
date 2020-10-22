package com.dotcms.rest;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.InvalidLicenseException;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.mock;

public class IntegrityResourceIntegrationTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test(expected = InvalidLicenseException.class)
    public void generateShouldReturnLicenseException() throws Exception {

        runNoLicense(()-> {
            final HttpServletRequest request = mock(HttpServletRequest.class);

            final IntegrityResource integrityResource = new IntegrityResource();

            integrityResource.generateIntegrityData(request);
        });

    }

    @Test(expected = InvalidLicenseException.class)
    public void fixConflictsShouldReturnLicenseException() throws Exception {

        runNoLicense(()-> {
            final HttpServletRequest request = mock(HttpServletRequest.class);

            final IntegrityResource integrityResource = new IntegrityResource();

            integrityResource.fixConflictsFromRemote(request, null, null);
        });

    }
}

package com.dotcms.rest;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.InvalidLicenseException;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

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

    @Test
    public void fixConflictsShouldReturnLicenseException() throws Exception {

        runNoLicense(()-> {
            final HttpServletRequest request = mock(HttpServletRequest.class);

            final IntegrityResource integrityResource = new IntegrityResource();

            try {
                integrityResource.fixConflictsFromRemote(request, null, null);
            } catch(InvalidLicenseException e) {
                //expected
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }
}
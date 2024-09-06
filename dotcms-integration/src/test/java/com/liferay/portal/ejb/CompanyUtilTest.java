package com.liferay.portal.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.dotcms.datagen.CompanyDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Logger;
import com.liferay.portal.NoSuchCompanyException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.Company;
import org.junit.BeforeClass;
import org.junit.Test;

public class CompanyUtilTest {

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting the test user
        IntegrationTestInitService.getInstance().init();
    }


    @Test
    public void Test_New_Company_Then_Update_Then_Verify_Changes_Are_Saved() {

        final Company company = new CompanyDataGen()
                .name("TestCompany")
                .shortName("TC")
                .authType("email")
                .autoLogin(true)
                .emailAddress("lol@dotCMS.com")
                .homeURL("localhost")
                .city("NYC")
                .mx("MX")
                .type("test")
                .phone("5552368")
                .portalURL("/portalURL")
                .nextPersisted();
        assertNotNull(company.getCompanyId());
        try {
            final Company retrievedCompany =  CompanyUtil.findByPrimaryKey(company.getCompanyId());
            assertEquals(company.getCompanyId(), retrievedCompany.getCompanyId());

            final String updatedName = "Test-Company.";
            final String updatedShortName = "TC.";
            final String updatedAuthType = "userID";
            final boolean updatedAutoLogin = false;
            final String updatedEmail = "lal@dotCMS.com";
            final String updatedHomeURL = "127.0.0.1";
            final String updatedCity = "SJ";
            final String updatedMx = "mx";
            final String updatedType = "RealWorld";
            final String updatedPhone = "555-2368";
            final String updatedPortalURL = "/portal-URL";

            retrievedCompany.setName(updatedName);
            retrievedCompany.setShortName(updatedShortName);
            retrievedCompany.setAuthType(updatedAuthType);
            retrievedCompany.setAutoLogin(updatedAutoLogin);
            retrievedCompany.setEmailAddress(updatedEmail);
            retrievedCompany.setHomeURL(updatedHomeURL);
            retrievedCompany.setCity(updatedCity);
            retrievedCompany.setMx(updatedMx);
            retrievedCompany.setType(updatedType);
            retrievedCompany.setPhone(updatedPhone);
            retrievedCompany.setPortalURL(updatedPortalURL);

            CompanyUtil.update(retrievedCompany);

            final Company updatedCompany = CompanyUtil.findByPrimaryKey(company.getCompanyId());
            assertEquals(updatedCompany.getCompanyId(), retrievedCompany.getCompanyId());

            assertEquals(updatedName,updatedCompany.getName());
            assertEquals(updatedShortName,updatedCompany.getShortName());
            assertEquals(updatedAuthType,updatedCompany.getAuthType());
            assertEquals(updatedAutoLogin,updatedCompany.getAutoLogin());
            assertEquals(updatedEmail,updatedCompany.getEmailAddress());
            assertEquals(updatedHomeURL,updatedCompany.getHomeURL());
            assertEquals(updatedCity,updatedCompany.getCity());
            assertEquals(updatedMx,updatedCompany.getMx());
            assertEquals(updatedType,updatedCompany.getType());
            assertEquals(updatedPhone,updatedCompany.getPhone());
            assertEquals(updatedPortalURL,updatedCompany.getPortalURL());

        } catch (SystemException | NoSuchCompanyException e) {
            Logger.error(CompanyUtilTest.class, e);
            fail("Fail updating company");
        }
    }


    @Test(expected = NoSuchCompanyException.class)
    public void Test_New_Company_Then_Remove_Then_Find_Expect_Exception()
            throws SystemException, NoSuchCompanyException {

        final Company company = new CompanyDataGen()
                .name("TestCompany2")
                .shortName("TC-2")
                .authType("email")
                .autoLogin(true)
                .emailAddress("lol@dotCMS.com")
                .homeURL("localhost")
                .city("NYC")
                .mx("MX")
                .type("test")
                .phone("5552368")
                .portalURL("/portalURL")
                .nextPersisted();
        assertNotNull(company.getCompanyId());

        CompanyUtil.remove(company.getCompanyId());
        assertNotNull(CompanyUtil.findByPrimaryKey(company.getCompanyId()));
    }


}

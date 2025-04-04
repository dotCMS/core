package com.dotmarketing.portlets.workflows.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.company.CompanyAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple2;
import java.util.Locale;
import org.junit.Test;

public class WorkflowEmailUtilTest {

    /**
     * Method to test {@link WorkflowEmailUtil#resolveSenderInfo(User, UserAPI, CompanyAPI)}
     * Given Scenario: regardless of the user passed to the function
     * Expected Result: we expect the company's email address
     * @throws DotDataException
     */
    @Test
    public void Test_Resolve_Sender() throws DotDataException {

        final String companyId = "dotcms.org";
        final String companyEmailAddress = "support@dotCMS.com";
        final UserAPI userAPI = mock(UserAPI.class);
        final CompanyAPI companyAPI = mock(CompanyAPI.class);

        final Company company = mock(Company.class);
        when(company.getCompanyId()).thenReturn(companyId);
        when(company.getAdminName()).thenReturn("dotCMS admin");
        when(company.getEmailAddress()).thenReturn(companyEmailAddress);

        when(companyAPI.getDefaultCompany()).thenReturn(company);
        final User anon = mock(User.class);
        when(anon.getUserId()).thenReturn(UserAPI.CMS_ANON_USER_ID);
        when(anon.getEmailAddress()).thenReturn(UserAPI.CMS_ANON_USER_EMAIL);
        when(anon.getFullName()).thenReturn("Anonymous");
        when(anon.getLocale()).thenReturn(Locale.getDefault());
        when(anon.isBackendUser()).thenReturn(false);
        when(anon.getCompanyId()).thenReturn(companyId);

        final User system = mock(User.class);
        when(system.getUserId()).thenReturn(UserAPI.SYSTEM_USER_ID);
        when(system.getEmailAddress()).thenReturn(UserAPI.SYSTEM_USER_EMAIL);
        when(system.getFullName()).thenReturn("System");
        when(system.getLocale()).thenReturn(Locale.getDefault());
        when(system.isBackendUser()).thenReturn(false);
        when(system.getCompanyId()).thenReturn(companyId);

        final String ringosAddress  = "ringo@beatles.org";
        final User ringo = mock(User.class);
        when(ringo.getUserId()).thenReturn("156");
        when(ringo.getEmailAddress()).thenReturn(ringosAddress);
        when(ringo.getFullName()).thenReturn("Ringo Starr");
        when(ringo.getLocale()).thenReturn(Locale.getDefault());
        when(ringo.isBackendUser()).thenReturn(false);
        when(ringo.getCompanyId()).thenReturn(companyId);

        when(userAPI.getAnonymousUser()).thenReturn(anon);
        when(userAPI.getSystemUser()).thenReturn(system);

        final Tuple2<String, String> calledWithAnonSender = WorkflowEmailUtil.resolveSenderInfo(anon, userAPI, companyAPI);
        assertEquals(companyEmailAddress, calledWithAnonSender._1);

        final Tuple2<String, String> calledWithSystem = WorkflowEmailUtil.resolveSenderInfo(system, userAPI, companyAPI);
        assertEquals(companyEmailAddress, calledWithSystem._1);

        final Tuple2<String, String> calledWithRingo = WorkflowEmailUtil.resolveSenderInfo(ringo, userAPI, companyAPI);
        assertEquals(companyEmailAddress, calledWithRingo._1);


    }

    /**
     * Method to test {@link WorkflowEmailUtil#resolveSenderInfo(User, UserAPI, CompanyAPI)}
     * Given Scenario: We pass anonymous user and the company does not have a default email address set.
     * Expected Result: We expect a fallback email address here.
     * @throws DotDataException
     */
    @Test
    public void Test_Resolve_Sender_No_Company_Email_FallBack() throws DotDataException {
        final String companyId = "dotcms.org";
        final String companyEmailAddress = StringPool.BLANK;
        final UserAPI userAPI = mock(UserAPI.class);
        final CompanyAPI companyAPI = mock(CompanyAPI.class);

        final Company company = mock(Company.class);
        when(company.getCompanyId()).thenReturn(companyId);
        when(company.getAdminName()).thenReturn("dotCMS admin");
        when(company.getEmailAddress()).thenReturn(companyEmailAddress);

        when(companyAPI.getDefaultCompany()).thenReturn(company);
        final User anon = mock(User.class);
        when(anon.getUserId()).thenReturn(UserAPI.CMS_ANON_USER_ID);
        when(anon.getEmailAddress()).thenReturn(UserAPI.CMS_ANON_USER_EMAIL);
        when(anon.getFullName()).thenReturn("Anonymous");
        when(anon.getLocale()).thenReturn(Locale.getDefault());
        when(anon.isBackendUser()).thenReturn(false);
        when(anon.getCompanyId()).thenReturn(companyId);

        when(userAPI.getAnonymousUser()).thenReturn(anon);

        final Tuple2<String, String> calledWithAnonSender = WorkflowEmailUtil.resolveSenderInfo(anon, userAPI, companyAPI);
        assertEquals(WorkflowEmailUtil.FALLBACK_FROM_ADDRESS, calledWithAnonSender._1);
    }

    /**
     *  Method to test {@link WorkflowEmailUtil#resolveSenderInfo(User, UserAPI, CompanyAPI)}
     * Given Scenario: We support having a company e-address of the form `dotCMS User <user@dotcms.website.com>`
     * Expected Result: If the company address has both components in the specified form we should be able to recover the two components apart.
     * @throws DotDataException
     */
    @Test
    public void Test_Resolve_Sender_Included_On_The_Company_EmailAddress() throws DotDataException {

        final String sender = "dotCMS User";
        final String email = "user@dotcms.website.com";
        final String companyEmailAddress = String.format(" %s <%s>", sender, email);

        final String companyId = UUIDUtil.uuid();
        final UserAPI userAPI = mock(UserAPI.class);
        final CompanyAPI companyAPI = mock(CompanyAPI.class);

        final Company company = mock(Company.class);
        when(company.getCompanyId()).thenReturn(companyId);
        when(company.getAdminName()).thenReturn("dotCMS admin");
        when(company.getEmailAddress()).thenReturn(companyEmailAddress);


        when(companyAPI.getDefaultCompany()).thenReturn(company);
        final User anon = mock(User.class);
        when(anon.getUserId()).thenReturn(UserAPI.CMS_ANON_USER_ID);
        when(anon.getEmailAddress()).thenReturn(UserAPI.CMS_ANON_USER_EMAIL);
        when(anon.getFullName()).thenReturn("Anonymous");
        when(anon.getLocale()).thenReturn(Locale.getDefault());
        when(anon.isBackendUser()).thenReturn(false);
        when(anon.getCompanyId()).thenReturn(companyId);

        when(userAPI.getAnonymousUser()).thenReturn(anon);

        final Tuple2<String, String> calledWithSenderAndEmail = WorkflowEmailUtil.resolveSenderInfo(anon, userAPI, companyAPI);
        assertEquals(email, calledWithSenderAndEmail._1);
        assertEquals(sender, calledWithSenderAndEmail._2);
    }

}

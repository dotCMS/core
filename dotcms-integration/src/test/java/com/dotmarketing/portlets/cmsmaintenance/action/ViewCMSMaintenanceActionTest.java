package com.dotmarketing.portlets.cmsmaintenance.action;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.cmsmaintenance.struts.CmsMaintenanceForm;
import com.liferay.portal.model.User;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class ViewCMSMaintenanceActionTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void searchAndReplace_nonAdminSessionWithAdminIdInForm_isRefused() throws Exception {
        final User nonAdmin = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();

        final CmsMaintenanceForm form = new CmsMaintenanceForm();
        form.setUserId(APILocator.systemUser().getUserId());
        form.setSearchString("NOMATCH_" + System.currentTimeMillis());
        form.setReplaceString("NOMATCH_REPLACEMENT");

        final Map<String, String> result =
                new ViewCMSMaintenanceAction().searchAndReplace(nonAdmin, form);

        assertTrue(result.isEmpty());
    }

    @Test
    public void isAuthorizedAdmin_nonAdminUser_isFalse() throws Exception {
        final User nonAdmin = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();

        assertFalse(new ViewCMSMaintenanceAction().isAuthorizedAdmin(nonAdmin));
    }

    @Test
    public void isAuthorizedAdmin_adminUser_isTrue() throws Exception {
        assertTrue(new ViewCMSMaintenanceAction().isAuthorizedAdmin(APILocator.systemUser()));
    }

    @Test
    public void isAuthorizedAdmin_null_isFalse() {
        assertFalse(new ViewCMSMaintenanceAction().isAuthorizedAdmin(null));
    }
}

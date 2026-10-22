package com.dotmarketing.portlets.cmsmaintenance.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.cmsmaintenance.struts.CmsMaintenanceForm;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class ViewCMSMaintenanceActionTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private static User newNonAdmin() throws com.dotmarketing.exception.DotDataException {
        return new UserDataGen().roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
    }

    private static User newAdmin() throws Exception {
        return new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(),
                        APILocator.getRoleAPI().loadCMSAdminRole())
                .nextPersisted();
    }

    @Test
    public void searchAndReplace_nonAdminSessionUser_isRefused() throws Exception {
        final User nonAdmin = newNonAdmin();
        final String marker = "SRMARKER_" + UUIDGenerator.generateUuid();
        final Container container = new ContainerDataGen().code("<div>" + marker + "</div>").nextPersisted();
        try {
            final CmsMaintenanceForm form = new CmsMaintenanceForm();
            form.setUserId(APILocator.systemUser().getUserId());
            form.setSearchString(marker);
            form.setReplaceString(marker + "_CHANGED");

            final Map<String, String> result =
                    new ViewCMSMaintenanceAction().searchAndReplace(nonAdmin, form);

            assertTrue(result.isEmpty());
            final Container reloaded = APILocator.getContainerAPI()
                    .getWorkingContainerById(container.getIdentifier(), APILocator.systemUser(), false);
            assertTrue(reloaded.getCode().contains(marker));
            assertFalse(reloaded.getCode().contains(marker + "_CHANGED"));
        } finally {
            ContainerDataGen.remove(container);
        }
    }

    @Test
    public void searchAndReplace_adminSession_runs() throws Exception {
        final CmsMaintenanceForm form = new CmsMaintenanceForm();
        form.setSearchString("ZZNOMATCH_" + UUIDGenerator.generateUuid());
        form.setReplaceString("ZZNOMATCH_REPLACEMENT");

        final Map<String, String> result =
                new ViewCMSMaintenanceAction().searchAndReplace(newAdmin(), form);

        assertEquals("message", result.get("type"));
    }

    @Test
    public void isAuthorizedAdmin_nonAdminUser_isFalse() throws Exception {
        assertFalse(ViewCMSMaintenanceAction.isAuthorizedAdmin(newNonAdmin()));
    }

    @Test
    public void isAuthorizedAdmin_adminUser_isTrue() throws Exception {
        assertTrue(ViewCMSMaintenanceAction.isAuthorizedAdmin(newAdmin()));
    }

    @Test
    public void isAuthorizedAdmin_null_isFalse() {
        assertFalse(ViewCMSMaintenanceAction.isAuthorizedAdmin(null));
    }
}

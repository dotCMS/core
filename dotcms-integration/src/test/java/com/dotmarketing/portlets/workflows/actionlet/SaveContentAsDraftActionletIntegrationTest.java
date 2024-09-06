package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.DateUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Save Draft test
 * @author jsanca
 */
public class SaveContentAsDraftActionletIntegrationTest extends BaseWorkflowIntegrationTest  {


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();

        setDebugMode(false);
    }
    /**
     * Method to test: {@link SaveContentAsDraftActionlet#executeAction(WorkflowProcessor, Map)}
     * Given Scenario: Adding permissions to the workflow processor
     * ExpectedResult: The permissions should be added
     */
    @Test
    public void save_draft_with_permissions() throws DotDataException {

        final Contentlet contentlet = new ContentletDataGen("webPageContent")
                .host(APILocator.systemHost())
                .setProperty("title", "title-test")
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .nextPersisted();

        final Optional<Role> role = APILocator.getRoleAPI().findAllAssignableRoles(true).stream().findAny();

        assertTrue(role.isPresent());

        final List<Permission> permissions = new ArrayList<>();
        permissions.add(new Permission(null, role.get().getId(), PermissionAPI.PERMISSION_READ));
        final ContentletDependencies contentletDependencies =
                new ContentletDependencies.Builder()
                        .modUser(APILocator.systemUser())
                        .permissions(permissions)
                        .build();
        final WorkflowProcessor processor = new WorkflowProcessor(contentlet, APILocator.systemUser());
        processor.setContentletDependencies(contentletDependencies);
        new SaveContentAsDraftActionlet().executeAction(processor, null);
        DateUtil.sleep(DateUtil.SECOND_MILLIS*2); // we need to wait for the commit listener that saves the permissions

        final List<Permission> permissionsRecovery = APILocator.getPermissionAPI().getPermissions(contentlet);
        assertNotNull(permissionsRecovery);
        assertFalse(permissionsRecovery.isEmpty());
        assertTrue(permissionsRecovery.stream().anyMatch(permission -> contentlet.getIdentifier().equals(permission.getInode()) &&
                role.get().getId().equals(permission.getRoleId()) && PermissionAPI.PERMISSION_READ == permission.getPermission()));

    }
}

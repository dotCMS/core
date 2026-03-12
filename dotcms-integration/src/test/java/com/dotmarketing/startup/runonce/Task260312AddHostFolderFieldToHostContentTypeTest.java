package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the {@link Task260312AddHostFolderFieldToHostContentType} Upgrade Task runs as
 * expected.
 *
 * @author dotCMS
 * @since Mar 12th, 2026
 */
public class Task260312AddHostFolderFieldToHostContentTypeTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Restores the test environment by removing the {@code parentHost} field from the Host content
     * type if it was added during any test, so subsequent runs start from a clean state.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        removeParentHostField();
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task260312AddHostFolderFieldToHostContentType#executeUpgrade()}</li>
     *     <li><b>Given Scenario:</b> The {@code parentHost} field does not exist on the Host
     *     content type.</li>
     *     <li><b>Expected Result:</b> After {@code executeUpgrade()}, the field is present on the
     *     Host content type and {@code forceRun()} returns {@code false}.</li>
     * </ul>
     */
    @Test
    public void upgradeTaskExecution() throws Exception {
        removeParentHostField();

        final Task260312AddHostFolderFieldToHostContentType task =
                new Task260312AddHostFolderFieldToHostContentType();

        assertTrue("parentHost field was removed, so forceRun() should return true",
                task.forceRun());

        task.executeUpgrade();

        assertFalse("parentHost field has been added, so forceRun() should return false",
                task.forceRun());

        // Verify the field is actually present and of the correct type
        final ContentType hostType = getHostContentType();
        assertNotNull("Host content type must be found", hostType);
        final Optional<Field> field = hostType.fields().stream()
                .filter(f -> Task260312AddHostFolderFieldToHostContentType.PARENT_HOST_FIELD_VAR
                        .equalsIgnoreCase(f.variable()))
                .findFirst();
        assertTrue("parentHost field must be present on Host content type", field.isPresent());
        assertTrue("parentHost field must be a HostFolderField",
                field.get() instanceof HostFolderField);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link Task260312AddHostFolderFieldToHostContentType#executeUpgrade()}</li>
     *     <li><b>Given Scenario:</b> The upgrade task is executed twice.</li>
     *     <li><b>Expected Result:</b> No error or exception is thrown on the second call
     *     (idempotency guarantee).</li>
     * </ul>
     */
    @Test
    public void checkUpgradeTaskIdempotency() throws Exception {
        removeParentHostField();

        final Task260312AddHostFolderFieldToHostContentType task =
                new Task260312AddHostFolderFieldToHostContentType();

        task.executeUpgrade();
        // Second invocation must not throw
        task.executeUpgrade();

        assertFalse("After running twice, forceRun() should return false", task.forceRun());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static ContentType getHostContentType() throws Exception {
        final ContentTypeAPI api = APILocator.getContentTypeAPI(APILocator.systemUser());
        return api.find(Host.HOST_VELOCITY_VAR_NAME);
    }

    /**
     * Removes the {@code parentHost} field from the Host content type so each test starts from a
     * predictable clean state.
     */
    private static void removeParentHostField() throws Exception {
        final ContentType hostType = getHostContentType();
        if (hostType == null) {
            return;
        }
        final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();
        for (final Field f : hostType.fields()) {
            if (Task260312AddHostFolderFieldToHostContentType.PARENT_HOST_FIELD_VAR
                    .equalsIgnoreCase(f.variable())) {
                fieldAPI.delete(f);
                break;
            }
        }
        CacheLocator.getContentTypeCache2().clearCache();
    }
}

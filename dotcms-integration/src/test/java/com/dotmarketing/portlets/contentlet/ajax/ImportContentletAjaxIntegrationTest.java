package com.dotmarketing.portlets.contentlet.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ajax.DwrWebContextRule;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Integration tests for {@link ImportContentletAjax} against stored {@code import_audit} rows:
 * only the user who started an import, or a CMS administrator, can cancel it or read its status.
 */
public class ImportContentletAjaxIntegrationTest {

    private static final long UNKNOWN_IMPORT_ID = 987_654_321_012L;

    @ClassRule
    public static final DwrWebContextRule dwrContext = new DwrWebContextRule();

    private static User owner;
    private static User otherUser;
    private static User admin;

    private final List<Long> createdImports = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        owner = backendUser("import-owner");
        otherUser = backendUser("import-other");
        admin = TestUserUtils.getAdminUser();
    }

    /** Deletes the test users, handing whatever they own to the admin user. */
    @AfterClass
    public static void removeUsers() throws Exception {
        final User replacement = TestUserUtils.getAdminUser();
        for (final User user : new User[]{owner, otherUser}) {
            if (null != user) {
                APILocator.getUserAPI().delete(user, replacement, APILocator.systemUser(), false);
            }
        }
    }

    @After
    public void removeImports() throws DotDataException {
        for (final Long importId : createdImports) {
            ImportAuditUtil.cancelledImports.remove(importId);
            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL("DELETE FROM import_audit WHERE id = ?");
            dotConnect.addParam(importId);
            dotConnect.loadResult();
        }
        ImportAuditUtil.cancelledImports.remove(UNKNOWN_IMPORT_ID);
    }

    /**
     * Method to test: {@link ImportContentletAjax#cancelImport(Long)}
     * Given: a pending import started by another back-end user, cancelled by a user who is not a
     * CMS administrator
     * Expected: DotSecurityException; the import stays pending and is not flagged as cancelled
     */
    @Test
    public void cancelImport_requiresImportOwnerOrAdmin() throws Exception {
        final long importId = pendingImportOf(owner.getUserId());

        dwrContext.loginAs(otherUser);
        assertThrows(DotSecurityException.class, () -> new ImportContentletAjax().cancelImport(importId));

        assertEquals(ImportAuditUtil.STATUS_PENDING, storedStatus(importId));
        assertFalse(ImportAuditUtil.cancelledImports.containsKey(importId));
    }

    /**
     * Method to test: {@link ImportContentletAjax#cancelImport(Long)}
     * Given: an id with no import row, cancelled by a user who is not a CMS administrator
     * Expected: the same DotSecurityException as for another user's import, and nothing is flagged
     * as cancelled
     */
    @Test
    public void cancelImport_unknownIdTreatedAsNotOwned() throws Exception {
        dwrContext.loginAs(otherUser);

        assertThrows(DotSecurityException.class,
                () -> new ImportContentletAjax().cancelImport(UNKNOWN_IMPORT_ID));

        assertFalse(ImportAuditUtil.cancelledImports.containsKey(UNKNOWN_IMPORT_ID));
    }

    /**
     * Method to test: {@link ImportContentletAjax#cancelImport(Long)}
     * Given: a pending import cancelled by the back-end user who started it
     * Expected: the import is stored as stopped by the user and flagged as cancelled
     */
    @Test
    public void cancelImport_allowsImportOwner() throws Exception {
        final long importId = pendingImportOf(owner.getUserId());

        dwrContext.loginAs(owner);
        assertEquals(importId, new ImportContentletAjax().cancelImport(importId));

        assertEquals(ImportAuditUtil.STATUS_USERSTOPPED, storedStatus(importId));
        assertTrue(ImportAuditUtil.cancelledImports.containsKey(importId));
    }

    /**
     * Method to test: {@link ImportContentletAjax#cancelImport(Long)}
     * Given: a pending import whose stored owner id has trailing blanks, cancelled by that owner
     * Expected: the import is stored as stopped by the user
     */
    @Test
    public void cancelImport_ownerIdComparedTrimmed() throws Exception {
        final long importId = pendingImportOf(owner.getUserId() + "  ");

        dwrContext.loginAs(owner);
        new ImportContentletAjax().cancelImport(importId);

        assertEquals(ImportAuditUtil.STATUS_USERSTOPPED, storedStatus(importId));
    }

    /**
     * Method to test: {@link ImportContentletAjax#cancelImport(Long)}
     * Given: a pending import of another user, cancelled by a CMS administrator
     * Expected: the import is stored as stopped by the user and flagged as cancelled
     */
    @Test
    public void cancelImport_allowsAdmin() throws Exception {
        final long importId = pendingImportOf(owner.getUserId());

        dwrContext.loginAs(admin);
        new ImportContentletAjax().cancelImport(importId);

        assertEquals(ImportAuditUtil.STATUS_USERSTOPPED, storedStatus(importId));
        assertTrue(ImportAuditUtil.cancelledImports.containsKey(importId));
    }

    /**
     * Method to test: {@link ImportContentletAjax#checkImportStatus(Long)}
     * Given: a pending import of another user, polled by a user who is not a CMS administrator
     * Expected: {@code true}, the answer an unknown import gets, and the import stays pending
     */
    @Test
    public void checkImportStatus_otherUsersImportReportsFinished() throws Exception {
        final long importId = pendingImportOf(owner.getUserId());

        dwrContext.loginAs(otherUser);
        assertTrue(new ImportContentletAjax().checkImportStatus(importId));

        assertEquals(ImportAuditUtil.STATUS_PENDING, storedStatus(importId));
    }

    /**
     * Method to test: {@link ImportContentletAjax#checkImportStatus(Long)}
     * Given: a pending import polled by the user who started it
     * Expected: {@code false} (still running)
     */
    @Test
    public void checkImportStatus_returnsStatusToOwner() throws Exception {
        final long importId = pendingImportOf(owner.getUserId());

        dwrContext.loginAs(owner);

        assertFalse(new ImportContentletAjax().checkImportStatus(importId));
    }

    /**
     * Method to test: {@link ImportContentletAjax#checkImportStatus(Long)}
     * Given: an id with no import row, polled by a user who is not a CMS administrator
     * Expected: {@code true}, the same answer as for another user's import
     */
    @Test
    public void checkImportStatus_unknownIdReportsFinished() throws Exception {
        dwrContext.loginAs(otherUser);

        assertTrue(new ImportContentletAjax().checkImportStatus(UNKNOWN_IMPORT_ID));
    }

    /**
     * Method to test: {@link ImportContentletAjax#checkImportStatus(Long)}
     * Given: no import id, polled by a user who is not a CMS administrator
     * Expected: {@code true}, the same answer as for another user's import
     */
    @Test
    public void checkImportStatus_nullIdReportsFinished() throws Exception {
        dwrContext.loginAs(otherUser);

        assertTrue(new ImportContentletAjax().checkImportStatus(null));
    }

    /**
     * Method to test: {@link ImportContentletAjax#checkImportStatus(Long)}
     * Given: a pending import of another user, polled by a CMS administrator
     * Expected: {@code false} (still running), and the import stays pending
     */
    @Test
    public void checkImportStatus_returnsStatusToAdmin() throws Exception {
        final long importId = pendingImportOf(owner.getUserId());

        dwrContext.loginAs(admin);

        assertFalse(new ImportContentletAjax().checkImportStatus(importId));
        assertEquals(ImportAuditUtil.STATUS_PENDING, storedStatus(importId));
    }

    private static User backendUser(final String label) throws DotDataException {
        return new UserDataGen()
                .emailAddress(label + System.nanoTime() + "@dotcms.com")
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
    }

    private long pendingImportOf(final String storedUserId) throws DotDataException {
        final long importId = ImportAuditUtil.createAuditRecord(storedUserId, "import-" + System.nanoTime() + ".csv");
        createdImports.add(importId);
        assertEquals(ImportAuditUtil.STATUS_PENDING, storedStatus(importId));
        return importId;
    }

    private static int storedStatus(final long importId) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();
        dotConnect.setSQL("SELECT status FROM import_audit WHERE id = ?");
        dotConnect.addParam(importId);
        final List<Map<String, Object>> rows = dotConnect.loadObjectResults();
        return ((Number) rows.get(0).get("status")).intValue();
    }
}

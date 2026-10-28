package com.dotmarketing.portlets.contentlet.ajax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.action.ImportAuditUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Unit tests for the access rules of {@link ImportContentletAjax}: an import can be checked or
 * cancelled only by the user who started it or by a CMS administrator. The import audit table is
 * simulated by mocking every {@link DotConnect} the code creates.
 */
class ImportContentletAjaxTest {

    private static final long IMPORT_ID = 4242L;
    private static final String CALLER_ID = "back-end-user";
    /** Text of the data error a failing database read raises, as the driver words it. */
    private static final String DATABASE_ERROR_TEXT =
            "org.postgresql.util.PSQLException: ERROR: relation \"import_audit\" does not exist";

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final UserWebAPI userWebAPI = mock(UserWebAPI.class);

    /** Owner stored in the audit row of {@link #IMPORT_ID}; {@code null} means there is no row. */
    private String importOwner;
    /** Status stored in the audit row of {@link #IMPORT_ID}. */
    private int importStatus = ImportAuditUtil.STATUS_PENDING;
    /** Whether reading the owner of an import fails with a data error. */
    private boolean ownerLookupFails;

    /** Names of the {@link SecurityLogger} methods called during a test. */
    private final List<String> securityLoggerCalls = new ArrayList<>();

    private MockedStatic<WebContextFactory> webContextFactory;
    private MockedStatic<WebAPILocator> webAPILocator;
    private MockedStatic<PortalUtil> portalUtil;
    private MockedStatic<ImportAuditUtil> importAudit;
    private MockedStatic<Logger> logger;
    private MockedStatic<SecurityLogger> securityLogger;
    private MockedConstruction<DotConnect> dotConnect;

    @BeforeEach
    void setUp() {
        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);
        webContextFactory = mockStatic(WebContextFactory.class);
        webContextFactory.when(WebContextFactory::get).thenReturn(webContext);
        webAPILocator = mockStatic(WebAPILocator.class);
        webAPILocator.when(WebAPILocator::getUserWebAPI).thenReturn(userWebAPI);
        portalUtil = mockStatic(PortalUtil.class);
        logger = mockStatic(Logger.class);
        securityLogger = mockStatic(SecurityLogger.class, invocation -> {
            securityLoggerCalls.add(invocation.getMethod().getName());
            return RETURNS_DEFAULTS.answer(invocation);
        });

        dotConnect = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                (mock, context) -> {
                    final List<Map<String, Object>> rows = importOwner == null
                            ? List.of() : List.of(Map.of("userid", importOwner));
                    when(mock.loadObjectResults()).thenAnswer(invocation -> {
                        if (ownerLookupFails) {
                            throw new DotDataException(DATABASE_ERROR_TEXT);
                        }
                        return rows;
                    });
                    when(mock.loadResults()).thenAnswer(invocation -> new ArrayList<>(rows));
                    when(mock.getString("userid")).thenAnswer(invocation -> importOwner);
                    when(mock.getInt("status")).thenAnswer(
                            invocation -> importOwner == null ? 0 : importStatus);
                });
        importAudit = mockStatic(ImportAuditUtil.class, CALLS_REAL_METHODS);
    }

    @AfterEach
    void tearDown() {
        ImportAuditUtil.cancelledImports.remove(IMPORT_ID);
        importAudit.close();
        dotConnect.close();
        securityLogger.close();
        logger.close();
        portalUtil.close();
        webAPILocator.close();
        webContextFactory.close();
    }

    /**
     * Logs in a back-end user for the DWR request.
     *
     * @param admin whether the user is a CMS administrator
     */
    private void givenLoggedInUser(final boolean admin) throws Exception {
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(CALLER_ID);
        when(user.getEmailAddress()).thenReturn("user@test.local");
        when(user.isAdmin()).thenReturn(admin);
        when(user.isBackendUser()).thenReturn(true);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(user);
        when(userWebAPI.isLoggedToBackend(request)).thenReturn(true);
        portalUtil.when(() -> PortalUtil.getUser(request)).thenReturn(user);
    }

    private void assertImportNotCancelled() {
        importAudit.verify(() -> ImportAuditUtil.cancelImport(anyLong()), never());
        assertFalse(ImportAuditUtil.cancelledImports.containsKey(IMPORT_ID),
                "the import must not be flagged as cancelled");
    }

    /**
     * Given: a user who did not start the import and is not a CMS administrator
     * When: cancelImport is called
     * Then: a DotSecurityException is thrown, the import is not cancelled and the denial is
     * recorded once in the security log
     */
    @Test
    void cancelImport_requiresImportOwnerOrAdmin() throws Exception {
        givenLoggedInUser(false);
        importOwner = "another-user";

        assertThrows(DotSecurityException.class, () -> new ImportContentletAjax().cancelImport(IMPORT_ID));
        assertImportNotCancelled();
        assertEquals(1, securityLoggerCalls.stream().filter(name -> name.startsWith("log")).count(),
                "one security log entry per denial, got " + securityLoggerCalls);
    }

    /**
     * Given: the user who started the import, not a CMS administrator
     * When: cancelImport is called
     * Then: the import is cancelled
     */
    @Test
    void cancelImport_allowsImportOwner() throws Exception {
        givenLoggedInUser(false);
        importOwner = CALLER_ID;

        assertEquals(IMPORT_ID, new ImportContentletAjax().cancelImport(IMPORT_ID));
        importAudit.verify(() -> ImportAuditUtil.cancelImport(IMPORT_ID));
    }

    /**
     * Given: a CMS administrator who did not start the import
     * When: cancelImport is called
     * Then: the import is cancelled
     */
    @Test
    void cancelImport_allowsAdmin() throws Exception {
        givenLoggedInUser(true);
        importOwner = "another-user";

        assertEquals(IMPORT_ID, new ImportContentletAjax().cancelImport(IMPORT_ID));
        importAudit.verify(() -> ImportAuditUtil.cancelImport(IMPORT_ID));
    }

    /**
     * Given: an import id with no audit record and a user who is not a CMS administrator
     * When: cancelImport is called
     * Then: the same DotSecurityException as for an import of another user
     */
    @Test
    void cancelImport_unknownIdTreatedAsNotOwned() throws Exception {
        givenLoggedInUser(false);
        importOwner = null;

        assertThrows(DotSecurityException.class, () -> new ImportContentletAjax().cancelImport(IMPORT_ID));
        assertImportNotCancelled();
    }

    /**
     * Given: a pending import started by another user, and a caller who is not a CMS administrator
     * When: checkImportStatus is called
     * Then: true is returned, as for an unknown id, and the import status is not read
     */
    @Test
    void checkImportStatus_otherUsersImportReportsFinished() throws Exception {
        givenLoggedInUser(false);
        importOwner = "another-user";
        importStatus = ImportAuditUtil.STATUS_PENDING;

        assertTrue(new ImportContentletAjax().checkImportStatus(IMPORT_ID));
        importAudit.verify(() -> ImportAuditUtil.isImportfinished(anyLong()), never());
    }

    /**
     * Given: the user who started a pending import
     * When: checkImportStatus is called
     * Then: false is returned
     */
    @Test
    void checkImportStatus_returnsStatusToOwner() throws Exception {
        givenLoggedInUser(false);
        importOwner = CALLER_ID;
        importStatus = ImportAuditUtil.STATUS_PENDING;

        assertFalse(new ImportContentletAjax().checkImportStatus(IMPORT_ID));
    }

    /**
     * Given: the user who started the import, stored with trailing blanks in the audit record
     * When: cancelImport is called
     * Then: the import is cancelled
     */
    @Test
    void cancelImport_ownerIdComparedTrimmed() throws Exception {
        givenLoggedInUser(false);
        importOwner = CALLER_ID + "  ";

        assertEquals(IMPORT_ID, new ImportContentletAjax().cancelImport(IMPORT_ID));
        importAudit.verify(() -> ImportAuditUtil.cancelImport(IMPORT_ID));
    }

    /**
     * Given: no import id and a user who is not a CMS administrator
     * When: cancelImport is called
     * Then: the same DotSecurityException as for an import of another user
     */
    @Test
    void cancelImport_nullIdTreatedAsNotOwned() throws Exception {
        givenLoggedInUser(false);
        importOwner = "another-user";

        assertThrows(DotSecurityException.class, () -> new ImportContentletAjax().cancelImport(null));
        assertImportNotCancelled();
    }

    /**
     * Given: a pending import started by another user, and a caller who is not a CMS administrator
     * When: the page polls checkImportStatus
     * Then: nothing is written to the security log
     */
    @Test
    void checkImportStatus_otherUsersImportWritesNoSecurityLog() throws Exception {
        givenLoggedInUser(false);
        importOwner = "another-user";

        new ImportContentletAjax().checkImportStatus(IMPORT_ID);

        securityLogger.verifyNoInteractions();
    }

    /**
     * Given: a user who is not a CMS administrator, and an import audit table that cannot be read
     * when the owner of the import is looked up
     * When: cancelImport is called
     * Then: the data error reaches the caller as a DotDataException, not as a denial, with a fixed
     * message instead of the database text; the import is not cancelled and nothing is written to
     * the security log
     */
    @Test
    void cancelImport_ownerLookupDataErrorIsNotReportedAsDenial() throws Exception {
        givenLoggedInUser(false);
        importOwner = CALLER_ID;
        ownerLookupFails = true;

        final DotDataException thrown = assertThrows(DotDataException.class,
                () -> new ImportContentletAjax().cancelImport(IMPORT_ID));
        assertFalse(thrown.getMessage().contains(DATABASE_ERROR_TEXT),
                "the database text must not reach the caller, got: " + thrown.getMessage());
        assertImportNotCancelled();
        securityLogger.verifyNoInteractions();
    }

    /**
     * Given: a user who is not a CMS administrator, and an import audit table that cannot be read
     * when the owner of the import is looked up
     * When: checkImportStatus is called
     * Then: the data error reaches the caller as a DotDataException with a fixed message, instead
     * of the import being reported as finished, and nothing is written to the security log
     */
    @Test
    void checkImportStatus_ownerLookupDataErrorIsNotReportedAsFinished() throws Exception {
        givenLoggedInUser(false);
        importOwner = CALLER_ID;
        ownerLookupFails = true;

        final DotDataException thrown = assertThrows(DotDataException.class,
                () -> new ImportContentletAjax().checkImportStatus(IMPORT_ID));
        assertFalse(thrown.getMessage().contains(DATABASE_ERROR_TEXT),
                "the database text must not reach the caller, got: " + thrown.getMessage());
        securityLogger.verifyNoInteractions();
    }

    /**
     * Given: an import id with no audit record and a user who is not a CMS administrator
     * When: checkImportStatus is called
     * Then: true is returned, the same answer as for an import of another user, and no status is
     * read
     */
    @Test
    void checkImportStatus_unknownIdReportsFinished() throws Exception {
        givenLoggedInUser(false);
        importOwner = null;

        assertTrue(new ImportContentletAjax().checkImportStatus(IMPORT_ID));
        importAudit.verify(() -> ImportAuditUtil.isImportfinished(anyLong()), never());
        securityLogger.verifyNoInteractions();
    }

    /**
     * Given: no import id and a user who is not a CMS administrator
     * When: checkImportStatus is called
     * Then: true is returned, the same answer as for an import of another user, and no status is
     * read
     */
    @Test
    void checkImportStatus_nullIdReportsFinished() throws Exception {
        givenLoggedInUser(false);
        importOwner = "another-user";

        assertTrue(new ImportContentletAjax().checkImportStatus(null));
        importAudit.verify(() -> ImportAuditUtil.isImportfinished(anyLong()), never());
        securityLogger.verifyNoInteractions();
    }

    /**
     * Given: a pending import started by another user, and a CMS administrator as caller
     * When: checkImportStatus is called
     * Then: the stored status is read and false is returned (still running)
     */
    @Test
    void checkImportStatus_returnsStatusToAdmin() throws Exception {
        givenLoggedInUser(true);
        importOwner = "another-user";
        importStatus = ImportAuditUtil.STATUS_PENDING;

        assertFalse(new ImportContentletAjax().checkImportStatus(IMPORT_ID));
        importAudit.verify(() -> ImportAuditUtil.isImportfinished(IMPORT_ID));
    }
}

package com.dotmarketing.portlets.cmsmaintenance.ajax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.business.ContentletIndexAPI;
import com.dotcms.publisher.assets.business.PushedAssetsAPI;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.ContentTypeCache;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.fixtask.FixTasksExecutor;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.portlets.cmsmaintenance.util.CleanAssetsThread;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

/**
 * Unit tests for the access rules of {@link CMSMaintenanceAjax}: every public method requires a
 * back-end user who is a CMS administrator and holds the Maintenance portlet, and deletions run as
 * the session user. The logged-in user is resolved from a mocked DWR request; every collaborator is
 * mocked.
 */
class CMSMaintenanceAjaxTest {

    private static final String MAINTENANCE_PORTLET = "maintenance";

    /** A call to one public method of the class. */
    @FunctionalInterface
    interface MaintenanceCall {
        void run(CMSMaintenanceAjax ajax) throws Exception;
    }

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final UserWebAPI userWebAPI = mock(UserWebAPI.class);
    private final LayoutAPI layoutAPI = mock(LayoutAPI.class);
    private final PushedAssetsAPI pushedAssetsAPI = mock(PushedAssetsAPI.class);
    private final ContentletAPI contentletAPI = mock(ContentletAPI.class);
    private final FixTasksExecutor fixTasksExecutor = mock(FixTasksExecutor.class);

    /** Names of the {@link Logger} and {@link SecurityLogger} methods called during a test. */
    private final List<String> loggerCalls = new ArrayList<>();
    private final List<String> securityLoggerCalls = new ArrayList<>();

    private MockedStatic<WebContextFactory> webContextFactory;
    private MockedStatic<WebAPILocator> webAPILocator;
    private MockedStatic<PortalUtil> portalUtil;
    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<ConfigUtils> configUtils;
    private MockedStatic<FixTasksExecutor> fixTasks;
    private MockedStatic<CMSMaintenanceFactory> maintenanceFactory;
    private MockedStatic<CleanAssetsThread> cleanAssetsThread;
    private MockedStatic<UserLocalManagerUtil> userLocalManager;
    private MockedStatic<LanguageUtil> languageUtil;
    private MockedStatic<Logger> logger;
    private MockedStatic<SecurityLogger> securityLogger;

    @BeforeEach
    void setUp() {
        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);
        webContextFactory = mockStatic(WebContextFactory.class);
        webContextFactory.when(WebContextFactory::get).thenReturn(webContext);

        webAPILocator = mockStatic(WebAPILocator.class);
        webAPILocator.when(WebAPILocator::getUserWebAPI).thenReturn(userWebAPI);
        portalUtil = mockStatic(PortalUtil.class);

        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getLayoutAPI).thenReturn(layoutAPI);
        apiLocator.when(APILocator::getPushedAssetsAPI).thenReturn(pushedAssetsAPI);
        apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);

        configUtils = mockStatic(ConfigUtils.class);
        fixTasks = mockStatic(FixTasksExecutor.class);
        fixTasks.when(FixTasksExecutor::getInstance).thenReturn(fixTasksExecutor);
        maintenanceFactory = mockStatic(CMSMaintenanceFactory.class);
        cleanAssetsThread = mockStatic(CleanAssetsThread.class);
        userLocalManager = mockStatic(UserLocalManagerUtil.class);
        languageUtil = mockStatic(LanguageUtil.class);

        logger = mockStatic(Logger.class, invocation -> {
            loggerCalls.add(invocation.getMethod().getName());
            return RETURNS_DEFAULTS.answer(invocation);
        });
        securityLogger = mockStatic(SecurityLogger.class, invocation -> {
            securityLoggerCalls.add(invocation.getMethod().getName());
            return RETURNS_DEFAULTS.answer(invocation);
        });
    }

    @AfterEach
    void tearDown() {
        securityLogger.close();
        logger.close();
        languageUtil.close();
        userLocalManager.close();
        cleanAssetsThread.close();
        maintenanceFactory.close();
        fixTasks.close();
        configUtils.close();
        apiLocator.close();
        portalUtil.close();
        webAPILocator.close();
        webContextFactory.close();
    }

    /**
     * Logs in a back-end user for the DWR request.
     *
     * @param admin             whether the user is a CMS administrator
     * @param maintenancePortlet whether the user has access to the Maintenance portlet
     * @return the logged-in user
     */
    private User givenLoggedInUser(final boolean admin, final boolean maintenancePortlet)
            throws Exception {
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(admin ? "admin-user" : "back-end-user");
        when(user.getEmailAddress()).thenReturn("user@test.local");
        when(user.isAdmin()).thenReturn(admin);
        when(user.isBackendUser()).thenReturn(true);
        when(userWebAPI.getLoggedInUser(request)).thenReturn(user);
        when(userWebAPI.isLoggedToBackend(request)).thenReturn(true);
        portalUtil.when(() -> PortalUtil.getUser(request)).thenReturn(user);
        when(layoutAPI.doesUserHaveAccessToPortlet(MAINTENANCE_PORTLET, user))
                .thenReturn(maintenancePortlet);
        return user;
    }

    /** A user who passes the old portlet-only check but is not a CMS administrator. */
    private User givenPortletHolderWhoIsNotAdmin() throws Exception {
        return givenLoggedInUser(false, true);
    }

    /**
     * Given: a logged-in back-end user who is not a CMS administrator and has no Maintenance portlet
     * When: validateUser is called
     * Then: a DotSecurityException is thrown
     */
    @Test
    void validateUser_rejectsUserWithoutAdminOrPortlet() throws Exception {
        givenLoggedInUser(false, false);

        assertThrows(DotSecurityException.class, () -> new CMSMaintenanceAjax().validateUser());
    }

    /**
     * Given: a back-end user with the Maintenance portlet who is not a CMS administrator
     * When: validateUser is called
     * Then: a DotSecurityException is thrown
     */
    @Test
    void validateUser_rejectsPortletHolderWhoIsNotAdmin() throws Exception {
        givenPortletHolderWhoIsNotAdmin();

        assertThrows(DotSecurityException.class, () -> new CMSMaintenanceAjax().validateUser());
    }

    /**
     * Given: a CMS administrator without the Maintenance portlet
     * When: validateUser is called
     * Then: a DotSecurityException is thrown
     */
    @Test
    void validateUser_rejectsAdminWithoutPortlet() throws Exception {
        givenLoggedInUser(true, false);

        assertThrows(DotSecurityException.class, () -> new CMSMaintenanceAjax().validateUser());
    }

    /**
     * Given: a CMS administrator with the Maintenance portlet
     * When: validateUser is called
     * Then: it returns true
     */
    @Test
    void validateUser_allowsAdminWithPortlet() throws Exception {
        givenLoggedInUser(true, true);

        assertTrue(new CMSMaintenanceAjax().validateUser());
    }

    /**
     * Given: no logged-in user in the request
     * When: validateUser is called
     * Then: a DotSecurityException is thrown
     */
    @Test
    void validateUser_rejectsMissingUser() {
        assertThrows(DotSecurityException.class, () -> new CMSMaintenanceAjax().validateUser());
    }

    /**
     * Given: a user who is not a CMS administrator with the Maintenance portlet
     * When: removeOldVersions is called with a valid date
     * Then: a DotSecurityException is thrown and no old version is deleted
     */
    @Test
    void removeOldVersions_requiresAdminWithMaintenancePortlet() throws Exception {
        givenPortletHolderWhoIsNotAdmin();

        assertThrows(DotSecurityException.class,
                () -> new CMSMaintenanceAjax().removeOldVersions("01/31/2026"));
        maintenanceFactory.verify(() -> CMSMaintenanceFactory.deleteOldAssetVersions(any()), never());
    }

    /**
     * Given: a user who is not a CMS administrator with the Maintenance portlet
     * When: cleanAssets is called
     * Then: a DotSecurityException is thrown and the clean assets job is not created or started
     */
    @Test
    void cleanAssets_requiresAdminWithMaintenancePortlet() throws Exception {
        givenPortletHolderWhoIsNotAdmin();

        assertThrows(DotSecurityException.class, () -> new CMSMaintenanceAjax().cleanAssets());
        cleanAssetsThread.verify(() -> CleanAssetsThread.getInstance(any(), anyBoolean()), never());
    }

    /**
     * Given: a user who is not a CMS administrator with the Maintenance portlet
     * When: deletePushedAssets is called
     * Then: a DotSecurityException is thrown, not a result string, and the push history is kept
     */
    @Test
    void deletePushedAssets_requiresAdminWithMaintenancePortlet() throws Exception {
        givenPortletHolderWhoIsNotAdmin();

        assertThrows(DotSecurityException.class,
                () -> new CMSMaintenanceAjax().deletePushedAssets());
        verify(pushedAssetsAPI, never()).deleteAllPushedAssets();
    }

    /**
     * Given: a user who is not a CMS administrator with the Maintenance portlet
     * When: getCleanAssetsStatus is called
     * Then: a DotSecurityException is thrown
     */
    @Test
    void getCleanAssetsStatus_requiresAdminWithMaintenancePortlet() throws Exception {
        givenPortletHolderWhoIsNotAdmin();

        assertThrows(DotSecurityException.class,
                () -> new CMSMaintenanceAjax().getCleanAssetsStatus());
        cleanAssetsThread.verify(() -> CleanAssetsThread.getInstance(any(), anyBoolean()), never());
    }

    /**
     * Given: a user who is not a CMS administrator with the Maintenance portlet
     * When: getFixAssetsProgress is called
     * Then: a DotSecurityException is thrown
     */
    @Test
    void getFixAssetsProgress_requiresAdminWithMaintenancePortlet() throws Exception {
        givenPortletHolderWhoIsNotAdmin();

        assertThrows(DotSecurityException.class,
                () -> new CMSMaintenanceAjax().getFixAssetsProgress());
        verify(fixTasksExecutor, never()).getTasksresults();
    }

    /** The public methods that already called the portlet check before the admin requirement. */
    static Stream<Arguments> alreadyGatedMethods() {
        return Stream.of(
                Arguments.of("getReindexationProgress", (MaintenanceCall) CMSMaintenanceAjax::getReindexationProgress),
                Arguments.of("deleteIndex", (MaintenanceCall) ajax -> ajax.deleteIndex("index-name")),
                Arguments.of("stopReindexation", (MaintenanceCall) CMSMaintenanceAjax::stopReindexation),
                Arguments.of("stopReindexationAndSwitchover", (MaintenanceCall) CMSMaintenanceAjax::stopReindexationAndSwitchover),
                Arguments.of("cleanReindexStructure", (MaintenanceCall) ajax -> ajax.cleanReindexStructure("type-inode")),
                Arguments.of("optimizeIndices", (MaintenanceCall) CMSMaintenanceAjax::optimizeIndices),
                Arguments.of("flushIndiciesCache", (MaintenanceCall) CMSMaintenanceAjax::flushIndiciesCache),
                Arguments.of("fixAssetsInconsistencies", (MaintenanceCall) CMSMaintenanceAjax::fixAssetsInconsistencies),
                Arguments.of("deleteContentletsFromIdList", (MaintenanceCall) ajax -> ajax.deleteContentletsFromIdList("id-1", "back-end-user")));
    }

    /**
     * Given: a back-end user with the Maintenance portlet who is not a CMS administrator
     * When: each of the other public methods is called (reindex, index, cache, fix assets, id-list delete)
     * Then: every call throws a DotSecurityException
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("alreadyGatedMethods")
    void alreadyGatedMethods_requireAdminWithMaintenancePortlet(final String method,
            final MaintenanceCall call) throws Exception {
        givenPortletHolderWhoIsNotAdmin();

        assertThrows(DotSecurityException.class, () -> call.run(new CMSMaintenanceAjax()),
                method + " must require a CMS administrator with the Maintenance portlet");
    }

    /**
     * Given: a CMS administrator with the Maintenance portlet
     * When: removeOldVersions is called with 01/31/2026
     * Then: old versions older than that date are deleted and their count is returned
     */
    @Test
    void removeOldVersions_runsForAdminWithPortlet() throws Exception {
        givenLoggedInUser(true, true);
        final Date expectedDate = new SimpleDateFormat("MM/dd/yyyy").parse("01/31/2026");
        maintenanceFactory.when(() -> CMSMaintenanceFactory.deleteOldAssetVersions(expectedDate))
                .thenReturn(7);

        assertEquals(7, new CMSMaintenanceAjax().removeOldVersions("01/31/2026"));
        maintenanceFactory.verify(() -> CMSMaintenanceFactory.deleteOldAssetVersions(expectedDate));
    }

    /**
     * Given: a CMS administrator with the Maintenance portlet
     * When: cleanAssets is called
     * Then: the clean assets job is created, started, and its initial status is returned
     */
    @Test
    void cleanAssets_startsJobForAdminWithPortlet() throws Exception {
        givenLoggedInUser(true, true);
        final CleanAssetsThread job = mock(CleanAssetsThread.class);
        final CleanAssetsThread.BasicProcessStatus status = mock(CleanAssetsThread.BasicProcessStatus.class);
        final Map<String, String> initialStatus = Map.of("running", "true");
        when(status.buildStatusMap()).thenReturn(initialStatus);
        when(job.getProcessStatus()).thenReturn(status);
        cleanAssetsThread.when(() -> CleanAssetsThread.getInstance(true, true)).thenReturn(job);

        assertEquals(initialStatus, new CMSMaintenanceAjax().cleanAssets());
        verify(job).start();
    }

    /**
     * Given: a CMS administrator with the Maintenance portlet
     * When: deletePushedAssets is called
     * Then: the whole push history is deleted and "success" is returned
     */
    @Test
    void deletePushedAssets_deletesHistoryForAdminWithPortlet() throws Exception {
        givenLoggedInUser(true, true);

        assertEquals("success", new CMSMaintenanceAjax().deletePushedAssets());
        verify(pushedAssetsAPI).deleteAllPushedAssets();
    }

    /**
     * Given: a CMS administrator with the Maintenance portlet
     * When: fixAssetsInconsistencies is called
     * Then: the fix tasks are run and their results are returned
     */
    @Test
    void fixAssetsInconsistencies_runsFixTasksForAdminWithPortlet() throws Exception {
        givenLoggedInUser(true, true);
        final List<Map<String, Integer>> results = List.of(Map.of("total", 3));
        when(fixTasksExecutor.getTasksresults()).thenReturn(results);

        assertEquals(results, new CMSMaintenanceAjax().fixAssetsInconsistencies());
        verify(fixTasksExecutor).execute(isNull());
    }

    /**
     * Given: a CMS administrator with the Maintenance portlet
     * When: cleanReindexStructure is called for a content type
     * Then: that type's content is removed from the index, the type is reindexed and the
     * confirmation message is returned
     */
    @Test
    void cleanReindexStructure_reindexesTypeForAdminWithPortlet() throws Exception {
        givenLoggedInUser(true, true);
        final Structure type = mock(Structure.class);
        final ContentTypeCache contentTypeCache = mock(ContentTypeCache.class);
        when(contentTypeCache.getStructureByInode("type-inode")).thenReturn(type);
        final ContentletIndexAPI contentletIndexAPI = mock(ContentletIndexAPI.class);
        apiLocator.when(APILocator::getContentletIndexAPI).thenReturn(contentletIndexAPI);
        final Company company = mock(Company.class);
        when(company.getCompanyId()).thenReturn("company-id");
        when(company.getLocale()).thenReturn(Locale.US);
        languageUtil.when(() -> LanguageUtil.get("company-id", Locale.US,
                "message.cmsmaintenance.cache.indexrebuilt")).thenReturn("Index rebuilt");

        try (MockedStatic<CacheLocator> cacheLocator = mockStatic(CacheLocator.class);
                MockedStatic<PublicCompanyFactory> companies = mockStatic(PublicCompanyFactory.class)) {
            cacheLocator.when(CacheLocator::getContentTypeCache).thenReturn(contentTypeCache);
            companies.when(PublicCompanyFactory::getDefaultCompany).thenReturn(company);

            assertEquals("Index rebuilt", new CMSMaintenanceAjax().cleanReindexStructure("type-inode"));
        }
        verify(contentletIndexAPI).removeContentFromIndexByStructureInode("type-inode");
        verify(contentletAPI).refresh(type);
    }

    /**
     * Given: a CMS administrator with the Maintenance portlet and a userId argument naming another user
     * When: deleteContentletsFromIdList is called
     * Then: the content is destroyed as the session user and the other user is never looked up
     */
    @Test
    void deleteContentletsFromIdList_usesSessionUser() throws Exception {
        final User sessionUser = givenLoggedInUser(true, true);
        final User otherUser = mock(User.class);
        userLocalManager.when(() -> UserLocalManagerUtil.getUserById("other-user")).thenReturn(otherUser);
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getIdentifier()).thenReturn("id-1");
        when(contentletAPI.getSiblings("id-1")).thenReturn(List.of(contentlet));
        when(contentletAPI.destroy(any(Contentlet.class), any(User.class), anyBoolean())).thenReturn(true);

        new CMSMaintenanceAjax().deleteContentletsFromIdList("id-1", "other-user");

        verify(contentletAPI).destroy(contentlet, sessionUser, true);
        verify(contentletAPI, never()).destroy(any(Contentlet.class), eq(otherUser), anyBoolean());
        userLocalManager.verify(() -> UserLocalManagerUtil.getUserById(anyString()), never());
    }

    /**
     * Given: a user who is not allowed
     * When: validateUser is called
     * Then: the denial is recorded once in the security log and nothing goes to the error or warning log
     */
    @Test
    void validateUser_logsDenialOnce() throws Exception {
        givenPortletHolderWhoIsNotAdmin();

        assertThrows(DotSecurityException.class, () -> new CMSMaintenanceAjax().validateUser());

        assertEquals(1, securityLoggerCalls.stream().filter(name -> name.startsWith("log")).count(),
                "one security log entry per denial, got " + securityLoggerCalls);
        assertTrue(loggerCalls.stream().noneMatch(name -> name.equals("error") || name.equals("warn")),
                "a denial is not an error, got " + loggerCalls);
    }
}

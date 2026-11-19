package com.dotmarketing.webdav;

import static com.dotcms.unittest.TestUtil.upperCaseRandom;
import static org.mockito.Mockito.when;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.FileResource;
import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.DotCMSMockRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DotWebdavHelperTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private final DotWebdavHelper helper = new DotWebdavHelper();

    /** Address every authentication in these tests is sent from; API tokens are issued from it too. */
    private static final String CLIENT_IP = "192.0.2.10";

    /** Start of every security-log line about a token-based WebDAV authentication decision. */
    private static final String AUDIT_PREFIX = "WebDAV token authentication";

    private static final List<User> createdUsers = new ArrayList<>();
    private static final List<ApiToken> createdTokens = new ArrayList<>();
    private static final List<Host> createdSites = new ArrayList<>();

    /** Removes the tokens and users the authentication tests created. */
    @AfterClass
    public static void cleanup() {
        try {
            createdTokens.forEach(token ->
                    APILocator.getApiTokenAPI().deleteToken(token, APILocator.systemUser()));
            createdTokens.clear();
        } finally {
            createdUsers.forEach(UserDataGen::remove);
            createdUsers.clear();
            removeCreatedSites();
        }
    }

    /** Archives and deletes the sites the live/working tree tests created, with their folders and files. */
    private static void removeCreatedSites() {
        for (final Host site : createdSites) {
            try {
                APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            } catch (final Exception e) {
                Logger.warn(DotWebdavHelperTest.class, "Unable to delete test site: " + e.getMessage());
            }
        }
        createdSites.clear();
    }

    private static User newUserWithPassword(final String password) {
        final User user = new UserDataGen().password(password).nextPersisted();
        createdUsers.add(user);
        return user;
    }

    @Test
    public void Test_Get_Folder_Resource_Then_Get_File_Resource() throws IOException, DotDataException, DotSecurityException {
        final SiteDataGen siteDataGen = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host host = siteDataGen.nextPersisted();
        final Folder parent = folderDataGen.site(host).nextPersisted();
        final Folder child = folderDataGen.parent(parent).nextPersisted();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");
        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(child, file);
        fileAssetDataGen.nextPersisted();
        final String folderPath = String.format("http://localhost:8080/webdav/live/1/%s/%s/",host.getName(),parent.getName());
        final Resource folderResource = helper.getResourceFromURL(folderPath);
        Assert.assertNotNull(folderResource);
        Assert.assertTrue(folderResource instanceof FolderResource);
        final String fileResourcePath = String.format("http://localhost:8080/webdav/live/1/%s/%s/%s/%s",host.getName(),parent.getName(),child.getName(),file.getName());
        final Resource fileResource = helper.getResourceFromURL(fileResourcePath);
        Assert.assertNotNull(fileResource);
        Assert.assertTrue(fileResource instanceof FileResource);
    }


    @Test
    public void Test_Get_Folder_Resource_Then_Get_File_Resource_Shuffled_Casing() throws IOException, DotDataException, DotSecurityException {
        final SiteDataGen siteDataGen = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host host = siteDataGen.nextPersisted();
        final Folder parent = folderDataGen.site(host).nextPersisted();
        final Folder child = folderDataGen.parent(parent).nextPersisted();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");
        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(child, file);
        fileAssetDataGen.nextPersisted();
        final String folderPath = upperCaseRandom(String.format("http://localhost:8080/webdav/live/1/%s/%s/",host.getName(),parent.getName()), 8);

        final Resource folderResource = helper.getResourceFromURL(folderPath);
        Assert.assertNotNull(folderResource);
        Assert.assertTrue(folderResource instanceof FolderResource);
        final String fileResourcePath = upperCaseRandom(String.format("http://localhost:8080/webdav/live/1/%s/%s/%s/%s",host.getName(),parent.getName(),child.getName(),file.getName()),8);
        final Resource fileResource = helper.getResourceFromURL(fileResourcePath);
        Assert.assertNotNull(fileResource);
        Assert.assertTrue(fileResource instanceof FileResource);
    }

    @Test
    public void Test_Get_Folder_Resource_For_Non_Existing_Path() throws IOException, DotDataException, DotSecurityException {
        String path = "http://localhost:8080/webdav/live/1/demo.dotcms.com/images/black.png";
        final Resource folderResource = helper.getResourceFromURL(path);
        Assert.assertNull(folderResource);
    }

    @Test
    public void Test_Same_Resource_For_Paths_With_Different_Casing() throws IOException, DotDataException, DotSecurityException {
        final SiteDataGen siteDataGen = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host host = siteDataGen.nextPersisted();
        final Folder parent = folderDataGen.site(host).nextPersisted();
        final Folder child = folderDataGen.parent(parent).nextPersisted();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");
        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(child, file);
        fileAssetDataGen.nextPersisted();
        final String path = String.format("http://localhost:8080/webdav/live/1/%s/%s/%s/%s",host.getName(),parent.getName(),child.getName(),file.getName());
        final String fileResourcePath1 = upperCaseRandom(path,8);
        final String fileResourcePath2 = upperCaseRandom(path,10);
        Assert.assertNotEquals(fileResourcePath1,fileResourcePath2);
        Assert.assertTrue(helper.isSameResourceURL(fileResourcePath1,fileResourcePath2, file.getName()));
    }

    /**
     * Method to test: DotWebdavHelper.setResourceContent
     * <p>
     * Given Scenario: A file whose name carries a listed extension, in upper or lower case, is
     * written through WebDAV.
     * <p>
     * ExpectedResult: The write is refused and no file resource exists at that path afterwards.
     */
    @Test
    public void Test_setResourceContent_listed_extension_any_case_is_rejected() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        final var user = APILocator.getUserAPI().getSystemUser();
        try (MockedStatic<HttpManager> mocked = Mockito.mockStatic(HttpManager.class)) {
            final Request mockedRequest = Mockito.mock(Request.class);
            when(mockedRequest.getUserAgentHeader()).thenReturn("Cyberduck");
            mocked.when(HttpManager::request).thenReturn(mockedRequest);
            for (final String name : new String[] {"sample-1.jsp", "sample-2.JSP"}) {
                final var resourceUri = String.format("/webdav/live/1/%s/%s", host.getName(), name);
                try (InputStream content = new ByteArrayInputStream("qa".getBytes())) {
                    Assert.assertThrows(name, IllegalArgumentException.class,
                            () -> helper.setResourceContent(resourceUri, content, "",
                                    null, java.util.Calendar.getInstance().getTime(), user, true));
                }
                Assert.assertNull("no file may exist at " + resourceUri, helper.loadFile(resourceUri, user));
            }
        }
    }

    /**
     * Method to test: DotWebdavHelper.setResourceContent
     * <p>
     * Given Scenario: A file is created with no content, then the same file is updated also with no
     * content.
     * <p>
     * ExpectedResult: The file is created and updated without exceptions.
     *
     * @throws Exception if an error occurs while updating the file with no content.
     */
    @Test
    public void Test_publishing_existing_file_with_empty_file_should_not_fail() throws Exception {

        final SiteDataGen siteDataGen = new SiteDataGen();
        final Host host = siteDataGen.nextPersisted();
        final var user = APILocator.getUserAPI().getSystemUser();

        final var resourceUri = String.format("/webdav/live/1/%s/test-file.vtl", host.getName());

        try (MockedStatic<HttpManager> mocked = Mockito.mockStatic(HttpManager.class)) {

            // Create a mocked request object
            Request mockedRequest = Mockito.mock(Request.class);
            when(mockedRequest.getUserAgentHeader()).thenReturn("Cyberduck");
            mocked.when(HttpManager::request).thenReturn(mockedRequest);

            try (InputStream emptyFileInputStream =
                    new ByteArrayInputStream(new byte[0])) {// Empty file

                // First publish, the contentlet should be created
                helper.setResourceContent(resourceUri, emptyFileInputStream, "",
                        null, java.util.Calendar.getInstance().getTime(), user, true);

                // Second publish, the contentlet should be updated without exceptions
                helper.setResourceContent(resourceUri, emptyFileInputStream, "",
                        null, java.util.Calendar.getInstance().getTime(), user, true);

                // Third publish, the contentlet should be updated without exceptions
                helper.setResourceContent(resourceUri, emptyFileInputStream, "",
                        null, java.util.Calendar.getInstance().getTime(), user, true);
            }
        }
    }

    // ---- Token-based authentication: decision + audit line ------------------------------------

    /**
     * Method to test: {@link DotWebdavHelper#authorizePrincipal(String, String)} in token mode.
     * <p>
     * Given Scenario: The Basic-auth mode is {@code JWT_TOKEN} and the client sends its login with a
     * valid API token of that same account as the password.
     * <p>
     * ExpectedResult: The account is returned and exactly one accepted line is written to the security
     * log at INFO, naming the user id, the token id and the client address. No line written while the
     * request was authenticated contains the credential.
     */
    @Test
    public void Test_authorizePrincipal_token_mode_accepted_api_token_is_audited() throws Exception {
        final User user = newUserWithPassword("aValidPassword-" + System.currentTimeMillis());
        final ApiToken token = newApiToken(user);
        final String jwt = jwtOf(token);
        final User[] authorized = new User[1];

        inMode("JWT_TOKEN", () -> {
            final List<AuditEntry> entries = captureSecurityLog(
                    () -> authorized[0] = helper.authorizePrincipal(davLogin(user), jwt));

            Assert.assertNotNull("token WebDAV auth should resolve a user", authorized[0]);
            Assert.assertEquals(user.getUserId(), authorized[0].getUserId());
            final AuditEntry decision = singleDecision(entries);
            Assert.assertEquals(decision.message, Level.INFO, decision.level);
            Assert.assertTrue(decision.message, decision.message.contains("accepted"));
            Assert.assertTrue(decision.message, decision.message.contains(user.getUserId()));
            Assert.assertTrue(decision.message, decision.message.contains(token.getId()));
            Assert.assertTrue(decision.message, decision.message.contains(CLIENT_IP));
            assertCredentialAbsent(entries, jwt);
        });
    }

    /**
     * Method to test: {@link DotWebdavHelper#authorizePrincipal(String, String)} in token mode.
     * <p>
     * Given Scenario: The Basic-auth mode is {@code JWT_TOKEN} and the password field carries a valid
     * login (session) token of the named account instead of an API token.
     * <p>
     * ExpectedResult: Authentication is refused with a {@link DotSecurityException} and exactly one
     * rejected line is written to the security log at WARN, giving the token-type reason and the
     * requested login. The token itself resolves as a valid JWT, so the refusal is by token type and
     * not because the token is malformed. No line contains the credential.
     */
    @Test
    public void Test_authorizePrincipal_token_mode_rejected_login_token_is_audited() throws Exception {
        final User user = newUserWithPassword("aValidPassword-" + System.currentTimeMillis());
        final String jwt = JsonWebTokenUtils.getInstance().createUserToken(user, 1);

        inMode("JWT_TOKEN", () -> {
            Assert.assertTrue("login token should resolve as a valid JWT",
                    APILocator.getApiTokenAPI().fromJwt(jwt, CLIENT_IP).isPresent());

            final List<AuditEntry> entries = captureSecurityLog(
                    () -> Assert.assertThrows(DotSecurityException.class,
                            () -> helper.authorizePrincipal(davLogin(user), jwt)));

            final AuditEntry decision = singleDecision(entries);
            Assert.assertEquals(decision.message, Level.WARN, decision.level);
            Assert.assertTrue(decision.message, decision.message.contains("rejected"));
            Assert.assertTrue(decision.message, decision.message.contains("WRONG_TOKEN_TYPE"));
            Assert.assertTrue(decision.message, decision.message.contains(davLogin(user)));
            Assert.assertTrue(decision.message, decision.message.contains(CLIENT_IP));
            assertCredentialAbsent(entries, jwt);
            // A login token's id is the account's remember-me token: it is token material and must
            // not be written either.
            final String rememberMeToken = user.getRememberMeToken();
            Assert.assertTrue("the persisted user carries a remember-me token", UtilMethods.isSet(rememberMeToken));
            for (final AuditEntry entry : entries) {
                Assert.assertFalse("login token id must not be logged: " + entry.message,
                        entry.message.contains(rememberMeToken));
            }
        });
    }

    /**
     * Method to test: {@link DotWebdavHelper#authorizePrincipal(String, String)} in token mode.
     * <p>
     * Given Scenario: The Basic-auth mode is {@code JWT_TOKEN} and the client sends one account's
     * login with a valid API token that belongs to a different account.
     * <p>
     * ExpectedResult: Authentication is refused with a {@link DotSecurityException} and exactly one
     * rejected line is written to the security log at WARN, giving the owner-mismatch reason, the
     * requested login and the token id. No line contains the credential.
     */
    @Test
    public void Test_authorizePrincipal_token_mode_rejected_token_of_another_account_is_audited() throws Exception {
        final User requested = newUserWithPassword("aValidPassword-" + System.currentTimeMillis());
        final User owner = newUserWithPassword("anotherValidPassword-" + System.currentTimeMillis());
        final ApiToken token = newApiToken(owner);
        final String jwt = jwtOf(token);

        inMode("JWT_TOKEN", () -> {
            final List<AuditEntry> entries = captureSecurityLog(
                    () -> Assert.assertThrows(DotSecurityException.class,
                            () -> helper.authorizePrincipal(davLogin(requested), jwt)));

            final AuditEntry decision = singleDecision(entries);
            Assert.assertEquals(decision.message, Level.WARN, decision.level);
            Assert.assertTrue(decision.message, decision.message.contains("rejected"));
            Assert.assertTrue(decision.message, decision.message.contains("OWNER_MISMATCH"));
            Assert.assertTrue(decision.message, decision.message.contains(davLogin(requested)));
            Assert.assertTrue(decision.message, decision.message.contains(token.getId()));
            Assert.assertFalse("a rejected line names no authenticated user: " + decision.message,
                    decision.message.contains("accepted"));
            assertCredentialAbsent(entries, jwt);
        });
    }

    /**
     * Method to test: {@link DotWebdavHelper#authorizePrincipal(String, String)} in token mode.
     * <p>
     * Given Scenario: The Basic-auth mode is {@code JWT_TOKEN} and the password field carries a value
     * that is not a JWT at all.
     * <p>
     * ExpectedResult: Authentication is refused with a {@link DotSecurityException} and exactly one
     * rejected line is written to the security log at WARN, saying the credential was not a token and
     * naming the requested login. No line contains the value that was sent.
     */
    @Test
    public void Test_authorizePrincipal_token_mode_rejected_plain_password_is_audited() throws Exception {
        final User user = newUserWithPassword("aValidPassword-" + System.currentTimeMillis());
        final String credential = "not-a-password-nor-a-token-" + System.currentTimeMillis();

        inMode("JWT_TOKEN", () -> {
            final List<AuditEntry> entries = captureSecurityLog(
                    () -> Assert.assertThrows(DotSecurityException.class,
                            () -> helper.authorizePrincipal(davLogin(user), credential)));

            final AuditEntry decision = singleDecision(entries);
            Assert.assertEquals(decision.message, Level.WARN, decision.level);
            Assert.assertTrue(decision.message, decision.message.contains("rejected"));
            Assert.assertTrue(decision.message, decision.message.contains("NOT_A_TOKEN"));
            Assert.assertTrue(decision.message, decision.message.contains(davLogin(user)));
            assertCredentialAbsent(entries, credential);
        });
    }

    /**
     * Method to test: {@link DotWebdavHelper#authorizePrincipal(String, String)} in legacy mode.
     * <p>
     * Given Scenario: The Basic-auth mode is {@code ALL} and the client sends an account's login
     * with that same account's browser login token (a session token) as the password, which is
     * not the stored password.
     * <p>
     * ExpectedResult: Authentication is refused with a {@link DotSecurityException} and exactly one
     * rejected line is written to the security log at WARN naming the token type. No line contains
     * the credential or the login token's id.
     */
    @Test
    public void Test_authorizePrincipal_legacy_mode_rejected_login_token_of_same_account() throws Exception {
        final User user = newUserWithPassword("aValidPassword-" + System.currentTimeMillis());
        final String jwt = JsonWebTokenUtils.getInstance().createUserToken(user, 1);
        final String delayBefore = Config.getStringProperty(
                WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, "true");
        try {
            // The per-attempt sleep after the password mismatch is not under test.
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, false);
            inMode("ALL", () -> {
                Assert.assertTrue("login token should resolve as a valid JWT",
                        APILocator.getApiTokenAPI().fromJwt(jwt, CLIENT_IP).isPresent());

                final List<AuditEntry> entries = captureSecurityLog(
                        () -> Assert.assertThrows(DotSecurityException.class,
                                () -> helper.authorizePrincipal(davLogin(user), jwt)));

                final AuditEntry decision = singleDecision(entries);
                Assert.assertEquals(decision.message, Level.WARN, decision.level);
                Assert.assertTrue(decision.message, decision.message.contains("rejected"));
                Assert.assertTrue(decision.message, decision.message.contains("WRONG_TOKEN_TYPE"));
                Assert.assertFalse(decision.message, decision.message.contains("accepted"));
                assertCredentialAbsent(entries, jwt);
                final String rememberMeToken = user.getRememberMeToken();
                for (final AuditEntry entry : entries) {
                    Assert.assertFalse("login token id must not be logged: " + entry.message,
                            entry.message.contains(rememberMeToken));
                }
            });
        } finally {
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, delayBefore);
        }
    }

    /**
     * Method to test: {@link DotWebdavHelper#authorizePrincipal(String, String)} in legacy mode.
     * <p>
     * Given Scenario: The Basic-auth mode is {@code ALL}, so the stored password is checked first and
     * fails, and the password field is then evaluated as a token: a valid API token of the named account.
     * <p>
     * ExpectedResult: The account is returned and exactly one accepted line is written to the security
     * log at INFO with the user id and token id, the same shape as in token mode. No line contains the
     * credential.
     */
    @Test
    public void Test_authorizePrincipal_legacy_mode_accepted_api_token_is_audited() throws Exception {
        final User user = newUserWithPassword("aValidPassword-" + System.currentTimeMillis());
        final ApiToken token = newApiToken(user);
        final String jwt = jwtOf(token);
        final User[] authorized = new User[1];
        final String delayBefore = Config.getStringProperty(
                WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, "true");
        try {
            // The per-attempt sleep after the password mismatch is not under test.
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, false);
            inMode("ALL", () -> {
                final List<AuditEntry> entries = captureSecurityLog(
                        () -> authorized[0] = helper.authorizePrincipal(davLogin(user), jwt));

                Assert.assertNotNull("token WebDAV auth should resolve a user", authorized[0]);
                Assert.assertEquals(user.getUserId(), authorized[0].getUserId());
                final AuditEntry decision = singleDecision(entries);
                Assert.assertEquals(decision.message, Level.INFO, decision.level);
                Assert.assertTrue(decision.message, decision.message.contains("accepted"));
                Assert.assertTrue(decision.message, decision.message.contains(user.getUserId()));
                Assert.assertTrue(decision.message, decision.message.contains(token.getId()));
                assertCredentialAbsent(entries, jwt);
            });
        } finally {
            Config.setProperty(WebKeys.AUTH_FAILED_ATTEMPTS_DELAY_STRATEGY_ENABLED, delayBefore);
        }
    }

    // ---- helpers -------------------------------------------------------------------------------

    /** Work that may throw, run under a pinned mode or a log capture. */
    @FunctionalInterface
    private interface Call {
        void run() throws Exception;
    }

    /** One security-log entry: its level and its formatted text. */
    private static final class AuditEntry {
        private final Level level;
        private final String message;

        private AuditEntry(final Level level, final String message) {
            this.level = level;
            this.message = message;
        }
    }

    /**
     * Persists an API token for the user, issued from {@link #CLIENT_IP}, and remembers it for cleanup.
     */
    private static ApiToken newApiToken(final User user) {
        final ApiToken token = APILocator.getApiTokenAPI().persistApiToken(user.getUserId(),
                Date.from(Instant.now().plus(1, ChronoUnit.DAYS)),
                APILocator.systemUser().getUserId(), CLIENT_IP);
        createdTokens.add(token);
        return token;
    }

    /** The compact JWT a client would send for the token. */
    private static String jwtOf(final ApiToken token) {
        return APILocator.getApiTokenAPI().getJWT(token, APILocator.systemUser());
    }

    /** The login a WebDAV client sends for the user, following the company's email-or-id convention. */
    private static String davLogin(final User user) {
        return Company.AUTH_TYPE_ID.equals(PublicCompanyFactory.getDefaultCompany().getAuthType())
                ? user.getUserId() : user.getEmailAddress();
    }

    /**
     * Runs the call with the Basic-auth mode pinned and a request from {@link #CLIENT_IP} bound to the
     * thread, then restores both and releases any connection the lockout bookkeeping opened.
     */
    private static void inMode(final String mode, final Call call) throws Exception {
        final String previousMode = Config.getStringProperty("ALLOW_BASIC_AUTH", null);
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        try {
            Config.setProperty("ALLOW_BASIC_AUTH", mode);
            final DotCMSMockRequest request = new DotCMSMockRequest();
            request.setRemoteAddr(CLIENT_IP);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
            call.run();
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
            Config.setProperty("ALLOW_BASIC_AUTH", previousMode);
            DbConnectionFactory.closeSilently();
        }
    }

    /**
     * Runs the call and returns every entry the security log received meanwhile, at INFO and above,
     * leaving the logger's level and appenders as they were.
     */
    private static List<AuditEntry> captureSecurityLog(final Call call) throws Exception {
        final List<AuditEntry> entries = new CopyOnWriteArrayList<>();
        final AbstractAppender appender = new AbstractAppender("webdav-token-audit-capture",
                null, null, true, null) {
            @Override
            public void append(final LogEvent event) {
                entries.add(new AuditEntry(event.getLevel(), event.getMessage().getFormattedMessage()));
            }
        };
        appender.start();
        final org.apache.logging.log4j.core.Logger logger =
                (org.apache.logging.log4j.core.Logger) LogManager.getLogger(SecurityLogger.class);
        final Level previousLevel = logger.getLevel();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        try {
            call.run();
        } finally {
            logger.removeAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
        return entries;
    }

    /** The one WebDAV token-login decision line among the captured entries; fails on none or several. */
    private static AuditEntry singleDecision(final List<AuditEntry> entries) {
        final List<AuditEntry> decisions = entries.stream()
                .filter(entry -> entry.message.contains(AUDIT_PREFIX))
                .collect(Collectors.toList());
        Assert.assertEquals("one audit line per decision, got: " + messages(entries), 1, decisions.size());
        return decisions.get(0);
    }

    /** Fails when any captured entry carries the credential, or its signature segment, in any form. */
    private static void assertCredentialAbsent(final List<AuditEntry> entries, final String credential) {
        final String signature = credential.substring(credential.lastIndexOf('.') + 1);
        for (final AuditEntry entry : entries) {
            Assert.assertFalse("credential must not be logged: " + entry.message,
                    entry.message.contains(credential));
            Assert.assertFalse("credential segment must not be logged: " + entry.message,
                    signature.length() >= 8 && entry.message.contains(signature));
        }
    }

    private static List<String> messages(final List<AuditEntry> entries) {
        return entries.stream().map(entry -> entry.level + " " + entry.message).collect(Collectors.toList());
    }

    // ---- Live and working trees: which version each one lists and serves ----------------------

    /**
     * Method to test: {@link DotWebdavHelper#getChildrenOfFolder(Folder, User, boolean, long)}
     * <p>
     * Given Scenario: A folder holds one published file and one file that was saved but never
     * published.
     * <p>
     * ExpectedResult: The live tree lists only the published file.
     */
    @Test
    public void Test_live_folder_listing_shows_only_published_files() throws Exception {
        final Folder folder = new FolderDataGen().site(newSite()).nextPersisted();
        final Contentlet published = ContentletDataGen.publish(newFileAsset(folder, "published-file.txt"));
        final Contentlet unpublished = newFileAsset(folder, "unpublished-file.txt");

        final List<String> names;
        try (MockedStatic<HttpManager> mocked = mockRequestMethod(Request.Method.PROPFIND)) {
            names = resourceNames(helper.getChildrenOfFolder(
                    folder, APILocator.systemUser(), true, defaultLanguageId()));
        }

        Assert.assertTrue(names.toString(), names.contains(fileName(published)));
        Assert.assertFalse(names.toString(), names.contains(fileName(unpublished)));
    }

    /**
     * Method to test: {@link DotWebdavHelper#getChildrenOfFolder(Folder, User, boolean, long)}
     * <p>
     * Given Scenario: A folder holds one published file and one file that was saved but never
     * published.
     * <p>
     * ExpectedResult: The working tree lists both files.
     */
    @Test
    public void Test_working_folder_listing_shows_all_files() throws Exception {
        final Folder folder = new FolderDataGen().site(newSite()).nextPersisted();
        final Contentlet published = ContentletDataGen.publish(newFileAsset(folder, "published-file.txt"));
        final Contentlet unpublished = newFileAsset(folder, "unpublished-file.txt");

        final List<String> names;
        try (MockedStatic<HttpManager> mocked = mockRequestMethod(Request.Method.PROPFIND)) {
            names = resourceNames(helper.getChildrenOfFolder(
                    folder, APILocator.systemUser(), false, defaultLanguageId()));
        }

        Assert.assertTrue(names.toString(), names.contains(fileName(published)));
        Assert.assertTrue(names.toString(), names.contains(fileName(unpublished)));
    }

    /**
     * Method to test: {@link DotWebdavHelper#getChildrenOfFolder(Folder, User, boolean, long)}
     * <p>
     * Given Scenario: A published file has a newer version that was saved but not published.
     * <p>
     * ExpectedResult: The live tree lists the file once, as its published version.
     */
    @Test
    public void Test_live_folder_listing_shows_published_version_of_edited_file() throws Exception {
        final Folder folder = new FolderDataGen().site(newSite()).nextPersisted();
        final Contentlet published = ContentletDataGen.publish(newFileAsset(folder, "edited-file.txt"));
        ContentletDataGen.checkin(ContentletDataGen.checkout(published));

        final List<FileResourceImpl> files;
        try (MockedStatic<HttpManager> mocked = mockRequestMethod(Request.Method.PROPFIND)) {
            files = helper.getChildrenOfFolder(
                            folder, APILocator.systemUser(), true, defaultLanguageId()).stream()
                    .filter(FileResourceImpl.class::isInstance)
                    .map(FileResourceImpl.class::cast)
                    .collect(Collectors.toList());
        }

        Assert.assertEquals(1, files.size());
        Assert.assertEquals(published.getInode(), files.get(0).getFile().getInode());
    }

    /**
     * Method to test: {@link HostResourceImpl#getChildren()}
     * <p>
     * Given Scenario: The root of a site holds one published file and one file that was saved but
     * never published.
     * <p>
     * ExpectedResult: The live tree lists only the published file; the working tree lists both.
     */
    @Test
    public void Test_site_root_listing_shows_published_files_in_live_tree() throws Exception {
        final Host site = newSite();
        final Contentlet published = ContentletDataGen.publish(newFileAsset(site, "published-root-file.txt"));
        final Contentlet unpublished = newFileAsset(site, "unpublished-root-file.txt");

        try (MockedStatic<HttpManager> mocked = mockRequestMethod(Request.Method.PROPFIND)) {
            final List<String> live = resourceNames(siteRoot(site, "live").getChildren());
            Assert.assertTrue(live.toString(), live.contains(fileName(published)));
            Assert.assertFalse(live.toString(), live.contains(fileName(unpublished)));

            final List<String> working = resourceNames(siteRoot(site, "working").getChildren());
            Assert.assertTrue(working.toString(), working.contains(fileName(published)));
            Assert.assertTrue(working.toString(), working.contains(fileName(unpublished)));
        }
    }

    /**
     * Method to test: {@link ResourceFactoryImpl#getResource(String, String, DotWebdavHelper, HostAPI)}
     * <p>
     * Given Scenario: A file was saved but never published, and is read (GET) under each tree.
     * <p>
     * ExpectedResult: No resource is found under the live tree; under the working tree it is.
     */
    @Test
    public void Test_read_in_live_tree_does_not_find_unpublished_file() throws Exception {
        final Folder folder = new FolderDataGen().site(newSite()).nextPersisted();
        final Contentlet unpublished = newFileAsset(folder, "unpublished-read.txt");

        try (MockedStatic<HttpManager> mocked = mockRequestMethod(Request.Method.GET)) {
            Assert.assertNull(ResourceFactoryImpl.getResource("localhost",
                    fileUrl("live", folder, unpublished), helper, APILocator.getHostAPI()));
            Assert.assertNotNull(ResourceFactoryImpl.getResource("localhost",
                    fileUrl("working", folder, unpublished), helper, APILocator.getHostAPI()));
        }
    }

    /**
     * Method to test: {@link ResourceFactoryImpl#getResource(String, String, DotWebdavHelper, HostAPI)}
     * <p>
     * Given Scenario: A published file has a newer version that was saved but not published, and is
     * read (GET) under each tree.
     * <p>
     * ExpectedResult: The live tree serves the published version; the working tree the newer one.
     */
    @Test
    public void Test_read_serves_version_matching_the_tree() throws Exception {
        final Folder folder = new FolderDataGen().site(newSite()).nextPersisted();
        final Contentlet published = ContentletDataGen.publish(newFileAsset(folder, "versioned-read.txt"));
        final Contentlet newer = ContentletDataGen.checkin(ContentletDataGen.checkout(published));

        try (MockedStatic<HttpManager> mocked = mockRequestMethod(Request.Method.GET)) {
            final Resource live = ResourceFactoryImpl.getResource("localhost",
                    fileUrl("live", folder, published), helper, APILocator.getHostAPI());
            Assert.assertEquals(published.getInode(), ((FileResourceImpl) live).getFile().getInode());

            final Resource working = ResourceFactoryImpl.getResource("localhost",
                    fileUrl("working", folder, published), helper, APILocator.getHostAPI());
            Assert.assertEquals(newer.getInode(), ((FileResourceImpl) working).getFile().getInode());
        }
    }

    /**
     * Method to test: {@link ResourceFactoryImpl#getResource(String, String, DotWebdavHelper, HostAPI)}
     * <p>
     * Given Scenario: A file was saved but never published, and other read methods (HEAD, PROPFIND)
     * address it under the live tree.
     * <p>
     * ExpectedResult: No resource is found, as for GET.
     */
    @Test
    public void Test_other_read_methods_in_live_tree_do_not_find_unpublished_file() throws Exception {
        final Folder folder = new FolderDataGen().site(newSite()).nextPersisted();
        final Contentlet unpublished = newFileAsset(folder, "unpublished-head.txt");

        for (final Request.Method method : new Request.Method[] {Request.Method.HEAD, Request.Method.PROPFIND}) {
            try (MockedStatic<HttpManager> mocked = mockRequestMethod(method)) {
                Assert.assertNull(method.name(), ResourceFactoryImpl.getResource("localhost",
                        fileUrl("live", folder, unpublished), helper, APILocator.getHostAPI()));
            }
        }
    }

    /**
     * Method to test: {@link ResourceFactoryImpl#getResource(String, String, DotWebdavHelper, HostAPI)}
     * <p>
     * Given Scenario: A file was saved but never published, and write methods (PUT, DELETE, MOVE,
     * COPY, LOCK) address it under the live tree.
     * <p>
     * ExpectedResult: The saved file is found for each of them, so writes under the live tree keep
     * working on saved files.
     */
    @Test
    public void Test_write_methods_in_live_tree_find_unpublished_file() throws Exception {
        final Folder folder = new FolderDataGen().site(newSite()).nextPersisted();
        final Contentlet unpublished = newFileAsset(folder, "unpublished-write.txt");

        for (final Request.Method method : new Request.Method[] {Request.Method.PUT, Request.Method.DELETE,
                Request.Method.MOVE, Request.Method.COPY, Request.Method.LOCK}) {
            try (MockedStatic<HttpManager> mocked = mockRequestMethod(method)) {
                final Resource resource = ResourceFactoryImpl.getResource("localhost",
                        fileUrl("live", folder, unpublished), helper, APILocator.getHostAPI());
                Assert.assertNotNull(method.name(), resource);
                Assert.assertEquals(method.name(), unpublished.getInode(),
                        ((FileResourceImpl) resource).getFile().getInode());
            }
        }
    }

    /**
     * Method to test: {@link ResourceFactoryImpl#getResource(String, String, DotWebdavHelper, HostAPI)}
     * <p>
     * Given Scenario: A file was saved but never published, and is looked up under the live tree
     * outside a WebDAV request.
     * <p>
     * ExpectedResult: The saved file is found, as before.
     */
    @Test
    public void Test_lookup_outside_a_request_finds_unpublished_file() throws Exception {
        final Folder folder = new FolderDataGen().site(newSite()).nextPersisted();
        final Contentlet unpublished = newFileAsset(folder, "unpublished-internal.txt");

        try (MockedStatic<HttpManager> mocked = Mockito.mockStatic(HttpManager.class)) {
            mocked.when(HttpManager::request).thenReturn(null);
            Assert.assertNotNull(ResourceFactoryImpl.getResource("localhost",
                    fileUrl("live", folder, unpublished), helper, APILocator.getHostAPI()));
        }
    }

    /**
     * Method to test: {@link FolderResourceImpl#child(String)}
     * <p>
     * Given Scenario: A folder in the live tree holds a file that was saved but never published, and
     * a write (COPY onto that name) asks the folder for the child.
     * <p>
     * ExpectedResult: The child is found, so the client's overwrite rules apply to the saved file.
     */
    @Test
    public void Test_child_lookup_for_write_in_live_tree_finds_unpublished_file() throws Exception {
        final Folder folder = new FolderDataGen().site(newSite()).nextPersisted();
        final Contentlet unpublished = newFileAsset(folder, "unpublished-child.txt");
        final Host site = APILocator.getHostAPI().find(folder.getHostId(), APILocator.systemUser(), false);
        final FolderResourceImpl folderResource = new FolderResourceImpl(folder, String.format(
                "/webdav/live/%d/%s%s", defaultLanguageId(), site.getHostname(), folder.getPath()));

        try (MockedStatic<HttpManager> mocked = mockRequestMethod(Request.Method.COPY)) {
            final Resource child = folderResource.child(fileName(unpublished));
            Assert.assertNotNull(child);
            Assert.assertTrue(((FileResourceImpl) child).getPath(),
                    ((FileResourceImpl) child).getPath().startsWith("/webdav/live/"));
        }
    }

    /**
     * Method to test: {@link DotWebdavHelper#copyFolderToTemp(Folder, java.io.File, User, String, boolean, long)}
     * <p>
     * Given Scenario: A folder in the live tree holds a file that was saved but never published, and
     * a write (COPY) copies the folder to temporary storage.
     * <p>
     * ExpectedResult: The saved file is copied too.
     */
    @Test
    public void Test_folder_copy_in_live_tree_keeps_unpublished_file() throws Exception {
        final Folder folder = new FolderDataGen().site(newSite()).nextPersisted();
        final Contentlet unpublished = newFileAsset(folder, "unpublished-copy.txt");
        final String copyName = "folder-copy-" + System.nanoTime();
        final java.io.File copy = new java.io.File(helper.getTempDir(), copyName);

        try (MockedStatic<HttpManager> mocked = mockRequestMethod(Request.Method.COPY)) {
            helper.copyFolderToTemp(folder, helper.getTempDir(), APILocator.systemUser(), copyName,
                    true, defaultLanguageId());
            Assert.assertTrue(new java.io.File(copy, fileName(unpublished)).exists());
        } finally {
            FileUtil.deltree(copy, true);
        }
    }

    /** Creates a site that is removed after the class runs. */
    private static Host newSite() {
        final Host site = new SiteDataGen().nextPersisted();
        createdSites.add(site);
        return site;
    }

    /** Saves (without publishing) a small text file asset in the given folder. */
    private static Contentlet newFileAsset(final Folder folder, final String name) throws Exception {
        return new FileAssetDataGen(folder, tempFile(name)).nextPersisted();
    }

    /** Saves (without publishing) a small text file asset at the root of the given site. */
    private static Contentlet newFileAsset(final Host site, final String name) throws Exception {
        return new FileAssetDataGen(site, tempFile(name)).nextPersisted();
    }

    /** Creates a temporary file whose name is unique per call and ends with the given name. */
    private static java.io.File tempFile(final String name) throws IOException {
        final java.io.File dir = java.nio.file.Files.createTempDirectory("webdav-tree").toFile();
        dir.deleteOnExit();
        final java.io.File file = new java.io.File(dir, System.nanoTime() + "-" + name);
        FileUtil.write(file, "content of " + name);
        file.deleteOnExit();
        return file;
    }

    /** The file name WebDAV shows for a file asset. */
    private static String fileName(final Contentlet fileAsset) {
        return APILocator.getFileAssetAPI().fromContentlet(fileAsset).getFileName();
    }

    /** The site-root resource of the given tree ({@code live} or {@code working}). */
    private static HostResourceImpl siteRoot(final Host site, final String tree) {
        final HostResourceImpl root = new HostResourceImpl(String.format("/webdav/%s/%d/%s/",
                tree, defaultLanguageId(), site.getHostname()));
        root.setHost(site);
        return root;
    }

    /** The WebDAV URL of a file in a folder, in the given tree ({@code live} or {@code working}). */
    private static String fileUrl(final String tree, final Folder folder, final Contentlet file)
            throws DotDataException, DotSecurityException {
        final Host site = APILocator.getHostAPI().find(folder.getHostId(), APILocator.systemUser(), false);
        return String.format("/webdav/%s/%d/%s%s%s", tree, defaultLanguageId(), site.getHostname(),
                folder.getPath(), fileName(file));
    }

    /**
     * Makes the current WebDAV request use the given method, sent by the system user. Close the
     * returned mock when done.
     */
    private static MockedStatic<HttpManager> mockRequestMethod(final Request.Method method) {
        final MockedStatic<HttpManager> mocked = Mockito.mockStatic(HttpManager.class);
        final Request request = Mockito.mock(Request.class);
        final Auth auth = Mockito.mock(Auth.class);
        when(auth.getTag()).thenReturn(APILocator.systemUser());
        when(request.getAuthorization()).thenReturn(auth);
        when(request.getMethod()).thenReturn(method);
        mocked.when(HttpManager::request).thenReturn(request);
        return mocked;
    }

    private static long defaultLanguageId() {
        return APILocator.getLanguageAPI().getDefaultLanguage().getId();
    }

    private static List<String> resourceNames(final List<? extends Resource> resources) {
        return resources.stream().map(Resource::getName).collect(Collectors.toList());
    }

}

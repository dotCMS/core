package com.dotcms.rest.api.v1.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerAsFileDataGen;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.test.util.ApiTokenTestUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.PaginationUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Covers which user the {@link ContainerResource} endpoints read and write with: the lookup of a
 * container by folder path when it is only found on the default site, and the form endpoints that
 * find or create the widget a form is placed on a page with.
 */
public class ContainerResourceCallerPermissionTest {

    @ClassRule
    public static final ApiTokenTestUtils TOKEN_UTILS = new ApiTokenTestUtils();

    private static final String PASSWORD = "admin";
    /** The key prefix ContainerResource locks a form's widget creation on. */
    private static final String FORM_WIDGET_LOCK_PREFIX = "form-widget-";

    private static ContainerResource resource;
    private static HttpServletResponse response;
    private static User adminUser;

    private static final List<User> users = new ArrayList<>();
    private static final List<ContentType> contentTypes = new ArrayList<>();
    private static final List<Container> containers = new ArrayList<>();
    private static final List<Host> sites = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new ContainerResource();
        adminUser = TestUserUtils.getAdminUser();
        response = new MockHttpResponse();
        if (null == formWidgetType()) {
            APILocator.getFormAPI().createFormWidgetInstanceStructure();
        }
    }

    @After
    public void clearThreadRequest() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    @AfterClass
    public static void cleanUp() {
        contentTypes.forEach(type -> Try.run(() -> {
            widgetsFor(type.id()).forEach(widget -> Try.run(() -> {
                APILocator.getContentletAPI().unpublish(widget, APILocator.systemUser(), false);
                APILocator.getContentletAPI().archive(widget, APILocator.systemUser(), false);
                APILocator.getContentletAPI().delete(widget, APILocator.systemUser(), false);
            }));
            APILocator.getContentTypeAPI(APILocator.systemUser()).delete(type);
        }).onFailure(e -> Logger.warn(ContainerResourceCallerPermissionTest.class,
                "Could not remove content type " + type.variable(), e)));
        containers.forEach(container -> Try.run(() -> ContainerDataGen.remove(container))
                .onFailure(e -> Logger.warn(ContainerResourceCallerPermissionTest.class,
                        "Could not remove container " + container.getIdentifier(), e)));
        users.forEach(user -> Try.run(() -> UserDataGen.remove(user))
                .onFailure(e -> Logger.warn(ContainerResourceCallerPermissionTest.class,
                        "Could not remove user " + user.getUserId(), e)));
        sites.forEach(site -> Try.run(() -> {
            APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
            APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
        }).onFailure(e -> Logger.warn(ContainerResourceCallerPermissionTest.class,
                "Could not remove site " + site.getIdentifier(), e)));
    }

    /**
     * Method to test: {@link ContainerResource#getWorkingById}
     * Given: a container stored as files only on the default site, a request made from another
     *        site, and a back-end user who can read both sites but not the container
     * When: the container is asked for by its relative folder path
     * Should: refuse it, because the default site's copy is read with the caller's permissions
     */
    @Test
    public void test_getWorkingById_containerOnlyOnTheDefaultSite_unreadableByTheCaller_refused()
            throws Exception {
        final String folderName = "caller-permission-" + System.nanoTime();
        final FileAssetContainer container = defaultSiteContainer(folderName);
        final Host otherSite = freshSite();
        final User user = user(TestUserUtils.getBackendRole());
        grantRead(user, defaultSite());
        grantRead(user, otherSite);
        assertFalse("the fixture should keep the container unreadable for the user",
                APILocator.getPermissionAPI().doesUserHavePermission(
                        container, PermissionAPI.PERMISSION_READ, user, false));

        assertThrows(DotSecurityException.class, () -> resource.getWorkingById(
                requestFrom(user, otherSite), response, relativePath(folderName), false));
    }

    /**
     * Method to test: {@link ContainerResource#getWorkingById}
     * Given: the same container stored only on the default site, asked for from another site
     * When: an administrator asks for it by its relative folder path
     * Should: return it from the default site, as before
     */
    @Test
    public void test_getWorkingById_containerOnlyOnTheDefaultSite_readableByTheCaller_returned()
            throws Exception {
        final String folderName = "caller-permission-" + System.nanoTime();
        final FileAssetContainer container = defaultSiteContainer(folderName);
        final Host otherSite = freshSite();

        final Response result = resource.getWorkingById(
                requestFrom(adminUser, otherSite), response, relativePath(folderName), false);

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertEquals(container.getIdentifier(),
                ((ContainerView) ((ResponseEntityView<?>) result.getEntity()).getEntity())
                        .getContainer().getIdentifier());
    }

    /**
     * Method to test: {@link ContainerResource#containerFormByQueryParam}
     * Given: a front-end user
     * When: the user asks for a form's widget
     * Should: refuse the request, since only the page editor places forms
     */
    @Test
    public void test_getForm_frontEndUser_refused() throws Exception {
        final ContentType form = formType();
        final User user = user(APILocator.getRoleAPI().loadFrontEndUserRole());
        grantRead(user, form);

        assertThrows(SecurityException.class, () -> resource.containerFormByQueryParam(
                request(user), response, readableContainer(user).getIdentifier(), form.id()));
    }

    /**
     * Method to test: {@link ContainerResource#containerFormByQueryParam}
     * Given: a content type that is not a form
     * When: an administrator asks for its widget
     * Should: answer 400 and create nothing
     */
    @Test
    public void test_getForm_typeThatIsNotAForm_badRequest() throws Exception {
        final ContentType notAForm = track(new ContentTypeDataGen().nextPersisted());

        assertThrows(BadRequestException.class, () -> resource.containerFormByQueryParam(
                request(adminUser), response, readableContainer(adminUser).getIdentifier(),
                notAForm.id()));
        assertTrue("no widget should exist for a type that is not a form",
                widgetsFor(notAForm.id()).isEmpty());
    }

    /**
     * Method to test: {@link ContainerResource#containerFormByQueryParam}
     * Given: a form the back-end user cannot read
     * When: the user asks for its widget
     * Should: refuse it
     */
    @Test
    public void test_getForm_formTheUserCannotRead_refused() throws Exception {
        final ContentType form = formType();
        final User user = user(TestUserUtils.getBackendRole());
        assertFalse("the fixture should keep the form unreadable for the user",
                APILocator.getPermissionAPI().doesUserHavePermission(
                        form, PermissionAPI.PERMISSION_READ, user, false));

        assertThrows(DotSecurityException.class, () -> resource.containerFormByQueryParam(
                request(user), response, readableContainer(user).getIdentifier(), form.id()));
    }

    /**
     * Method to test: {@link ContainerResource#containerFormByQueryParam}
     * Given: a form with no widget yet, readable by an administrator
     * When: the administrator asks for its widget with a GET
     * Should: answer that there is none, and create nothing
     */
    @Test
    public void test_getForm_withoutAWidget_notFoundAndNothingCreated() throws Exception {
        final ContentType form = formType();

        assertThrows(DoesNotExistException.class, () -> resource.containerFormByQueryParam(
                request(adminUser), response, readableContainer(adminUser).getIdentifier(),
                form.id()));
        assertTrue("a GET should not create a widget", widgetsFor(form.id()).isEmpty());
    }

    /**
     * Method to test: {@link ContainerResource#createContainerFormByQueryParam}
     * Given: a form with no widget yet, and a back-end user who can read the form but cannot
     *        create widget content
     * When: the user asks for the widget to be created
     * Should: refuse it and create nothing
     */
    @Test
    public void test_createForm_userWhoCannotCreateWidgets_refusedAndNothingCreated()
            throws Exception {
        final ContentType form = formType();
        final User user = user(TestUserUtils.getBackendRole());
        grantRead(user, form);

        assertThrows(DotSecurityException.class, () -> resource.createContainerFormByQueryParam(
                request(user), response, readableContainer(user).getIdentifier(), form.id()));
        assertTrue("no widget should be created", widgetsFor(form.id()).isEmpty());
    }

    /**
     * Method to test: {@link ContainerResource#createContainerFormByQueryParam}
     * Given: a form with no widget yet, and a back-end user who can create widget content but not
     *        publish it
     * When: the user asks for the widget to be created
     * Should: refuse it and leave no live widget
     */
    @Test
    public void test_createForm_userWhoCannotPublishWidgets_refusedAndNothingLive()
            throws Exception {
        final ContentType form = formType();
        final User user = user(TestUserUtils.getBackendRole());
        grantRead(user, form);
        grant(user, formWidgetType(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);

        assertThrows(DotSecurityException.class, () -> resource.createContainerFormByQueryParam(
                request(user), response, readableContainer(user).getIdentifier(), form.id()));
        assertTrue("no widget should be live",
                widgetsFor(form.id()).stream().noneMatch(widget -> Try.of(widget::isLive)
                        .getOrElse(false)));
    }

    /**
     * Method to test: {@link FormAPI#getFormContent(String, User)}
     * Given: a form with a widget
     * When: the widget is asked for by the form's variable, and by a value that is no content type
     * Should: find the form's widget by its variable, and find nothing for the other value
     */
    @Test
    public void test_getFormContent_byVariableAndByAValueThatIsNoContentType() throws Exception {
        final ContentType form = formType();
        final String created = widgetIdentifier(resource.createContainerFormByQueryParam(
                request(adminUser), response, readableContainer(adminUser).getIdentifier(),
                form.id()));

        final Contentlet byVariable = APILocator.getFormAPI()
                .getFormContent(form.variable(), adminUser);
        assertNotNull("the widget should be found by the form's variable", byVariable);
        assertEquals(created, byVariable.getIdentifier());

        assertEquals("a value that is no content type should find no widget", null,
                APILocator.getFormAPI().getFormContent("*", adminUser));
    }

    /**
     * Method to test: {@link ContainerResource#createContainerFormByQueryParam}
     * Given: a form with no widget yet
     * When: several requests ask for its widget to be created at the same time
     * Should: create a single widget and return it to all of them
     * <p>
     * The requests carry an API token rather than a password: login lockout refuses password logins
     * from one address beyond {@code auth.max.failures.limit} in flight at once, which is not what
     * this test is about.
     */
    @Test
    public void test_createForm_concurrentRequests_createOneWidget() throws Exception {
        final ContentType form = formType();
        final String containerId = readableContainer(adminUser).getIdentifier();
        final int requests = 6;
        final CountDownLatch start = new CountDownLatch(1);
        final ExecutorService pool = Executors.newFixedThreadPool(requests);
        final ApiToken apiToken = APILocator.getApiTokenAPI().persistApiToken(adminUser.getUserId(),
                Date.from(Instant.now().plus(Duration.ofHours(1))), adminUser.getUserId(), "127.0.0.1");
        final String bearer = "Bearer " + APILocator.getApiTokenAPI().getJWT(apiToken, adminUser);
        try {
            final List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return widgetIdentifier(resource.createContainerFormByQueryParam(
                            request(bearer), response, containerId, form.id()));
                }));
            }
            start.countDown();

            final Set<String> returned = new HashSet<>();
            for (final Future<String> result : results) {
                returned.add(result.get(60, TimeUnit.SECONDS));
            }
            assertEquals("every request should get the same widget", 1, returned.size());
            assertEquals("a single widget should be created", 1, widgetsFor(form.id()).size());
        } finally {
            pool.shutdownNow();
            APILocator.getApiTokenAPI().deleteToken(apiToken, APILocator.systemUser());
        }
    }

    /**
     * Method to test: {@link FormAPI#createDefaultFormContent(String, User)}
     * Given: a form with no widget yet
     * When: its widget is created by the form's variable
     * Should: store the widget under the form's id, so that looking it up by id finds it
     */
    @Test
    public void test_createDefaultFormContent_byVariable_isFoundById() throws Exception {
        final ContentType form = formType();

        final Contentlet created = APILocator.getFormAPI()
                .createDefaultFormContent(form.variable(), adminUser);

        assertEquals(form.id(),
                created.getStringProperty(FormAPI.FORM_WIDGET_FORM_ID_FIELD_VELOCITY_VAR_NAME));
        final Contentlet found = APILocator.getFormAPI().getFormContent(form.id(), adminUser);
        assertNotNull("the widget should be found by the form's id", found);
        assertEquals(created.getIdentifier(), found.getIdentifier());
    }

    /**
     * Method to test: {@link ContainerResource#createContainerFormByQueryParam}
     * Given: a form with no widget yet, and a container id that names content that is not a
     *        container
     * When: an administrator asks for the widget to be created in it
     * Should: answer that the container does not exist, and create nothing
     */
    @Test
    public void test_createForm_idThatIsNoContainer_notFoundAndNothingCreated() throws Exception {
        final ContentType form = formType();
        final Contentlet notAContainer = new ContentletDataGen(
                track(new ContentTypeDataGen().nextPersisted()).id()).nextPersisted();

        assertThrows(DoesNotExistException.class, () -> resource.createContainerFormByQueryParam(
                request(adminUser), response, notAContainer.getIdentifier(), form.id()));
        assertTrue("no widget should be created", widgetsFor(form.id()).isEmpty());
    }

    /**
     * Method to test: {@link FormAPI#getFormContent(String, User)}
     * Given: a widget stored under the form's variable, as the earlier way of creating widgets did
     *        when it was given the variable
     * When: the widget is asked for by the form's id
     * Should: find it
     */
    @Test
    public void test_getFormContent_widgetStoredUnderTheVariable_isFoundById() throws Exception {
        final ContentType form = formType();
        final Contentlet stored = APILocator.getFormAPI().createDefaultFormContent(form.variable());
        assertEquals(form.variable(),
                stored.getStringProperty(FormAPI.FORM_WIDGET_FORM_ID_FIELD_VELOCITY_VAR_NAME));

        final Contentlet found = APILocator.getFormAPI().getFormContent(form.id(), adminUser);

        assertNotNull("the widget stored under the variable should be found", found);
        assertEquals(stored.getIdentifier(), found.getIdentifier());
    }

    /**
     * Method to test: {@link ContainerResource#createContainerFormByQueryParam}
     * Given: a form whose own permissions name a role that is locked against permission changes
     * When: an administrator asks for its widget to be created
     * Should: create it
     */
    @Test
    public void test_createForm_formNamingALockedRole_createsTheWidget() throws Exception {
        final ContentType form = formType();
        final Role locked = APILocator.getRoleAPI().loadCMSAdminRole();
        assertFalse("the fixture needs a role locked against permission changes",
                locked.isEditPermissions());
        new DotConnect().setSQL("insert into permission (id, permission_type, inode_id, roleid, permission)"
                        + " values (nextval('permission_seq'), ?, ?, ?, ?)")
                .addParam(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE)
                .addParam(form.getPermissionId())
                .addParam(locked.getId())
                .addParam(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT
                        | PermissionAPI.PERMISSION_PUBLISH)
                .loadResult();
        CacheLocator.getPermissionCache().clearCache();

        final String created = widgetIdentifier(resource.createContainerFormByQueryParam(
                request(adminUser), response, readableContainer(adminUser).getIdentifier(),
                form.id()));

        assertEquals(1, widgetsFor(form.id()).size());
        assertEquals(created, widgetsFor(form.id()).get(0).getIdentifier());
    }

    /**
     * Method to test: {@link ContainerResource#createContainerFormByQueryParam}
     * Given: a role the form names without read, edit or publish, that may read widgets in general
     * When: the form's widget is created
     * Should: leave that role unable to read the new widget, as it is unable to read the form
     */
    @Test
    public void test_createForm_roleTheFormNamesWithoutReadEditOrPublish_getsNothing()
            throws Exception {
        final ContentType form = formType();
        final User named = user(TestUserUtils.getBackendRole());
        final Role namedRole = APILocator.getRoleAPI().loadRoleByKey(named.getUserId());
        grant(named, formWidgetType(), PermissionAPI.PERMISSION_READ);
        grant(named, form, PermissionAPI.PERMISSION_EDIT_PERMISSIONS);

        final String created = widgetIdentifier(resource.createContainerFormByQueryParam(
                request(adminUser), response, readableContainer(adminUser).getIdentifier(),
                form.id()));

        final Contentlet widget = APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage(created);
        assertFalse("a role that cannot read the form should not read its widget",
                APILocator.getPermissionAPI().doesRoleHavePermission(widget,
                        PermissionAPI.PERMISSION_READ, namedRole));
    }

    /**
     * Method to test: {@link ContainerResource#createContainerFormByQueryParam}
     * Given: another request for the same form that holds the form's lock without creating its
     *        widget, for longer than a request waits for it twice
     * When: the widget is asked to be created
     * Should: answer that there is a conflict, so the editor can try again, and create nothing
     */
    @Test
    public void test_createForm_whileAnotherRequestHoldsTheFormWithoutAWidget_conflict()
            throws Exception {
        final ContentType form = formType();
        final String containerId = readableContainer(adminUser).getIdentifier();
        final CountDownLatch holding = new CountDownLatch(1);
        final ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            other.submit(() -> {
                try {
                    return DotConcurrentFactory.getInstance().getIdentifierStripedLock().tryLock(
                            FORM_WIDGET_LOCK_PREFIX + form.id(), () -> {
                                holding.countDown();
                                // Longer than a request waits for the lock twice.
                                Thread.sleep(TimeUnit.SECONDS.toMillis(8));
                                return null;
                            });
                } catch (final Throwable e) {
                    throw new Exception(e);
                }
            });
            assertTrue("the other request should hold the form",
                    holding.await(60, TimeUnit.SECONDS));

            assertThrows(ConflictException.class, () -> resource.createContainerFormByQueryParam(
                    request(adminUser), response, containerId, form.id()));
            assertTrue("no widget should be created", widgetsFor(form.id()).isEmpty());
        } finally {
            other.shutdownNow();
        }
    }

    /**
     * Method to test: {@link ContainerResource#createContainerFormByQueryParam}
     * Given: another request for the same form that created its widget and is still holding the
     *        form's lock for longer than a request waits for it
     * When: the widget is asked to be created
     * Should: return the widget the other request created instead of failing
     */
    @Test
    public void test_createForm_whileAnotherRequestHoldsTheFormPastTheWait_returnsItsWidget()
            throws Exception {
        final ContentType form = formType();
        final String containerId = readableContainer(adminUser).getIdentifier();
        final CountDownLatch created = new CountDownLatch(1);
        final ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            final Future<String> holder = other.submit(() -> {
                try {
                    return DotConcurrentFactory.getInstance().getIdentifierStripedLock().tryLock(
                            FORM_WIDGET_LOCK_PREFIX + form.id(), () -> {
                                final Contentlet widget = APILocator.getFormAPI()
                                        .createDefaultFormContent(form.id(), adminUser);
                                created.countDown();
                                // Longer than a request waits for the lock.
                                Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                                return widget.getIdentifier();
                            });
                } catch (final Throwable e) {
                    throw new Exception(e);
                }
            });
            assertTrue("the other request should create the widget",
                    created.await(60, TimeUnit.SECONDS));

            final String returned = widgetIdentifier(resource.createContainerFormByQueryParam(
                    request(adminUser), response, containerId, form.id()));

            assertEquals("the widget the other request created should be returned",
                    holder.get(60, TimeUnit.SECONDS), returned);
            assertEquals("no second widget should be created", 1, widgetsFor(form.id()).size());
        } finally {
            other.shutdownNow();
        }
    }

    /**
     * Method to test: {@link ContainerResource#createContainerFormByQueryParam}
     * Given: a form with no widget yet, and a back-end user who can read the form and create and
     *        publish widget content, but cannot read the container the form is placed in
     * When: the user asks for the widget to be created in that container
     * Should: refuse it and create nothing, since the form cannot be placed there
     */
    @Test
    public void test_createForm_containerTheUserCannotRead_refusedAndNothingCreated()
            throws Exception {
        final ContentType form = formType();
        final User user = user(TestUserUtils.getBackendRole());
        grantRead(user, form);
        grant(user, formWidgetType(), PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);
        final Container container = new ContainerDataGen().nextPersisted();
        containers.add(container);
        assertFalse("the fixture should keep the container unreadable for the user",
                APILocator.getPermissionAPI().doesUserHavePermission(
                        container, PermissionAPI.PERMISSION_READ, user, false));

        assertThrows(DotSecurityException.class, () -> resource.createContainerFormByQueryParam(
                request(user), response, container.getIdentifier(), form.id()));
        assertTrue("no widget should be created", widgetsFor(form.id()).isEmpty());
    }

    /**
     * Method to test: {@link ContainerResource#createContainerFormByQueryParam} and
     * {@link ContainerResource#containerFormByQueryParam}
     * Given: a form with no widget yet, and a back-end user who can read the form and create and
     *        publish widget content
     * When: the user asks for the widget to be created, then asks for it again both ways
     * Should: create one live widget saved as that user, readable by visitors but not editable or
     *         publishable by them, and return that same widget afterwards
     */
    @Test
    public void test_createForm_userWhoCanCreateAndPublishWidgets_createsOneWidgetAsThatUser()
            throws Exception {
        final ContentType form = formType();
        final User user = user(TestUserUtils.getBackendRole());
        grantRead(user, form);
        grant(user, formWidgetType(), PermissionAPI.PERMISSION_READ
                | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);
        // The forms portlet sets these permissions on every form it creates.
        final Role anonymous = APILocator.getRoleAPI().loadCMSAnonymousRole();
        APILocator.getPermissionAPI().save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                form.getPermissionId(), anonymous.getId(), PermissionAPI.PERMISSION_READ
                        | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_PUBLISH, true),
                form, APILocator.systemUser(), false);
        // A role that may only read the form should only be able to read its widget.
        final User viewer = user(TestUserUtils.getBackendRole());
        grantRead(viewer, form);
        final Role viewerRole = APILocator.getRoleAPI().loadRoleByKey(viewer.getUserId());
        // What a role may do with the form's submissions is not what it may do with the form.
        APILocator.getPermissionAPI().save(new Permission(Contentlet.class.getCanonicalName(),
                form.getPermissionId(), viewerRole.getId(), PermissionAPI.PERMISSION_READ
                        | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_PUBLISH, true),
                form, APILocator.systemUser(), false);
        final String containerId = readableContainer(user).getIdentifier();

        final String created = widgetIdentifier(
                resource.createContainerFormByQueryParam(request(user), response, containerId,
                        form.id()));

        final List<Contentlet> widgets = widgetsFor(form.id());
        assertEquals("one widget should be created", 1, widgets.size());
        final Contentlet widget = widgets.get(0);
        assertEquals(created, widget.getIdentifier());
        assertTrue("the widget should be live", widget.isLive());
        assertEquals("the widget should be saved as the user", user.getUserId(),
                widget.getModUser());
        assertEquals("the widget should be owned by the user", user.getUserId(),
                widget.getOwner());

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        assertTrue("visitors should read the widget",
                permissionAPI.doesRoleHavePermission(widget, PermissionAPI.PERMISSION_READ,
                        anonymous));
        assertFalse("visitors should not edit the widget",
                permissionAPI.doesRoleHavePermission(widget, PermissionAPI.PERMISSION_EDIT,
                        anonymous));
        assertFalse("visitors should not publish the widget",
                permissionAPI.doesRoleHavePermission(widget, PermissionAPI.PERMISSION_PUBLISH,
                        anonymous));
        assertTrue("a role that reads the form should read the widget",
                permissionAPI.doesRoleHavePermission(widget, PermissionAPI.PERMISSION_READ,
                        viewerRole));
        assertFalse("a role that only reads the form should not edit the widget",
                permissionAPI.doesRoleHavePermission(widget, PermissionAPI.PERMISSION_EDIT,
                        viewerRole));
        assertFalse("a role that only reads the form should not publish the widget",
                permissionAPI.doesRoleHavePermission(widget, PermissionAPI.PERMISSION_PUBLISH,
                        viewerRole));

        assertEquals("a GET should return the same widget", created, widgetIdentifier(
                resource.containerFormByQueryParam(request(user), response, containerId,
                        form.id())));
        assertEquals("a second create should return the same widget", created, widgetIdentifier(
                resource.createContainerFormByQueryParam(request(user), response, containerId,
                        form.id())));
        assertEquals("no second widget should be created", 1, widgetsFor(form.id()).size());
    }

    /**
     * Method to test: {@link ContainerResource#getContainers}
     * Given: a content type used by two containers, one of them archived, and a back-end user
     *        who can read the content type and both containers
     * When: the user lists the containers that use the content type, archived ones included
     * Should: get both containers, and a total that counts both
     */
    @Test
    public void test_getContainers_byContentType_userReadsAllContainers_getsAll() throws Exception {
        final ContainerUsage usage = new ContainerUsage();
        try {
            usage.create();
            final User user = user(TestUserUtils.getBackendRole());
            grantRead(user, usage.contentType);
            grantRead(user, usage.active);
            grantRead(user, usage.archived);

            final Response result = usage.list(user);

            assertEquals(Set.of(usage.active.getIdentifier(), usage.archived.getIdentifier()),
                    identifiersIn(result));
            assertEquals("2", result.getHeaderString(PaginationUtil.PAGINATION_TOTAL_ENTRIES_HEADER_NAME));
        } finally {
            usage.cleanUp();
        }
    }

    /**
     * Method to test: {@link ContainerResource#getContainers}
     * Given: a content type used by two containers, one of them archived, and a back-end user
     *        who can read the content type but neither container
     * When: the user lists the containers that use the content type, archived ones included
     * Should: get an empty list and a total of zero, so no container is disclosed
     */
    @Test
    public void test_getContainers_byContentType_userReadsNoContainer_getsNone() throws Exception {
        final ContainerUsage usage = new ContainerUsage();
        try {
            usage.create();
            final User user = user(TestUserUtils.getBackendRole());
            grantRead(user, usage.contentType);

            final Response result = usage.list(user);

            assertEquals(Set.of(), identifiersIn(result));
            assertEquals("0", result.getHeaderString(PaginationUtil.PAGINATION_TOTAL_ENTRIES_HEADER_NAME));
        } finally {
            usage.cleanUp();
        }
    }

    /**
     * Method to test: {@link ContainerResource#getContainers}
     * Given: a content type used by an active and an archived container, and a back-end user who
     *        can read the content type and only one of the containers
     * When: the user lists the containers that use the content type, archived ones included, once
     *       with each container as the readable one
     * Should: get only the readable container, and a total that counts only that one, whether it
     *         is the active or the archived one
     */
    @Test
    public void test_getContainers_byContentType_userReadsSomeContainers_getsOnlyThose()
            throws Exception {
        final ContainerUsage usage = new ContainerUsage();
        try {
            usage.create();
            final User readsActive = user(TestUserUtils.getBackendRole());
            grantRead(readsActive, usage.contentType);
            grantRead(readsActive, usage.active);
            final User readsArchived = user(TestUserUtils.getBackendRole());
            grantRead(readsArchived, usage.contentType);
            grantRead(readsArchived, usage.archived);

            final Response activeOnly = usage.list(readsActive);
            final Response archivedOnly = usage.list(readsArchived);

            assertEquals(Set.of(usage.active.getIdentifier()), identifiersIn(activeOnly));
            assertEquals("1", activeOnly.getHeaderString(PaginationUtil.PAGINATION_TOTAL_ENTRIES_HEADER_NAME));
            assertEquals(Set.of(usage.archived.getIdentifier()), identifiersIn(archivedOnly));
            assertEquals("1", archivedOnly.getHeaderString(PaginationUtil.PAGINATION_TOTAL_ENTRIES_HEADER_NAME));
        } finally {
            usage.cleanUp();
        }
    }

    /**
     * Reads the identifiers of the containers a listing returned.
     *
     * @param result the response of {@link ContainerResource#getContainers}
     * @return the containers' identifiers
     */
    private static Set<String> identifiersIn(final Response result) {
        return ((List<?>) ((ResponseEntityView<?>) result.getEntity()).getEntity()).stream()
                .map(view -> ((ContainerView) view).getContainer().getIdentifier())
                .collect(Collectors.toSet());
    }

    /**
     * A content type used by an active and an archived container, owned by no one in particular.
     * It removes itself, containers first, so a failed test leaves nothing behind that the class
     * cleanup would remove in the wrong order.
     */
    private static class ContainerUsage {

        private ContentType contentType;
        private Container active;
        private Container archived;

        void create() throws Exception {
            contentType = new ContentTypeDataGen().host(defaultSite()).nextPersisted();
            active = newContainer();
            archived = newContainer();
            APILocator.getContainerAPI().archive(archived, APILocator.systemUser(), false);
        }

        private Container newContainer() {
            return new ContainerDataGen().withContentType(contentType, "$!{body}").nextPersisted();
        }

        /**
         * Lists the containers that use the content type, archived ones included, as the user.
         *
         * @param user the user who lists them
         * @return the response
         */
        Response list(final User user) {
            return resource.getContainers(request(user), response, null, 1, 40, "title", "ASC",
                    null, false, true, contentType.id());
        }

        void cleanUp() {
            for (final Container container : new Container[] {archived, active}) {
                if (null != container) {
                    Try.run(() -> ContainerDataGen.remove(container))
                            .onFailure(e -> Logger.warn(ContainerResourceCallerPermissionTest.class,
                                    "Could not remove container " + container.getIdentifier(), e));
                }
            }
            if (null != contentType) {
                Try.run(() -> ContentTypeDataGen.remove(contentType))
                        .onFailure(e -> Logger.warn(ContainerResourceCallerPermissionTest.class,
                                "Could not remove content type " + contentType.variable(), e));
            }
        }
    }

    /**
     * Creates a container stored as files on the default site.
     *
     * @param folderName the container's folder name, used by no other site
     * @return the container
     * @throws Exception if the container cannot be created
     */
    private static FileAssetContainer defaultSiteContainer(final String folderName)
            throws Exception {
        return new ContainerAsFileDataGen()
                .host(defaultSite())
                .folderName(folderName)
                .nextPersisted();
    }

    private static String relativePath(final String folderName) {
        return Constants.CONTAINER_FOLDER_PATH + "/" + folderName;
    }

    /**
     * Creates a container the given user can read.
     *
     * @param user the user who reads it
     * @return the container
     * @throws Exception if the container or the permission cannot be saved
     */
    private static Container readableContainer(final User user) throws Exception {
        final Container container = new ContainerDataGen().nextPersisted();
        containers.add(container);
        grantRead(user, container);
        return container;
    }

    /**
     * Creates a form content type, removed after the class runs together with its widgets.
     *
     * @return the form type
     */
    private static ContentType formType() {
        return track(TestDataUtils.getFormLikeContentType("CallerForm" + System.nanoTime(), null));
    }

    private static ContentType track(final ContentType type) {
        contentTypes.add(type);
        return type;
    }

    /**
     * Loads the content type form widgets are saved as.
     *
     * @return the type, or {@code null} when it does not exist yet
     */
    private static ContentType formWidgetType() {
        return Try.of(() -> APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME)).getOrNull();
    }

    /**
     * Finds every widget saved for a form, in any state, read as the system user.
     *
     * @param formId the form's content type id
     * @return the widgets
     * @throws Exception if the index cannot be searched
     */
    private static List<Contentlet> widgetsFor(final String formId) throws Exception {
        return APILocator.getContentletAPI().search("+contentType:"
                        + FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME + " +"
                        + FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME + "."
                        + FormAPI.FORM_WIDGET_FORM_ID_FIELD_VELOCITY_VAR_NAME + ":" + formId
                        + " +working:true", 10, 0, null, APILocator.systemUser(), false);
    }

    @SuppressWarnings("unchecked")
    private static String widgetIdentifier(final Response result) {
        final Map<String, Object> entity =
                (Map<String, Object>) ((ResponseEntityView<?>) result.getEntity()).getEntity();
        final Map<String, Object> content = (Map<String, Object>) entity.get("content");
        assertNotNull("the response should carry the widget", content);
        return (String) content.get("identifier");
    }

    private static Host defaultSite() throws Exception {
        return APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
    }

    private static Host freshSite() {
        final Host site = new SiteDataGen().nextPersisted();
        sites.add(site);
        return site;
    }

    private static User user(final Role role) throws Exception {
        final User user = new UserDataGen().roles(role).nextPersisted();
        user.setPassword(PASSWORD);
        APILocator.getUserAPI().save(user, APILocator.systemUser(), false);
        users.add(user);
        return user;
    }

    private static void grantRead(final User user, final Permissionable asset) throws Exception {
        grant(user, asset, PermissionAPI.PERMISSION_READ);
    }

    /**
     * Gives the user's own role the given permissions on the asset, and, for a content type, on
     * the content created from it.
     *
     * @param user        the user
     * @param asset       the asset
     * @param permissions the permission bits
     * @throws Exception if a permission cannot be saved
     */
    private static void grant(final User user, final Permissionable asset, final int permissions)
            throws Exception {
        final String roleId = APILocator.getRoleAPI().loadRoleByKey(user.getUserId()).getId();
        APILocator.getPermissionAPI().save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                asset.getPermissionId(), roleId, permissions, true), asset,
                APILocator.systemUser(), false);
        if (asset instanceof ContentType) {
            APILocator.getPermissionAPI().save(new Permission(Contentlet.class.getCanonicalName(),
                    asset.getPermissionId(), roleId, permissions, true), asset,
                    APILocator.systemUser(), false);
        }
    }

    /**
     * Builds a request carrying an API JWT owned by the given user.
     *
     * @param user the user the request authenticates as
     * @return the request
     */
    private static HttpServletRequest request(final User user) {
        return request(TOKEN_UTILS.bearerHeader(user));
    }

    /**
     * Builds a request that carries the given {@code Authorization} header.
     *
     * @param authorization the value of the {@code Authorization} header
     * @return the request
     */
    private static HttpServletRequest request(final String authorization) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());
        request.setHeader("Authorization", authorization);
        return request;
    }

    /**
     * Builds a request made from the given site. The site is set on the request and the request is
     * registered for the current thread, where the container lookup reads the current site from.
     *
     * @param user the user the request authenticates as
     * @param site the site the request is made from
     * @return the request
     */
    private static HttpServletRequest requestFrom(final User user, final Host site) {
        final HttpServletRequest request = request(user);
        request.setAttribute(WebKeys.CURRENT_HOST, site);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        return request;
    }
}

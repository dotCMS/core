package com.dotmarketing.portlets.contentlet.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.portlets.contentlet.struts.ImportContentletsForm;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.PortletSessionImpl;
import com.liferay.util.servlet.SessionMessages;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link ImportContentletsAction#processAction}: the gate that refuses a
 * CSV import is exercised through the action itself, on the {@code publish} command.
 *
 * <p>This is the half {@link ContentImportAccessTest} cannot reach. That class proves
 * {@link ContentImportAccess#canImportInto} answers correctly; these tests prove the action asks
 * it, asks it about the Content Type the import will actually use rather than the one this
 * request happens to carry, and that the sentence it shows cannot carry markup to the screen.</p>
 *
 * <p><b>What these tests do not cover.</b> The {@code preview} command, which is where the upload
 * is stored. Reaching it means {@code PortalUtil.getUploadPortletRequest} parsing a real multipart
 * body, and the ordering it protects — that the copy into the shared asset store happens only at
 * the two {@code _generatePreview} call sites, below every rejection path — is left to manual QA.
 * The {@code publish} gate tested here is the one that stands between a caller and the import
 * itself.</p>
 */
public class ImportContentletsActionTest {

    private static final String MARKUP_IN_NAME = "<b>Bold</b> & \"quoted\"";

    private static PermissionAPI permissionAPI;
    private static Host site;
    private static ContentType restrictedType;
    private static ContentType allowedType;
    private static ContentType typeNamedWithMarkup;
    private static User importer;
    private static final List<User> createdUsers = new ArrayList<>();
    private static final List<ContentType> createdTypes = new ArrayList<>();

    /**
     * Creates a site the test user has no access to, a Content Type they may write to, one they
     * may not, and one they may not whose name carries markup.
     */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        permissionAPI = APILocator.getPermissionAPI();

        site = new SiteDataGen().nextPersisted();
        restrictedType = newContentType("restricted-" + System.currentTimeMillis());
        allowedType = newContentType("allowed-" + System.currentTimeMillis());
        typeNamedWithMarkup = newContentType(MARKUP_IN_NAME + System.currentTimeMillis());

        importer = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        createdUsers.add(importer);
        permissionAPI.save(new Permission(allowedType.getPermissionId(),
                        APILocator.getRoleAPI().getUserRole(importer).getId(),
                        PermissionAPI.PERMISSION_WRITE, true),
                allowedType, APILocator.systemUser(), false);

        assertTrue("The fixture needs a type this user can import into",
                permissionAPI.doesUserHavePermission(allowedType, PermissionAPI.PERMISSION_WRITE,
                        importer, false));
        assertFalse("The fixture needs a type this user cannot import into",
                permissionAPI.doesUserHavePermission(restrictedType,
                        PermissionAPI.PERMISSION_WRITE, importer, false));
        assertFalse("The user must not be able to add content to the site either, or the site "
                        + "branch of the gate would let them through",
                permissionAPI.doesUserHavePermission(site,
                        PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, importer, false));
    }

    /**
     * Removes the Content Types, the users and the site created for these tests.
     */
    @AfterClass
    public static void cleanup() {
        createdUsers.forEach(user -> UserDataGen.remove(user, true));
        createdTypes.forEach(type -> {
            try {
                ContentTypeDataGen.remove(type);
            } catch (final Exception e) {
                Logger.warn(ImportContentletsActionTest.class,
                        "Could not remove test Content Type: " + e.getMessage());
            }
        });
        if (site != null) {
            try {
                APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            } catch (final Exception e) {
                Logger.warn(ImportContentletsActionTest.class,
                        "Could not remove test site: " + e.getMessage());
            }
        }
    }

    /**
     * Method to test: {@link ImportContentletsAction#processAction}
     * Given Scenario: A back-end user with no write access to the Content Type the preview step
     *   stored in the session, and no right to add content to the selected site, posts
     *   {@code cmd=publish}
     * Expected Result: The import does not start; the screen comes back with an error
     */
    @Test
    public void processAction_publish_userWithoutPermission_isRefused() throws Exception {
        final ActionRequestImpl request = publishRequest(importer, restrictedType);

        new ImportContentletsAction().processAction(null, formFor(restrictedType), null, request,
                mock(ActionResponse.class));

        assertRefused(request);
    }

    /**
     * Method to test: {@link ImportContentletsAction#processAction}
     * Given Scenario: The session holds the form the preview step stored, naming a Content Type
     *   the user cannot write to, while the request carries a form naming one they can
     * Expected Result: The import is still refused, because the background import reads the
     *   session form and that is what the gate has to be asked about
     */
    @Test
    public void processAction_publish_sessionFormDecides_notTheRequestForm() throws Exception {
        final ActionRequestImpl request = publishRequest(importer, restrictedType);

        new ImportContentletsAction().processAction(null, formFor(allowedType), null, request,
                mock(ActionResponse.class));

        assertRefused(request);
    }

    /**
     * Method to test: {@link ImportContentletsAction#processAction}
     * Given Scenario: The refused Content Type's name carries markup
     * Expected Result: The sentence left for the screen carries the name HTML-encoded. The screen
     *   renders this sentence as markup, so the name has to arrive already encoded
     */
    @Test
    public void processAction_publish_refusalMessage_encodesTheContentTypeName() throws Exception {
        final ActionRequestImpl request = publishRequest(importer, typeNamedWithMarkup);

        new ImportContentletsAction().processAction(null, formFor(typeNamedWithMarkup), null,
                request, mock(ActionResponse.class));

        final Object message = SessionMessages.get(request, ImportContentletsAction.ERROR);
        assertNotNull("The refusal must leave a message on the screen", message);
        assertFalse("The Content Type's name must not reach the screen as markup",
                message.toString().contains("<b>"));
        assertTrue("The name is expected HTML-encoded instead",
                message.toString().contains("&lt;b&gt;"));
    }

    /**
     * Asserts that the request was refused: back to the import screen, with an error on it and no
     * import id handed to the next page.
     *
     * @param request the request that was processed
     */
    private static void assertRefused(final ActionRequestImpl request) {
        assertEquals("A refused import must come back to the import screen",
                ImportContentletsAction.PORTLET_EXT_CONTENTLET_IMPORT_CONTENTLETS,
                request.getAttribute(com.liferay.portal.util.WebKeys.PORTLET_STRUTS_FORWARD));
        assertTrue("A refused import must say so on the screen",
                SessionMessages.contains(request, ImportContentletsAction.ERROR));
        assertFalse("A refused import must not have started",
                null != request.getAttribute("importId"));
    }

    /**
     * Builds the publish request the action reads: the command, the calling user, the site
     * selected in the back end, and a session holding the form the preview step would have
     * stored.
     *
     * <p>The form bound to the request is not set here: that one is the {@code form} argument
     * {@code processAction} receives, and each test passes it directly.</p>
     *
     * @param user          the {@link User} making the request
     * @param typeInSession the Content Type named by the form stored in the session, which is the
     *                      one the import would actually use
     * @return the request to hand to {@code processAction}
     */
    private static ActionRequestImpl publishRequest(final User user,
            final ContentType typeInSession) {
        final Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("form_to_import", formFor(typeInSession));
        sessionAttributes.put("fileName", "import.csv");
        sessionAttributes.put(WebKeys.CMS_SELECTED_HOST_ID, site.getIdentifier());

        final HttpSession httpSession = mock(HttpSession.class);
        when(httpSession.getAttribute(anyString()))
                .thenAnswer(call -> sessionAttributes.get(call.<String>getArgument(0)));
        doAnswer(call -> sessionAttributes.put(call.getArgument(0), call.getArgument(1)))
                .when(httpSession).setAttribute(anyString(), any());

        final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);
        when(httpRequest.getSession()).thenReturn(httpSession);
        when(httpRequest.getSession(false)).thenReturn(httpSession);

        final PortletSessionImpl portletSession = mock(PortletSessionImpl.class);
        when(portletSession.getSession()).thenReturn(httpSession);

        final Map<String, Object> requestAttributes = new HashMap<>();
        final ActionRequestImpl request = mock(ActionRequestImpl.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(request.getPortletSession()).thenReturn(portletSession);
        when(request.getParameter(Constants.CMD))
                .thenReturn(com.dotmarketing.util.Constants.PUBLISH);
        when(request.getAttribute(anyString()))
                .thenAnswer(call -> requestAttributes.get(call.<String>getArgument(0)));
        doAnswer(call -> requestAttributes.put(call.getArgument(0), call.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        return request;
    }

    /**
     * An import form naming the given Content Type, as the screen would submit it.
     *
     * @param contentType the {@link ContentType} the import is for
     * @return the form
     */
    private static ImportContentletsForm formFor(final ContentType contentType) {
        final ImportContentletsForm form = new ImportContentletsForm();
        form.setStructure(contentType.inode());
        form.setFileName("import.csv");
        return form;
    }

    /**
     * Creates a persisted Content Type on the test site, with no permissions of its own.
     *
     * @param name the type's name
     * @return the persisted {@link ContentType}
     */
    private static ContentType newContentType(final String name) {
        final ContentType contentType = new ContentTypeDataGen()
                .baseContentType(BaseContentType.CONTENT)
                .host(site)
                .name(name)
                // The generator's default variable name is built from the clock, so three types
                // created in the same millisecond would collide on it.
                .velocityVarName("importGate" + createdTypes.size() + System.nanoTime())
                .nextPersisted();
        createdTypes.add(contentType);
        return contentType;
    }
}

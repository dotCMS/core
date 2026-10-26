package com.dotcms.rest.api.v1.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldRelationshipDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.json.JSONArray;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link ContentRelationshipsResource}: which related contents are returned
 * to anonymous visitors and to back-end users, depending on what each one can read.
 */
public class ContentRelationshipsResourceIntegrationTest {

    private static final String PASSWORD = "Rel" + System.currentTimeMillis() + "!";

    private static User systemUser;
    private static User anonymousUser;
    private static PermissionAPI permissionAPI;

    private static ContentType parentType;
    private static ContentType childType;
    private static Relationship relationship;
    private static Role readerRole;
    private static User backendUser;
    private static User backendReader;

    private static Contentlet parent;
    private static Contentlet notReadable;
    private static Contentlet published;
    private static Contentlet publishedWithNewerVersion;
    private static Contentlet neverPublished;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        anonymousUser = APILocator.getUserAPI().getAnonymousUser();
        permissionAPI = APILocator.getPermissionAPI();
        final Role anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();

        parentType = new ContentTypeDataGen().nextPersisted();
        childType = new ContentTypeDataGen().nextPersisted();
        relationship = new FieldRelationshipDataGen().parent(parentType).child(childType)
                .nextPersisted();

        readerRole = new RoleDataGen().nextPersisted();
        backendUser = new UserDataGen().password(PASSWORD)
                .roles(TestUserUtils.getBackendRole()).nextPersisted();
        backendReader = new UserDataGen().password(PASSWORD)
                .roles(TestUserUtils.getBackendRole(), readerRole).nextPersisted();

        parent = new ContentletDataGen(parentType).nextPersistedAndPublish();
        grantRead(parent, anonymousRole);

        notReadable = new ContentletDataGen(childType).nextPersistedAndPublish();
        grantRead(notReadable, readerRole);

        published = new ContentletDataGen(childType).nextPersistedAndPublish();
        grantRead(published, anonymousRole);

        publishedWithNewerVersion = new ContentletDataGen(childType).nextPersistedAndPublish();
        grantRead(publishedWithNewerVersion, anonymousRole);
        ContentletDataGen.checkin(ContentletDataGen.checkout(publishedWithNewerVersion));

        neverPublished = new ContentletDataGen(childType).nextPersisted();
        grantRead(neverPublished, anonymousRole);

        APILocator.getContentletAPI().relateContent(parent, relationship,
                List.of(notReadable, published, publishedWithNewerVersion, neverPublished),
                systemUser, false);

        assertFalse("Setup: anonymous visitors must not be able to read the item limited to a role",
                permissionAPI.doesUserHavePermission(notReadable, PermissionAPI.PERMISSION_READ,
                        anonymousUser, true));
        assertFalse("Setup: the back-end user without the role must not be able to read it",
                permissionAPI.doesUserHavePermission(notReadable, PermissionAPI.PERMISSION_READ,
                        backendUser, true));
        assertTrue("Setup: the back-end user holding the role must be able to read it",
                permissionAPI.doesUserHavePermission(notReadable, PermissionAPI.PERMISSION_READ,
                        backendReader, true));
    }

    @AfterClass
    public static void cleanup() {
        for (final Contentlet contentlet : new Contentlet[]{parent, notReadable, published,
                publishedWithNewerVersion, neverPublished}) {
            if (contentlet != null) {
                ContentletDataGen.destroy(contentlet, true);
            }
        }
        if (parentType != null) {
            ContentTypeDataGen.remove(parentType, true);
        }
        if (childType != null) {
            ContentTypeDataGen.remove(childType, true);
        }
        for (final User user : new User[]{backendUser, backendReader}) {
            if (user != null) {
                UserDataGen.remove(user, true);
            }
        }
        if (readerRole != null) {
            RoleDataGen.remove(readerRole, true);
        }
    }

    /**
     * Method to test: {@link ContentRelationshipsResource#getContent}
     * <p>
     * Given scenario: a published parent anonymous visitors can read, related to a published
     * item that only a role can read.
     * <p>
     * Expected result: an anonymous request does not get that item, anywhere in the response.
     */
    @Test
    public void getContent_leavesOutRelatedContentAnonymousCannotRead() throws Exception {
        final String body = getRelatedContent(anonymousRequest(), parent.getIdentifier());

        assertFalse("A related item anonymous visitors cannot read must not be returned",
                body.contains(notReadable.getIdentifier()));
    }

    /**
     * Method to test: {@link ContentRelationshipsResource#getContent}
     * <p>
     * Given scenario: a published related item that anonymous visitors can read.
     * <p>
     * Expected result: an anonymous request gets it under the relationship.
     */
    @Test
    public void getContent_includesPublishedRelatedContentAnonymousCanRead() throws Exception {
        final String body = getRelatedContent(anonymousRequest(), parent.getIdentifier());

        assertTrue("A published related item anonymous visitors can read must be returned",
                relatedIdentifiers(body).contains(published.getIdentifier()));
    }

    /**
     * Method to test: {@link ContentRelationshipsResource#getContent}
     * <p>
     * Given scenario: a related item anonymous visitors can read, published and then saved again
     * without publishing, so its current version is not the published one.
     * <p>
     * Expected result: an anonymous request does not get its current version.
     */
    @Test
    public void getContent_leavesOutUnpublishedVersionsOfRelatedContentForAnonymous()
            throws Exception {
        final String body = getRelatedContent(anonymousRequest(), parent.getIdentifier());

        assertFalse("A version that is not published must not be returned to anonymous visitors",
                relatedIdentifiers(body).contains(publishedWithNewerVersion.getIdentifier()));
    }

    /**
     * Method to test: {@link ContentRelationshipsResource#getContent}
     * <p>
     * Given scenario: a related item anonymous visitors can read that was never published.
     * <p>
     * Expected result: an anonymous request does not get it.
     */
    @Test
    public void getContent_leavesOutNeverPublishedRelatedContentForAnonymous() throws Exception {
        final String body = getRelatedContent(anonymousRequest(), parent.getIdentifier());

        assertFalse("Content that was never published must not be returned to anonymous visitors",
                relatedIdentifiers(body).contains(neverPublished.getIdentifier()));
    }

    /**
     * Method to test: {@link ContentRelationshipsResource#getContent}
     * <p>
     * Given scenario: a back-end user who can read the parent but not a related item.
     * <p>
     * Expected result: the user does not get that item, anywhere in the response.
     */
    @Test
    public void getContent_leavesOutRelatedContentTheBackendUserCannotRead() throws Exception {
        final String body = getRelatedContent(requestFor(backendUser), parent.getIdentifier());

        assertFalse("A related item the user cannot read must not be returned",
                body.contains(notReadable.getIdentifier()));
    }

    /**
     * Method to test: {@link ContentRelationshipsResource#getContent}
     * <p>
     * Given scenario: a back-end user who can read a related item through one of their roles.
     * <p>
     * Expected result: the user gets that item under the relationship.
     */
    @Test
    public void getContent_includesRelatedContentTheBackendUserCanReadThroughARole()
            throws Exception {
        final String body = getRelatedContent(requestFor(backendReader), parent.getIdentifier());

        assertTrue("A related item the user can read through a role must be returned",
                relatedIdentifiers(body).contains(notReadable.getIdentifier()));
    }

    /**
     * Method to test: {@link ContentRelationshipsResource#getContent}
     * <p>
     * Given scenario: an identifier that does not belong to any content.
     * <p>
     * Expected result: an empty list, as for content the caller cannot read.
     */
    @Test
    public void getContent_returnsEmptyListForAnIdentifierThatDoesNotExist() throws Exception {
        final String body = getRelatedContent(anonymousRequest(),
                UUID.randomUUID().toString());

        assertEquals("[]", body);
    }

    /**
     * Gives READ on the content to the role only, through individual permissions.
     */
    private static void grantRead(final Contentlet contentlet, final Role role)
            throws Exception {
        permissionAPI.save(new Permission(contentlet.getPermissionId(), role.getId(),
                PermissionAPI.PERMISSION_READ, true), contentlet, systemUser, false);
    }

    /**
     * Calls the resource for the given identifier and returns the response body, after checking
     * that the request succeeded.
     */
    private static String getRelatedContent(final HttpServletRequest request,
            final String identifier) throws Exception {
        final Response response = new ContentRelationshipsResource()
                .getContent(request, mock(HttpServletResponse.class), "id/" + identifier);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        return (String) response.getEntity();
    }

    /**
     * Reads the identifiers listed under the test relationship for the first content returned.
     */
    private static List<String> relatedIdentifiers(final String body) {
        final JSONArray contents = new JSONArray(body);
        final List<String> identifiers = new ArrayList<>();
        if (contents.length() == 0) {
            return identifiers;
        }
        final JSONArray related = contents.getJSONObject(0)
                .getJSONArray(relationship.getRelationTypeValue());
        for (int i = 0; i < related.length(); i++) {
            identifiers.add(related.getJSONObject(i).getString("identifier"));
        }
        return identifiers;
    }

    /**
     * Creates a request with no credentials.
     */
    private static HttpServletRequest anonymousRequest() {
        return new MockAttributeRequest(
                new MockSessionRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()
                ).request()
        ).request();
    }

    /**
     * Creates a request authenticated as the given user.
     */
    private static HttpServletRequest requestFor(final User user) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request()
                        ).request()
                ).request()
        );
        request.setHeader("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString((user.getEmailAddress() + ":" + PASSWORD).getBytes()));
        return request;
    }
}

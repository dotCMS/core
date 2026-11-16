package com.dotcms.rest;

import static com.dotcms.util.CollectionsUtils.list;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.tag.RestTag;
import com.dotcms.rest.tag.TagForm;
import com.dotcms.rest.tag.UpdateTagForm;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.tag.business.TagTestFixtures;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class TagResourceIntegrationTest extends IntegrationTestBase {

    private static Host demoHost;

    private static final TagTestFixtures fixtures = new TagTestFixtures();

    @AfterClass
    public static void cleanUpTagPermissionFixtures() throws Exception {
        fixtures.cleanUp();
    }

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        if (null == demoHost) {
            demoHost = new SiteDataGen().nextPersisted();
        }

        
        APILocator.getPermissionAPI().setDefaultCMSAnonymousPermissions(demoHost);
        
        
    }

    private static final List<String> tagsKnownNamesSystemHost =
            list("extreme" + UUIDGenerator.generateUuid(),
                    "external" + UUIDGenerator.generateUuid(),
                    "extension" + UUIDGenerator.generateUuid());

    private static final List<String> tagsKnownNamesDemoHost =
            list("terminal" + UUIDGenerator.generateUuid(),
                    "terminator" + UUIDGenerator.generateUuid(),
                    "termometer" + UUIDGenerator.generateUuid());

    @DataProvider
    public static Object[] listTestCases() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        if (null == demoHost) {
            demoHost = new SiteDataGen().nextPersisted();
        }
        APILocator.getPermissionAPI().setDefaultCMSAnonymousPermissions(demoHost);
        final Folder demoFolder = new FolderDataGen().site(demoHost).nextPersisted();
        final String demoFolderId = demoFolder.getIdentifier();

        // tag name provided, demo site id provided, should return tags filtered by name and host
        final TagResourceTestCase case1 = new TagResourceTestCase();
        case1.setTagName("ter");
        case1.setSiteOrFolderId(demoFolderId);
        case1.setExpectedTags(tagsKnownNamesDemoHost);

        // tag name provided, NO host id provided, should not return tags created under DEMO
        final TagResourceTestCase case2 = new TagResourceTestCase();
        case2.setTagName("ter");
        case2.setUnexpectedTags(tagsKnownNamesDemoHost);

        // tag name provided, NOT host id provided, should return created tags under SYSTEM_HOST
        final TagResourceTestCase case3 = new TagResourceTestCase();
        case3.setTagName("ext");
        case3.setExpectedTags(tagsKnownNamesSystemHost);

        // no tag name provided, no site/folder id provided, should return all tags
        final TagResourceTestCase case4 = new TagResourceTestCase();
        case4.setExpectedTags(tagsKnownNamesSystemHost);

        return new TagResourceTestCase[] {
                case1,
                case2,
                case3,
                case4
        };
    }


    @Test
    @UseDataProvider("listTestCases")
    public void testList(final TagResourceTestCase testCase)
            throws DotSecurityException, DotDataException {
        // let's create some tags under SYSTEM_HOST
        final TagAPI tagAPI = APILocator.getTagAPI();
        final HostAPI hostAPI = APILocator.getHostAPI();
        final FolderAPI folderAPI = APILocator.getFolderAPI();
        final User systemUser = APILocator.systemUser();

        final List<String> tagsKnownNamesSystemHostIds = new ArrayList<>();
        final List<String> tagsKnownNamesDemoHostIds = new ArrayList<>();

        try {

            tagsKnownNamesSystemHost.forEach((tagName) -> Sneaky.sneak(() ->
                    tagsKnownNamesSystemHostIds.add(
                            tagAPI.saveTag(tagName, systemUser.getUserId(), Host.SYSTEM_HOST)
                                    .getTagId()
                    )
            ));

            tagsKnownNamesDemoHost.forEach((tagName) -> Sneaky.sneak(() ->
                    tagsKnownNamesDemoHostIds.add(
                            tagAPI.saveTag(tagName, systemUser.getUserId(),
                                    demoHost.getIdentifier())
                            .getTagId()
                    )
            ));

            // let's mock things
            final HttpServletRequest request = mock(HttpServletRequest.class);
            final WebResource webResource = mock(WebResource.class);
            final InitDataObject dataObject = mock(InitDataObject.class);
            when(dataObject.getUser()).thenReturn(APILocator.systemUser());

            when(webResource.init(any(InitBuilder.class)))
                    .thenReturn(dataObject);

            final TagResource tagResource = new TagResource(tagAPI, hostAPI, folderAPI, webResource);
            final Map<String, RestTag> returnedTags =
                    tagResource.list(request, new MockHttpResponse(), testCase.getTagName(),
                            testCase.getSiteOrFolderId());

            final List<String> returnedTagsNames = returnedTags.values().stream()
                    .map((tag) -> tag.label).collect(Collectors.toList());

            if(UtilMethods.isSet(testCase.getExpectedTags())) {
                Assert.assertTrue("Returned tags should contain all expected tags",
                        returnedTagsNames.containsAll(testCase.getExpectedTags()));
            }

            if(UtilMethods.isSet(testCase.getUnexpectedTags())) {
                Assert.assertFalse("Returned tags should NOT contain any expected tags",
                        returnedTagsNames.containsAll(testCase.getUnexpectedTags()));
            }

        } finally {
            // clean env
            tagsKnownNamesSystemHostIds.forEach((tagId) -> {
                try {
                    tagAPI.deleteTag(tagId);
                } catch (DotDataException e) {
                    Logger.error(this, "Error deleting tags in test.", e);
                }
            }
            );

            tagsKnownNamesDemoHostIds.forEach((tagId) ->{
                try {
                    tagAPI.deleteTag(tagId);
                } catch (DotDataException e) {
                    Logger.error(this, "Error deleting tags in test.", e);
                }
            });
        }
    }

    /**
     * Given: a contentlet with tags that the user can't edit
     * Expected: removing the contentlet's tags returns 403 and the tags are kept
     */
    @Test
    public void test_deleteTagInodesByInode_userCannotEditContent_shouldBeForbidden() throws Exception {
        final TagAPI tagAPI = APILocator.getTagAPI();
        final User systemUser = APILocator.systemUser();
        final User limitedUser = new UserDataGen().roles(new RoleDataGen().nextPersisted()).nextPersisted();
        final Contentlet contentlet = TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
        final Tag tag = tagAPI.getTagAndCreate("inode-v1-" + UUIDGenerator.shorty(),
                systemUser.getUserId(), Host.SYSTEM_HOST);

        try {
            tagAPI.addContentletTagInode(tag, contentlet.getInode(), "tags");

            final WebResource webResource = mock(WebResource.class);
            final InitDataObject dataObject = mock(InitDataObject.class);
            when(dataObject.getUser()).thenReturn(limitedUser);
            when(webResource.init(any(InitBuilder.class))).thenReturn(dataObject);
            final TagResource tagResource = new TagResource(tagAPI, APILocator.getHostAPI(),
                    APILocator.getFolderAPI(), webResource);

            try {
                tagResource.deleteTagInodesByInode(mock(HttpServletRequest.class),
                        new MockHttpResponse(), contentlet.getInode());
                Assert.fail("Removing tags from content the user can't edit should be forbidden");
            } catch (final ForbiddenException e) {
                // expected
            }
            Assert.assertFalse("The contentlet should keep its tags",
                    tagAPI.getTagInodesByInode(contentlet.getInode()).isEmpty());
        } finally {
            tagAPI.deleteTag(tag.getTagId());
            UserDataGen.remove(limitedUser);
        }
    }

    /**
     * Given: a Tags portlet user creates a tag owned by another user
     * Expected: 403 and the other user gets no tag
     */
    @Test
    public void test_addTag_ownerIsOtherUser_shouldBeForbidden() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS.toString());
        final User other = fixtures.backEndUser();
        final String tagName = "v1-owner-other-" + UUIDGenerator.shorty();

        try {
            Assert.assertThrows(ForbiddenException.class, () -> tagResource(user).addTag(
                    tagsRequest(), new MockHttpResponse(), tagForm(tagName, other.getUserId())));
        } finally {
            fixtures.track(APILocator.getTagAPI().getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
        }
        Assert.assertTrue("The other user should get no tag",
                APILocator.getTagAPI().getTagsForUserByUserId(other.getUserId()).isEmpty());
    }

    /**
     * Given: a Tags portlet user creates a tag owned by themselves
     * Expected: 200 and the tag is added to the user
     */
    @Test
    public void test_addTag_ownerIsSelf_shouldBindTag() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS.toString());
        final String tagName = "v1-owner-self-" + UUIDGenerator.shorty();

        try {
            final Response result = tagResource(user).addTag(tagsRequest(), new MockHttpResponse(),
                    tagForm(tagName, user.getUserId()));
            Assert.assertEquals(200, result.getStatus());
        } finally {
            fixtures.track(APILocator.getTagAPI().getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
        }
        Assert.assertTrue(APILocator.getTagAPI().getTagsForUserByUserId(user.getUserId()).stream()
                .anyMatch(tag -> tag.getTagName().equals(tagName)));
    }

    /**
     * Given: a Tags portlet user creates a tag with no owner
     * Expected: 200 and the tag is created
     */
    @Test
    public void test_addTag_noOwner_shouldCreateTag() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS.toString());
        final String tagName = "v1-no-owner-" + UUIDGenerator.shorty();

        try {
            final Response result = tagResource(user).addTag(tagsRequest(), new MockHttpResponse(),
                    tagForm(tagName, null));
            Assert.assertEquals(200, result.getStatus());
        } finally {
            fixtures.track(APILocator.getTagAPI().getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
        }
        Assert.assertNotNull(APILocator.getTagAPI().getTagByNameAndHost(tagName, Host.SYSTEM_HOST));
    }

    /**
     * Given: a Tags portlet user reads another user's tags
     * Expected: 403
     */
    @Test
    public void test_getTagsByUserId_otherUser_shouldBeForbidden() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS.toString());
        final User other = fixtures.backEndUser();
        APILocator.getTagAPI().addUserTagInode(fixtures.tag("v1-owner-read"), other.getUserId());

        Assert.assertThrows(ForbiddenException.class, () -> tagResource(user).getTagsByUserId(
                tagsRequest(), new MockHttpResponse(), other.getUserId()));
    }

    /**
     * Given: a Tags portlet user links a tag to content they can only read
     * Expected: 403 and the content keeps no link to the tag
     */
    @Test
    public void test_linkTagsAndInode_contentWithoutEdit_shouldBeForbidden() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS.toString());
        final Contentlet contentlet = fixtures.contentWithPermission(user, PermissionAPI.PERMISSION_READ);
        final Tag tag = fixtures.tag("v1-link-denied");

        Assert.assertThrows(ForbiddenException.class, () -> tagResource(user).linkTagsAndInode(
                tagsRequest(), new MockHttpResponse(), tag.getTagId(), contentlet.getInode()));
        Assert.assertTrue(APILocator.getTagAPI().getTagInodesByInode(contentlet.getInode()).isEmpty());
    }

    /**
     * Given: a Tags portlet user reads the tags of content they can't read
     * Expected: 403
     */
    @Test
    public void test_findTagsByInode_contentWithoutRead_shouldBeForbidden() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS.toString());
        final Contentlet contentlet = fixtures.contentWithPermission(user, 0);
        APILocator.getTagAPI().addContentletTagInode(fixtures.tag("v1-inode-read"),
                contentlet.getInode(), "tags");

        Assert.assertThrows(ForbiddenException.class, () -> tagResource(user).findTagsByInode(
                tagsRequest(), new MockHttpResponse(), contentlet.getInode()));
    }

    /**
     * Given: a Tags portlet user renames a tag on content they can't edit
     * Expected: 403 and the name is kept
     */
    @Test
    public void test_updateTag_tagOnContentWithoutEdit_shouldBeForbidden() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS.toString());
        final Contentlet contentlet = fixtures.contentWithPermission(user, PermissionAPI.PERMISSION_READ);
        final Tag tag = fixtures.tag("v1-rename-denied");
        APILocator.getTagAPI().addContentletTagInode(tag, contentlet.getInode(), "tags");

        Assert.assertThrows(ForbiddenException.class, () -> tagResource(user).updateTag(
                tagsRequest(), new MockHttpResponse(),
                updateTagForm(tag.getTagId(), tag.getTagName() + "-renamed")));
        Assert.assertEquals(tag.getTagName(),
                APILocator.getTagAPI().getTagByTagId(tag.getTagId()).getTagName());
    }

    /**
     * Given: a Tags portlet user renames a tag nothing uses
     * Expected: 200 and the tag is renamed
     */
    @Test
    public void test_updateTag_unusedTag_shouldUpdate() throws Exception {
        final User user = fixtures.backEndUser(PortletID.TAGS.toString());
        final Tag tag = fixtures.tag("v1-rename-allowed");
        final String newName = tag.getTagName() + "-renamed";

        final Response result = tagResource(user).updateTag(tagsRequest(), new MockHttpResponse(),
                updateTagForm(tag.getTagId(), newName));

        Assert.assertEquals(200, result.getStatus());
        Assert.assertEquals(newName, APILocator.getTagAPI().getTagByTagId(tag.getTagId()).getTagName());
    }

    private static TagResource tagResource(final User user) {
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(user);
        when(webResource.init(any(InitBuilder.class))).thenReturn(dataObject);
        return new TagResource(APILocator.getTagAPI(), APILocator.getHostAPI(),
                APILocator.getFolderAPI(), webResource);
    }

    private static HttpServletRequest tagsRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/tags");
        return request;
    }

    private static TagForm tagForm(final String tagName, final String ownerId) {
        final RestTag restTag = new RestTag.Builder().key(tagName).label(tagName)
                .siteId(Host.SYSTEM_HOST).build();
        return new TagForm(Map.of(tagName, restTag), ownerId);
    }

    private static UpdateTagForm updateTagForm(final String tagId, final String tagName) throws Exception {
        return new ObjectMapper().readValue(String.format(
                "{\"tagId\":\"%s\",\"tagName\":\"%s\",\"siteId\":\"%s\"}",
                tagId, tagName, Host.SYSTEM_HOST), UpdateTagForm.class);
    }

}

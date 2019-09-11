package com.dotcms.rest;

import static com.dotcms.util.CollectionsUtils.list;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.tag.business.TagAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class TagResourceIntegrationTest extends IntegrationTestBase {

    private static Host demoHost;

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        if (null == demoHost) {
            demoHost = new SiteDataGen().nextPersisted();
        }

        
        APILocator.getPermissionAPI().setDefaultCMSAnonymousPermissions(demoHost);
        
        
    }

    private static List<String> tagsKnownNamesSystemHost =
            list("extreme" + UUIDGenerator.generateUuid(),
                    "external" + UUIDGenerator.generateUuid(),
                    "extension" + UUIDGenerator.generateUuid());

    private static List<String> tagsKnownNamesDemoHost =
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
        final String DEMO_HOST_IDENTIFIER = demoHost.getIdentifier();

        // tag name provided, demo site id provided, should return tags filtered by name and host
        final TagResourceTestCase case1 = new TagResourceTestCase();
        case1.setTagName("ter");
        case1.setSiteOrFolderId(DEMO_HOST_IDENTIFIER);
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

            final TagResource tagResource = new TagResource(tagAPI, webResource);
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

}

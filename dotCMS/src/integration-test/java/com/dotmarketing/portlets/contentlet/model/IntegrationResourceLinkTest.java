package com.dotmarketing.portlets.contentlet.model;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.ResourceLink.ResourceLinkBuilder;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.FileUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for the ResourceLink
 * @author jsanca
 */
public class IntegrationResourceLinkTest extends IntegrationTestBase {

    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        user      = APILocator.getUserAPI().getSystemUser();
    }

    private Tuple2<Field, ContentType> createDotAssetContentType (final Host host, final String accept, final String variable) throws DotSecurityException, DotDataException {

        final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        ContentType dotAssetContentType     = contentTypeAPI
                .save(ContentTypeBuilder.builder(DotAssetContentType.class).folder(FolderAPI.SYSTEM_FOLDER)
                        .host(host.getIdentifier()).name(variable)
                        .owner(user.getUserId()).build());
        final Map<String, Field> fieldMap = dotAssetContentType.fieldMap();
        com.dotcms.contenttype.model.field.Field binaryField           = fieldMap.get(DotAssetContentType.ASSET_FIELD_VAR);
        final FieldVariable allowFileTypes = ImmutableFieldVariable.builder().key(BinaryField.ALLOWED_FILE_TYPES)
                .value(accept).fieldId(binaryField.id()).build();
        binaryField.constructFieldVariables(Arrays.asList(allowFileTypes));

        dotAssetContentType = contentTypeAPI.save(dotAssetContentType);
        binaryField = fieldAPI.save(binaryField, user);
        fieldAPI.save(allowFileTypes, user);

        return Tuple.of(binaryField, dotAssetContentType);
    }

    /**
     * Method to test:  build
     * Given Scenario: Creates a dotAsset with a binary field as a txt and figured out the version path and id path, the user is an admin
     * ExpectedResult: paths should have some tokens such as dA, shorty id or inode and the file name, download must be allowed
     * @throws Exception
     */
    @Test
    public void test_Text_ResourceLink_Expect_Downloadable_No_Port_Number_Dot_Asset() throws Exception{

        final Host host = APILocator.systemHost();
        final String mimeType = "text/plain";
        final boolean isSecure = false;
        final File file = FileUtil.createTemporalFile("comments-list", "txt", "This is a test temporal file");
        final String htmlFileName = file.getName();
        final User adminUser = TestUserUtils.getAdminUser();

        final Tuple2<Field, ContentType> fieldDotTextAssetContentType = this.createDotAssetContentType(host,
                "text/*", "textDotAsset" + System.currentTimeMillis());

        final Contentlet contentlet = new ContentletDataGen(fieldDotTextAssetContentType._2().variable())
                .setProperty(DotAssetContentType.ASSET_FIELD_VAR, file).nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn(host.getIdentifier());
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);


        final ResourceLinkBuilder resourceLinkBuilder = new ResourceLinkBuilder();
        final ResourceLink link = resourceLinkBuilder.build(request, adminUser, contentlet);

        assertEquals(mimeType, link.getMimeType());
        assertFalse(link.isDownloadRestricted());

        assertTrue(link.getIdPath().contains("/dA/") && link.getIdPath().contains("/"+htmlFileName)
                && link.getIdPath().contains(APILocator.getShortyAPI().shortify(contentlet.getIdentifier())));
        assertTrue(link.getVersionPath().contains("/dA/") && link.getVersionPath().contains("/"+htmlFileName)
                && link.getVersionPath().contains(APILocator.getShortyAPI().shortify(contentlet.getInode())));
    }

    private User mockLimitedUser(){
        final User adminUser = mock(User.class);
        when(adminUser.getUserId()).thenReturn("anonymous");
        when(adminUser.getEmailAddress()).thenReturn("anonymous@dotcmsfakeemail.org");
        when(adminUser.getFirstName()).thenReturn("anonymous user");
        when(adminUser.getLastName()).thenReturn("anonymous");
        return adminUser;
    }

    /**
     * Method to test:  build
     * Given Scenario: Creates a dotAsset with a binary field as a txt and figured out the version path and id path, the user is an limited
     * ExpectedResult: paths should have some tokens such as dA, shorty id or inode and the file name, download must be restricted
     * @throws Exception
     */
    @Test
    public void test_vtl_ResourceLink_WithLimitedUser_Expect_Downloadable_No_Port_Number() throws Exception{

        final Host host = APILocator.systemHost();
        final boolean isSecure = false;
        final File file = FileUtil.createTemporalFile("comments-list", ".vtl", "This is a test temporal file");
        final String htmlFileName = file.getName();

        final User limitedUser = mockLimitedUser();

        final Tuple2<Field, ContentType> fieldDotTextAssetContentType = this.createDotAssetContentType(host,
                "text/*", "textDotAsset" + System.currentTimeMillis());

        final Contentlet contentlet = new ContentletDataGen(fieldDotTextAssetContentType._2().variable())
                .setProperty(DotAssetContentType.ASSET_FIELD_VAR, file).nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn(host.getIdentifier());
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);

        final ResourceLinkBuilder resourceLinkBuilder = new ResourceLinkBuilder();
        final ResourceLink link = resourceLinkBuilder.build(request, limitedUser, contentlet);
        assertTrue(link.isDownloadRestricted());
        assertTrue(link.getIdPath().contains("/dA/") && link.getIdPath().contains("/"+htmlFileName)
                && link.getIdPath().contains(APILocator.getShortyAPI().shortify(contentlet.getIdentifier())));
        assertTrue(link.getVersionPath().contains("/dA/") && link.getVersionPath().contains("/"+htmlFileName)
                && link.getVersionPath().contains(APILocator.getShortyAPI().shortify(contentlet.getInode())));

    }


    /**
     * Method to test:  build
     * Given Scenario: this contentlet does not have any binary and the user is limited
     * ExpectedResult: download shoud be restricted and links should be blank
     * @throws Exception
     */
    @Test
    public void test_newContentlet_withoutBinary_expectEmptyLink() throws Exception{

        final Host host = APILocator.systemHost();
        final boolean isSecure = false;
        final File file = FileUtil.createTemporalFile("comments-list", "txt", "This is a test temporal file");
        final User adminUser = mockLimitedUser();

        final Tuple2<Field, ContentType> fieldDotTextAssetContentType = this.createDotAssetContentType(host,
                "text/*", "textDotAsset" + System.currentTimeMillis());

        final Contentlet contentlet = new ContentletDataGen(fieldDotTextAssetContentType._2().variable())
                .next();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ResourceLink.HOST_REQUEST_ATTRIBUTE)).thenReturn(host.getIdentifier());
        when(request.isSecure()).thenReturn(isSecure);
        when(request.getServerPort()).thenReturn(80);

        contentlet.setProperty(DotAssetContentType.ASSET_FIELD_VAR, null);
        final ResourceLinkBuilder resourceLinkBuilder = new ResourceLinkBuilder();
        final ResourceLink link = resourceLinkBuilder.build(request, adminUser, contentlet);
        assertTrue(link.isDownloadRestricted());
        assertEquals(StringPool.BLANK, link.getResourceLinkAsString());
        assertEquals(StringPool.BLANK, link.getVersionPath());
        assertEquals(StringPool.BLANK, link.getIdPath());
    }


}

package com.dotcms.content.business.json;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The issue this test is mean to verify lies in the methods @JsonIgnore public Permissionable getParentPermissionable()
 * when they return an object that contains another parentPermissionable property within itself,
 * and this new parentPermissionable has another parentPermissionable inside it,
 * causing an infinite loop.
 * The parentPermissionable property has already been marked with com.fasterxml.jackson.annotation.JsonIgnore,
 * but our src/main/java/com/dotmarketing/util/json/JSONObject.java didn't understand such annotation
 * as it only recognizes the com.dotmarketing.util.json.JSONIgnore So I made it aware of both annotations.
 */
public class LegacyJSONObjectRenderTest extends IntegrationTestBase {


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Test the rendering of a recursive reference in a Contentlet
     * Given scenario Contentlet's parent permissionable is a Host which has another reference to the same type of Contentlet (Host) as parent permissionable
     * There is a recursive reference in the Contentlet
     * Expected result is that the recursive reference is not rendered
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void TestRenderRecursiveReference()
            throws DotDataException, DotSecurityException, IOException {

        Host host = mock(Host.class);
        when(host.getIdentifier()).thenReturn("test");
        when(host.getInode()).thenReturn("test");
        when(host.getName()).thenReturn("test");
        when(host.getContentType()).thenReturn(mock(SimpleContentType.class));
        when(host.getHostname()).thenReturn("test.com");

        when(host.getParentPermissionable()).thenReturn(APILocator.systemHost());

        SimpleContentType contentType = mock(SimpleContentType.class);

        // Basic properties
        when(contentType.variable()).thenReturn("test");
        when(contentType.baseType()).thenReturn(BaseContentType.CONTENT);
        when(contentType.host()).thenReturn("test.com");
        when(contentType.inode()).thenReturn("123");
        when(contentType.name()).thenReturn("test");
        when(contentType.system()).thenReturn(false);
        when(contentType.fields()).thenReturn(List.of());
        when(contentType.fieldMap()).thenReturn(Map.of());
        when(contentType.folder()).thenReturn("/");
        when(contentType.id()).thenReturn("101");
        when(contentType.modDate()).thenReturn(new Date());

        // Permission and parent permissionable here where the recursive reference is
        when(contentType.getParentPermissionable()).thenReturn(contentType);
        when(contentType.getPermissionId()).thenReturn("102");

        // Additional properties
        when(contentType.detailPage()).thenReturn("myDetailPage");
        when(contentType.description()).thenReturn("test");
        when(contentType.icon()).thenReturn("test");
        when(contentType.urlMapPattern()).thenReturn("*");
        when(contentType.fixed()).thenReturn(false);

        // Additional default properties
        when(contentType.defaultType()).thenReturn(false);
        when(contentType.versionable()).thenReturn(true);
        when(contentType.multilingualable()).thenReturn(false);
        when(contentType.publishDateVar()).thenReturn(null);
        when(contentType.expireDateVar()).thenReturn(null);
        when(contentType.owner()).thenReturn("ownerTest");
        when(contentType.iDate()).thenReturn(new Date());
        when(contentType.sortOrder()).thenReturn(0);
        when(contentType.metadata()).thenReturn(Map.of());
        when(contentType.languageFallback()).thenReturn(false);
        when(contentType.markedForDeletion()).thenReturn(false);

       // Manifest info and permissionable behavior
        when(contentType.getManifestInfo()).thenReturn(mock(ManifestInfo.class));
        when(contentType.permissionable()).thenReturn(contentType);
        when(contentType.acceptedPermissions()).thenReturn(List.of());

        Contentlet contentlet = mock(Contentlet.class);

        // Mock basic methods
        when(contentlet.getIdentifier()).thenReturn("");
        when(contentlet.getContentType()).thenReturn(contentType);
        when(contentlet.getModDate()).thenReturn(new Date());
        when(contentlet.getModUser()).thenReturn("test");
        when(contentlet.getOwner()).thenReturn("test");
        when(contentlet.isLive()).thenReturn(false);
        when(contentlet.getLanguageId()).thenReturn(1L);
        when(contentlet.getInode()).thenReturn("inodeValue");
        when(contentlet.getSortOrder()).thenReturn(1L);
        when(contentlet.getHost()).thenReturn("hostValue");
        when(contentlet.getFolder()).thenReturn("folderValue");
        when(contentlet.isWorking()).thenReturn(true);
        when(contentlet.isArchived()).thenReturn(false);
        when(contentlet.getVersionId()).thenReturn("versionId");
        when(contentlet.getVersionType()).thenReturn("content");
        when(contentlet.getPermissionId()).thenReturn("permissionId");
        when(contentlet.getPermissionType()).thenReturn("permissionType");
        when(contentlet.isParentPermissionable()).thenReturn(true);

       // Mock content properties and metadata
        when(contentlet.getStringProperty(anyString())).thenReturn("stringValue");
        when(contentlet.getLongProperty(anyString())).thenReturn(123L);
        when(contentlet.getFloatProperty(anyString())).thenReturn(123.45F);
        when(contentlet.getBoolProperty(anyString())).thenReturn(true);
        when(contentlet.getDateProperty(anyString())).thenReturn(new Date());

        // Mock related contentlet methods
        when(contentlet.getRelated(anyString(), any(User.class))).thenReturn(List.of());
        when(contentlet.getMap()).thenReturn(Map.of());

        // Mock indexing and workflow methods
        when(contentlet.getIndexPolicy()).thenReturn(mock(IndexPolicy.class));
        when(contentlet.needsReindex()).thenReturn(false);
        when(contentlet.isDisableWorkflow()).thenReturn(false);
        when(contentlet.isWorkflowInProgress()).thenReturn(false);

        // Mock methods related to versioning and states
        when(contentlet.hasLiveVersion()).thenReturn(true);
        when(contentlet.isNew()).thenReturn(false);

        // Additional methods
        when(contentlet.getActionId()).thenReturn("actionId");
        when(contentlet.getVariantId()).thenReturn("variantId");
        when(contentlet.getBinary(anyString())).thenReturn(new File("path/to/file"));
        when(contentlet.isDotAsset()).thenReturn(false);
        when(contentlet.isHTMLPage()).thenReturn(false);

        // Mock ManifestItem methods
        when(contentlet.getManifestInfo()).thenReturn(mock(ManifestInfo.class));

        when(contentlet.getParentPermissionable()).thenReturn(host);

        final JSONObject jsonObject = new JSONObject(contentlet);
        Assert.assertNotNull(jsonObject.getAsMap());
        Assert.assertFalse(jsonObject.containsKey("parentPermissionable"));

    }


}

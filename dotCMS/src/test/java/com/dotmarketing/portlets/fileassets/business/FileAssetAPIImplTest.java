package com.dotmarketing.portlets.fileassets.business;

import com.dotcms.api.system.event.SystemEventsAPI;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class FileAssetAPIImplTest {
    private static FileAssetAPIImpl fileAssetAPI;

    private static ContentletAPI contAPI;
    private static PermissionAPI perAPI;
    private static SystemEventsAPI systemEventsAPI;
    private static IdentifierAPI identifierAPI;
    private static FileAssetFactory fileAssetFactory;
    private static  ContentletCache contentletCache;

    @BeforeClass
    public static void init(){
        contAPI = mock(ContentletAPI.class);
        perAPI = mock(PermissionAPI.class);
        systemEventsAPI  = mock(SystemEventsAPI.class);
        identifierAPI  = mock(IdentifierAPI.class);
        fileAssetFactory = mock(FileAssetFactory.class);
        contentletCache = mock(ContentletCache.class);

        fileAssetAPI = new FileAssetAPIImpl(contAPI, perAPI, systemEventsAPI, identifierAPI, fileAssetFactory, contentletCache);
    }

    /**
     * When: Try to get the FileAsset by parent folder and Elasticsearch is down
     * Should: Get the files from data base
     */
    @Test
    public void getFileAssetFromDataBase() throws DotSecurityException, DotDataException {

        final Contentlet fileAsset = mock(Contentlet.class);
        final List<Contentlet> contentlets = new ArrayList<>();
        contentlets.add(fileAsset);
        final String parentFolderInode = "Folder-" + System.currentTimeMillis();
        final Folder parentFolder = mock(Folder.class);
        final User user = mock(User.class);
        final boolean respectFrontendRoles = false;

        final String luceneQuery = "+structureType:" + Structure.STRUCTURE_TYPE_FILEASSET+" +conFolder:" + parentFolderInode;
        final String contentTypeId = "contentTypeId";
        final String contentTypeInode = "contentTypeInode";
        final String hostId = "hostId";
        final Map<String, Object> contentMap = new HashMap<>();

        when(parentFolder.getInode()).thenReturn(parentFolderInode);
        when(fileAsset.isFileAsset()).thenReturn(true);
        when(fileAsset.getContentTypeId()).thenReturn(contentTypeId);
        when(fileAsset.getHost()).thenReturn(hostId);
        when(fileAsset.getMap()).thenReturn(contentMap);
        when(fileAsset.getInode()).thenReturn(contentTypeInode);

        final ConnectException connectException = new ConnectException("ConnectException");
        final DotRuntimeException exception = new DotRuntimeException(connectException);

        when(
                contAPI.search(luceneQuery, -1, 0, null , user, respectFrontendRoles)
        ).thenThrow(exception);

        when(fileAssetFactory.findFileAssetsByFolderInDB(parentFolder, user, false))
                .thenReturn(contentlets);

        when(perAPI.filterCollection(contentlets, PermissionAPI.PERMISSION_READ, respectFrontendRoles, user))
                .thenReturn(contentlets);

        final List<FileAsset> fileAssetsByFolder = fileAssetAPI
                .findFileAssetsByFolder(parentFolder, user, respectFrontendRoles);

        verify(fileAssetFactory, times(1)).findFileAssetsByFolderInDB(parentFolder, user, false);

        assertEquals(1,  contentlets.size());
        assertEquals(contentTypeId, fileAssetsByFolder.get(0).getContentTypeId());
        assertEquals(hostId, fileAssetsByFolder.get(0).getHost());

        verify(contAPI, times(1)).copyProperties(fileAssetsByFolder.get(0), contentMap);
        verify(contentletCache, times(1)).add(fileAssetsByFolder.get(0));
    }
}

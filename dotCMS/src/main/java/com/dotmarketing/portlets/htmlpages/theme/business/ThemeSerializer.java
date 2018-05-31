package com.dotmarketing.portlets.htmlpages.theme.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

import java.io.IOException;

/**
 * JsonSerializer of Theme {@link com.dotmarketing.portlets.contentlet.model.Contentlet}
 */
public class ThemeSerializer extends JsonSerializer<Contentlet> {

    private final HostAPI hostAPI;
    private final FileAssetAPI fileAssetAPI;
    private final FolderAPI folderAPI;
    private final User systemUser;
    private static final String THEME_PNG = "theme.png";

    public ThemeSerializer(final HostAPI hostAPI, final FolderAPI folderAPI, final UserAPI userAPI, final FileAssetAPI fileAssetAPI) {

        this.hostAPI      = hostAPI;
        this.folderAPI    = folderAPI;
        this.fileAssetAPI = fileAssetAPI;

        try {
            this.systemUser = userAPI.getSystemUser();
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public void serialize(final Contentlet contentlet, final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        final String folderId = contentlet.getFolder();
        final String themeHostId = contentlet.getHost();

        try {
            final Host host = hostAPI.find(themeHostId, systemUser, false);
            final Folder folder = folderAPI.find(folderId, systemUser, false);

            final ImmutableMap.Builder<Object, Object> map = ImmutableMap.builder()
                    .put("name", folder.getName())
                    .put("title", folder.getTitle())
                    .put("inode", folder.getInode())
                    .put("host", host.getMap());

            if (fileAssetAPI.isFileAsset(contentlet)){
                FileAsset fa = fileAssetAPI.fromContentlet( contentlet );

                if (UtilMethods.isSet(fa.getFileName()) && fa.getFileName().equals(THEME_PNG)){
                    map.put("identifier", fa.getIdentifier());
                }
            }

            jsonGenerator.writeObject(map.build());

        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
}

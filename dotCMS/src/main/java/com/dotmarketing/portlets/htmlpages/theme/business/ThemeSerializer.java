package com.dotmarketing.portlets.htmlpages.theme.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

import java.io.IOException;

/**
 * JsonSerializer of Theme {@link com.dotmarketing.portlets.folders.model.Folder}
 */
public class ThemeSerializer extends JsonSerializer<Folder> {

    private final HostAPI hostAPI;
    private final User systemUser;


    public ThemeSerializer(final HostAPI hostAPI, final UserAPI userAPI) {

        this.hostAPI = hostAPI;

        try {
            this.systemUser = userAPI.getSystemUser();
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public void serialize(final Folder folder, final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {

        try {

            final Host host = hostAPI.find(folder.getHostId(), systemUser, false);

            final ImmutableMap.Builder<Object, Object> map = ImmutableMap.builder()
                    .put("name", folder.getName())
                    .put("title", folder.getTitle())
                    .put("inode", folder.getInode())
                    .put("identifier", folder.getIdentifier()!=null?folder.getIdentifier():"null")
                    .put("host", host.getMap());

            jsonGenerator.writeObject(map.build());

        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
}

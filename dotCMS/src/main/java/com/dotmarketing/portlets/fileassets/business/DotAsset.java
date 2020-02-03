package com.dotmarketing.portlets.fileassets.business;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * Encapsulates the instance of a type which is similar to the {@link FileAsset} but only requiries the binary field called asset to exists
 * @author jsanca
 */
public interface DotAsset extends Serializable, Versionable, Ruleable {

    String UNKNOWN_MIME_TYPE = "unknown";

    String getMetaData();

    void setMetaData(String metaData);

    String getParent();

    long getAssetSize();

    int getHeight();

    int getWidth();

    String getUnderlyingFileName();

    String getFileName();

    String getMimeType();

    InputStream getInputStream() throws IOException;

    void setBinary(String velocityVarName, File newFile)throws IOException;

    void setBinary(com.dotcms.contenttype.model.field.Field field, File newFile)throws IOException;

    File getFileAsset();

    boolean isDeleted() throws DotStateException, DotDataException, DotSecurityException;

    boolean isArchived() throws DotStateException, DotDataException, DotSecurityException;

    boolean isLive() throws DotStateException, DotDataException, DotSecurityException;

    boolean isLocked() throws DotStateException, DotDataException, DotSecurityException;

    String getType();

    String getExtension();

    Map<String, Object> getMap() throws DotRuntimeException;

    @Override
    String toString();
}

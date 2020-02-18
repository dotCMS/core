package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

import java.io.File;
import java.util.Optional;

/**
 * API to handle the @{@link com.dotcms.contenttype.model.type.DotAssetContentType}
 * @author jsanca
 */
public interface DotAssetAPI {

    /**
     * Represents a wildcard for all mime types
     */
    String ALL_MIME_TYPE = "*/*";

    /**
     * Represent the right part of the partial mime type such as
     * application/*, if a mime type ends with PARTIAL_MIME_TYPE is a partial mime type
     */
    String PARTIAL_MIME_TYPE = "/*";

    /**
     * Tries to match a @{@link com.dotcms.contenttype.model.type.BaseContentType#DOTASSET} which the "accept" field variable match the file mime type witch some of
     * the DotAsset Content Types
     * @param file {@link File} file is needed to figure out the mime type to try to match the dotasset
     * @param currentHost {@link Host} the dotAsset will be filtered by this host and the system host
     * @param user {@link User} the user will be used to filter the base content types.
     * @return Optional of ContentType, present if any match, empty if not.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<ContentType> tryMatch (final File file, final Host currentHost, final User user) throws DotDataException, DotSecurityException;

    /**
     * Tries to match a @{@link com.dotcms.contenttype.model.type.BaseContentType#DOTASSET} which the "accept" field variable match the file mime type witch some of
     * the DotAsset Content Types
     * @param mimeType {@link File} actual mime type to find the match
     * @param currentHost {@link Host} the dotAsset will be filtered by this host and the system host
     * @param user {@link User} the user will be used to filter the base content types.
     * @return Optional of ContentType, present if any match, empty if not.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<ContentType> tryMatch (final String mimeType, final Host currentHost, final User user) throws DotSecurityException, DotDataException;
}

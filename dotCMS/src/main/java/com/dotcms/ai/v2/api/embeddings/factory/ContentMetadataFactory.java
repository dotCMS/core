package com.dotcms.ai.v2.api.embeddings.factory;

import com.dotcms.ai.v2.api.dto.ContentMetadataDTO;
import com.dotmarketing.exception.DotDataException;

import java.sql.Connection;
import java.util.Optional;

/**
 * DAO for dot_content_metadata table.
 * Provides upsert and basic lookups
 * @author jsanca
 */
public interface ContentMetadataFactory {

    /**
     * Upserts a metadata row and returns the id (generated or existing).
     */
    long upsert(final ContentMetadataDTO contentMetadataDTO) throws DotDataException;

    /**
     * Upserts a metadata row and returns the id (generated or existing).
     */
    long upsert(final Connection connection, final ContentMetadataDTO contentMetadataDTO) throws DotDataException;

    /**
     * Finds a row by the unique business key (identifier, language, host, variant).
     */
    Optional<ContentMetadataDTO> findUnique(final ContentMetadataInput contentMetadataInput)
            throws DotDataException;

    /**
     * Deletes by id.
     */
    int deleteById(final long id) throws DotDataException;

}

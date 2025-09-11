package com.dotcms.ai.v2.api.embeddings.factory;

import com.dotmarketing.exception.DotDataException;

import java.sql.Connection;

/**
 * DAO for dot_embeddings.
 * Provides insert/upsert and cleanup helpers.
 * @author jsanca
 */
public interface EmbeddingsFactory {

    /**
     * Inserts a new embedding row.
     * @return generated id
     */
    long insert(EmbeddingInput embeddingInput)
            throws DotDataException;

    /**
     * Inserts a new embedding row.
     * @return generated id
     */
    long upsert(EmbeddingInput embeddingInput)
            throws DotDataException;

    /**
     * Inserts a new embedding row.
     * @return generated id
     */
    long upsert(Connection connection, EmbeddingInput embeddingInput)
            throws DotDataException;

    /**
     * Inserts a new embedding row.
     * @return generated id
     */
    int deleteByMetadataId(long metadataId) throws DotDataException;
}

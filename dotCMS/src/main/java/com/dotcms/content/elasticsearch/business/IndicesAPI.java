package com.dotcms.content.elasticsearch.business;

import java.sql.Connection;


import com.dotmarketing.exception.DotDataException;
import java.util.Optional;

/**
 * An API to store and retrieve information about current Elastic Search Indices
 * 
 * @author Jorge Urdaneta
 */
public interface IndicesAPI {

    /**
     * Loads information about legacy indices.
     *
     * @return an {@code IndicesInfo} object containing details about the legacy indices
     * @throws DotDataException if there is an error while fetching the indices information
     */
    IndicesInfo loadLegacyIndices() throws DotDataException;

    /**
     * Loads information about legacy indices from the database connection provided.
     *
     * @param conn the database connection used to fetch the legacy indices information
     * @return an {@code IndicesInfo} object containing details about the legacy indices
     * @throws DotDataException if there is an error while fetching the indices information
     */
    IndicesInfo loadLegacyIndices(Connection conn) throws DotDataException;

    /**
     * Loads the new indices information.
     * @return
     * @throws DotDataException
     */
    Optional<IndicesInfo> loadIndices() throws DotDataException;

    /**
     * Updates the current indices information with the provided {@code IndicesInfo} object.
     * This method can be used to reassign or redefine indices details like live, working,
     * reindexing, or site search indices.
     *
     * @param newInfo the new indices information to set, encapsulated in an {@code IndicesInfo} object
     * @throws DotDataException if there is an error updating the indices information
     */
    void point(IndicesInfo newInfo) throws DotDataException;

}

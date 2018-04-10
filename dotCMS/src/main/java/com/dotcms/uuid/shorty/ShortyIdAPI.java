package com.dotcms.uuid.shorty;

import java.util.Optional;

/**
 * The Shorty encapsulates a way to get short ids (smallest than the common uuid) and also the inverse way (from the shorty to the long id)
 */
public interface ShortyIdAPI {

    /**
     * Convert a shorty from an optional ShortyId that contains the long id.
     * This will be by default ShortyType.CONTENT
     * @param shorty String
     * @return ShortyId
     */
    Optional<ShortyId> getShorty(String shorty);

    /**
     * Convert a shorty from an optional ShortyId that contains the long id.
     * @param shorty String
     * @param shortyType ShortyType
     * @return ShortyId
     */
    Optional<ShortyId> getShorty(String shorty, ShortyType shortyType);

    long getDbHits();

    void validShorty(final String test);

    String uuidIfy(String shorty);

    default ShortyId noShorty(String shorty){ 
        return  new ShortyId(shorty, ShortType.CACHE_MISS.toString(), ShortType.CACHE_MISS, ShortType.CACHE_MISS);
    }

    /**
     * Convert a long id to shorty representation
     * @param shorty String
     * @return String
     */
    String shortify(String shorty);

    String randomShorty();

    /**
     * Encapsulates the entities supported by shorty api.
     */
    enum ShortyType { // todo; rename to Shorty Input Type
        CONTENT, WORKFLOW_SCHEME, WORKFLOW_STEP, WORKFLOW_ACTION
    }

}

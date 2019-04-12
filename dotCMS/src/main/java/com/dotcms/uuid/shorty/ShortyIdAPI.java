package com.dotcms.uuid.shorty;

import java.util.Optional;

/**
 * The Shorty encapsulates a way to get short ids (smallest than the common uuid) and also the inverse way (from the shorty to the long id)
 */
public interface ShortyIdAPI {

    /**
     * Convert a shorty from an optional ShortyId that contains the long id.
     * This will be by default ShortyInputType.CONTENT
     * @param shorty String
     * @return ShortyId
     */
    Optional<ShortyId> getShorty(String shorty);

    /**
     * Convert a shorty from an optional ShortyId that contains the long id.
     * @param shorty String
     * @param shortyType ShortyInputType
     * @return ShortyId
     */
    Optional<ShortyId> getShorty(String shorty, ShortyInputType shortyType);

    long getDbHits();

    /**
     * Validates if a shorty is valid, basically if has valid characters
     * @param test {@link String}
     */
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
    enum ShortyInputType {
        CONTENT, WORKFLOW_SCHEME, WORKFLOW_STEP, WORKFLOW_ACTION
    }

}

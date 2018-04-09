package com.dotcms.uuid.shorty;

import java.util.Optional;

public interface ShortyIdAPI {

    /**
     * Convert a shorty from an optional ShortyId that contains the long id.
     * @param shorty String
     * @return ShortyId
     */
    Optional<ShortyId> getShorty(String shorty);

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

}

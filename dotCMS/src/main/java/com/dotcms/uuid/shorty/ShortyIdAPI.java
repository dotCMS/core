package com.dotcms.uuid.shorty;

import java.util.Optional;

public interface ShortyIdAPI {


    Optional<ShortyId> getShorty(String shorty);

    long getDbHits();

    void validShorty(final String test);

    String uuidIfy(String shorty);

    default ShortyId noShorty(String shorty){ 
        return  new ShortyId(shorty, ShortType.CACHE_MISS.toString(), ShortType.CACHE_MISS, ShortType.CACHE_MISS);
    }

    String shortify(String shorty);

}

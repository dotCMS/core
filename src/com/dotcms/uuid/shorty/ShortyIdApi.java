package com.dotcms.uuid.shorty;

import java.util.Optional;

public interface ShortyIdApi {


    Optional<ShortyId> getShorty(String shorty);

    default ShortyId noShorty(String shorty){ 
        return  new ShortyId(shorty, ShortType.CACHE_MISS.toString(), ShortType.CACHE_MISS, ShortType.CACHE_MISS);
    }

}

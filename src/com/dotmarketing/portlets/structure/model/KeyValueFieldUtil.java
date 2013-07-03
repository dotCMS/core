package com.dotmarketing.portlets.structure.model;

import com.dotmarketing.portlets.contentlet.business.ContentletCache;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class to handle Key Value field
 *
 * @author Roger
 */
public class KeyValueFieldUtil {

    private static final JsonFactory factory = new JsonFactory();
    private static final ObjectMapper mapper = new ObjectMapper( factory );

    public static Map<String, Object> JSONValueToHashMap ( final String json ) {

        LinkedHashMap<String, Object> keyValueMap = new LinkedHashMap<String, Object>();
        if ( UtilMethods.isSet( json ) ) {

            if ( json.equals( ContentletCache.CACHED_METADATA ) ) {

                /*
                 Contentlet.get(key) already verify for the metadata field if the data is cached in order to get the information from cache.

                 Anyone calling this method for metadata should do the same, verify if the data is cached and if it is load
                 that cached data before to call this method.
                 */
                Logger.error( KeyValueFieldUtil.class, "Trying to parse JSON content for cached Metadata, it is required first to search the data into the cache." );
                return keyValueMap;
            }
//			TypeReference<LinkedHashMap<String,Object>> typeRef = new TypeReference<LinkedHashMap<String,Object>>() {}; 
//			try {
//				keyValueMap = mapper.readValue(json, typeRef);
//			} catch (Exception e) {
//				//TODO
//			}

            Gson gson = new Gson();
            return gson.fromJson( json, new TypeToken<LinkedHashMap<String, String>>() {
            }.getType() );
        }
        return keyValueMap;
    }

}
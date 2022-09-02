package com.dotcms.util.transform;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.db.DbConnectionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import org.postgresql.util.PGobject;

/**
 * Utility class to convert from a JSON read from DB to the corresponding Java Class.
 */

public class DBColumnToJSONConverter {

    public static <T> T getObjectFromDBJson(final Object jsonInDB, final Class<T> clazzToMap) {
        final ObjectMapper mapper = DotObjectMapperProvider.getInstance()
                .getDefaultObjectMapper();

        if(DbConnectionFactory.isPostgres()) {
            PGobject json = (PGobject) jsonInDB;
            return Try.of(()->mapper.readValue(json.getValue(), clazzToMap))
                    .getOrNull();
        } else  {
            return Try.of(()->mapper.readValue((String) jsonInDB, clazzToMap))
                    .getOrNull();
        }
    }
}

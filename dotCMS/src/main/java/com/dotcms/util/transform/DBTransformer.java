package com.dotcms.util.transform;

import com.dotcms.experiments.model.Scheduling;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.db.DbConnectionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import java.util.List;
import org.postgresql.util.PGobject;

/**
 * Interface that contains the definition that a transformer needs to implement in order to convert
 * DB objects into entities(eg. Folder, Template, Containers)
 * @param <T>
 */
public interface DBTransformer<T> {

    /**
     * @return List of converted objects
     */
    List<T> asList();

    default T findFirst() {
        return this.asList().stream().findFirst().orElse(null);
    }

}

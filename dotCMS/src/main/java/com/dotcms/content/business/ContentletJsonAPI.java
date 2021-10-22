package com.dotcms.content.business;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 */
public interface ContentletJsonAPI {

    String SAVE_CONTENTLET_AS_JSON = "persist.contentlet.as.json";

    String SAVE_CONTENTLET_AS_COLUMNS =  "persist.contentlet.as.columns";

    String CONTENTLET_AS_JSON = "contentlet_as_json";

    /**
     * Legacy Regular contentlet to json string
     * @param contentlet
     * @return
     * @throws JsonProcessingException
     * @throws DotDataException
     */
    String toJson(final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet)
            throws JsonProcessingException, DotDataException;

    /**
     * Json String to Regular Contentlet
     * @param json
     * @return
     * @throws JsonProcessingException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    com.dotmarketing.portlets.contentlet.model.Contentlet mapContentletFieldsFromJson(final String json)
            throws JsonProcessingException, DotDataException, DotSecurityException;

    /**
     * This basically tells Weather or not we support saving content as json and we have not turned it off
     * @return
     */
    default boolean isPersistContentAsJson(){
        return DbConnectionFactory.isPostgres() && Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
    }

    /**
     * if we do not support content as json this has to return true
     * @return
     */
    default boolean isPersistContentletInColumns(){
        return Config.getBooleanProperty(SAVE_CONTENTLET_AS_COLUMNS, !isPersistContentAsJson());
    }

}

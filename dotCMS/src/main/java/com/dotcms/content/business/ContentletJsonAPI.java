package com.dotcms.content.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface ContentletJsonAPI {

    String SAVE_CONTENTLET_AS_JSON = "save.contentlet.as.json";

    String CONTENTLET_AS_JSON = "contentlet_as_json";

    String toJson(final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet)
            throws JsonProcessingException, DotDataException;

    com.dotmarketing.portlets.contentlet.model.Contentlet mapContentletFieldsFromJson(final String json)
            throws JsonProcessingException, DotDataException, DotSecurityException;

}

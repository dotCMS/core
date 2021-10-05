package com.dotcms.content.business;

import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface ContentletJsonAPI {

    String SAVE_CONTENTLET_AS_JSON = "save.contentlet.as.json";

    String CONTENTLET_AS_JSON = "content_as_json";

    String toJson(final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet)
            throws JsonProcessingException, DotDataException;



}

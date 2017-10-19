package com.dotcms.rest.api.v1.page;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provides the appropriate JSON mapping configuration for the JSON representation of an HTML Page.
 *
 * @author Will Ezell
 * @version 4.2
 * @since Oct 9, 2017
 */
public class JsonMapper {

    private JsonMapper() {

    }

    static final ObjectMapper mapper = new ObjectMapper()
            .addMixIn(Permissionable.class, PermissionableMixIn.class)
            .addMixIn(Contentlet.class, ContentletMixIn.class)
            .addMixIn(HTMLPageAsset.class, ContentletMixIn.class)
            .addMixIn(Host.class, ContentletMixIn.class);

}

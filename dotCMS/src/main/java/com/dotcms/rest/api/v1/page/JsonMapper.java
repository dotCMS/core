package com.dotcms.rest.api.v1.page;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMapper {

    static final ObjectMapper mapper = new ObjectMapper()
            .addMixIn(Permissionable.class, PermissionableMixIn.class)
            .addMixIn(Contentlet.class, ContentletMixIn.class)
            .addMixIn(HTMLPageAsset.class, ContentletMixIn.class)
            .addMixIn(Host.class, ContentletMixIn.class);

}

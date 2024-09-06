package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;


/**
 * Provides the appropriate JSON mapping configuration for the JSON representation of an HTML Page.
 *
 * @author Will Ezell
 * @version 4.2
 * @since Oct 9, 2017
 */
public class JsonMapper {

    public static final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    static {
        mapper.addMixInAnnotations(Permissionable.class, PermissionableMixIn.class);
        mapper.addMixInAnnotations(Contentlet.class, ContentletMixIn.class);
        mapper.addMixInAnnotations(HTMLPageAsset.class, ContentletMixIn.class);
        mapper.addMixInAnnotations(Host.class, ContentletMixIn.class);
        mapper.addMixInAnnotations(Template.class, WebAssetMixIn.class);
        mapper.addMixInAnnotations(Container.class, WebAssetMixIn.class);
    }

    private JsonMapper() {

    }

}

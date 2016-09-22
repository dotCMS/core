package com.dotcms.util.marshal;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.Visibility;
import com.dotcms.repackage.com.google.gson.*;
import com.dotcms.util.deserializer.ContentletDeserializer;
import com.dotcms.util.deserializer.PayloadDeserializer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import static com.dotcms.util.ReflectionUtils.getClassFor;

import java.lang.reflect.Type;

/**
 * Encapsulates some of the custom type serializer and deserializer for dotcms own types.
 * @author jsanca
 */
public class CustomDotCmsTypeGsonConfigurator implements GsonConfigurator {

    @Override
    public void configure(final GsonBuilder gsonBuilder) {

        gsonBuilder.registerTypeAdapter( Payload.class, new PayloadDeserializer() );
        gsonBuilder.registerTypeAdapter(Contentlet.class, new ContentletDeserializer() );
    }

    @Override
    public boolean excludeDefaultConfiguration() {

        return false;
    }
} // E:O:F:CustomDotCmsTypeGsonConfigurator.

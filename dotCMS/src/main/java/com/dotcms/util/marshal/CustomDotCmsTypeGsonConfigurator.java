package com.dotcms.util.marshal;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import com.dotcms.api.system.event.Payload;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.deserializer.ImmutableTypeAdapter;
import com.dotcms.api.system.event.verifier.ExcludeOwnerVerifierBean;
import com.dotcms.util.deserializer.ContentletDeserializer;
import com.dotcms.util.deserializer.ExcludeOwnerVerifierAdapter;
import com.dotcms.util.deserializer.PayloadAdapter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;

import java.util.ServiceLoader;

/**
 * Encapsulates some of the custom type serializer and deserializer for dotcms own types.
 * @author jsanca
 */
public class CustomDotCmsTypeGsonConfigurator implements GsonConfigurator {

    @Override
    public void configure(final GsonBuilder gsonBuilder) {

        gsonBuilder.registerTypeAdapter( Payload.class, new PayloadAdapter() );
        gsonBuilder.registerTypeAdapter(ExcludeOwnerVerifierBean.class, new ExcludeOwnerVerifierAdapter());
        gsonBuilder.registerTypeAdapter(Contentlet.class, new ContentletDeserializer() ); // todo: for 4.2 use just one instance for all of them.
        gsonBuilder.registerTypeAdapter(HTMLPageAsset.class, new ContentletDeserializer() );
        gsonBuilder.registerTypeAdapter(Host.class, new ContentletDeserializer() );

        // Immutables.io:  Type Adapter registration
        gsonBuilder.registerTypeAdapter(ContentType.class, new ImmutableTypeAdapter<ContentType>(gsonBuilder) );
        for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
            gsonBuilder.registerTypeAdapter(factory.getClass(),  new ImmutableTypeAdapter<>(gsonBuilder) );
        }
    }

    @Override
    public boolean excludeDefaultConfiguration() {

        return false;
    }
} // E:O:F:CustomDotCmsTypeGsonConfigurator.

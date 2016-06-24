package com.dotmarketing.util.marshal;

import com.dotcms.repackage.com.google.gson.*;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ReflectionUtils;
import com.dotmarketing.util.jwt.JsonWebTokenService;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Date;

/**
 * Marshal Factory, provides the default implementation for the MarhalUtils
 * @author jsanca
 */
public class MarshalFactory implements Serializable {

    private static MarshalFactory instance = null;

    private MarshalUtils marshalUtils = null;

    /**
     * In order to set a custom gson configuration in case you use the gon
     */
    public static final String GSON_CONFIGURATOR_KEY = "gson.configurator";

    /**
     * Get the  marshal factory implementation from the dotmarketing-config.properties
     */
    public static final String MARSHAL_FACTORY_KEY = "marshal.factory";

    private MarshalFactory () {
        // singleton
    }

    /**
     * Get the instance.
     * @return MarshalFactory
     */
    public static MarshalFactory getInstance() {

        if (instance == null) {
            // Thread Safe. Might be costly operation in some case
            synchronized (MarshalFactory.class) {

                if (instance == null) {
                    instance = new MarshalFactory();
                }
            }
        }

        return instance;
    } // getInstance.

    /**
     * Get the default marshal implementation
     * @return MarshalUtils
     */
    public MarshalUtils getMarshalUtils () {

        String marshalFactoryClass = null;

        if (null == this.marshalUtils) {

            synchronized (MarshalFactory.class) {

                if (null == this.marshalUtils) { // by default we use Gson, but eventually we can introduce a key on the dotmarketing-config.properties to use a custom one.

                    marshalFactoryClass =
                            Config.getStringProperty
                                    (MARSHAL_FACTORY_KEY, null);

                    if (null != marshalFactoryClass && !"null".equals(marshalFactoryClass)) {

                        if (Logger.isDebugEnabled(JsonWebTokenService.class)) {

                            Logger.debug(JsonWebTokenService.class,
                                    "Using the singning key factory class: " + marshalFactoryClass);
                        }

                        this.marshalUtils =
                                (MarshalUtils) ReflectionUtils.newInstance(marshalFactoryClass);

                        if (null == this.marshalUtils) {

                            if (Logger.isDebugEnabled(JsonWebTokenService.class)) {

                                Logger.debug(JsonWebTokenService.class,
                                        "Could not used this class: " + marshalFactoryClass +
                                        ", using the default implementations");
                            }

                            this.marshalUtils =
                                    new GsonMarshalUtils();
                        }
                    } else {

                        this.marshalUtils =
                                new GsonMarshalUtils();
                    }
                }
            }
        }

        return this.marshalUtils;
    } // getMarshalUtils.

    /**
     * Gson Implementation
     */
    private class GsonMarshalUtils implements MarshalUtils {

        private final Gson gson;


        GsonMarshalUtils () {

            final GsonBuilder gsonBuilder =
                    new GsonBuilder();

            this.configure(gsonBuilder);

            this.gson = gsonBuilder.create();
        }

        private void configure (final GsonBuilder gsonBuilder) {

            GsonConfigurator gsonConfigurator  = null;
            final String gsonConfiguratorClass =
                    Config.getStringProperty(GSON_CONFIGURATOR_KEY, null);

            if (null != gsonBuilder) {

                if (null != gsonConfiguratorClass && !"null".equals(gsonConfiguratorClass)) {
                    gsonConfigurator =
                            (GsonConfigurator) ReflectionUtils.newInstance(gsonConfiguratorClass);
                }

                if (null != gsonConfigurator) {

                    if (!gsonConfigurator.excludeDefaultConfiguration()) {

                        this.customConfiguration(gsonBuilder);
                    }

                    gsonConfigurator.configure(gsonBuilder);
                } else {

                    this.customConfiguration(gsonBuilder);
                }

            } else {

                throw new IllegalArgumentException("GsonBuilder can not be null");
            }
        }

        private void customConfiguration (final GsonBuilder gsonBuilder) {

            if (ConfigUtils.isDevMode()) {

                gsonBuilder.setPrettyPrinting(); // should be this only on dev mode.
            }

            // do not want escaping??? gsonBuilder.disableHtmlEscaping();
            // wants nulls? gsonBuilder.serializeNulls();

            gsonBuilder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {


                @Override
                public Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                    return new Date(jsonElement.getAsLong());
                }
            });

            gsonBuilder.registerTypeAdapter(Date.class, new JsonSerializer<Date>() {


                @Override
                public JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {

                    return new JsonPrimitive(date.getTime());
                }
            });
        }

        @Override
        public String marshal(final Object object) {

            return this.gson.toJson(object);
        }

        @Override
        public void marshal(final Writer writer, final Object object) {

            this.gson.toJson(object, writer);
        }

        @Override
        public <T> T unmarshal(final String s, final Class<? extends T> clazz) {

            return this.gson.fromJson(s, clazz);
        }

        @Override
        public <T> T unmarshal(final Reader reader, final Class<? extends T> clazz) {

            return this.gson.fromJson(reader, clazz);
        }

        @Override
        public <T> T unmarshal(final InputStream inputStream, final Class<T> clazz) {

            return this.gson.fromJson(new InputStreamReader(inputStream), clazz);
        }
    } // GsonMarshalUtils.

} // E:O:F:MarshalFactory.

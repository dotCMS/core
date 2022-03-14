package com.dotcms.util.marshal;

import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.io.Serializable;

/**
 * Marshal Factory, provides the default implementation for the MarhalUtils
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 14, 2016
 */
public class MarshalFactory implements Serializable {

    /**
     * Used to keep the instance of the MarshalUtils.
     * Should be volatile to avoid thread-caching
     */
    private volatile MarshalUtils marshalUtils = null;

    /**
     * Get the  marshal class implementation from the dotmarketing-config.properties
     */
    public static final String MARSHAL_IMPLEMENTATION_KEY = "marshal.implementation";

    private MarshalFactory() {
        // singleton
    }

    private static class SingletonHolder {
        private static final MarshalFactory INSTANCE = new MarshalFactory();
    }


    /**
     * Get the instance.
     * @return MarshalFactory
     */
    public static MarshalFactory getInstance() {

        return SingletonHolder.INSTANCE;
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
                                    (MARSHAL_IMPLEMENTATION_KEY, null);

                    if (UtilMethods.isSet(marshalFactoryClass)) {

                        if (Logger.isDebugEnabled(MarshalFactory.class)) {

                            Logger.debug(MarshalFactory.class,
                                    "Using the marshall class: " + marshalFactoryClass);
                        }

                        this.marshalUtils =
                                (MarshalUtils) ReflectionUtils.newInstance(marshalFactoryClass);

                        if (null == this.marshalUtils) {

                            if (Logger.isDebugEnabled(MarshalFactory.class)) {

                                Logger.debug(MarshalFactory.class,
                                        "Could not used this class: " + marshalFactoryClass +
                                        ", using the default Gson implementation");
                            }

                            this.marshalUtils =
                                    new JacksonMarshalUtilsImpl();
                        }
                    } else {

                        this.marshalUtils =
                                new JacksonMarshalUtilsImpl();
                    }
                }
            }
        }

        return this.marshalUtils;
    } // getMarshalUtils.

} // E:O:F:MarshalFactory.

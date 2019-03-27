package com.dotcms.rest.api.v1;

import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

/**
 * Just a wrapper for the object mapper to do marshalling stuff from json to object and viceversa.
 * Note: this class has been made to be used only on Resources, since it is the same mechanism that resources use for marshall {@link com.dotcms.repackage.javax.ws.rs.core.Response}
 * however if you need to convert from Json or vicerversa on the app, please see {@link com.dotcms.util.marshal.MarshalUtils}
 * @author jsanca
 */
public class ResourceMarshaller {

    private final ObjectMapper defaultObjectMapper;

    private ResourceMarshaller() {

        this(DotObjectMapperProvider.getInstance().getDefaultObjectMapper());
    }

    @VisibleForTesting
    protected ResourceMarshaller(final ObjectMapper defaultObjectMapper) {

        this.defaultObjectMapper = defaultObjectMapper;
    }

    private static class SingletonHolder {
        private static final ResourceMarshaller INSTANCE = new ResourceMarshaller();
    }

    /**
     * Get the instance.
     * @return ResourceMarshaller
     */
    public static ResourceMarshaller getInstance() {

        return ResourceMarshaller.SingletonHolder.INSTANCE;
    } // getInstance.


    /**
     * Marshall an string to Object, null if it couldn't be converted
     * @param string {@link CharSequence}
     * @param aClass {@link Class}
     * @param <T>
     * @return T
     */
    public <T> T toObject(final CharSequence string, final Class<? extends T> aClass)  {

        return (UtilMethods.isSet(string))?
                this.toObject(new StringReader(string.toString()), aClass):null;
    } // toObject.

    /**
     * Marshall a reader to Object, null if it couldn't be converted
     * @param reader {@link Reader}
     * @param aClass {@link Class}
     * @param <T>
     * @return T
     */
    public <T> T toObject(final Reader reader, final Class<? extends T> aClass)  {

        T t = null;

        try {

            t = this.defaultObjectMapper.readValue(reader, aClass);
        } catch (IOException e) {

            Logger.error(this, e.getMessage(), e);
        }

        return t;
    } // toObject.

    /**
     * Marshall an input stream to Object, null if it couldn't be converted
     * @param inputStream {@link InputStream}
     * @param aClass {@link Class}
     * @param <T>
     * @return T
     */
    public <T> T toObject(final InputStream inputStream, final Class<T> aClass)  {

        T t = null;

        try {

            t = this.defaultObjectMapper.readValue(inputStream, aClass);
        } catch (IOException e) {

            Logger.error(this, e.getMessage(), e);
        }

        return t;
    } // toObject.
} // E:O:F:ResourceMarshaller.

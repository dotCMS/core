package com.dotcms.util.marshal;

import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;

/**
 * Encapsulates the logic to marshal an object to JSON and viceversa.
 *
 * @author jsanca
 * @version 3.7
 * @since Jun 14, 2016
 */
public interface MarshalUtils extends Serializable {

    /**
     * Marshal an object to json
     * @param object Object
     */
    public String marshal    (final Object object);

    /**
     * Marshal an object to json using the writer
     * @param writer Writer
     * @param object Object
     */
    public void marshal    (final Writer writer, final Object object);

    /**
     * Un marshal from a reader to a object with the clazz type
     * @param s String
     * @param clazz Class
     * @param <T>
     * @return T
     */
    public <T> T unmarshal (final String s, Class<? extends T> clazz);

    /**
     * Un marshal from a reader to a object with the clazz type
     * @param reader Reader
     * @param clazz Class
     * @param <T>
     * @return T
     */
    public <T> T unmarshal (final Reader reader, Class<? extends T> clazz);

    /**
     * Un marshal from an input stream to a object with the clazz type
     * @param inputStream InputStream
     * @param clazz Class
     * @param <T>
     * @return T
     */
    public <T> T unmarshal (final InputStream inputStream, final Class<T> clazz);


} // E:O:F:MarshalUtils.

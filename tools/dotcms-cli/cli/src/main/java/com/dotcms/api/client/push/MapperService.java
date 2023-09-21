package com.dotcms.api.client.push;

import com.dotcms.api.client.push.exception.MappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

/**
 * MapperService is an interface that provides a method to map a File to an object of a given
 * class.
 */
public interface MapperService {

    /**
     * Returns an instance of ObjectMapper based on the input file format.
     *
     * @param file the file to be processed
     * @return an instance of ObjectMapper for processing the given file
     */
    ObjectMapper objectMapper(final File file);

    /**
     * Returns an instance of ObjectMapper for general processing.
     *
     * @return an instance of ObjectMapper for general processing
     */
    ObjectMapper objectMapper();

    /**
     * Maps the contents of a file to an instance of the given class.
     *
     * @param file  the file to read and map
     * @param clazz the class to map the file contents to
     * @return the mapped instance of the given class
     * @throws MappingException if there is an error during the mapping process
     */
    <T> T map(final File file, Class<T> clazz) throws MappingException;

}

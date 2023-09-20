package com.dotcms.api.client.push;

import com.dotcms.api.client.push.exception.MappingException;
import java.io.File;

/**
 * MapperService is an interface that provides a method to map a File to an object of a given
 * class.
 */
public interface MapperService {

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

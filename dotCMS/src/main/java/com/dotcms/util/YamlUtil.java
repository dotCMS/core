package com.dotcms.util;

import com.dotmarketing.exception.DotRuntimeException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Yaml Utils to parse and save yaml files
 * @author jsanca
 */
public class YamlUtil {

    private final static ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory())
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .findAndRegisterModules();

    /**
     * Parse the file in order to convert to T object
     * @param file {@link File}
     * @param tClass T class to convert
     * @param <T>
     * @return Object Yaml converted
     */
    public static <T> T parse(final File file, final Class<T> tClass) {

        return parse(Paths.get(file.getPath()), tClass);
    }

    /**
     * Parse the path in order to convert to T object
     * @param path {@link Path}
     * @param tClass T class to convert
     * @param <T>
     * @return Object Yaml converted
     */
    public static <T> T parse(final Path path, final Class<T> tClass) {

        try(final InputStream inputStream = Files.newInputStream(path)) {

            return parse(inputStream, tClass);
        }catch (Exception e){
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Parse the input stream in order to convert to T object
     * @param inputStream {@link InputStream}
     * @param tClass T class to convert
     * @param <T>
     * @return Object Yaml converted
     */
    public static <T> T parse(final InputStream inputStream, final Class<T> tClass) {

        try {
            return ymlMapper
                    .readValue(inputStream, tClass);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Write into a file a Yaml
     * @param file {@link File} destiny file to save the yaml
     * @param yaml T yaml file
     * @param <T>
     */
    public static <T> void write (final File file, final T yaml) {

        try {
            ymlMapper.writeValue(file, yaml);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

}
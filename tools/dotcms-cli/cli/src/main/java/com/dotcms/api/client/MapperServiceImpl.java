package com.dotcms.api.client;

import com.dotcms.api.client.exception.MappingException;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.api.provider.YAMLMapperSupplier;
import com.dotcms.cli.common.InputOutputFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.DefaultBean;
import java.io.File;
import java.io.IOException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * The {@code MapperServiceImpl} class implements the {@code MapperService} interface and provides
 * methods for mapping data from a file to an object using different formats.
 * <p>
 * This class is annotated with {@code @DefaultBean} and {@code @Dependent} to indicate that it is
 * the default implementation of the {@code MapperService} interface and that it belongs to the
 * dependent scope.
 */
@DefaultBean
@Dependent
public class MapperServiceImpl implements MapperService {

    @Inject
    Logger logger;

    /**
     * Returns an instance of ObjectMapper based on the input file format.
     *
     * @param file The file from which the input will be read.
     * @return An instance of ObjectMapper for the given input file format.
     */
    @ActivateRequestContext
    public ObjectMapper objectMapper(final File file) {

        if (null == file) {
            var message = "Trying to obtain ObjectMapper with null file";
            logger.error(message);
            throw new MappingException(message);
        }

        InputOutputFormat inputOutputFormat;
        if (isJSONFile(file)) {
            inputOutputFormat = InputOutputFormat.JSON;
        } else {
            inputOutputFormat = InputOutputFormat.YML;
        }

        return objectMapper(inputOutputFormat);
    }

    /**
     * Returns an instance of ObjectMapper based on the specified input/output format.
     *
     * @param inputOutputFormat The input/output format for which the ObjectMapper will be
     *                          returned.
     * @return An instance of ObjectMapper for the given input/output format.
     */
    @ActivateRequestContext
    public ObjectMapper objectMapper(final InputOutputFormat inputOutputFormat) {

        if (inputOutputFormat == InputOutputFormat.JSON) {
            return new ClientObjectMapper().getContext(null);
        }

        return new YAMLMapperSupplier().get();
    }

    /**
     * Returns an instance of ObjectMapper with default input/output format YAML.
     *
     * @return An instance of ObjectMapper with default input/output format YAML.
     */
    @ActivateRequestContext
    public ObjectMapper objectMapper() {
        return objectMapper(InputOutputFormat.YAML);
    }

    /**
     * Maps the given file to an object of the specified class.
     *
     * @param file  The file to be mapped.
     * @param clazz The class of the object to be mapped to.
     * @return The mapped object.
     * @throws MappingException If there is an error mapping the file.
     */
    @ActivateRequestContext
    public <T> T map(final File file, Class<T> clazz) throws MappingException {

        if (null == file) {
            var message = String.format("Trying to map empty file for type [%s]", clazz.getName());
            logger.error(message);
            throw new MappingException(message);
        }

        try {
            ObjectMapper objectMapper = objectMapper(file);
            return objectMapper.readValue(file, clazz);
        } catch (IOException e) {

            var message = String.format("Error mapping file [%s] for type [%s]",
                    file.getAbsolutePath(), clazz.getName());
            logger.error(message, e);

            throw new MappingException(message, e);
        }
    }

    /**
     * Checks if the given file is a JSON file.
     *
     * @param file The file to be checked.
     * @return True if the file is a JSON file, false otherwise.
     */
    private boolean isJSONFile(final File file) {
        return file.getName().toLowerCase().endsWith(".json");
    }

}

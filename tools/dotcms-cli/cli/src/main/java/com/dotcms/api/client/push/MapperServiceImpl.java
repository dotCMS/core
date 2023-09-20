package com.dotcms.api.client.push;

import com.dotcms.api.client.push.exception.MappingException;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.api.provider.YAMLMapperSupplier;
import com.dotcms.cli.common.InputOutputFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.DefaultBean;
import java.io.File;
import java.io.IOException;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
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
            ObjectMapper objectMapper;
            InputOutputFormat inputOutputFormat;

            if (isJSONFile(file)) {
                inputOutputFormat = InputOutputFormat.JSON;
            } else {
                inputOutputFormat = InputOutputFormat.YML;
            }

            if (inputOutputFormat == InputOutputFormat.JSON) {
                objectMapper = new ClientObjectMapper().getContext(null);
            } else {
                objectMapper = new YAMLMapperSupplier().get();
            }

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

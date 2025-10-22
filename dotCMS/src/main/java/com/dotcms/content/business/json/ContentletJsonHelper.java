package com.dotcms.content.business.json;

import com.dotcms.content.model.Contentlet;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Contentlet Json related logic falls here
 * In case we need to parse stuff outside an API
 * For example: We do not use APIs in our UpgradeTasks etc..
 */
public class ContentletJsonHelper {

    /**
     * Jackson mapper configuration and lazy initialized instance.
     */
    private final Lazy<ObjectMapper> objectMapper = Lazy.of(() -> {
        return DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
    });

    /**
     * Short hand to parse the json but access directly the field of interest.
     * Beware: The Type specified on the Optional is the expected type returned in the field if the conversion fails the optional comes back empty
     * Be mindful of the expected type.
     * @param jsonInput
     * @param fieldName
     * @param <R>
     * @return
     */
    public <R> Optional<R> fieldValue(final String jsonInput, final String fieldName){
        final R fieldValue = Try.of(()-> (R)immutableFromJson(jsonInput).fields().get(fieldName).value()).getOrNull();
        return Optional.ofNullable(fieldValue);
    }

    /**
     *
     * @param json
     * @return
     * @throws JsonProcessingException
     */
    public Contentlet immutableFromJson(final String json) throws JsonProcessingException {
        return objectMapper.get().readValue(json, Contentlet.class);
    }

    /**
     * Loads a list of {@link Contentlet} from a json file
     * @param file
     * @return {@link List<Contentlet>}
     * @throws JsonProcessingException
     */
    public List<Contentlet> readContentletListFromJsonFile(final File file){
        try(BufferedInputStream input = new BufferedInputStream(Files.newInputStream(file.toPath()))){
            return objectMapper.get().readValue(input, new TypeReference<>(){});
        } catch (IOException e) {
            Logger.error(this, "Error reading file " + file.getAbsolutePath(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Serializes a list of {@link Contentlet} to JSON and saves it to a file
     * @param contentletList {@link Contentlet}
     * @param output {@link File}
     * @throws IOException
     */
    public void writeContentletListToFile(final List<Contentlet> contentletList, final File output) throws IOException {
        objectMapper.get().writeValue(output, contentletList);
    }

    /**
     *
     * @param object
     * @return
     * @throws JsonProcessingException
     */
    public String writeAsString(final Object object) throws JsonProcessingException {
        return objectMapper.get().writeValueAsString(object);
    }

    /**
     * Returns the current instance of the JSON Object Mapper class.
     *
     * @return The instance of the {@link ObjectMapper} class.
     */
    public ObjectMapper objectMapper() {
        return this.objectMapper.get();
    }

    public enum INSTANCE {
        INSTANCE;
        private final ContentletJsonHelper helper = new ContentletJsonHelper();

        public static ContentletJsonHelper get() {
            return INSTANCE.helper;
        }

    }

}

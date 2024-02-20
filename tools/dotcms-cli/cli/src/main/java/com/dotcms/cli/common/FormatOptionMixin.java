package com.dotcms.cli.common;

import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.api.provider.YAMLMapperSupplier;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import picocli.CommandLine;

public class FormatOptionMixin {

    @CommandLine.Option(names = {"-fmt", "--format"}, description = "Enum values: ${COMPLETION-CANDIDATES}")
    InputOutputFormat inputOutputFormat = InputOutputFormat.defaultFormat();

    public ObjectMapper objectMapper(final File file) {

        if (null != file){
            if (isJSONFile(file)){
                inputOutputFormat = InputOutputFormat.JSON;
            } else {
                inputOutputFormat = InputOutputFormat.YML;
            }
        }

        ObjectMapper objectMapper;
        if (inputOutputFormat == InputOutputFormat.JSON) {
            objectMapper = new ClientObjectMapper().getContext(null);
        } else {
            objectMapper = new YAMLMapperSupplier().get();
        }

        return objectMapper;
    }

    public ObjectMapper objectMapper() {
        return objectMapper(null);
    }

    private boolean isJSONFile(final File file){
        return file.getName().toLowerCase().endsWith(".json");
    }

    public InputOutputFormat getInputOutputFormat() {
        return inputOutputFormat;
    }
}

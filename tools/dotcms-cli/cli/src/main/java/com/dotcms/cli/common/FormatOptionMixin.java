package com.dotcms.cli.common;

import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.api.provider.YAMLMapperSupplier;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;

public class FormatOptionMixin {

    @CommandLine.Option(names = {"-fmt", "--format"}, description = "Enum values: ${COMPLETION-CANDIDATES}")
    InputOutputFormat inputOutputFormat = InputOutputFormat.defaultFormat();

    private ObjectMapper objectMapper;

    public ObjectMapper objectMapper() {
        if (null != objectMapper) {
            return objectMapper;
        }

        if (inputOutputFormat == InputOutputFormat.JSON) {
            objectMapper = new ClientObjectMapper().getContext(null);
        } else {
            objectMapper = new YAMLMapperSupplier().get();
        }

        return objectMapper;
    }

    public InputOutputFormat getInputOutputFormat() {
        return inputOutputFormat;
    }
}

package com.dotmarketing.portlets.contentlet.business.json;

import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.json.transfomer.DateFieldTransformer;
import com.dotmarketing.portlets.contentlet.business.json.transfomer.TextFieldTransformer;
import com.dotmarketing.portlets.contentlet.business.json.types.DataTypeSerializer;
import com.dotmarketing.portlets.contentlet.business.json.types.SerializerProvider;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SchemelessContentJsonSupport {

    final List<String> systemFieldNames = ImmutableList
            .of("inode", "identifier", "contentType", "baseType", "modDate", "modUser",
                    "showOnMenu", "languageId", "sortOrder", "editState", "friendlyName");

    private final ImmutableMap<String, JsonFieldTransformer<?, ?>> transformers = ImmutableMap
            .<String, JsonFieldTransformer<?, ?>>builder()
            .put(TextField.class.getSimpleName(), new TextFieldTransformer())
            .put(DateField.class.getSimpleName(), new DateFieldTransformer())
            //.put(DateTimeField.class.getSimpleName(),new DateFieldTransformer())
            //.put(TimeField.class.getSimpleName(),new DateFieldTransformer())
            .build();


    public Optional<String> toJson(final Contentlet contentlet) throws DotDataException {

        if (!UtilMethods.isSet(contentlet.get("identifier")) || !UtilMethods
                .isSet(contentlet.get("inode"))) {
            return Optional.empty();
        }

        final ObjectMapper objectMapper = createMapper();

        final Map<String, Serializable> source = new HashMap<>();

        source.put("dotSystemFields", getSystemFields(contentlet));

        for (final Field field : contentlet.getContentType().fields()) {
            final Serializable value = (Serializable) contentlet.get(field.variable());
            if (null != value) {
                final JsonFieldTransformer transformer = transformers
                        .get(field.type().getSimpleName());
                if (null != transformer) {
                    source.putAll(transformer.toJsonMap(field, value));
                }
            }
        }

        return Optional.of(Try.of(() -> objectMapper.writeValueAsString(source))
                .getOrElseThrow(DotDataException::new));
    }

    private HashMap<String, Serializable> getSystemFields(final Contentlet contentlet) {
        return systemFieldNames.stream().map(systemField -> {
            final Serializable serializable = (Serializable) contentlet.get(systemField);
            final DataTypeSerializer serializer = SerializerProvider.INSTANCE.get()
                    .getSerializer(serializable.getClass());
            return serializable == null ? null
                    : Tuple.of(systemField, serializer.write(serializable));
        }).filter(Objects::nonNull).collect(Collectors
                .toMap(Tuple2::_1, Tuple2::_2, (k1, k2) -> k1, HashMap::new));
    }

    public static ObjectMapper createMapper() {

        final ObjectMapper result = new ObjectMapper();
        result.disable(DeserializationFeature.WRAP_EXCEPTIONS);

        if (Config.getBooleanProperty("dotcms.rest.sort.json.properties", true)) {
            // result.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            result.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            result.configure(SerializationFeature.INDENT_OUTPUT, true);
        }
        return result;
    }

    private SchemelessContentJsonSupport() {
    }

    public enum INSTANCE {
        INSTANCE;
        private final SchemelessContentJsonSupport provider = new SchemelessContentJsonSupport();

        public static SchemelessContentJsonSupport get() {
            return INSTANCE.provider;
        }
    }

}

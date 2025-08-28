package com.dotcms.content.elasticsearch.business.field;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.DotBinaryFieldException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotDateFieldException;
import com.dotmarketing.portlets.contentlet.business.DotJsonFieldException;
import com.dotmarketing.portlets.contentlet.business.DotNumericFieldException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang3.BooleanUtils.toBoolean;

/**
 * Factory to get the FieldHandlerStrategy
 * @author jsanca
 */
public class FieldHandlerStrategyFactory {

    private final Map<String, FieldHandlerStrategy> fieldHandlersMap = new ConcurrentSkipListMap<>();
    private final List<Tuple2<Predicate<Field>, FieldHandlerStrategy>> matchingFieldHandlersList = new CopyOnWriteArrayList<>();

    private FieldHandlerStrategyFactory () {
        initStrategies();
    }

    private static final Set<String> TEXT_STRATEGY_EXCLUSIONS_TYPES = Set.of(
            LegacyFieldTypes.JSON_FIELD.legacyValue()
    );

    private boolean isExcludedTextField(Field field) {
        return TEXT_STRATEGY_EXCLUSIONS_TYPES.contains(LegacyFieldTypes.getLegacyName(field.getClass()));
    }

    private void initStrategies () {

        addFieldStrategy(LegacyFieldTypes.CATEGORY.legacyValue(), this::doNothingStrategy);
        addFieldStrategy(LegacyFieldTypes.CONSTANT.legacyValue(), this::doNothingStrategy);
        addFieldStrategy(LegacyFieldTypes.KEY_VALUE.legacyValue(), this::keyValueStrategy);
        addFieldStrategy(LegacyFieldTypes.BINARY.legacyValue(), this::binaryStrategy);
        addFieldStrategy(LegacyFieldTypes.JSON_FIELD.legacyValue(), this::jsonStrategy);

        addMatchingFieldStrategy(field ->
                field.dbColumn().startsWith("text") &&
                        !isExcludedTextField(field)
                        , this::textStrategy);

        addMatchingFieldStrategy(field ->
                field.dbColumn().startsWith("long_text"), this::longTextStrategy);

        addMatchingFieldStrategy(field ->
                field.dbColumn().startsWith("date"), this::dateStrategy);

        addMatchingFieldStrategy(field ->
                field.dbColumn().startsWith("bool"), this::booleanStrategy);

        addMatchingFieldStrategy(field ->
                field.dbColumn().startsWith("float"), this::floatStrategy);

        addMatchingFieldStrategy(field ->
                field.dbColumn().startsWith("integer"), this::integerStrategy);

        addMatchingFieldStrategy(field ->
                field.dbColumn().startsWith("system_field"), this::systemFieldStrategy);
    }

    private static class SingletonHolder {
        private static final FieldHandlerStrategyFactory INSTANCE = new FieldHandlerStrategyFactory();
    }

    /**
     * Get the instance.
     * @return FieldHandlerStrategyFactory
     */
    public static FieldHandlerStrategyFactory getInstance() {

        return FieldHandlerStrategyFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Adds a new field strategy based on a type
     * @param strategy
     */
    public void addFieldStrategy (final String fieldType, final FieldHandlerStrategy strategy) {

        if (Objects.nonNull(fieldType) && Objects.nonNull(strategy)) {

            fieldHandlersMap.put(fieldType, strategy);
        }
    }

    /**
     * Adds a new field strategy associated to a predicate
     * @param fieldTypePredicate
     * @param strategy
     */
    public void addMatchingFieldStrategy (final Predicate<Field> fieldTypePredicate, final FieldHandlerStrategy strategy) {

        if (Objects.nonNull(fieldTypePredicate) && Objects.nonNull(strategy)) {

            matchingFieldHandlersList.add(Tuple.of(fieldTypePredicate, strategy));
        }
    }

    /**
     * Get the strategy, throws DotContentletStateException if the strategy is not found for a field type
     * @param field
     * @return FieldHandlerStrategy
     */
    public FieldHandlerStrategy get (final Field field) {

        final String legacyName = LegacyFieldTypes.getLegacyName(field.getClass());
        if  (fieldHandlersMap.containsKey(legacyName)) {
            return fieldHandlersMap.get(legacyName);
        }

        return matchingFieldHandlersList.stream()
                .filter(tuple -> tuple._1.test(field))
                .findFirst()
                .map(Tuple2::_2)
                .orElseThrow(() -> new DotContentletStateException("Unable to set value : Unknown field type: " + field));
    }

    private void doNothingStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        Logger.debug(this,
                "No strategy to set contentlet field value on field type" + field.getClass().getName() + " with value: " + value);
    }

    /**
     * If you're changing this strategy have a look at jsonStrategy
     * @param contentlet
     * @param field
     * @param value
     * @throws DotContentletStateException
     */
    private void keyValueStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        if (value instanceof String) {
            JsonUtil.JSONValidationResult validationResult = JsonUtil.validateJSON((String) value);

            if (validationResult.isValid()) {
                contentlet.setStringProperty(field.variable(), validationResult.node.toString());
            } else {
                // Throw a backwards compatible but detailed exception
                throw DotJsonFieldException.keyValueJsonException(
                        field.variable(),
                        (String) value,
                        validationResult.line,
                        validationResult.column,
                        validationResult.errorMessage
                );
            }

        } else if (value instanceof Map) {
            contentlet.setStringProperty(field.variable(),
                    Try.of(() -> JsonUtil.getJsonAsString((Map<String, Object>) value))
                            .getOrElse("{}"));
        } else {

            throw new DotContentletStateException(
                    "Invalid JSON field provided. Key Value Field variable: " +
                            field.variable());
        }
    }

    /**
     * If you're changing this strategy have a look at keyValueStrategy
     * @param contentlet
     * @param field
     * @param value
     * @throws DotContentletStateException
     */
    private void jsonStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {
        if (value instanceof String) {
            JsonUtil.JSONValidationResult validationResult = JsonUtil.validateJSON((String) value);

            if (validationResult.isValid()) {
                contentlet.setStringProperty(field.variable(), validationResult.node.toString());
            } else {
                // Throw a backwards compatible but detailed exception
                throw DotJsonFieldException.jsonFieldException(
                        field.variable(),
                        (String) value,
                        validationResult.line,
                        validationResult.column,
                        validationResult.errorMessage
                );
            }

        } else if (value instanceof Map) {
            contentlet.setStringProperty(field.variable(),
                    Try.of(() -> JsonUtil.getJsonAsString((Map<String, Object>) value))
                            .getOrElse("{}"));
        } else {

            throw new DotContentletStateException(
                    "Invalid JSON field provided. Field variable: " +
                            field.variable());
        }
    }

    private void binaryStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {
        try {

            // only if the value is a file or a tempFile
            if (value.getClass() == File.class) {
                contentlet.setBinary(field.variable(), (java.io.File) value);
            }
            // if this value is a String and a temp resource, use it to populate the
            // binary field
            else if (value instanceof String && APILocator.getTempFileAPI().isTempResource((String) value)) {
                final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
                // we use the session to verify access to the temp resource
                final Optional<DotTempFile> tempFileOptional = APILocator.getTempFileAPI()
                        .getTempFile(request, (String) value);

                if (tempFileOptional.isPresent()) {
                    contentlet.setBinary(field.variable(),
                            tempFileOptional.get().file);
                } else {
                    throw DotBinaryFieldException.invalidTempFileBuilder(field.variable(), value)
                            .fieldType(field.typeName())
                            .expectedFormat("Valid temporary file")
                            .addContext("tempFileResource", value.toString())
                            .build();
                }

            }
        } catch (IOException e) {
            throw DotBinaryFieldException.ioErrorBuilder(field.variable(), value)
                    .fieldType(field.typeName())
                    .expectedFormat("File or temporary file resource")
                    .addContext("providedType", value != null ? value.getClass().getSimpleName() : "null")
                    .cause(e)
                    .build();
        }
    }

    private void textStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        try {
            contentlet.setStringProperty(field.variable(), (String) value);
        } catch (Exception e) {
            contentlet.setStringProperty(field.variable(), value.toString());
        }
    }

    private void longTextStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        textStrategy(contentlet, field, value);
    }

    private void dateStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        this.parseDate(contentlet, field, value, APILocator.getContentletAPIImpl().getContentletDateFormats());
    }

    /**
     * This method parse the date value and set it to the contentlet
     * The value could be a number (translated to an unix timestamp as Date)
     * A string (translated to a Date using the dateFormats)
     * Or an actual date (which is just set as it is)
     * Otherwise throws an exception ({@link DotContentletStateException}).
     * @param contentlet
     * @param field
     * @param value
     * @param dateFormats
     */
    public void parseDate(final Contentlet contentlet,
                                 final Field field,
                                 final Object value,
                                 final String... dateFormats) {
        if (value instanceof Number) { // is a timestamp
            contentlet.setDateProperty(field.variable(),
                    DateUtil.convertDate(Number.class.cast(value).longValue()));
        } else if (value instanceof Date) {
            contentlet.setDateProperty(field.variable(), (Date) value);
        } else if (value instanceof String) {
            final String trimmedValue = ((String) value).trim();
            if (!trimmedValue.isEmpty()) {
                try {
                    contentlet.setDateProperty(field.variable(),
                            DateUtil.convertDate(trimmedValue, dateFormats));
                } catch (Exception e) {

                    throw DotDateFieldException.conversionErrorBuilder(field.variable(), value)
                            .fieldType(field.typeName())
                            .acceptedFormats(dateFormats)
                            .addContext("errorMessage", e.getMessage())
                            .build();
                }
            } else {

                contentlet.setDateProperty(field.variable(), null);
            }
        } else if (field.required() && value == null) {

            throw DotDateFieldException.invalidTypeBuilder(field.variable(), value)
                    .fieldType(field.typeName())
                    .expectedFormat("String or Date")
                    .addContext("providedType", value != null ? value.getClass().getSimpleName() : "null")
                    .build();
        }
    }


    private void booleanStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        if (value instanceof Boolean) {
            contentlet.setBoolProperty(field.variable(), (Boolean) value);
        } else if (value instanceof String) {
            try {
                final String auxValue = (String) value;
                //toBoolean deals with ("true","yes","1","t","on")
                final boolean auxBoolean = toBoolean(auxValue);
                contentlet.setBoolProperty(field.variable(), auxBoolean);
            } catch (Exception e) {
                //This exception isn't really ever thrown
                // As anything outside the range of values accepted as true will be set as false
                throw new DotContentletStateException(
                        "Unable to set string value as a Boolean for the field: " +
                                field.variable());
            }
        } else {
            throw new DotContentletStateException(
                    "Boolean fields must either be of type String or Boolean for the field: " + field.variable());
        }
    }

    private void floatStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        if (value instanceof Number) {
            contentlet.setFloatProperty(field.variable(),
                    ((Number) value).floatValue());
        } else if (value instanceof String) {
            final String stringValue = value.toString();
            try {
                contentlet.setFloatProperty(field.variable(),
                        Float.parseFloat(stringValue));
            } catch (Exception e) {
                if (!stringValue.isEmpty()) {
                    contentlet.getMap().put(field.variable(), stringValue);
                }
                throw DotNumericFieldException.floatFieldBuilder(field.variable(), value)
                        .fieldType(field.typeName())
                        .build();
            }
        }
    }

    private void systemFieldStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        if (value.getClass() == java.lang.String.class) {
            try {
                contentlet.setStringProperty(field.variable(), (String) value);
            } catch (Exception e) {
                contentlet.setStringProperty(field.variable(), value.toString());
            }
        }
    }

    private void integerStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        if (value instanceof Number) {

            contentlet.setLongProperty(field.variable(),
                    ((Number) value).longValue());
        } else if (value instanceof String) {
            try {
                contentlet.setLongProperty(field.variable(),
                        Long.parseLong((String)value));
            } catch (Exception e) {
                //If we throw this exception here... the contentlet will never get to the validateContentlet Method
                throw DotNumericFieldException.longFieldBuilder(field.variable(), value)
                        .fieldType(field.typeName())
                        .build();
            }
        }
    }

}

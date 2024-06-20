package com.dotcms.content.elasticsearch.business.field;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Logger;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

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

    private void initStrategies () {

        addFieldStrategy(Field.FieldType.CATEGORY.toString(), this::doNothingStrategy);
        addFieldStrategy(Field.FieldType.CATEGORIES_TAB.toString(), this::doNothingStrategy);
        addFieldStrategy(Field.FieldType.CONSTANT.toString(), this::doNothingStrategy);
        addFieldStrategy(Field.FieldType.KEY_VALUE.toString(), this::keyValueStrategy);
        addFieldStrategy(Field.FieldType.BINARY.toString(), this::binaryStrategy);
        addFieldStrategy(Field.FieldType.JSON_FIELD.toString(), this::jsonStrategy);

        addMatchingFieldStrategy(field ->
                APILocator.getFieldAPI().isElementConstant(field), this::doNothingStrategy);

        addMatchingFieldStrategy(field ->
                field.getFieldContentlet().startsWith("text") &&
                        !Field.FieldType.JSON_FIELD.toString().equals(field.getFieldType()), this::textStrategy);

        addMatchingFieldStrategy(field ->
                field.getFieldContentlet().startsWith("long_text"), this::longTextStrategy);

        addMatchingFieldStrategy(field ->
                field.getFieldContentlet().startsWith("date"), this::dateStrategy);

        addMatchingFieldStrategy(field ->
                field.getFieldContentlet().startsWith("bool"), this::booleanStrategy);

        addMatchingFieldStrategy(field ->
                field.getFieldContentlet().startsWith("float"), this::floatStrategy);

        addMatchingFieldStrategy(field ->
                field.getFieldContentlet().startsWith("integer"), this::integerStrategy);

        addMatchingFieldStrategy(field ->
                field.getFieldContentlet().startsWith("system_field"), this::systemFieldStrategy);
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
     * Adds a new field strategy associated to a starts with text
     * @param strategy
     */
    public void addStartsWithFieldStrategy (final String fieldTypeText, final FieldHandlerStrategy strategy) {

        if (Objects.nonNull(fieldTypeText) && Objects.nonNull(strategy)) {

            addMatchingFieldStrategy(field -> field.getFieldType().startsWith(fieldTypeText), strategy);
        }
    }

    /**
     * Adds a new field strategy associated to a predicate
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

        if  (fieldHandlersMap.containsKey(field.getFieldType())) {
            return fieldHandlersMap.get(field.getFieldType());
        }

        return matchingFieldHandlersList.stream()
                .filter(tuple -> tuple._1.test(field))
                .findFirst()
                .map(Tuple2::_2)
                .orElseThrow(() -> new DotContentletStateException("Unable to set value : Unknown field type: " + field));
    }

    private void doNothingStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        Logger.debug(this,
                "Cannot set contentlet field value on field type" + field.getFieldType());
    }

    private void keyValueStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        if ((value instanceof String) && (JsonUtil.isValidJSON((String) value))) {

            contentlet.setStringProperty(field.getVelocityVarName(), Try.of(
                            () -> JsonUtil.JSON_MAPPER.readTree((String) value).toString())
                    .getOrElse("{}"));
        } else if (value instanceof Map) {

            contentlet.setStringProperty(field.getVelocityVarName(),
                    Try.of(() -> JsonUtil.getJsonAsString((Map<String, Object>) value))
                            .getOrElse("{}"));
        } else {

            throw new DotContentletStateException(
                    "Invalid JSON field provided. Key Value Field variable: " +
                            field.getVelocityVarName());
        }
    }

    private void binaryStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {
        try {

            // only if the value is a file or a tempFile
            if (value.getClass() == File.class) {
                contentlet.setBinary(field.getVelocityVarName(), (java.io.File) value);
            }
            // if this value is a String and a temp resource, use it to populate the
            // binary field
            else if (value instanceof String && APILocator.getTempFileAPI().isTempResource((String) value)) {
                final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
                // we use the session to verify access to the temp resource
                final Optional<DotTempFile> tempFileOptional = APILocator.getTempFileAPI()
                        .getTempFile(request, (String) value);

                if (tempFileOptional.isPresent()) {
                    contentlet.setBinary(field.getVelocityVarName(),
                            tempFileOptional.get().file);
                } else {
                    throw new DotStateException("Invalid Temp File provided");
                }

            }
        } catch (IOException e) {
            throw new DotContentletStateException(
                    "Unable to set binary file Object: " + e.getMessage(), e);
        }
    }

    private void textStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        try {
            contentlet.setStringProperty(field.getVelocityVarName(), (String) value);
        } catch (Exception e) {
            contentlet.setStringProperty(field.getVelocityVarName(), value.toString());
        }
    }

    private void longTextStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        textStrategy(contentlet, field, value);
    }

    private void dateStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        ESContentletAPIImpl.parseDate(contentlet, field, value, APILocator.getContentletAPIImpl().getContentletDateFormats());
    }

    private void booleanStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        if (value instanceof Boolean) {

            contentlet.setBoolProperty(field.getVelocityVarName(), (Boolean) value);
        } else if (value instanceof String) {
            try {
                final String auxValue = (String) value;
                final Boolean auxBoolean =
                        (auxValue.equalsIgnoreCase("1") || auxValue.equalsIgnoreCase("true")
                                || auxValue.equalsIgnoreCase("t")) ? Boolean.TRUE
                                : Boolean.FALSE;
                contentlet.setBoolProperty(field.getVelocityVarName(), auxBoolean);
            } catch (Exception e) {
                throw new DotContentletStateException(
                        "Unable to set string value as a Boolean");
            }
        } else {
            throw new DotContentletStateException(
                    "Boolean fields must either be of type String or Boolean");
        }
    }

    private void floatStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        if (value instanceof Number) {
            contentlet.setFloatProperty(field.getVelocityVarName(),
                    ((Number) value).floatValue());
        } else if (value instanceof String) {
            try {
                contentlet.setFloatProperty(field.getVelocityVarName(),
                        Float.valueOf((String)value));
            } catch (Exception e) {
                if (value != null && value.toString().length() != 0) {
                    contentlet.getMap().put(field.getVelocityVarName(), (String) value);
                }
                throw new DotContentletStateException("Unable to set string value as a Float");
            }
        }
    }

    private void systemFieldStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        if (value.getClass() == java.lang.String.class) {
            try {
                contentlet.setStringProperty(field.getVelocityVarName(), (String) value);
            } catch (Exception e) {
                contentlet.setStringProperty(field.getVelocityVarName(), value.toString());
            }
        }
    }

    private void integerStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {

        if (value instanceof Number) {

            contentlet.setLongProperty(field.getVelocityVarName(),
                    ((Number) value).longValue());
        } else if (value instanceof String) {
            try {
                contentlet.setLongProperty(field.getVelocityVarName(),
                        Long.valueOf((String)value));
            } catch (Exception e) {
                //If we throw this exception here.. the contentlet will never get to the validateContentlet Method
                throw new DotContentletStateException("Unable to set string value as a Long");
            }
        }
    }

    private void jsonStrategy(final Contentlet contentlet, final Field field, final Object value) throws DotContentletStateException {
        if ((value instanceof String) && (JsonUtil.isValidJSON((String) value))) {
            contentlet.setStringProperty(field.getVelocityVarName(), Try.of(
                            () -> JsonUtil.JSON_MAPPER.readTree((String) value).toString())
                    .getOrElse("{}"));
        } else if (value instanceof Map) {
            contentlet.setStringProperty(field.getVelocityVarName(),
                    Try.of(() -> JsonUtil.getJsonAsString((Map<String, Object>) value))
                            .getOrElse("{}"));
        } else {
            throw new DotContentletStateException(
                    "Invalid JSON field provided. Field variable: " +
                            field.getVelocityVarName());
        }
    }
}

package com.dotcms.rest.api.v1.content.search.handlers;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.JSONField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.rest.api.v1.content.search.strategies.FieldHandlerId;
import com.dotcms.rest.api.v1.content.search.strategies.FieldStrategy;
import com.dotcms.rest.api.v1.content.search.strategies.FieldStrategyFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.liferay.util.StringPool.BLANK;

/**
 * This class generates a registry of the different Field Handlers that generate the Lucene query
 * for different searchable field in a Content Type. Each Field Handler category has a set of
 * fields who share the same value formatting and specifications ina Lucene query.
 * <p>For instance, text-based fields -- e.g., Text, Block Editor, Text Area, WYSIWYG, Select, etc.
 * -- can be queried via Lucene the same way. However, a Binary Field needs a different query
 * format, as well as Date, Time, and Date and Time fields have their own.</p>
 *
 * @author Jose Castro
 * @since Jan 30, 2025
 */
public class FieldHandlerRegistry {

    private static final Map<Class<? extends Field>, Function<FieldContext, String>> handlers = new HashMap<>();

    /**
     * Handler for fields whose Data Type is True/False. Keyed by Data Type rather than field type, so
     * it lives outside the {@link #handlers} map — see {@link #getHandler(Field)}.
     */
    private static final Function<FieldContext, String> BOOLEAN_HANDLER =
            toHandler(FieldHandlerId.BOOLEAN);

    static {
        // Here, we associate each type of User Searchable field in a Content Type with their
        // specific type of Field Handler. This way, we can determine how a field can be queried via
        // Lucene in the expected format
        registerHandler(Set.of(TextField.class, StoryBlockField.class, CheckboxField.class,
                        CustomField.class, JSONField.class, MultiSelectField.class,
                        RadioField.class, SelectField.class, TextAreaField.class,
                        WysiwygField.class),
                FieldHandlerId.TEXT);

        registerHandler(Set.of(BinaryField.class), FieldHandlerId.BINARY);

        registerHandler(Set.of(DateField.class, DateTimeField.class, TimeField.class), FieldHandlerId.DATE_TIME);

        registerHandler(Set.of(CategoryField.class), FieldHandlerId.CATEGORY);

        registerHandler(Set.of(KeyValueField.class), FieldHandlerId.KEY_VALUE);

        registerHandler(Set.of(RelationshipField.class), FieldHandlerId.RELATIONSHIP);

        registerHandler(Set.of(TagField.class), FieldHandlerId.TAG);
    }

    /**
     * Registers a group of fields that share the same query format.
     *
     * @param fieldTypes     The list of {@link Field} objects.
     * @param fieldHandlerId The {@link FieldHandlerId} that will be used to retrieve the
     *                       {@link FieldStrategy} that will be used to generate the Lucene
     *                       query for a given field.
     */
    public static void registerHandler(final Set<Class<? extends Field>> fieldTypes,
                                        final FieldHandlerId fieldHandlerId) {
        final Function<FieldContext, String> handler = toHandler(fieldHandlerId);
        fieldTypes.forEach(fieldType -> handlers.put(fieldType, handler));
    }

    /**
     * Wraps a {@link FieldHandlerId}'s {@link FieldStrategy} into the Field Handler function, which
     * only generates the Lucene query when the strategy's required values are present.
     *
     * @param fieldHandlerId The {@link FieldHandlerId} to wrap.
     *
     * @return The {@link Function} that generates the Lucene query.
     */
    private static Function<FieldContext, String> toHandler(final FieldHandlerId fieldHandlerId) {
        final FieldStrategy strategy = FieldStrategyFactory.getStrategy(fieldHandlerId);
        return context -> strategy.checkRequiredValues(context)
                ? strategy.generateQuery(context).trim()
                : BLANK;
    }

    /**
     * Retrieves the Field Handler for a given field, taking its <b>Data Type</b> into account as well
     * as its type.
     * <p>This is the overload callers should use. Field type alone is not enough to decide how a field
     * can be queried: a Radio or Select field whose Data Type is True/False is mapped in Elasticsearch
     * as a {@code boolean}, and the contains-style wildcard the TEXT handler produces for those field
     * types is rejected outright for a boolean-mapped field — which fails the entire query and
     * surfaces as an empty result set with no error. Such fields are routed to the
     * {@link FieldHandlerId#BOOLEAN} handler instead.</p>
     * <p>Note this deliberately does not affect Checkbox fields: they only accept the {@code TEXT} and
     * {@code LONG_TEXT} Data Types, so a checkbox is never boolean-mapped even when its option value
     * happens to be the text {@code "true"}.</p>
     *
     * @param field The {@link Field} whose Field Handler will be retrieved.
     *
     * @return The {@link Function} that generates the Lucene query for the given field.
     */
    public static Function<FieldContext, String> getHandler(final Field field) {
        if (null == field) {
            return context -> BLANK;
        }
        if (DataTypes.BOOL == field.dataType()) {
            return BOOLEAN_HANDLER;
        }
        return getHandler(field.type());
    }

    /**
     * Retrieves the Field Handler for a given field in a Content Type. This handler will call a
     * specific {@link FieldStrategy} that is the one in charge of generating the Lucene query
     * for the given field. It has access to the {@link FieldContext} object which provides all the
     * information the Field Strategy needs to generate the appropriate Lucene query.
     *
     * @param fieldType The {@link Class} of the field whose Field Handler will be retrieved.
     *
     * @return The {@link Function} that generates the Lucene query for the given field.
     */
    public static Function<FieldContext, String> getHandler(Class<? extends Field> fieldType) {
        return handlers.getOrDefault(fieldType, context -> BLANK);
    }

}

package com.dotcms.rest.api.v1.content.search.handlers;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.CustomField;
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
        final FieldStrategy strategy = FieldStrategyFactory.getStrategy(fieldHandlerId);
        fieldTypes.forEach(fieldType -> handlers.put(fieldType,
                context -> strategy.checkRequiredValues(context)
                        ? strategy.generateQuery(context).trim()
                        : BLANK));
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

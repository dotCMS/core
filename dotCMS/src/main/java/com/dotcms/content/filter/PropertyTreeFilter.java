package com.dotcms.content.filter;

import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.JSONField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Filters a JSON-like response tree ({@link java.util.Map}, {@link java.util.Collection},
 * {@link com.dotmarketing.util.json.JSONObject}, {@link com.dotmarketing.util.json.JSONArray} and
 * scalars) for one caller. Every object loses the user properties the given
 * {@link ContentFieldFilter} hides, and objects that are contentlets also get the configured content
 * property lists. The values of a contentlet's JSON, Key/Value and Custom fields are customer data and
 * are left as they are. A Story Block value held as JSON text, and its {@code _raw} copy, is read and
 * filtered the same way, since it carries the data of the contentlets embedded in it. The result is a copy;
 * the input tree is never changed.
 *
 * @author hassandotcms
 */
public final class PropertyTreeFilter {

    /** Field types whose values are customer data and are never walked into. */
    private static final Set<String> CUSTOMER_VALUE_FIELD_TYPES = Set.of(
            JSONField.class.getName(), KeyValueField.class.getName(), CustomField.class.getName());

    /** Suffix of the property that repeats a Story Block value as stored, e.g. {@code blogContent_raw}. */
    private static final String RAW_SUFFIX = "_raw";

    /**
     * Resolves the type of a contentlet field, so the values of JSON, Key/Value and Custom fields can be
     * left untouched.
     */
    @FunctionalInterface
    public interface FieldTypes {

        /**
         * Returns the type of a field of a content type.
         *
         * @param contentType the variable name or identifier of the contentlet's content type
         * @param fieldVariable the variable name of the field
         * @return the fully qualified class name of the field type, or {@code null} when it cannot be resolved
         */
        String typeOf(String contentType, String fieldVariable);
    }

    private PropertyTreeFilter() {
    }

    /**
     * Returns a filtered copy of the given tree.
     *
     * @param tree the response tree; may be {@code null} or a scalar
     * @param filter the {@link ContentFieldFilter} built for the caller
     * @param fieldTypes resolves field types of the contentlets found in the tree
     * @return the filtered copy, of the same JSON types as the input; scalars are returned as they are
     */
    public static Object filter(final Object tree, final ContentFieldFilter filter, final FieldTypes fieldTypes) {
        if (null == filter || !filter.isActive()) {
            return tree;
        }
        return copy(tree, filter, fieldTypes);
    }

    /**
     * Returns the field types of the content types stored in dotCMS, read as the system user. Each content
     * type is looked up once per returned instance, so one instance should serve one response.
     *
     * @return the field types of the stored content types; {@code null} for a type or field that cannot be found
     */
    public static FieldTypes storedContentTypes() {
        final Map<String, Optional<ContentType>> types = new HashMap<>();
        return (contentType, fieldVariable) -> types
                .computeIfAbsent(contentType, variable -> Try.of(() -> APILocator
                        .getContentTypeAPI(APILocator.systemUser()).find(variable)).toJavaOptional())
                .map(type -> type.fieldMap().get(fieldVariable))
                .map(PropertyTreeFilter::typeName)
                .orElse(null);
    }

    /**
     * Names the type of a field by the field family it belongs to, so the generated implementations of
     * a field type answer the same as the type itself.
     *
     * @param field the {@link Field} to name
     * @return the class name of its field family, or of the field itself for other families
     */
    private static String typeName(final Field field) {
        if (field instanceof JSONField) {
            return JSONField.class.getName();
        }
        if (field instanceof KeyValueField) {
            return KeyValueField.class.getName();
        }
        if (field instanceof CustomField) {
            return CustomField.class.getName();
        }
        if (field instanceof StoryBlockField) {
            return StoryBlockField.class.getName();
        }
        return field.getClass().getName();
    }

    /**
     * Copies one node of the tree, filtering every object found in it.
     *
     * @param node the node to copy
     * @param filter the caller's filter
     * @param fieldTypes the field type resolver
     * @return the filtered copy, or the node itself when it is a scalar
     */
    private static Object copy(final Object node, final ContentFieldFilter filter, final FieldTypes fieldTypes) {
        if (node instanceof Map) {
            return copyObject((Map<?, ?>) node, filter, fieldTypes);
        }
        if (node instanceof Collection) {
            return copyArray((Collection<?>) node, filter, fieldTypes);
        }
        return node;
    }

    /**
     * Copies an object without the user properties to hide. A contentlet also gets the configured content
     * property lists, and the values of its JSON, Key/Value and Custom fields are copied as they are.
     *
     * @param object the object to copy
     * @param filter the caller's filter
     * @param fieldTypes the field type resolver
     * @return the filtered copy; a {@link JSONObject} when the input is one
     */
    private static Map<?, ?> copyObject(final Map<?, ?> object, final ContentFieldFilter filter,
                                        final FieldTypes fieldTypes) {
        final Set<String> userProperties = filter.userPropertiesToHide();
        final String contentType = isContentlet(object) ? contentTypeOf(object) : null;
        final boolean jsonObject = object instanceof JSONObject;
        final JSONObject jsonCopy = jsonObject ? new JSONObject() : null;
        final Map<Object, Object> mapCopy = jsonObject ? null : new LinkedHashMap<>();
        object.forEach((key, value) -> {
            final String name = String.valueOf(key);
            if (userProperties.contains(name.toLowerCase(Locale.ROOT))) {
                return;
            }
            final Object copiedValue;
            if (isCustomerValue(contentType, name, fieldTypes)) {
                copiedValue = value;
            } else if (value instanceof String && isStoryBlock(contentType, name, fieldTypes)) {
                copiedValue = copyStoryBlockText((String) value, filter, fieldTypes);
            } else {
                copiedValue = copy(value, filter, fieldTypes);
            }
            if (jsonObject) {
                jsonCopy.put(name, copiedValue);
            } else {
                mapCopy.put(key, copiedValue);
            }
        });
        final Map<?, ?> copy = jsonObject ? jsonCopy : mapCopy;
        return null != contentType || isContentlet(object) ? filter.apply(copy) : copy;
    }

    /**
     * Copies an array, filtering every element.
     *
     * @param array the array to copy
     * @param filter the caller's filter
     * @param fieldTypes the field type resolver
     * @return the filtered copy; a {@link JSONArray} when the input is one
     */
    private static Collection<?> copyArray(final Collection<?> array, final ContentFieldFilter filter,
                                           final FieldTypes fieldTypes) {
        if (array instanceof JSONArray) {
            final JSONArray copy = new JSONArray();
            array.forEach(element -> copy.put(copy(element, filter, fieldTypes)));
            return copy;
        }
        final List<Object> copy = new ArrayList<>(array.size());
        array.forEach(element -> copy.add(copy(element, filter, fieldTypes)));
        return copy;
    }

    /**
     * Tells whether an object is a contentlet: it has an identifier or an inode, and a content type or a
     * base type.
     *
     * @param object the object to check
     * @return {@code true} for a contentlet
     */
    private static boolean isContentlet(final Map<?, ?> object) {
        return (object.containsKey(Contentlet.IDENTIFIER_KEY) || object.containsKey(Contentlet.INODE_KEY))
                && (object.containsKey(Contentlet.CONTENT_TYPE_KEY) || object.containsKey(Contentlet.BASE_TYPE_KEY));
    }

    /**
     * Returns the content type of a contentlet object, or {@code null} when it only has a base type.
     *
     * @param contentlet the contentlet object
     * @return the content type variable name or identifier, or {@code null}
     */
    private static String contentTypeOf(final Map<?, ?> contentlet) {
        final Object contentType = contentlet.get(Contentlet.CONTENT_TYPE_KEY);
        return null == contentType ? null : String.valueOf(contentType);
    }

    /**
     * Tells whether a property of a contentlet holds a Story Block value: the field itself or its
     * {@code _raw} copy.
     *
     * @param contentType the contentlet's content type, or {@code null} when the object is not a contentlet
     * @param name the property name
     * @param fieldTypes the field type resolver
     * @return {@code true} for a Story Block value
     */
    private static boolean isStoryBlock(final String contentType, final String name, final FieldTypes fieldTypes) {
        if (null == contentType) {
            return false;
        }
        final String field = name.endsWith(RAW_SUFFIX) ? name.substring(0, name.length() - RAW_SUFFIX.length()) : name;
        return StoryBlockField.class.getName().equals(Try.of(() -> fieldTypes.typeOf(contentType, field)).getOrNull());
    }

    /**
     * Filters a Story Block value held as JSON text and returns it as text again. Text that cannot name a
     * filtered property, or that is not a JSON object or array (such as HTML kept from a converted field), is
     * returned as it is.
     *
     * @param text the Story Block value
     * @param filter the caller's filter
     * @param fieldTypes the field type resolver
     * @return the filtered JSON text, or the original text
     */
    private static String copyStoryBlockText(final String text, final ContentFieldFilter filter,
                                             final FieldTypes fieldTypes) {
        if (!mayName(text, filter)) {
            return text;
        }
        final Object tree = Try.of(() -> JsonUtil.getObjectFromJson(text, Object.class)).getOrNull();
        if (!(tree instanceof Map) && !(tree instanceof Collection)) {
            return text;
        }
        return Try.of(() -> JsonUtil.JSON_MAPPER.writeValueAsString(copy(tree, filter, fieldTypes))).get();
    }

    /**
     * Tells whether a text may hold, as a JSON key, a property the filter masks or removes. Text with
     * {@code \}{@code u} character codes is always read, since a key could be written with them.
     *
     * @param text the text to check
     * @param filter the caller's filter
     * @return {@code false} only when the text cannot hold any of them
     */
    private static boolean mayName(final String text, final ContentFieldFilter filter) {
        if (text.contains("\\u")) {
            return true;
        }
        final String lowerCased = text.toLowerCase(Locale.ROOT);
        return filter.restrictedProperties().stream().anyMatch(name -> lowerCased.contains('"' + name + '"'));
    }

    /**
     * Tells whether a property of a contentlet holds customer data that must not be walked into: the value
     * of a JSON, Key/Value or Custom field.
     *
     * @param contentType the contentlet's content type, or {@code null} when the object is not a contentlet
     * @param name the property name
     * @param fieldTypes the field type resolver
     * @return {@code true} when the value must be copied as it is
     */
    private static boolean isCustomerValue(final String contentType, final String name, final FieldTypes fieldTypes) {
        if (null == contentType) {
            return false;
        }
        final String type = Try.of(() -> fieldTypes.typeOf(contentType, name)).getOrNull();
        return null != type && CUSTOMER_VALUE_FIELD_TYPES.contains(type);
    }

}

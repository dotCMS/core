package com.dotcms.rest.api.v1.drive;

import com.dotcms.browser.FieldSearchCriteria;
import com.dotcms.browser.FieldSearchCriteria.FilterKind;
import com.dotcms.browser.FieldSearchCriteria.RoutingBucket;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses and validates the {@code userSearchable} map of a {@link DriveRequestForm} into a typed
 * list of {@link FieldSearchCriteria}.
 * <p>
 * The map is keyed by field variable name; the value is the raw JSON payload for that field
 * (string, {@code {from,to}} object, array or boolean). This resolver:
 * <ul>
 *   <li>resolves each key against the request's single content type,</li>
 *   <li>rejects unknown keys and fields that are not {@code searchable && indexed}
 *       (mirrors {@code StructureAjax.getSearchableStructureFields}),</li>
 *   <li>rejects out-of-scope field types (dividers, Binary, JSON, Key/Value, Block Editor,
 *       Constant, Hidden, Custom, Relationship — the latter is v1.1),</li>
 *   <li>infers the operator ({@link FilterKind}) from the value shape and validates it against
 *       the field type, and</li>
 *   <li>assigns the routing bucket ({@link RoutingBucket}) per the ADR-0018 contract: Tag → DB,
 *       everything else in scope → INDEX.</li>
 * </ul>
 * Any validation failure raises a {@link BadRequestException} (HTTP 400) naming the offending
 * field. This class only builds the typed criteria; translating them into DB predicates or ES
 * clauses is done downstream.
 *
 * @since 25.xx
 */
public class ContentDriveFieldFilterResolver {

    private static final String FROM_KEY = "from";
    private static final String TO_KEY = "to";

    /**
     * Parses the {@code userSearchable} map into typed, validated criteria.
     *
     * @param userSearchable the raw field-filter map, keyed by field variable name. May be null
     *                       or empty.
     * @param contentType    the single content type the request is scoped to, used to resolve
     *                       field definitions. Must be non-null when {@code userSearchable} is set.
     * @return the list of parsed criteria, never null (empty when there are no filters).
     * @throws BadRequestException when a key is unknown, not searchable, of an unsupported type, or
     *                             its value shape is incompatible with the field type.
     */
    public List<FieldSearchCriteria> parse(final Map<String, Object> userSearchable,
            final ContentType contentType) {

        if (!UtilMethods.isSet(userSearchable)) {
            return List.of();
        }
        if (null == contentType) {
            throw new BadRequestException(
                    "A single content type is required to resolve 'userSearchable' field filters.");
        }

        final Map<String, Field> fieldMap = contentType.fieldMap();
        final List<FieldSearchCriteria> criteria = new ArrayList<>();
        for (final Map.Entry<String, Object> entry : userSearchable.entrySet()) {
            criteria.add(parseEntry(entry.getKey(), entry.getValue(), fieldMap, contentType));
        }
        return criteria;
    }

    private FieldSearchCriteria parseEntry(final String fieldVariable, final Object rawValue,
            final Map<String, Field> fieldMap, final ContentType contentType) {

        final Field field = fieldMap.get(fieldVariable);
        if (null == field) {
            throw new BadRequestException(String.format(
                    "Unknown field '%s' for content type '%s'.", fieldVariable,
                    contentType.variable()));
        }
        if (!(field.searchable() && field.indexed())) {
            throw new BadRequestException(String.format(
                    "Field '%s' is not user-searchable and system-indexed; it cannot be filtered.",
                    fieldVariable));
        }

        final RoutingBucket bucket = routingBucketFor(field);
        if (null == bucket) {
            throw new BadRequestException(String.format(
                    "Field '%s' is of an unsupported type for field filtering (%s).",
                    fieldVariable, field.getClass().getSimpleName()));
        }

        final FilterKind kind = inferKind(fieldVariable, rawValue);
        if (!supportsKind(field, kind)) {
            throw new BadRequestException(String.format(
                    "Field '%s' (%s) does not support a %s filter.",
                    fieldVariable, field.getClass().getSimpleName(), kind));
        }

        return build(fieldVariable, field, bucket, kind, rawValue);
    }

    /**
     * Infers the operator from the JSON value shape: boolean, {@code {from,to}} range, array of
     * values, or scalar (string/number treated as string).
     */
    private FilterKind inferKind(final String fieldVariable, final Object rawValue) {
        if (null == rawValue) {
            throw new BadRequestException(
                    String.format("Field '%s' has a null filter value.", fieldVariable));
        }
        if (rawValue instanceof Boolean) {
            return FilterKind.BOOLEAN;
        }
        if (rawValue instanceof Map) {
            return FilterKind.RANGE;
        }
        if (rawValue instanceof Iterable) {
            return FilterKind.MULTI;
        }
        return FilterKind.SCALAR;
    }

    private FieldSearchCriteria build(final String fieldVariable, final Field field,
            final RoutingBucket bucket, final FilterKind kind, final Object rawValue) {

        switch (kind) {
            case BOOLEAN:
                return FieldSearchCriteria.bool(fieldVariable, field, bucket, (Boolean) rawValue);
            case RANGE:
                return buildRange(fieldVariable, field, bucket, rawValue);
            case MULTI:
                return buildMulti(fieldVariable, field, bucket, rawValue);
            case SCALAR:
            default:
                final String scalar = rawValue.toString().trim();
                if (scalar.isEmpty()) {
                    throw new BadRequestException(String.format(
                            "Field '%s' has an empty filter value.", fieldVariable));
                }
                return FieldSearchCriteria.scalar(fieldVariable, field, bucket, scalar);
        }
    }

    private FieldSearchCriteria buildRange(final String fieldVariable, final Field field,
            final RoutingBucket bucket, final Object rawValue) {

        final Map<?, ?> map = (Map<?, ?>) rawValue;
        final Object from = map.get(FROM_KEY);
        final Object to = map.get(TO_KEY);
        if (null == from && null == to) {
            throw new BadRequestException(String.format(
                    "Field '%s' range filter must set at least one of 'from'/'to'.", fieldVariable));
        }
        return FieldSearchCriteria.range(fieldVariable, field, bucket,
                null == from ? null : from.toString().trim(),
                null == to ? null : to.toString().trim());
    }

    private FieldSearchCriteria buildMulti(final String fieldVariable, final Field field,
            final RoutingBucket bucket, final Object rawValue) {

        final List<String> values = new ArrayList<>();
        for (final Object element : (Iterable<?>) rawValue) {
            if (null == element) {
                continue;
            }
            final String value = element.toString().trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        if (values.isEmpty()) {
            throw new BadRequestException(String.format(
                    "Field '%s' array filter has no usable values.", fieldVariable));
        }
        return FieldSearchCriteria.multi(fieldVariable, field, bucket, values);
    }

    /**
     * Maps a field type to its routing bucket per the ADR-0018 contract, or {@code null} when the
     * field type is out of scope (Relationship is deferred to v1.1; dividers, Binary, JSON,
     * Key/Value, Block Editor, Constant, Hidden, Custom, File/Image, Host/Folder are excluded).
     */
    private RoutingBucket routingBucketFor(final Field field) {
        if (field instanceof RelationshipField) {
            // v1.1 — DB path against the tree tables, not part of this deliverable.
            return null;
        }
        if (field instanceof TagField) {
            return RoutingBucket.DB;
        }
        if (isIndexRouted(field)) {
            return RoutingBucket.INDEX;
        }
        return null;
    }

    private boolean isIndexRouted(final Field field) {
        return field instanceof TextField
                || field instanceof TextAreaField
                || field instanceof WysiwygField
                || field instanceof SelectField
                || field instanceof RadioField
                || field instanceof MultiSelectField
                || field instanceof CheckboxField
                || field instanceof DateField
                || field instanceof TimeField
                || field instanceof DateTimeField
                || field instanceof CategoryField;
    }

    /**
     * Validates that the inferred operator is compatible with the field type, following the
     * value-shape → field-type table in the spec (§6.1/§6.4).
     */
    private boolean supportsKind(final Field field, final FilterKind kind) {
        switch (kind) {
            case BOOLEAN:
                return field instanceof CheckboxField;
            case RANGE:
                return field instanceof DateField
                        || field instanceof TimeField
                        || field instanceof DateTimeField;
            case MULTI:
                return field instanceof MultiSelectField
                        || field instanceof CheckboxField
                        || field instanceof TagField
                        || field instanceof CategoryField;
            case SCALAR:
                return field instanceof TextField
                        || field instanceof TextAreaField
                        || field instanceof WysiwygField
                        || field instanceof SelectField
                        || field instanceof RadioField;
            default:
                return false;
        }
    }
}

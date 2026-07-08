package com.dotcms.browser;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import java.util.List;
import java.util.Objects;

/**
 * Typed representation of a single content-type field filter parsed from the
 * {@code userSearchable} map of a Content Drive search request.
 * <p>
 * Each criterion carries the resolved {@link Field}, the operator inferred from the JSON value
 * shape ({@link FilterKind}) and the routing bucket that decides whether the criterion is resolved
 * against the database or the search index ({@link RoutingBucket}), per the ADR-0018 routing
 * contract. The concrete value(s) are exposed through the kind-specific accessors.
 * <p>
 * Instances are immutable and created through the static factory methods, which keep the
 * value storage consistent with the declared {@link FilterKind}.
 *
 * @since 25.xx
 */
public final class FieldSearchCriteria {

    /**
     * Operator inferred from the JSON value shape of a {@code userSearchable} entry.
     */
    public enum FilterKind {
        /** Single scalar value: contains (Text/Textarea/WYSIWYG) or equals (Select/Radio). */
        SCALAR,
        /** {@code {from,to}} range: Date/Time/Date-Time fields. */
        RANGE,
        /** Array of values: Multi-Select/Checkbox terms, Tag names, Category inodes. */
        MULTI,
        /** Boolean equality: Checkbox rendered as a boolean. */
        BOOLEAN
    }

    /**
     * Where the criterion is resolved, per the ADR-0018 database-first routing contract.
     */
    public enum RoutingBucket {
        /** Resolved against the database (Tag) to preserve read-your-writes. */
        DB,
        /** Resolved against the search index (Text/Select/Boolean/Date/Category). */
        INDEX
    }

    private final String fieldVariable;
    private final Field field;
    private final ContentType contentType;
    private final FilterKind kind;
    private final RoutingBucket bucket;
    private final List<String> values;
    private final String rangeFrom;
    private final String rangeTo;
    private final Boolean booleanValue;

    private FieldSearchCriteria(final String fieldVariable, final Field field,
            final ContentType contentType, final FilterKind kind, final RoutingBucket bucket,
            final List<String> values, final String rangeFrom, final String rangeTo,
            final Boolean booleanValue) {
        this.fieldVariable = fieldVariable;
        this.field = field;
        this.contentType = contentType;
        this.kind = kind;
        this.bucket = bucket;
        this.values = values == null ? List.of() : List.copyOf(values);
        this.rangeFrom = rangeFrom;
        this.rangeTo = rangeTo;
        this.booleanValue = booleanValue;
    }

    /**
     * Creates a {@link FilterKind#SCALAR} criterion (single value: contains or equals).
     */
    public static FieldSearchCriteria scalar(final String fieldVariable, final Field field,
            final ContentType contentType, final RoutingBucket bucket, final String value) {
        return new FieldSearchCriteria(fieldVariable, field, contentType, FilterKind.SCALAR, bucket,
                List.of(value), null, null, null);
    }

    /**
     * Creates a {@link FilterKind#MULTI} criterion (array of values: terms/names/inodes).
     */
    public static FieldSearchCriteria multi(final String fieldVariable, final Field field,
            final ContentType contentType, final RoutingBucket bucket, final List<String> values) {
        return new FieldSearchCriteria(fieldVariable, field, contentType, FilterKind.MULTI, bucket,
                values, null, null, null);
    }

    /**
     * Creates a {@link FilterKind#RANGE} criterion ({@code {from,to}}). At least one bound is set.
     */
    public static FieldSearchCriteria range(final String fieldVariable, final Field field,
            final ContentType contentType, final RoutingBucket bucket, final String from,
            final String to) {
        return new FieldSearchCriteria(fieldVariable, field, contentType, FilterKind.RANGE, bucket,
                null, from, to, null);
    }

    /**
     * Creates a {@link FilterKind#BOOLEAN} criterion (equals true/false).
     */
    public static FieldSearchCriteria bool(final String fieldVariable, final Field field,
            final ContentType contentType, final RoutingBucket bucket, final boolean value) {
        return new FieldSearchCriteria(fieldVariable, field, contentType, FilterKind.BOOLEAN, bucket,
                null, null, null, value);
    }

    /**
     * @return the field variable name this criterion filters on.
     */
    public String getFieldVariable() {
        return fieldVariable;
    }

    /**
     * @return the resolved content-type {@link Field}.
     */
    public Field getField() {
        return field;
    }

    /**
     * @return the {@link ContentType} the field belongs to (used to build the Lucene field name
     * and provide field-strategy context).
     */
    public ContentType getContentType() {
        return contentType;
    }

    /**
     * @return the operator inferred from the value shape.
     */
    public FilterKind getKind() {
        return kind;
    }

    /**
     * @return the routing bucket (DB or INDEX) for this criterion.
     */
    public RoutingBucket getBucket() {
        return bucket;
    }

    /**
     * @return the values for {@link FilterKind#SCALAR} (single element) and {@link FilterKind#MULTI}
     * criteria; empty for other kinds.
     */
    public List<String> getValues() {
        return values;
    }

    /**
     * @return the lower bound of a {@link FilterKind#RANGE} criterion, or {@code null} if open-ended.
     */
    public String getRangeFrom() {
        return rangeFrom;
    }

    /**
     * @return the upper bound of a {@link FilterKind#RANGE} criterion, or {@code null} if open-ended.
     */
    public String getRangeTo() {
        return rangeTo;
    }

    /**
     * @return the value of a {@link FilterKind#BOOLEAN} criterion, or {@code null} for other kinds.
     */
    public Boolean getBooleanValue() {
        return booleanValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FieldSearchCriteria)) {
            return false;
        }
        final FieldSearchCriteria that = (FieldSearchCriteria) o;
        return Objects.equals(fieldVariable, that.fieldVariable)
                && kind == that.kind
                && bucket == that.bucket
                && Objects.equals(values, that.values)
                && Objects.equals(rangeFrom, that.rangeFrom)
                && Objects.equals(rangeTo, that.rangeTo)
                && Objects.equals(booleanValue, that.booleanValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldVariable, kind, bucket, values, rangeFrom, rangeTo, booleanValue);
    }

    @Override
    public String toString() {
        return "FieldSearchCriteria{" +
                "fieldVariable='" + fieldVariable + '\'' +
                ", kind=" + kind +
                ", bucket=" + bucket +
                ", values=" + values +
                ", rangeFrom='" + rangeFrom + '\'' +
                ", rangeTo='" + rangeTo + '\'' +
                ", booleanValue=" + booleanValue +
                '}';
    }
}

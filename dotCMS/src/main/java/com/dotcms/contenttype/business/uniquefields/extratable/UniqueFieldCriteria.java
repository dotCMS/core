package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.model.Language;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static com.liferay.util.StringPool.BLANK;

/**
 * This class represents the criteria used to determine if the value of a Unique Field is indeed
 * unique or not. There are several attributes that can be taken into account to determine whether
 * a field is unique or not, for instance:
 * <ul>
 *     <li>The ID of the Content Type it belongs to.</li>
 *     <li>The Velocity Var Name of the field.</li>
 *     <li>The Language of the Contentlet using it.</li>
 *     <li>The unique value, which is case-insensitive.</li>
 *     <li>The Site where the Contentlet lives.</li>
 *     <li>The Identifiers of the Contentlets that can have the same unique value, for backwards
 *     compatibility.</li>
 *     <li>The Variant that the Contentlet belongs to.</li>
 *     <li>The value of the {@code uniquePerSite} Field Variable.</li>
 *     <li>Whether the Contentlet is live or not.</li>
 * </ul>
 *
 * @author Freddy Rodriguez
 * @since Oct 30th, 2024
 */
public class UniqueFieldCriteria {

    public static final String CONTENT_TYPE_ID_ATTR = "contentTypeId";
    public static final String FIELD_VARIABLE_NAME_ATTR = "fieldVariableName";
    public static final String LANGUAGE_ID_ATTR = "languageId";
    public static final String FIELD_VALUE_ATTR = "fieldValue";
    public static final String SITE_ID_ATTR = "siteId";
    public static final String CONTENTLET_IDS_ATTR = "contentletIds";
    public static final String VARIANT_ATTR = "variant";
    public static final String UNIQUE_PER_SITE_ATTR = "uniquePerSite";
    public static final String LIVE_ATTR = "live";

    private final ContentType contentType;
    private final Field field;
    private final Object value;
    private final Language language;
    private final Host site;
    private final String variantName;
    private final boolean isLive;

    private UniqueFieldCriteria(final Builder builder) {
        this.contentType = builder.contentType;
        this.field = builder.field;
        this.value = builder.value;
        this.language = builder.language;
        this.site = builder.site;
        this.variantName = builder.variantName;
        this.isLive = builder.isLive;
    }

    /**
     * Return a Map with the values in this Unique Field Criteria
     * @return
     */
    public Map<String, Object> toMap(){
        final Map<String, Object> map = new HashMap<>(Map.of(
                CONTENT_TYPE_ID_ATTR, Objects.requireNonNull(contentType.id()),
                FIELD_VARIABLE_NAME_ATTR, Objects.requireNonNull(field.variable()),
                FIELD_VALUE_ATTR, value.toString(),
                LANGUAGE_ID_ATTR, language.getId(),
                UNIQUE_PER_SITE_ATTR, isUniqueForSite(contentType.id(), field.variable()),
                VARIANT_ATTR, variantName,
                LIVE_ATTR, isLive
        ));

        if (site != null) {
            map.put(SITE_ID_ATTR, site.getIdentifier());
        }

        return map;
    }

    /**
     * return true if the uniquePerSite Field Variable is set to true.
     *
     * @param contentTypeId
     * @param fieldVariableName
     * @return
     */
    private static boolean isUniqueForSite(String contentTypeId, String fieldVariableName)  {
        try {
            final Field uniqueField = APILocator.getContentTypeFieldAPI().byContentTypeIdAndVar(contentTypeId, fieldVariableName);

            return uniqueField.fieldVariableValue(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                    .map(Boolean::valueOf).orElse(false);
        } catch (DotDataException e) {
            throw  new DotRuntimeException(
                    String.format("Impossible to get FieldVariable from Field: %s, Content Type: %s",
                            fieldVariableName, contentTypeId), e);
        }
    }

    /**
     * Return the list of criteria that will be used to generate the Hash for the Unique Field. The
     * criteria will follow these rules:
     * <ul>
     *     <li>If the {@code uniquePerSite} Field Variable is set to {@code true}, then concatenate
     *     the Content Type's ID, Field Variable Name, Language's ID, and Field Value.</li>
     *     <li>Otherwise, concatenate the same data as above plus the Site's ID.</li>
     * </ul>
     *
     * @return The criteria to be used to generate the Hash.
     */
    public String criteria(){
        return contentType.id() + field.variable() + language.getId() + value +
                ((isUniqueForSite(contentType.id(), field.variable())) ? site.getIdentifier() : BLANK);
    }

    public Field field() {
        return field;
    }

    public Object value() {
        return value;
    }

    public ContentType contentType() {
        return contentType;
    }

    public Language language() {
        return language;
    }

    @Override
    public String toString() {
        return "UniqueFieldCriteria{" +
                "contentType=" + contentType.id() +
                ", field=" + field.variable() +
                ", value=" + value +
                ", language=" + language.getId() +
                ", site=" + site.getIdentifier() +
                ", variantName='" + variantName + '\'' +
                '}';
    }

    /**
     * Generates the official unique field criteria based on the information provided in the map.
     * This method is particularly useful when reading the unique field criteria directly from the
     * database, and having to update specific attributes.
     *
     * @param supportingValues The map with the supporting values for a given unique value.
     *
     * @return The official unique field criteria.
     */
    public static String criteria(final Map<String, Object> supportingValues) {
        final String contentTypeId = supportingValues.getOrDefault(CONTENT_TYPE_ID_ATTR, BLANK).toString();
        final String fieldVariableName = supportingValues.getOrDefault(FIELD_VARIABLE_NAME_ATTR, BLANK).toString();
        final String languageId = supportingValues.getOrDefault(LANGUAGE_ID_ATTR, BLANK).toString();
        final String fieldValue = supportingValues.getOrDefault(FIELD_VALUE_ATTR, BLANK).toString();
        String siteId = BLANK;
        if (isUniqueForSite(contentTypeId, fieldVariableName)) {
            siteId = supportingValues.getOrDefault(SITE_ID_ATTR, BLANK).toString();
        }
        return contentTypeId + fieldVariableName + languageId + fieldValue + siteId;
    }

    public static class Builder {

        private ContentType contentType;
        private Field field;
        private Object value;
        private Language language;
        private Host site;
        private String variantName;
        private boolean isLive;

        public Builder setVariantName(final String variantName) {
            this.variantName = variantName;
            return this;
        }

        public Builder setContentType(final ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder setField(final Field field) {
            this.field = field;
            return this;
        }

        /**
         * Sets the value for the Unique Field in this builder. If the provided value is a String,
         * it trims and converts it to lowercase before assigning it. For other types, the value is
         * assigned as-is.
         *
         * @param value the value to set; can be of any object type
         *
         * @return the builder instance for method chaining
         */
        public Builder setValue(final Object value) {
            this.value = (value instanceof String)
                    ? ((String) value).trim().toLowerCase()
                    : value;
            return this;
        }

        public Builder setLanguage(final Language language) {
            this.language = language;
            return this;
        }

        public Builder setSite(final Host site) {
            this.site = site;
            return this;
        }

        public Builder setLive(boolean live) {
            this.isLive = live;
            return this;
        }

        public UniqueFieldCriteria build(){
            Objects.requireNonNull(contentType, "Content Type cannot be null");
            Objects.requireNonNull(field, "Field cannot be null");
            Objects.requireNonNull(value, "Value cannot be null");
            Objects.requireNonNull(language, "Language cannot be null");

            if (isUniqueForSite(contentType.id(), field.variable())) {
                Objects.requireNonNull(site);
            }

            return new UniqueFieldCriteria(this);
        }

    }

}

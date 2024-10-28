package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.StringUtils;
import com.liferay.util.StringPool;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represent the criteria used to determine if a value is unique or not
 */
class UniqueFieldCriteria {
    private final ContentType contentType;
    private final Field field;
    private final Object value;
    private final Language language;
    private final Host site;
    private String variantName;

    public UniqueFieldCriteria(final Builder builder) {
        this.contentType = builder.contentType;
        this.field = builder.field;
        this.value = builder.value;
        this.language = builder.language;
        this.site = builder.site;
        this.variantName = builder.variantName;
    }


    /**
     * Return a Map with the values in this Unique Field Criteria
     * @return
     */
    public Map<String, Object> toMap(){
        final Map<String, Object> map = new HashMap<>(Map.of(
                "contentTypeID", Objects.requireNonNull(contentType.id()),
                "fieldVariableName", Objects.requireNonNull(field.variable()),
                "fieldValue", value.toString(),
                "languageId", language.getId(),
                "uniquePerSite", isUniqueForSite(contentType.id(), field.variable()),
                "variant", variantName
        ));

        if (site != null) {
            map.put("hostId", site.getIdentifier());
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

            return uniqueField.fieldVariableValue(ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                    .map(Boolean::valueOf).orElse(false);
        } catch (DotDataException e) {
            throw  new DotRuntimeException(
                    String.format("Impossible to get FieldVariable from Field: %s, Content Type: %s",
                            fieldVariableName, contentTypeId), e);
        }
    }

    /**
     * Return a hash calculated as follow:
     *
     * - If the uniquePerSite Field Variable is set to true then concat the:
     * Content Type' id + Field Variable Name + Language's Id + Field Value
     *
     * - If the uniquePerSite Field Variable is set to false then concat the:
     * Content Type' id + Field Variable Name + Language's Id + Field Value + Site's id
     *
     * @return
     */
    public String hash(){
        return StringUtils.hashText(contentType.id() + field.variable() + language.getId() + value +
                ((isUniqueForSite(contentType.id(), field.variable())) ? site.getIdentifier() : StringPool.BLANK));
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


    public static class Builder {
        private ContentType contentType;
        private Field field;
        private Object value;
        private Language language;
        private Host site;
        private String variantName;

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

        public Builder setValue(final Object value) {
            this.value = value;
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

        public UniqueFieldCriteria build(){
            Objects.requireNonNull(contentType);
            Objects.requireNonNull(field);
            Objects.requireNonNull(value);
            Objects.requireNonNull(language);

            if (isUniqueForSite(contentType.id(), field.variable())) {
                Objects.requireNonNull(site);
            }

            return new UniqueFieldCriteria(this);
        }
    }
}

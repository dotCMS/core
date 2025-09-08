package com.dotcms.ai.v2.api.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FieldDefinitionDTO {

    private final String name;           // internal key
    private final String label;          // default locale
    private final String type;           // text, textarea, wysiwyg, select, tag, date, binary, etc.
    private final boolean required;
    private final boolean indexed;       // search/index hint
    private final boolean i18n;          // localized per language
    private final Integer maxLength;     // for text-like fields
    private final Map<String, Object> options; // e.g. select options, regex, min/max
    private final List<String> validations;    // small list of rule codes

    private FieldDefinitionDTO(Builder b) {
        this.name = b.name;
        this.label = b.label;
        this.type = b.type;
        this.required = b.required;
        this.indexed = b.indexed;
        this.i18n = b.i18n;
        this.maxLength = b.maxLength;
        this.options = b.options == null ? Collections.emptyMap() : Collections.unmodifiableMap(b.options);
        this.validations = b.validations == null ? Collections.emptyList() : Collections.unmodifiableList(b.validations);
    }

    public String getName(){ return name; }
    public String getLabel(){ return label; }
    public String getType(){ return type; }
    public boolean isRequired(){ return required; }
    public boolean isIndexed(){ return indexed; }
    public boolean isI18n(){ return i18n; }
    public Integer getMaxLength(){ return maxLength; }
    public Map<String,Object> getOptions(){ return options; }
    public List<String> getValidations(){ return validations; }

    public static class Builder {
        private String name, label, type;
        private boolean required, indexed, i18n;
        private Integer maxLength;
        private Map<String,Object> options;
        private List<String> validations;

        public Builder name(String v){ this.name=v; return this; }
        public Builder label(String v){ this.label=v; return this; }
        public Builder type(String v){ this.type=v; return this; }
        public Builder required(boolean v){ this.required=v; return this; }
        public Builder indexed(boolean v){ this.indexed=v; return this; }
        public Builder i18n(boolean v){ this.i18n=v; return this; }
        public Builder maxLength(Integer v){ this.maxLength=v; return this; }
        public Builder options(Map<String,Object> v){ this.options=v; return this; }
        public Builder validations(List<String> v){ this.validations=v; return this; }
        public FieldDefinitionDTO build(){ return new FieldDefinitionDTO(this); }
    }
}

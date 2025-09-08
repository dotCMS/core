package com.dotcms.ai.v2.api.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ContentTypeDTO {

    private final String varName;          // canonical
    private final String name;             // display name (default locale)
    private final String description;      // short
    private final String icon;             // optional
    private final boolean workflowEnabled; // quick hint
    private final List<FieldDefinitionDTO> fields;
    private final Map<String, Object> meta; // extras small: { category, hostRequired, ... }

    private ContentTypeDTO(Builder b) {
        this.varName = b.varName;
        this.name = b.name;
        this.description = b.description;
        this.icon = b.icon;
        this.workflowEnabled = b.workflowEnabled;
        this.fields = b.fields == null ? Collections.emptyList() : Collections.unmodifiableList(b.fields);
        this.meta = b.meta == null ? Collections.emptyMap() : Collections.unmodifiableMap(b.meta);
    }

    public String getVarName() { return varName; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public boolean isWorkflowEnabled() { return workflowEnabled; }
    public List<FieldDefinitionDTO> getFields() { return fields; }
    public Map<String, Object> getMeta() { return meta; }

    public static class Builder {
        private String varName, name, description, icon;
        private boolean workflowEnabled;
        private List<FieldDefinitionDTO> fields;
        private Map<String, Object> meta;

        public Builder varName(String v){ this.varName=v; return this; }
        public Builder name(String v){ this.name=v; return this; }
        public Builder description(String v){ this.description=v; return this; }
        public Builder icon(String v){ this.icon=v; return this; }
        public Builder workflowEnabled(boolean v){ this.workflowEnabled=v; return this; }
        public Builder fields(List<FieldDefinitionDTO> v){ this.fields=v; return this; }
        public Builder meta(Map<String,Object> v){ this.meta=v; return this; }
        public ContentTypeDTO build(){ return new ContentTypeDTO(this); }
    }
}

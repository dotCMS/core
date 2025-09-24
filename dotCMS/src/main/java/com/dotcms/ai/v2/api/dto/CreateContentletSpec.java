package com.dotcms.ai.v2.api.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(builder = CreateContentletSpec.Builder.class)
public final class CreateContentletSpec {

    private final String contentType;          // varName del Content Type (e.g. "pet_product")
    private final String host;                 // e.g. "SYSTEM_HOST" o hostname/hostId
    private final Long languageId;             // e.g. 1L (default), requerido en multi-idioma
    private final String folder;               // e.g. "SYSTEM_FOLDER" o path/uuid (opcional)
    private final boolean publish;             // true para publicar tras checkin
    private final Map<String, Object> fields;  // varName -> valor

    private CreateContentletSpec(Builder b) {
        this.contentType = b.contentType;
        this.host = b.host;
        this.languageId = b.languageId;
        this.folder = b.folder;
        this.publish = b.publish;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(b.fields));
    }

    public String getContentType() { return contentType; }
    public String getHost() { return host; }
    public Long getLanguageId() { return languageId; }
    public String getFolder() { return folder; }
    public boolean isPublish() { return publish; }
    public Map<String, Object> getFields() { return fields; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        @JsonProperty(required = true)
        private String contentType;
        @JsonProperty()
        private String host = "SYSTEM_HOST";
        @JsonProperty()
        private Long languageId = 1L;
        @JsonProperty()
        private String folder = "SYSTEM_FOLDER";
        @JsonProperty()
        private boolean publish = false;
        @JsonProperty()
        private final Map<String, Object> fields = new LinkedHashMap<>();

        public Builder contentType(String v){ this.contentType=v; return this; }
        public Builder host(String v){ this.host=v; return this; }
        public Builder languageId(Long v){ this.languageId=v; return this; }
        public Builder folder(String v){ this.folder=v; return this; }
        public Builder publish(boolean v){ this.publish=v; return this; }
        public Builder putField(String k, Object v){ if(k!=null && v!=null) this.fields.put(k, v); return this; }

        public CreateContentletSpec build(){ return new CreateContentletSpec(this); }
    }

    @Override
    public boolean equals(Object o){
        if (this==o) return true;
        if (!(o instanceof CreateContentletSpec)) return false;
        CreateContentletSpec that=(CreateContentletSpec)o;
        return publish==that.publish &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(host, that.host) &&
                Objects.equals(languageId, that.languageId) &&
                Objects.equals(folder, that.folder) &&
                Objects.equals(fields, that.fields);
    }
    @Override public int hashCode(){ return Objects.hash(contentType,host,languageId,folder,publish,fields); }
    @Override public String toString(){
        return "CreateContentletSpec{contentType='"+contentType+"', host='"+host+"', languageId="+languageId+
                ", folder='"+folder+"', publish="+publish+", fields="+fields+"}";
    }
}


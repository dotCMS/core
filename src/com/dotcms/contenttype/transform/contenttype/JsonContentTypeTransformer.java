package com.dotcms.contenttype.transform.contenttype;

import java.io.IOException;
import java.util.List;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonContentTypeTransformer implements ContentTypeTransformer {
    final List<ContentType> list;


    public JsonContentTypeTransformer(ContentType type) {
        this.list = ImmutableList.of(type);
    }

    public JsonContentTypeTransformer(String json) {
        this(fromJsonStr(json));
    }

    public JsonContentTypeTransformer(List<ContentType> list) {
        this.list = ImmutableList.copyOf(list);
    }


    private static String toJsonStr(ContentType type) throws DotStateException {
        ObjectMapper mapper = new ObjectMapper();

        
        JsonWrapper input = new JsonWrapper<>(type, type.baseType().immutableClass());
        try {
            return mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new DotStateException(e);
        }
    }

    private static ContentType fromJsonStr(String type) throws DotStateException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        try {
            ContentType wrap =  mapper.readValue(type, ImmutableSimpleContentType.class);
            //ContentType t = (ContentType) wrap.getInner();
            return wrap;
        } catch (IOException e) {
            throw new DotStateException(e);
        }
    }

    @Override
    public ContentType from() throws DotStateException {
        return this.list.get(0);
    }
    
    @Override
    public List<ContentType> asList() throws DotStateException {
        return this.list;
    }

    public String asJson() throws DotStateException {
        return toJsonStr(this.list.get(0));
    }


    public String asJsonList() throws DotStateException {
        StringBuilder sb = new StringBuilder('[');
        for (int i = 0; i < list.size(); i++) {
            sb.append(toJsonStr(list.get(i)));
            if (i != list.size()) {
                sb.append(',');
            }
        }
        sb.append(']');

        return sb.toString();
    }
    static class JsonWrapper<T> {
        @JsonUnwrapped
        final private T inner;
        final private Class implClass;

        public JsonWrapper(T inner, Class field) {
            this.inner = inner;
            this.implClass = field;
        }

        public T getInner() {
            return inner;
        }

        public Class getImplClass() {
            return implClass;
        }
    }
}


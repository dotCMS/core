package com.dotcms.model.contenttype;

import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.model.workflow.Workflow;
import com.dotcms.model.annotation.ValueType;
import com.dotcms.model.views.CommonViews;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.util.List;
import org.immutables.value.Value;

/**
 * This class maps a Request to Create of Update a ContentType
 * A Separate Class was created to deal with different forms of the attributes required by the save and update endpoints
 */
@JsonTypeInfo(use = Id.CLASS, property = "clazz")
@JsonTypeIdResolver(value = AbstractSaveContentTypeRequest.ClassNameAliasResolver.class)
@ValueType
@Value.Immutable
@JsonSerialize(as = SaveContentTypeRequest.class)
@JsonDeserialize(as = SaveContentTypeRequest.class)
public abstract class AbstractSaveContentTypeRequest extends ContentType {

    @JsonIgnore
    @Value.Default
    @Value.Auxiliary
    public Class<? extends ContentType> typeInf() {
        return SimpleContentType.class;
    }

    /**
     * This is a calculated field required only when sending the CT for a save or update
     * When pulling down the CT this shouldn't be present
     */
    @JsonView({CommonViews.ContentTypeExternalView.class})
    @Value.Derived
    public List<Workflow> workflow() {
        return workflows();
    }

    /**
     * We need to feed our API with the actual ContentType Class wrapped within this request
     * If we leave this as it is the clazz name attribute would end up have a fixed value like `SaveContentTypeRequest`
     * The Original ContentType class name is required
     */
    public static class ClassNameAliasResolver extends ClassNameIdResolver {

        static TypeFactory typeFactory = TypeFactory.defaultInstance();

        public ClassNameAliasResolver() {
            super(typeFactory.constructType(new TypeReference<ContentType>() {
            }), typeFactory, ClientObjectMapper.defaultPolymorphicTypeValidator());
        }

        @Override
        public String idFromValue(Object value) {
            final AbstractSaveContentTypeRequest request = (AbstractSaveContentTypeRequest)value;
            return request.typeInf().getName();
        }

    }

}

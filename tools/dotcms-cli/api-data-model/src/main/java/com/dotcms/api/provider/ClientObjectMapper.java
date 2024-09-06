package com.dotcms.api.provider;

import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableCategoryField;
import com.dotcms.contenttype.model.field.ImmutableCheckboxField;
import com.dotcms.contenttype.model.field.ImmutableColumnField;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.ImmutableCustomField;
import com.dotcms.contenttype.model.field.ImmutableDateField;
import com.dotcms.contenttype.model.field.ImmutableDateTimeField;
import com.dotcms.contenttype.model.field.ImmutableEmptyField;
import com.dotcms.contenttype.model.field.ImmutableFileField;
import com.dotcms.contenttype.model.field.ImmutableHiddenField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableImageField;
import com.dotcms.contenttype.model.field.ImmutableJSONField;
import com.dotcms.contenttype.model.field.ImmutableKeyValueField;
import com.dotcms.contenttype.model.field.ImmutableLineDividerField;
import com.dotcms.contenttype.model.field.ImmutableMultiSelectField;
import com.dotcms.contenttype.model.field.ImmutablePermissionTabField;
import com.dotcms.contenttype.model.field.ImmutableRadioField;
import com.dotcms.contenttype.model.field.ImmutableRelationshipField;
import com.dotcms.contenttype.model.field.ImmutableRelationshipsTabField;
import com.dotcms.contenttype.model.field.ImmutableRowField;
import com.dotcms.contenttype.model.field.ImmutableSelectField;
import com.dotcms.contenttype.model.field.ImmutableStoryBlockField;
import com.dotcms.contenttype.model.field.ImmutableTabDividerField;
import com.dotcms.contenttype.model.field.ImmutableTagField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.ImmutableTimeField;
import com.dotcms.contenttype.model.field.ImmutableWysiwygField;
import com.dotcms.contenttype.model.type.ImmutableDotAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFormContentType;
import com.dotcms.contenttype.model.type.ImmutableKeyValueContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutablePersonaContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.ImmutableVanityUrlContentType;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.ext.ContextResolver;

public class ClientObjectMapper implements ContextResolver<ObjectMapper> {

    public ClientObjectMapper() {
    }
    /**
     * according to: <a href="https://lankydan.dev/providing-your-own-jackson-objectmapper-in-quarkus">...</a>
     * this is how we customize the object mapper here we need to register GuavaModule as we use
     * ImmutableList in the generated code so jackson needs to know how to Serialize/Deserialize it
     * Other proposals can be found here <a href="https://stackoverflow.com/questions/61984336/how-to-configure-objectmapper-for-quarkus-rest-client">...</a>
     */
    @Override
    public ObjectMapper getContext(Class<?> type) {

        final ObjectMapper mapper = new ObjectMapper() {
            /**
             * Accessor for configured PolymorphicTypeValidator used for validating polymorphic subtypes used with explicit polymorphic types (annotation-based), but NOT one with "default typing"
             * For default typing see (activateDefaultTyping(PolymorphicTypeValidator) .
             * We can't have default typing since what it does is  filling the json with additional type-information see <a href="https://github.com/FasterXML/jackson-databind/issues/2838">...</a>
             * By default object mapper will provide LaissezFaireSubTypeValidator but Rest-Easy doesn't like that, and it'll replace it with a WhiteListPolymorphicTypeValidatorBuilder
             * It is hard to tell what the ideal point to inject a pre-configured validator is.  This will do, and it guarantees that no "default typing" is activated
             * @return
             */
            @Override
            public PolymorphicTypeValidator getPolymorphicTypeValidator() {
                return defaultPolymorphicTypeValidator();
            }
        };
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new Jdk8Module())
                .registerModule(new GuavaModule())
                .registerModule(new JavaTimeModule())
                .registerModule(new VersioningModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                //.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                ;

        return mapper;
    }

    public static PolymorphicTypeValidator defaultPolymorphicTypeValidator(){
        return BasicPolymorphicTypeValidator.builder()

                .allowIfSubType(ImmutableSimpleContentType.class)
                .allowIfSubType(ImmutableFileAssetContentType.class)
                .allowIfSubType(ImmutableDotAssetContentType.class)
                .allowIfSubType(ImmutableFormContentType.class)
                .allowIfSubType(ImmutableKeyValueContentType.class)
                .allowIfSubType(ImmutablePageContentType.class)
                .allowIfSubType(ImmutablePersonaContentType.class)
                .allowIfSubType(ImmutableVanityUrlContentType.class)
                .allowIfSubType(ImmutableWidgetContentType.class)

                .allowIfSubType(ImmutableBinaryField.class)
                .allowIfSubType(ImmutableCategoryField.class)
                .allowIfSubType(ImmutableCheckboxField.class)
                .allowIfSubType(ImmutableColumnField.class)
                .allowIfSubType(ImmutableConstantField.class)
                .allowIfSubType(ImmutableCustomField.class)
                .allowIfSubType(ImmutableDateField.class)
                .allowIfSubType(ImmutableDateTimeField.class)
                .allowIfSubType(ImmutableEmptyField.class)
                .allowIfSubType(ImmutableFileField.class)
                .allowIfSubType(ImmutableHiddenField.class)
                .allowIfSubType(ImmutableHostFolderField.class)
                .allowIfSubType(ImmutableImageField.class)
                .allowIfSubType(ImmutableKeyValueField.class)
                .allowIfSubType(ImmutableLineDividerField.class)
                .allowIfSubType(ImmutableMultiSelectField.class)
                .allowIfSubType(ImmutablePermissionTabField.class)
                .allowIfSubType(ImmutableRadioField.class)
                .allowIfSubType(ImmutableRelationshipField.class)
                .allowIfSubType(ImmutableRelationshipsTabField.class)
                .allowIfSubType(ImmutableRowField.class)
                .allowIfSubType(ImmutableSelectField.class)
                .allowIfSubType(ImmutableStoryBlockField.class)
                .allowIfSubType(ImmutableTabDividerField.class)
                .allowIfSubType(ImmutableTagField.class)
                .allowIfSubType(ImmutableTextAreaField.class)
                .allowIfSubType(ImmutableTextField.class)
                .allowIfSubType(ImmutableTimeField.class)
                .allowIfSubType(ImmutableWysiwygField.class)
                .allowIfSubType(ImmutableJSONField.class)

                .allowIfSubType(Map.class)
                .allowIfSubType(List.class)
                .build();
    }

}

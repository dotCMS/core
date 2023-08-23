package com.dotcms.api.provider;

import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.CheckboxField;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.EmptyField;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
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
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.PermissionTabField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.contenttype.model.type.FormContentType;
import com.dotcms.contenttype.model.type.ImmutableDotAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFormContentType;
import com.dotcms.contenttype.model.type.ImmutableKeyValueContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutablePersonaContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.ImmutableVanityUrlContentType;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.contenttype.model.type.PageContentType;
import com.dotcms.contenttype.model.type.PersonaContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ext.ContextResolver;

public class ClientObjectMapper implements ContextResolver<ObjectMapper> {

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
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new VersioningModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion( Include.NON_DEFAULT);
      //  TODO: We need to release failure proof for unknown properties
      //  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

                .allowIfSubType(Map.class)
                .allowIfSubType(List.class)
                .build();
    }

}

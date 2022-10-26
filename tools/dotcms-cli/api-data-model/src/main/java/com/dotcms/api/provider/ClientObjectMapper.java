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
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.contenttype.model.type.PageContentType;
import com.dotcms.contenttype.model.type.PersonaContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonInclude.Value;
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
                return BasicPolymorphicTypeValidator.builder()

                        .allowIfBaseType(FileAssetContentType.class)
                        .allowIfBaseType(DotAssetContentType.class)
                        .allowIfBaseType(FormContentType.class)
                        .allowIfBaseType(KeyValueContentType.class)
                        .allowIfBaseType(PageContentType.class)
                        .allowIfBaseType(PersonaContentType.class)
                        .allowIfBaseType(SimpleContentType.class)
                        .allowIfBaseType(VanityUrlContentType.class)
                        .allowIfBaseType(WidgetContentType.class)

                        .allowIfSubType(BinaryField.class)
                        .allowIfSubType(CategoryField.class)
                        .allowIfSubType(CheckboxField.class)
                        .allowIfSubType(ColumnField.class)
                        .allowIfSubType(ConstantField.class)
                        .allowIfSubType(CustomField.class)
                        .allowIfSubType(DateField.class)
                        .allowIfSubType(DateTimeField.class)
                        .allowIfSubType(EmptyField.class)
                        .allowIfSubType(FileField.class)
                        .allowIfSubType(HiddenField.class)
                        .allowIfSubType(HostFolderField.class)
                        .allowIfSubType(ImageField.class)
                        .allowIfSubType(KeyValueField.class)
                        .allowIfSubType(LineDividerField.class)
                        .allowIfSubType(MultiSelectField.class)
                        .allowIfSubType(PermissionTabField.class)
                        .allowIfSubType(RadioField.class)
                        .allowIfSubType(RelationshipField.class)
                        .allowIfSubType(RelationshipsTabField.class)
                        .allowIfSubType(RowField.class)
                        .allowIfSubType(SelectField.class)
                        .allowIfSubType(StoryBlockField.class)
                        .allowIfSubType(TabDividerField.class)
                        .allowIfSubType(TagField.class)
                        .allowIfSubType(TextAreaField.class)
                        .allowIfSubType(TextField.class)
                        .allowIfSubType(TimeField.class)
                        .allowIfSubType(WysiwygField.class)

                        .allowIfSubType(Map.class)
                        .allowIfSubType(List.class)
                        .build();
            }
        };
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new VersioningModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setDefaultPropertyInclusion(
                Value.construct(Include.NON_EMPTY, Include.CUSTOM, null,
                        ExcludeEmptyObjects.class)
        );
        return mapper;
    }

    /**
     * Custom exclusion class
     * We're supposed to exclude empty and defaults values
     */
    private static class ExcludeEmptyObjects {

        @Override
        public boolean equals(Object o) {

            if (o instanceof Boolean) {
                return !((Boolean) o);
            }

            if (o instanceof Number) {
                return ((Number) o).floatValue() == 0;
            }

            if (o instanceof Map) {
                return ((Map<?, ?>) o).isEmpty();
            }
            if (o instanceof Collection) {
                return ((Collection<?>) o).isEmpty();
            }
            return false;
        }

    }


}

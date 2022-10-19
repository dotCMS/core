package com.dotcms.api.client;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ext.ContextResolver;

public class ClientObjectMapper implements ContextResolver<ObjectMapper> {

    @Override
    public ObjectMapper getContext(Class<?> type) {

        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new VersioningModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mapper.activateDefaultTyping(BasicPolymorphicTypeValidator.builder()

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
                .build());

        mapper.deactivateDefaultTyping();

        return mapper;
    }
}

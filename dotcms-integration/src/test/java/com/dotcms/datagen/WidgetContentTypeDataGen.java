package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.util.stream.Collectors;

public class WidgetContentTypeDataGen extends ContentTypeDataGen {

    private String code;

    public WidgetContentTypeDataGen code(final String code){
        this.code = code;
        return this;
    }

    @WrapInTransaction
    @Override
    public ContentType persist(final ContentType contentType) {
        final ContentType persist = super.persist(contentType);

        final Field widgetCode = persist.fields(ConstantField.class).stream()
                .filter(field -> field.variable().equals("widgetCode"))
                .collect(Collectors.toList())
                .get(0);

        final ImmutableConstantField build = ImmutableConstantField.builder()
                .values(code)
                .variable(widgetCode.variable())
                .contentTypeId(widgetCode.contentTypeId())
                .name(widgetCode.name())
                .id(widgetCode.id())
                .build();

        try {
            APILocator.getContentTypeFieldAPI().save(build, APILocator.systemUser());
            return persist;
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}

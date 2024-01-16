package com.dotcms.cli.common;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.contenttype.AbstractSaveContentTypeRequest;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
        ContentType.ClassNameAliasResolver.class,
        AbstractSaveContentTypeRequest.ClassNameAliasResolver.class,
        Field.ClassNameAliasResolver.class
})
public class ReflectionConfiguration {

}
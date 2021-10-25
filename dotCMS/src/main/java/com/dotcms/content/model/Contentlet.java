package com.dotcms.content.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 *  Represents a content unit in the system. Ideally, every single domain object
 *  in dotCMS will be represented as a Contentlet.
 *  This is an interface to generate an immutable and more structured version of Contentlet
 */
@Value.Immutable
@JsonInclude(Include.NON_NULL)
@JsonSerialize(as = ImmutableContentlet.class)
@JsonDeserialize(as = ImmutableContentlet.class)
public interface Contentlet {

    @Nullable
    String title();
    String inode();
    String identifier();
    String contentType();
    Instant modDate();
    String baseType();
    @Nullable
    Boolean showOnMenu();
    String modUser();
    Long languageId();
    String host();
    String folder();
    @Nullable
    String owner();
    Long sortOrder();
    List<String> disabledWysiwyg();
    Map<String, FieldValue<?>> fields();
    @Nullable
    String friendlyName();

}

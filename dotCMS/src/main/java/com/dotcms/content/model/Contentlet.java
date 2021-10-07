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

@Value.Immutable
@JsonInclude(Include.NON_EMPTY)
@JsonSerialize(as = ImmutableContentlet.class)
@JsonDeserialize(as = ImmutableContentlet.class)
public interface Contentlet {

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

    //TODO: Verify if this is page specific or what other attributes outside contentlet are specific for "concrete" contentlet type
    @Nullable
    String friendlyName();

}

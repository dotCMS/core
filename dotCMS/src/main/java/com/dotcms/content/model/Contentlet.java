package com.dotcms.content.model;

import com.dotcms.content.model.version.ToCurrentVersionConverter;
import com.dotcms.variant.VariantAPI;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.jonpeterson.jackson.module.versioning.JsonSerializeToVersion;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import com.github.jonpeterson.jackson.module.versioning.JsonVersionedModel;

/**
 *  Represents a content unit in the system. Ideally, every single domain object
 *  in dotCMS will be represented as a Contentlet.
 *  This is an interface to generate an immutable and more structured version of Contentlet
 */
@Value.Immutable
@JsonInclude(Include.NON_NULL)
@JsonSerialize(as = ImmutableContentlet.class)
@JsonDeserialize(as = ImmutableContentlet.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonVersionedModel(currentVersion = Contentlet.CURRENT_MODEL_VERSION, defaultDeserializeToVersion = "1", toCurrentConverterClass = ToCurrentVersionConverter.class)
public interface Contentlet {

    String CURRENT_MODEL_VERSION = "2";

    @Value.Default
    @JsonProperty
    @JsonSerializeToVersion(defaultToSource = true)
    default String modelVersion() {return CURRENT_MODEL_VERSION;}

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
    @Nullable
    String owner();
    Long sortOrder();
    List<String> disabledWysiwyg();
    Map<String, FieldValue<?>> fields();
    @Nullable
    String friendlyName();

}

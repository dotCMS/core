package com.dotcms.model.content;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = Contentlet.class)
public interface AbstractContentlet {

    String stName();

    String title();

    String url();

    String friendlyName();

    String template();

    String sortOrder();

    String cachettl();

    String hostFolder();
}

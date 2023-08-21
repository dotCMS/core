package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Date;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableWorkflow.class)
@JsonDeserialize(as = ImmutableWorkflow.class)
public interface Workflow {

    @Nullable
    Boolean archived();

    @Nullable
    Date creationDate();

    @Nullable
    Boolean defaultScheme();

    @Nullable
    String description();

    @Nullable
    String entryActionId();

    @Nullable
    String id();

    @Nullable
    Boolean mandatory();

    @Nullable
    Date modDate();

    @Nullable
    String name();

    @Nullable
    Boolean system();

}

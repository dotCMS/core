package com.dotcms.model.contenttype;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.model.AbstractResponseEntityView;
import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = ContentTypesResponse.class)
public interface AbstractContentTypesResponse extends AbstractResponseEntityView<List<ContentType>> {


}

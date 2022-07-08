package com.dotcms.model.contenttype;

import com.dotcms.model.AbstractResponseEntityView;
import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = GetContentTypesAbstractResponseEntityView.class)
public interface AbstractGetContentTypesAbstractResponseEntityView extends
        AbstractResponseEntityView<List<ContentType>> {

}

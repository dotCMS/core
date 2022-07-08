package com.dotcms.model.site;

import com.dotcms.model.AbstractResponseEntityView;
import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = GetSitesAbstractResponseEntityView.class)
public interface AbstractGetSitesAbstractResponseEntityView extends
        AbstractResponseEntityView<List<Site>> {

}

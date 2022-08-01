package com.dotcms.rest.api.v1.site;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;

/**
 * Response for {@link HostVariable}
 * @author jsanca
 */
public class ResponseHostVariableEntityView extends ResponseEntityView<HostVariable> {


    public ResponseHostVariableEntityView(final HostVariable entity) {
        super(entity);
    }
}

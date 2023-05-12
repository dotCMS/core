package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;
import java.util.Map;

public class LayoutMapResponseEntityView extends ResponseEntityView<List<Map<String, Object>>> {

    public LayoutMapResponseEntityView(final List<Map<String, Object>> entity) {
        super(entity);
    }
}

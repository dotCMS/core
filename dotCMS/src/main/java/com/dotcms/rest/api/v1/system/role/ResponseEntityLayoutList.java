package com.dotcms.rest.api.v1.system.role;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.business.Layout;
import java.util.List;

public class ResponseEntityLayoutList extends ResponseEntityView<List<Layout>> {

    public ResponseEntityLayoutList(List<Layout> entity) {
        super(entity);
    }
}

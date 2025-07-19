package com.dotcms.rest.api.v1.menu;

import com.dotcms.rest.ResponseEntityView;
import java.util.Collection;

/**
 * Entity View for navigation menu responses.
 * Contains menu layout information and navigation items for authenticated users.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityMenuView extends ResponseEntityView<Collection<Menu>> {
    public ResponseEntityMenuView(final Collection<Menu> entity) {
        super(entity);
    }
}
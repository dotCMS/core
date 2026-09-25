package com.dotcms.rest.api.v1.layout;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response envelope for a single navigation section, returned by the create and update writes.
 *
 * @author hassandotcms
 */
public class ResponseEntitySectionView extends ResponseEntityView<SectionView> {

    public ResponseEntitySectionView(final SectionView entity) {
        super(entity);
    }
}

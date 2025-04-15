package com.dotcms.rest.api.v1.variants;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.variant.model.Variant;

/**
 * Response entity view for {@link Variant}
 * @author jsanca
 */
public class ResponseEntityVariantView extends ResponseEntityView<Variant> {

    public ResponseEntityVariantView(final Variant entity) {
        super(entity);
    }

}

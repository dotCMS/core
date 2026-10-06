package com.dotcms.rest.api.v1.company;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for company configuration endpoints.
 *
 * @author hassandotcms
 */
public class ResponseEntityCompanyConfigView extends ResponseEntityView<CompanyConfigView> {

    public ResponseEntityCompanyConfigView(final CompanyConfigView entity) {
        super(entity);
    }
}

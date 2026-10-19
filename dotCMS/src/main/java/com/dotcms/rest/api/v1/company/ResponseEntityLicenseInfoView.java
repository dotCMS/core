package com.dotcms.rest.api.v1.company;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for the license endpoint.
 *
 * @author hassandotcms
 */
public class ResponseEntityLicenseInfoView extends ResponseEntityView<LicenseInfoView> {

    public ResponseEntityLicenseInfoView(final LicenseInfoView entity) {
        super(entity);
    }
}

package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.business.web.SelectedExperiment;
import com.dotcms.experiments.business.web.SelectedExperiments;
import com.dotcms.rest.ResponseEntityView;

/**
 * {@link ResponseEntityView} for an array of {@link SelectedExperiment}
 */
public class ResponseEntityExperimentSelectedView extends ResponseEntityView<SelectedExperiments> {

    public ResponseEntityExperimentSelectedView(final SelectedExperiments entity) {
        super(entity);
    }
}
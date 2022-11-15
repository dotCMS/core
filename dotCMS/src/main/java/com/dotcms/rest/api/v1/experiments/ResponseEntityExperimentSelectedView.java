package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.business.web.SelectedExperiment;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.rest.ResponseEntityView;

/**
 * {@link ResponseEntityView} for {@link Experiment}
 */
public class ResponseEntityExperimentSelectedView extends
        ResponseEntityView<SelectedExperiment> {

    public ResponseEntityExperimentSelectedView(final SelectedExperiment entity) {
        super(entity);
    }
}
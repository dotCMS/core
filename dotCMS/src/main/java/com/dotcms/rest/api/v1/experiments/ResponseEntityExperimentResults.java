package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.business.result.ExperimentResults;
import com.dotcms.experiments.business.web.SelectedExperiments;
import com.dotcms.rest.ResponseEntityView;

public class ResponseEntityExperimentResults extends ResponseEntityView<ExperimentResults> {

    public ResponseEntityExperimentResults(final ExperimentResults entity) {
        super(entity);
    }
}

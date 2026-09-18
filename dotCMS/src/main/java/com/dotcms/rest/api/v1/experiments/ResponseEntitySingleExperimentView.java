package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.rest.ResponseEntityView;

public class ResponseEntitySingleExperimentView extends ResponseEntityView<ExperimentView>  {
    public ResponseEntitySingleExperimentView(final Experiment entity) {
        super(ExperimentView.of(entity));
    }
}

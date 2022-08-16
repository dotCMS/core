package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.rest.ResponseEntityView;
import java.util.List;

public class ResponseEntityExperimentView extends ResponseEntityView<List<Experiment>>  {
    public ResponseEntityExperimentView(final List<Experiment> entity) {
        super(entity);
    }
}

package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.rest.ResponseEntityView;
import java.util.List;

public class ResponseEntitySingleExperimentView extends ResponseEntityView<Experiment>  {
    public ResponseEntitySingleExperimentView(final Experiment entity) {
        super(entity);
    }
}

package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.rest.ResponseEntityView;
import java.util.List;
import java.util.stream.Collectors;

public class ResponseEntityExperimentView extends ResponseEntityView<List<ExperimentView>>  {
    public ResponseEntityExperimentView(final List<Experiment> entity) {
        super(entity.stream().map(ExperimentView::of).collect(Collectors.toList()));
    }
}

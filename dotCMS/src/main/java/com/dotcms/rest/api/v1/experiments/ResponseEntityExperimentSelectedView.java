package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.business.web.SelectedExperiment;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * {@link ResponseEntityView} for an array of {@link SelectedExperiment}
 */
public class ResponseEntityExperimentSelectedView extends
        ResponseEntityView<List<SelectedExperiment>> {

    public ResponseEntityExperimentSelectedView(final List<SelectedExperiment> entity) {
        super(entity);
    }
}
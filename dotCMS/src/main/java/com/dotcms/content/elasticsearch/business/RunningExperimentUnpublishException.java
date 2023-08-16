package com.dotcms.content.elasticsearch.business;

import com.dotcms.experiments.model.Experiment;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;

/**
 * Exception thrown when a Page is attempted to be unpublished but it has a RUNNING Experiment
 */
public class RunningExperimentUnpublishException extends DotRuntimeException {


    public RunningExperimentUnpublishException(final Contentlet page, final Experiment experiment) {
        super(String.format("Cannot unpublish a Page %s because it has a RUNNING Experiment: %s",
                page.getIdentifier(), experiment.id().get()));
    }
}

/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ExperimentBundler;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.publisher.pusher.wrapper.ExperimentWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Experiments-related information inside a
 * bundle and saves it in the receiving instance. This class will read and process only the {@link Experiment} data
 * files.
 *
 */
public class ExperimentHandler implements IHandler {

	private final PublisherConfig config;
	private final ExperimentsAPI experimentsAPI;
	public ExperimentHandler(final PublisherConfig config) {

		this.config = config;
		this.experimentsAPI = APILocator.getExperimentsAPI();
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@WrapInTransaction
	@Override
	public void handle(final File bundleFolder) throws Exception {

	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
	        throw new RuntimeException("need an enterprise pro license to run this");
        }

		final Collection<File> experiments = FileUtil.listFilesRecursively
				(bundleFolder, new ExperimentBundler().getFileFilter());

        handleExperiments(experiments);
	}

	private void handleExperiments(final Collection<File> experiments) throws DotPublishingException {
		File workingOn = null;
        Experiment experiment = null;
		try {
	        //Handle folders
	        for(final File experimentFile: experiments) {
				workingOn = experimentFile;
	        	if(experimentFile.isDirectory()) {

	        		continue;
				}

	        	final ExperimentWrapper experimentWrapper = BundlerUtil
						.jsonToObject(experimentFile, ExperimentWrapper.class);

				experiment = experimentWrapper.getExperiment();

	        	Optional<Experiment> localExperiment = experimentsAPI.find(experiment.id()
						.orElseThrow(), APILocator.systemUser());

	        	if(experimentWrapper.getOperation().equals(Operation.UNPUBLISH)) {
	        		// delete operation
	        	    if(localExperiment.isPresent()) {

						if(localExperiment.get().status()==Status.RUNNING ||
							localExperiment.get().status()==Status.SCHEDULED) {
							experimentsAPI.cancel(localExperiment.get().id().orElseThrow()
									, APILocator.systemUser());
						}

						experimentsAPI.delete(localExperiment.orElseThrow().id().orElseThrow(),
								APILocator.systemUser());
					}
	        	} else {
	        		// save or update Experiment
					if(experiment.status()== Status.RUNNING || experiment.status()== Status.SCHEDULED) {
						Experiment asDraft = Experiment.builder().from(experiment)
								.status(Status.DRAFT).build();

						final Optional<Scheduling> schedulingOptional = asDraft.scheduling();

						if(experiment.status()==Status.RUNNING) {
							asDraft = asDraft.withScheduling(Optional.empty());
						}

						asDraft = experimentsAPI.save(asDraft, APILocator.systemUser());

						if(experiment.status()==Status.RUNNING) {
							experimentsAPI.forceStart(asDraft.id().orElseThrow(),
									APILocator.systemUser(), schedulingOptional.get());
						} else {
							experimentsAPI.forceScheduled(asDraft.id().orElseThrow(),
									APILocator.systemUser(), schedulingOptional.get());
						}

					} else if(experiment.status()==Status.ENDED && localExperiment.isPresent()
							&& localExperiment.get().status()==Status.RUNNING) {
						experimentsAPI.end(localExperiment.orElseThrow().id().orElseThrow(),
									APILocator.systemUser());
					} else {
						experimentsAPI.save(experiment, APILocator.systemUser());
					}
				}
			}
    	} catch (final Exception e) {
			final String errorMsg = String.format("An error occurred when processing Experiment in '%s' with Name '%s' [%s]: %s",
					workingOn, null != experiment ? experiment.name() : "- null -",
						null != experiment ? experiment.id().orElseThrow() : "- null -", ExceptionUtil.getErrorMessage(e));
			Logger.error(this.getClass(), errorMsg, e);
			throw new DotPublishingException(errorMsg, e);
    	}
    }

}

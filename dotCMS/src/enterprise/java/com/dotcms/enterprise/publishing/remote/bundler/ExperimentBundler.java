/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditAPIImpl;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.ExperimentWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

/**
 * This bundler will take the list of {@link com.dotcms.experiments.model.Experiment} objects that are being pushed
 * and will write them in the file system in the form of an XML file. This
 * information will be part of the bundle that will be pushed to the destination
 * server.
 */
public class ExperimentBundler implements IBundler {

	public final static String EXTENSION = ".experiment.json";
	public final static String NAME = "Experiment bundler";

	private PushPublisherConfig config = null;

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		this.config = (PushPublisherConfig) pc;
	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	public void generate(final BundleOutput bundleOutput, final BundlerStatus status) throws DotBundleException {
		if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
			throw new RuntimeException("Need an enterprise pro license to run this bundler.");
		}
		final Set<String> experimentsIds = config.getExperiments();
		PublishAuditAPI publishAuditAPI = PublishAuditAPIImpl.getInstance();
		User systemUser = null;
		PublishAuditHistory currentStatusHistory = null;
		String experimentToProcess = "";
		try {
			// Updating the audit table
			if (!config.isDownloading() && experimentsIds.size() > 0) {
				currentStatusHistory = publishAuditAPI.getPublishAuditStatus(config.getId()).getStatusPojo();
				if (currentStatusHistory == null) {
					currentStatusHistory = new PublishAuditHistory();
				}
				if (currentStatusHistory.getBundleStart() == null) {
					currentStatusHistory.setBundleStart(new Date());
					PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
					publishAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.BUNDLING,
							currentStatusHistory);
				}
			}
			systemUser = APILocator.getUserAPI().getSystemUser();
			final ExperimentsAPI experimentsAPI = APILocator.getExperimentsAPI();
			for (String experimentId : experimentsIds) {
				experimentToProcess = experimentId;
				// Fill up the Experiment object with its respective data
				final Experiment experiment = experimentsAPI.find(experimentId, systemUser).orElseThrow();

				try {
					writeExperiment(bundleOutput, experiment);
				} catch (IOException e) {
					status.addFailure();
					throw new DotBundleException(this.getClass().getName() + ": An error occurred when writing Experiment ["
							+ experimentId + "] to the file system.", e);
				}
			}
			// Updating the audit table
			if (currentStatusHistory != null && !config.isDownloading() && experimentsIds.size() > 0) {
				currentStatusHistory = publishAuditAPI.getPublishAuditStatus(config.getId()).getStatusPojo();
				currentStatusHistory.setBundleEnd(new Date());
				PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
				publishAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.BUNDLING,
						currentStatusHistory);
			}
		} catch (DotDataException e) {
			status.addFailure();
			throw new DotBundleException(this.getClass().getName() + ": An error occurred when retrieving data from Experiment ["
					+ experimentToProcess + "]", e);
		} catch (DotSecurityException e) {
			status.addFailure();
			throw new DotBundleException(this.getClass().getName() + ": User "
					+ (systemUser != null ? systemUser.getUserId() : "") + " does not have permissions to access Experiment ["
					+ experimentToProcess + "]", e);
		} catch (DotPublisherException e) {
			status.addFailure();
			throw new DotBundleException(this.getClass().getName() + "Unable to update Publish Audit Status for bundle: "
					+ config.getId(), e);
		}
	}

	@Override
	public FileFilter getFileFilter() {
		return new ExperimentBundlerFilter();
	}

	/**
	 * A simple file filter that looks for Experiment data files inside a bundle.
	 */
	public class ExperimentBundlerFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			return (pathname.isDirectory() || pathname.getName().endsWith(EXTENSION));
		}

	}

	/**
	 * Writes the properties of a {@link Experiment} object to the file system, so
	 * that it can be bundled and pushed to the destination server.
	 * 
	 * @param bundleOutput
	 *            - The root location of the bundle in the file system.
	 * @param experiment
	 *            - The {@link Experiment} object to write.
	 * @throws IOException
	 *             An error occurred when writing the experiment to the file system.
	 * @throws DotDataException
	 *             An error occurred reading information from the database.
	 * @throws DotSecurityException
	 *             The current user does not have the required permissions to
	 *             perform this action.
	 */
	private void writeExperiment(final BundleOutput bundleOutput, final Experiment experiment)
			throws IOException, DotDataException, DotSecurityException {

		final ExperimentWrapper wrapper = new ExperimentWrapper();
		wrapper.setExperiment(experiment);
		wrapper.setOperation(config.getOperation());
		String uri = experiment.id().orElseThrow();
		if (!uri.endsWith(EXTENSION)) {
			uri.replace(EXTENSION, "").trim();
			uri += EXTENSION;
		}
		final User systemUser = APILocator.getUserAPI().getSystemUser();
		Host host;
		final Contentlet pageAsContentlet = APILocator.getContentletAPI()
				.findContentletByIdentifierAnyLanguage(experiment.pageId());
		if (UtilMethods.isSet(pageAsContentlet)) {
				host = APILocator.getHostAPI().find(pageAsContentlet.getHost(), systemUser, false);
		} else {
			throw new DotDataException("The page ID [" + experiment.pageId() + "] cannot be found for Experiment [" + experiment.id().get()
					+ "]");
		}
		final String experimentFileUrl = File.separator + "live" + File.separator + host.getHostname()
				+ File.separator + uri;

		if (!bundleOutput.exists(experimentFileUrl)) {
			try (final OutputStream outputStream = bundleOutput.addFile(experimentFileUrl)) {
				BundlerUtil.objectToJSON(wrapper, outputStream);
			}

			bundleOutput.setLastModified(experimentFileUrl, Calendar.getInstance().getTimeInMillis());
		}
		if (Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Experiment bundled for pushing -> Operation: " + config.getOperation() + "; ID: "
					+ experiment.id().get(), config.getId());
		}
	}

}

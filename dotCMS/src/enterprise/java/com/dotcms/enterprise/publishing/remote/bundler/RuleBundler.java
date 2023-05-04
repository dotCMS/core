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

import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.business.PermissionAPI;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditAPIImpl;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.RuleWrapper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PushPublishLogger;
import com.liferay.portal.model.User;

/**
 * This bundler will take the list of {@link Rule} objects that are being pushed
 * and will write them in the file system in the form of an XML file. This
 * information will be part of the bundle that will be pushed to the destination
 * server.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since Mar 8, 2016
 *
 */
public class RuleBundler implements IBundler {

	public final static String EXTENSION = ".rule.xml";
	public final static String NAME = "Rule bundler";

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
		final Set<String> ruleIds = config.getRules();
		PublishAuditAPI publishAuditAPI = PublishAuditAPIImpl.getInstance();
		User systemUser = null;
		PublishAuditHistory currentStatusHistory = null;
		String ruleToProcess = "";
		try {
			// Updating the audit table
			if (!config.isDownloading() && ruleIds.size() > 0) {
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
			final RulesAPI rulesAPI = APILocator.getRulesAPI();
			for (String ruleId : ruleIds) {
				ruleToProcess = ruleId;
				// Fill up the Rule object with its respective data
				final Rule rule = rulesAPI.getRuleById(ruleId, systemUser, false);
				for (ConditionGroup conditionGroup : rule.getGroups()) {
					conditionGroup.getConditions();
				}
				rule.getRuleActions();
				try {
					writeRule(bundleOutput, rule);
				} catch (IOException e) {
					status.addFailure();
					throw new DotBundleException(this.getClass().getName() + ": An error occurred when writing rule ["
							+ ruleId + "] to the file system.", e);
				}
			}
			// Updating the audit table
			if (currentStatusHistory != null && !config.isDownloading() && ruleIds.size() > 0) {
				currentStatusHistory = publishAuditAPI.getPublishAuditStatus(config.getId()).getStatusPojo();
				currentStatusHistory.setBundleEnd(new Date());
				PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
				publishAuditAPI.updatePublishAuditStatus(config.getId(), PublishAuditStatus.Status.BUNDLING,
						currentStatusHistory);
			}
		} catch (DotDataException e) {
			status.addFailure();
			throw new DotBundleException(this.getClass().getName() + ": An error occurred when retrieving data from rule ["
					+ ruleToProcess + "]", e);
		} catch (DotSecurityException e) {
			status.addFailure();
			throw new DotBundleException(this.getClass().getName() + ": User "
					+ (systemUser != null ? systemUser.getUserId() : "") + " does not have permissions to access rule ["
					+ ruleToProcess + "]", e);
		} catch (DotPublisherException e) {
			status.addFailure();
			throw new DotBundleException(this.getClass().getName() + "Unable to update Publish Audit Status for bundle: "
					+ config.getId(), e);
		}
	}

	@Override
	public FileFilter getFileFilter() {
		return new RuleBundlerFilter();
	}

	/**
	 * A simple file filter that looks for rule data files inside a bundle.
	 * 
	 * @author Jose Castro
	 * @version 1.0
	 * @since Mar 8, 2016
	 *
	 */
	public class RuleBundlerFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			return (pathname.isDirectory() || pathname.getName().endsWith(EXTENSION));
		}

	}

	/**
	 * Writes the properties of a {@link Rule} object to the file system, so
	 * that it can be bundled and pushed to the destination server.
	 * 
	 * @param bundleOutput
	 *            - The root location of the bundle in the file system.
	 * @param rule
	 *            - The {@link Rule} object to write.
	 * @throws IOException
	 *             An error occurred when writing the rule to the file system.
	 * @throws DotDataException
	 *             An error occurred reading information from the database.
	 * @throws DotSecurityException
	 *             The current user does not have the required permissions to
	 *             perform this action.
	 */
	private void writeRule(final BundleOutput bundleOutput, final Rule rule)
			throws IOException, DotDataException, DotSecurityException {

		final RuleWrapper wrapper = new RuleWrapper(rule);
		wrapper.setOperation(config.getOperation());
		String uri = rule.getId();
		if (!uri.endsWith(EXTENSION)) {
			uri.replace(EXTENSION, "").trim();
			uri += EXTENSION;
		}
		final User systemUser = APILocator.getUserAPI().getSystemUser();
		Host host = null;
		final List<Contentlet> contentlets = APILocator.getContentletAPI().searchByIdentifier(
				"+identifier:" + rule.getParent(), 1, 0, null, systemUser, false,
				PermissionAPI.PERMISSION_READ, true);
		if (contentlets != null && contentlets.size() > 0) {
			Contentlet ruleParent = contentlets.get(0);
			if (ruleParent.isHost()) {
				host = APILocator.getHostAPI().find(rule.getParent(), systemUser, false);
			} else if (ruleParent.isHTMLPage()) {
				Contentlet parentPage = contentlets.get(0);
				host = APILocator.getHostAPI().find(parentPage.getHost(), systemUser, false);
			} else {
				throw new DotDataException("The parent ID [" + ruleParent.getIdentifier() + "] is a non-valid parent.");
			}
		} else {
			throw new DotDataException("The parent ID [" + rule.getParent() + "] cannot be found for Rule [" + rule.getId()
					+ "]");
		}
		final String ruleFileUrl = File.separator + "live" + File.separator + host.getHostname()
				+ File.separator + uri;

		if (!bundleOutput.exists(ruleFileUrl)) {
			try (final OutputStream outputStream = bundleOutput.addFile(ruleFileUrl)) {
				BundlerUtil.objectToXML(wrapper, outputStream);
			}

			bundleOutput.setLastModified(ruleFileUrl, Calendar.getInstance().getTimeInMillis());
		}
		if (Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Rule bundled for pushing -> Operation: " + config.getOperation() + "; ID: "
					+ rule.getId(), config.getId());
		}
	}

}

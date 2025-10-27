/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
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

    public final static String[] RULE_EXTENSIONS = {".rule.xml", ".rule.json"};
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
        return new ExtensionFileFilter(RULE_EXTENSIONS);
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

        for (String extension : RULE_EXTENSIONS) {

            String uri = rule.getId();
            if (!uri.endsWith(extension)) {
                uri.replace(extension, "").trim();
                uri += extension;
            }

            final String ruleFileUrl = File.separator + "live" + File.separator + host.getHostname()
                    + File.separator + uri;

            if (!bundleOutput.exists(ruleFileUrl)) {
                try (final OutputStream outputStream = bundleOutput.addFile(ruleFileUrl)) {
                    BundlerUtil.writeObject(wrapper, outputStream, ruleFileUrl);
                }

                bundleOutput.setLastModified(ruleFileUrl, Calendar.getInstance().getTimeInMillis());
            }
        }
		if (Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Rule bundled for pushing -> Operation: " + config.getOperation() + "; ID: "
					+ rule.getId(), config.getId());
		}
	}

}

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
import com.dotcms.enterprise.publishing.remote.bundler.RuleBundler;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.RuleWrapper;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.PushPublishLogger.PushPublishAction;
import com.dotmarketing.util.PushPublishLogger.PushPublishHandler;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * This handler deals with Rules-related information inside a bundle and saves
 * it in the destination server. This class will read only the {@link Rule} data
 * files to retrieve their information and persist it in the destination server.
 * <p>
 * Given the nature of these objects, a {@link Rule} is not versionable. Also,
 * two or more rules with the same name can exist in the same Site as long as
 * their Identifiers are different. The Integrity Checker does not interact with
 * these objects.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since Mar 9, 2016
 *
 */
public class RuleHandler implements IHandler {

	private RulesAPI rulesAPI = null;
	private PublisherConfig config = null;
	private List<String> warnings;

	/**
	 * Default class constructor. Initializes the handler with the configuration
	 * of the Publisher selected to bundle the data.
	 * 
	 * @param config
	 *            - The {@link PublisherConfig} object that has the main
	 *            configuration values for the bundle that is being published.
	 */
	public RuleHandler(PublisherConfig config) {
		this.config = config;
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void handle(File bundleFolder) throws Exception {
		if (LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
			throw new RuntimeException("You need an enterprise pro license to run this.");
		}
		this.rulesAPI = APILocator.getRulesAPI();
        warnings = new ArrayList<>();
		final Collection<File> rules = FileUtil.listFilesRecursively(bundleFolder, new RuleBundler().getFileFilter());
		handleRules(rules);
	}

	/*
	* If the file is null, then is invalid
	* If the file does not exists, then is invalid
	* If the file can not be read, then is invalid
	* If the file is a directory, then is invalid
	* If the file is empty, then is invalid
	 */
	private boolean isInvalidFile (final File file) {

		return null == file || !file.exists() || !file.canRead() || file.isDirectory() || file.length() == 0;
	}

    @Override
    public List<String> getWarnings() {
        return warnings;
    }

    /**
	 * Reads the information of the rules contained in the bundle and saves them
	 * in the destination server. For each rule in the bundle, the persistence
	 * process is:
	 * <p>
	 * For <b>PUBLISH</b>:
	 * <ul>
	 * <li>The rule is saved or updated.</li>
	 * <li>If the rule exists, delete all existing Condition Groups, Conditions,
	 * Condition Values, and Actions.</li>
	 * <li>Re-created the Condition Groups, Conditions, Condition Values, and
	 * Actions using the data coming from the bundle.</li>
	 * </ul>
	 * <p>
	 * For <b>UNPUBLISH</b>:
	 * <ul>
	 * <li>Simply delete <b>all</b> the information related to the rule.</li>
	 * </ul>
	 * 
	 * @param rules
	 *            - The list of data files containing the rules information.
	 * @throws DotPublishingException
	 *             An error occurred when pushing the new content.
	 */
	private void handleRules(final Collection<File> rules) throws DotPublishingException {

		final XStream xStream = XStreamHandler.newXStreamInstance();
		String ruleToProcess  = StringPool.BLANK;
		File ruleFileToRead   = null;
		User systemUser;

		try {
			systemUser = APILocator.getUserAPI().getSystemUser();
        } catch (DotDataException e) {
            final String msg =
                    "Error processing rules for bundle ["
                            + this.config.getId()
                            + "]. Reason: " + e.getMessage()
                            + ". The publishing process will continue and ignore this warning...";
            Logger.warnAndDebug(this.getClass(), msg, e);
            warnings.add(msg);
            return;
        }

        for (final File ruleFile : rules) {
            try {
				if (this.isInvalidFile(ruleFile)) {
					continue;
				}

				ruleFileToRead = ruleFile;
				final RuleWrapper wrapper;

				try (final InputStream input = Files.newInputStream(ruleFile.toPath())){
					wrapper = (RuleWrapper) xStream.fromXML(input);
				}

				final Rule receivedRule = wrapper.getRule();
				final Rule localRule    = this.rulesAPI.getRuleById(receivedRule.getId(), systemUser, false);
				ruleToProcess           = receivedRule.getId();

                if (Config.getBooleanProperty("PUSH_PUBLISHING_RULES_OVERWRITE", true)
                        && localRule != null && StringUtils.isNotBlank(localRule.getId())) {
                    this.rulesAPI.deleteRule(localRule, systemUser, false);

                    PushPublishLogger.log(getClass(), PushPublishHandler.RULE,
                            PushPublishAction.UNPUBLISH,
                            localRule.getId(), null, localRule.getName(), config.getId());
                }
                if (wrapper.getOperation().equals(PushPublisherConfig.Operation.PUBLISH)) {
                    // Save/update the rule info
                    this.publishRule(systemUser, receivedRule, localRule);
                }
            } catch (FileNotFoundException e) {
                handleException("Cannot read data file [" + ruleFileToRead.getAbsolutePath() + "] in bundle ["
                        + this.config.getId() + "]. Reason: " + e.getMessage()
                        + ". The publishing process will continue and ignore this warning...", e);
            } catch (DotDataException e) {
                handleException("Cannot retrieve information of rule [" + ruleToProcess + "] in bundle ["
                                + this.config.getId()
                                + "]. Reason: " + e.getMessage()
                                + ". The publishing process will continue and ignore this warning...", e);
            } catch (DotSecurityException e) {
                handleException("User " + systemUser.getUserId()
                        + " does not have permissions to publish rule [" + ruleToProcess
                        + "] in bundle [" + this.config.getId() + "]. Reason: " + e.getMessage()
                        + ". The publishing process will continue and ignore this warning...", e);
            } catch (Exception e) {
                handleException("An error occurred when processing rule [" + ruleToProcess + "] in bundle ["
                                + this.config.getId()
                                + "]. Reason: " + e.getMessage()
                                + ". The publishing process will continue and ignore this warning...", e);
            }
        }
	}

	private void handleException(final String message, final Exception e){
        Logger.warnAndDebug(this.getClass(), message, e);
        warnings.add(message);
    }

	@WrapInTransaction
	private void publishRule(final User systemUser, final Rule receivedRule, final Rule localRule) throws DotDataException, DotSecurityException {

		this.rulesAPI.saveRule(receivedRule, systemUser, false);

		// Saving the Condition Groups
		final List<ConditionGroup> receivedGroups = receivedRule.getGroupsRaw();
		if (null != receivedGroups) {
			for (final ConditionGroup group : receivedGroups) {

				this.rulesAPI.saveConditionGroup(group, systemUser, false);

				final List<Condition> conditions = group.getConditionsRaw();
				if (null != conditions) {
					// Saving the Condition for each group
					for (final Condition condition : conditions) {

						this.rulesAPI.saveCondition(condition, systemUser, false);
					}
				}
			}
		}

		// Saving the Action
		final List<RuleAction> ruleActions = receivedRule.getRuleActionsRaw();
		if (null != ruleActions) {
			for (final RuleAction action : ruleActions) {

				this.rulesAPI.saveRuleAction(action, systemUser, false);
			}
		}

		PushPublishLogger.log(getClass(), PushPublishHandler.RULE, PushPublishAction.PUBLISH,
				receivedRule.getId(), null, receivedRule.getName(), config.getId());
	} // publishRule.

}

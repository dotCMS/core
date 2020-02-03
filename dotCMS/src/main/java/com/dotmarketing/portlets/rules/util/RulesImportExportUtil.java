package com.dotmarketing.portlets.rules.util;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 
 * @author Will Ezell
 * @version 1.0
 * @since Jan 25, 2016
 *
 */
public class RulesImportExportUtil {

	private static RulesImportExportUtil rulesImportExportUtil;

	public static RulesImportExportUtil getInstance() {
		if (rulesImportExportUtil == null) {
			synchronized ("RulesImportExportUtil.class") {
				if (rulesImportExportUtil == null) {
					rulesImportExportUtil = new RulesImportExportUtil();
				}
			}
		}
		return rulesImportExportUtil;
	}

	public String exportToJson() throws IOException, DotDataException, DotSecurityException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(buildExportObject(null));
	}

	public void export(File file) throws IOException {

		BufferedWriter out = null;
		try {
			FileWriter fstream = new FileWriter(file);
			out = new BufferedWriter(fstream);

			out.write(exportToJson());

		} catch (Exception e) {// Catch exception if any
			Logger.error(this.getClass(), "Error: " + e.getMessage(), e);
		} finally {
			out.close();
		}
	}

	public void importRules(final File file) throws IOException {

		final ObjectMapper mapper = new ObjectMapper();
		final StringWriter stringWriter = new StringWriter();
		RulesImportExportObject importer = null;

		try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(file.toURI()))) {

			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

			String str;
			while ((str = bufferedReader.readLine()) != null) {
				stringWriter.append(str);
			}

			importer = mapper.readValue
					((String) stringWriter.toString(), RulesImportExportObject.class);

			this.importRules(importer, APILocator.systemUser());
		} catch (Exception e) {
			Logger.error(this.getClass(), "Error: " + e.getMessage(), e);
		}
	}

	@WrapInTransaction
	public void importRules(final RulesImportExportObject importer,
			final User user) throws IOException, DotDataException {

		try {
			final RulesAPI rulesAPI = APILocator.getRulesAPI();

			//Saving the rules.
			for (Rule rule : importer.getRules()) {
				rule.setParentPermissionable(RulePermissionableUtil.findParentPermissionable(rule.getParent()));
				rulesAPI.saveRule(rule, user, false);

				//Saving the Condition Groups.
				for (ConditionGroup group : rule.getGroups()) {
					rulesAPI.saveConditionGroup(group, user, false);
					//Saving the Condition for each group.
					for (Condition condition : group.getConditions()) {
						rulesAPI.saveCondition(condition, user, false);
					}
				}
				//Saving the Action.
				for (RuleAction action : rule.getRuleActions()) {
					rulesAPI.saveRuleAction(action, user, false);
				}
			}
		} catch (Exception e) {
			Logger.error(this.getClass(), "Error: " + e.getMessage(), e);
			throw new DotDataException(e);
		}
	}

	@CloseDBIfOpened
	public RulesImportExportObject buildExportObject(Ruleable parent)
			throws DotDataException, DotSecurityException {

		RulesAPI rulesAPI = APILocator.getRulesAPI();
		User user = APILocator.getUserAPI().getSystemUser();

		List<Rule> rules = (parent == null) ? rulesAPI.getAllRules(user, false)
				: rulesAPI.getAllRulesByParent(parent, user, false);

		RulesImportExportObject export = new RulesImportExportObject();
		export.setRules(rules);

		return export;
	}

}

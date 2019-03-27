package com.dotmarketing.portlets.rules.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;


import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationFeature;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * 
 * @author Will Ezell
 * @version 1.0
 * @since Jan 25, 2016
 *
 */
public class RulesImportExportUtil {

	private static RulesImportExportUtil rulesImportExportUtil;

	/**
	 * 
	 * @return
	 */
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

	/**
	 * 
	 * @return
	 * @throws IOException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public String exportToJson() throws IOException, DotDataException, DotSecurityException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(buildExportObject(null));
	}

	/**
	 * 
	 * @param file
	 * @throws IOException
	 */
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

	/**
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void importRules(File file) throws IOException {

		RulesAPI rapi = APILocator.getRulesAPI();

		BufferedReader in = null;
		try {
			User user = APILocator.getUserAPI().getSystemUser();
			FileReader fstream = new FileReader(file);
			in = new BufferedReader(fstream);
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			StringWriter sw = new StringWriter();
			String str;
			while ((str = in.readLine()) != null) {
				sw.append(str);
			}

			RulesImportExportObject importer = mapper.readValue(sw.toString(), RulesImportExportObject.class);

			//Saving the rules.
			for (Rule rule : importer.getRules()) {
				rule.setParentPermissionable(RulePermissionableUtil.findParentPermissionable(rule.getParent()));
				rapi.saveRule(rule, user, false);

				//Saving the Condition Groups.
				for (ConditionGroup group : rule.getGroups()) {
					rapi.saveConditionGroup(group, user, false);
					//Saving the Condition for each group.
					for (Condition condition : group.getConditions()) {
						rapi.saveCondition(condition, user, false);
					}
				}
				//Saving the Action.
				for (RuleAction action : rule.getRuleActions()) {
					rapi.saveRuleAction(action, user, false);
				}
			}

		} catch (Exception e) {
			Logger.error(this.getClass(), "Error: " + e.getMessage(), e);
		} finally {
			in.close();
		}
	}

	/**
	 * 
	 * @param parent
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public RulesImportExportObject buildExportObject(Ruleable parent) throws DotDataException, DotSecurityException {

		RulesAPI rapi = APILocator.getRulesAPI();
		User user = APILocator.getUserAPI().getSystemUser();

		List<Rule> rules = (parent == null) ? rapi.getAllRules(user, false) : rapi.getAllRulesByParent(parent, user, false);

		RulesImportExportObject export = new RulesImportExportObject();
		export.setRules(rules);

		return export;
	}

}

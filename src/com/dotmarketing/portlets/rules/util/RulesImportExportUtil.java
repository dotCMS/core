package com.dotmarketing.portlets.rules.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import com.dotcms.enterprise.rules.RulesAPIImpl;
import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationFeature;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

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
		FilterRegistration fr = Config.CONTEXT.addFilter("DynamicFilter","com.sample.TestFilter");
		fr.setInitParameter("filterInitName", "filterInitValue");
		fr.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST),
		                                     true, "DynamicServlet");
		
		
		
	}

	public void importRules(File file) throws IOException {

		RulesAPIImpl rapi = (RulesAPIImpl) APILocator.getRulesAPI();

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

			RulesImportExportObject importer = mapper.readValue((String) sw.toString(), RulesImportExportObject.class);

			for (Rule rule : importer.getRules()) {
				rapi.saveRule(rule, user, false);
			}
			for (ConditionGroup group : importer.getConditionGroups()) {
				rapi.saveConditionGroup(group, user, false);
			}

			for (Condition condition : importer.getConditions()) {
				rapi.saveCondition(condition, user, false);
			}
			for (Condition condition : importer.getConditions()) {
				for (ParameterModel param : importer.getParameters()) {
					if (param.getOwnerId().equals(condition.getId())) {
						condition.addValue(param);
					}
				}
				rapi.saveCondition(condition, user, false);
			}

			for (RuleAction action : importer.getRuleActions()) {

				for (ParameterModel param : importer.getParameters()) {
					if (param.getOwnerId().equals(action.getId())) {
						action.addParameter(param);
					}
				}

				rapi.saveRuleAction(action, user, false);
			}

		} catch (Exception e) {// Catch exception if any
			Logger.error(this.getClass(), "Error: " + e.getMessage(), e);
		} finally {

			in.close();

		}
	}

	public RulesImportExportObject buildExportObject(Treeable parent) throws DotDataException, DotSecurityException {

		RulesImportExportObject obj = new RulesImportExportObject();
		RulesAPIImpl rapi = (RulesAPIImpl) APILocator.getRulesAPI();
		User user = APILocator.getUserAPI().getSystemUser();
		
		List<Rule> rules = null;//(parent == null) ? rapi.getAllRules(user, false) : rapi.getAllRulesByParent(parent, user, false);
		obj.setRules(rules);

		List<ConditionGroup> groups = Lists.newArrayList();
		List<Condition> conditions 	= Lists.newArrayList();
		List<RuleAction> actions 	= Lists.newArrayList();
		List<ParameterModel> params = Lists.newArrayList();

		for (Rule r : rules) {
			for (ConditionGroup group : rapi.getConditionGroupsByRule(r.getId(), user, false)) {
				groups.add(group);
				for (Condition condition : rapi.getConditionsByConditionGroup(group.getId(), user, false)) {
					conditions.add(condition);
					for (Map.Entry<String, ParameterModel> param : condition.getParameters().entrySet()) {
						params.add(param.getValue());
					}
				}
			}
			for (RuleAction action : rapi.getRuleActionsByRule(r.getId(), user, false)) {
				actions.add(action);
				for (Map.Entry<String, ParameterModel> param : action.getParameters().entrySet()) {
					params.add(param.getValue());
				}
			}
		}

		RulesImportExportObject export = new RulesImportExportObject();
		export.setRules(rules);
		export.setConditions(conditions);
		export.setConditionGroups(groups);
		export.setParameters(params);
		export.setRuleActions(actions);
		return export;

	}
}

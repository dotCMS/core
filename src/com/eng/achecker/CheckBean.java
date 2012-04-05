package com.eng.achecker;


import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.eng.achecker.model.ReflectionBean;
import com.eng.achecker.utility.LanguageUtility;

public class CheckBean extends ReflectionBean {
	
	private String lang;

	private int check_id;
	
	private int user_id;

	private String html_tag;
	
	private int confidence;
	
	private String note;

	private String name;
	
	private String err;
	
	private String description;
	
	private String search_str;
	
	private String long_description;
	
	private String rationale;
	
	private String how_to_repair;
	
	private String repair_example;
	
	private String question;
	
	private String decision_pass;
	
	private String decision_fail;
	
	private String test_procedure;
	
	private String test_expected_result;
	
	private String test_failed_result;
	
	private String func;
	
	private int open_to_public;
	
	// create_date	datetime 
	
	public CheckBean() {}
	
	public CheckBean(Map<String, Object> init) throws IllegalArgumentException, IntrospectionException, IllegalAccessException, InvocationTargetException {
		super(init);
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public int getCheck_id() {
		return check_id;
	}

	public void setCheck_id(int check_id) {
		this.check_id = check_id;
	}

	public int getUser_id() {
		return user_id;
	}

	public void setUser_id(int user_id) {
		this.user_id = user_id;
	}

	public String getHtml_tag() {
		return html_tag;
	}

	public void setHtml_tag(String html_tag) {
		this.html_tag = html_tag;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getName() {
		return localize(name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getErr() {
		return localize(err);
	}

	public void setErr(String err) {
		this.err = err;
	}

	public String getDescription() {
		return localize(description);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSearch_str() {
		return search_str;
	}

	public void setSearch_str(String search_str) {
		this.search_str = search_str;
	}

	public String getLong_description() {
		return long_description;
	}

	public void setLong_description(String long_description) {
		this.long_description = long_description;
	}

	public String getRationale() {
		return rationale;
	}

	public void setRationale(String rationale) {
		this.rationale = rationale;
	}

	public String getHow_to_repair() {
		return localize(how_to_repair);
	}

	public void setHow_to_repair(String how_to_repair) {
		this.how_to_repair = how_to_repair;
	}

	public String getRepair_example() {
		return localize(repair_example);
	}

	public void setRepair_example(String repair_example) {
		this.repair_example = repair_example;
	}

	public String getQuestion() {
		return localize(question);
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getDecision_pass() {
		return localize(decision_pass);
	}

	public void setDecision_pass(String decision_pass) {
		this.decision_pass = decision_pass;
	}

	public String getDecision_fail() {
		return localize(decision_fail);
	}

	public void setDecision_fail(String decision_fail) {
		this.decision_fail = decision_fail;
	}

	public String getTest_procedure() {
		return localize(test_procedure);
	}

	public void setTest_procedure(String test_procedure) {
		this.test_procedure = test_procedure;
	}

	public String getTest_expected_result() {
		return localize(test_expected_result);
	}

	public void setTest_expected_result(String test_expected_result) {
		this.test_expected_result = test_expected_result;
	}

	public String getTest_failed_result() {
		return localize(test_failed_result);
	}

	public void setTest_failed_result(String test_failed_result) {
		this.test_failed_result = test_failed_result;
	}

	public String getFunc() {
		return func;
	}

	public void setFunc(String func) {
		this.func = func;
	}

	public int getOpen_to_public() {
		return open_to_public;
	}

	public void setOpen_to_public(int open_to_public) {
		this.open_to_public = open_to_public;
	}

	public int getConfidence() {
		return confidence;
	}

	public void setConfidence(int confidence) {
		this.confidence = confidence;
	}

	public Confidence getConfidenceEnum() {
		return Confidence.make(this.confidence);
	}
	
	private String localize(String key) {
		return LanguageUtility._AC(key, lang);
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("{ ");
		buffer.append("ID: " + check_id);
		buffer.append(", Tag: " + html_tag);
		buffer.append(" }");
		return buffer.toString();
	}
	
	public String dump() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Check " + check_id + ": {\n");
		if (description != null) buffer.append("\tDescription: " + getDescription() + "\n");
		if (search_str != null)buffer.append("\tSearch String: " + getSearch_str() + "\n");
		if (long_description != null)buffer.append("\tLong Description: " + getLong_description() + "\n");
		if (rationale != null)buffer.append("\tRationale: " + getRationale() + "\n");
		if (how_to_repair != null)buffer.append("\tHow Repair: " + getHow_to_repair() + "\n");
		if (repair_example != null)buffer.append("\tRepair Example: " + getRepair_example() + "\n");
		if (question != null)buffer.append("\tQuestion: " + getQuestion() + "\n");
		if (decision_pass != null)buffer.append("\tDecision Pass: " + getDecision_pass() + "\n");
		if (decision_fail != null)buffer.append("\tDecision Fail: " + getDecision_fail() + "\n");
		if (test_procedure != null)buffer.append("\tTest Procedure:\n" + getTest_procedure() + "\n");
		if (test_expected_result != null)buffer.append("\tTest Expected Result: " + getTest_expected_result() + "\n");
		if (test_failed_result != null)buffer.append("\tTest Failed Result: " + getTest_failed_result() + "\n");
		if (func != null)buffer.append("\tFunc: " + getFunc() + "\n");
		buffer.append("}");
		return buffer.toString();
		
	}
	
}

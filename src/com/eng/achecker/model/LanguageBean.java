package com.eng.achecker.model;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class LanguageBean extends ReflectionBean {

	private String language_code;
	private String charset;
	private String reg_exp;
	private String native_name;
	private String english_name;
	private int status;

	public LanguageBean(Map<String, Object> init)
			throws IntrospectionException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		super(init);
	}

	public String getLanguage_code() {
		return language_code;
	}

	public void setLanguage_code(String languageCode) {
		language_code = languageCode;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public String getReg_exp() {
		return reg_exp;
	}

	public void setReg_exp(String regExp) {
		reg_exp = regExp;
	}

	public String getNative_name() {
		return native_name;
	}

	public void setNative_name(String nativeName) {
		native_name = nativeName;
	}

	public String getEnglish_name() {
		return english_name;
	}

	public void setEnglish_name(String englishName) {
		english_name = englishName;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	
}

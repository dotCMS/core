package com.dotcms.translate;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;
import com.liferay.portal.model.User;

import java.util.List;

public interface TranslationService {

	String translateString(String toTranslate, Language from, Language to) throws TranslationException;

	List<Contentlet> translateContent(Contentlet src, List<Language> langs, List<Field> fieldsToTranslate, User user)
		throws TranslationException;

	Contentlet translateContent(Contentlet src, Language lang, List<Field> fieldsToTranslate, User user)
		throws TranslationException;

	List<String> translateStrings(List<String> toTranslate, Language from, Language to) throws TranslationException;

	List<ServiceParameter> getServiceParameters();

	void setServiceParameters(List<ServiceParameter> params);

}

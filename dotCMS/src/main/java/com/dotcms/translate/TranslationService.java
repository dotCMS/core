package com.dotcms.translate;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Field;
import com.liferay.portal.model.User;

import java.util.List;

public interface TranslationService {

	String translateString(String toTranslate, Language from, Language to) throws TranslationException;


	/**
	 * Will return un-persisted new versions of the src content translated to the
	 * languages specified
	 *
	 * @param src               content to translate
	 * @param langs             list of language to translate the content to
	 * @param fieldsToTranslate list of fields to translate
	 * @return a list of un-persisted new versions of the content translated to the requested languages
	 */
	List<Contentlet> translateContent(Contentlet src, List<Language> langs, List<Field> fieldsToTranslate, User user)
		throws TranslationException;

	/**
	 * Will return un-persisted new versions of the src content translated to the
	 * language specified.
	 *
	 * @param src               content to translate
	 * @param lang       language to translate te content to
	 * @param fieldsToTranslate list of fields to translate
	 * @return the translated un-persisted content
	 */
	Contentlet translateContent(Contentlet src, Language lang, List<Field> fieldsToTranslate, User user)
		throws TranslationException;

	List<String> translateStrings(List<String> toTranslate, Language from, Language to) throws TranslationException;

	List<ServiceParameter> getServiceParameters();

	/**
	 * Set the Service Parameters, the host could be pass in order to get the configuration from the apps if exists.
	 * @param params {@link List}  parameters
	 * @param hostId {@link String}  hostId of the contentlet, to get config from apps.
	 */
	void setServiceParameters(List<ServiceParameter> params, final String hostId);

}

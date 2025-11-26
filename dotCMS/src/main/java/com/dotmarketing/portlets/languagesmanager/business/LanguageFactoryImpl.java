package com.dotmarketing.portlets.languagesmanager.business;

import static com.dotmarketing.portlets.languagesmanager.business.LanguageCacheImpl.LANG_404;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.portlets.languagesmanager.transform.LanguageTransformer;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.util.FileUtil;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation class for the {@link LanguageFactory}.
 *
 * @author  will
 * @author  david torres
 * @version N/A
 * @since Mar 22, 2012
 *
 */
public class LanguageFactoryImpl extends LanguageFactory {

	private static final String DELETE_FROM_LANGUAGE_WHERE_ID = "delete from language where id = ?";

	private static final String UPDATE_LANGUAGE_BY_ID="update language set language_code = ? ,country_code = ? ,language = ? ,country = ? where id=?";
	private static final String INSERT_LANGUAGE_BY_ID="insert into language (id,language_code,country_code,language,country) values (?,?,?,?,?)";
	private static final String SELECT_LANGUAGE_BY_LANG_AND_COUNTRY_CODES="select * from language where lower(language_code) = ? and lower(country_code) = ?";
	private static final String SELECT_LANGUAGE_BY_LANG_CODE_ONLY="select * from language where lower(language_code) = ? and (country_code = '' OR country_code IS NULL)";
	private static final String SELECT_FIRST_LANGUAGE_BY_LANG_CODE_ONLY="select * from language where lower(language_code) = ?";
	private static final String SELECT_LANGUAGE_BY_ID="select * from language where id = ?";
	private static final String SELECT_ALL_LANGUAGES="select * from language where id <> ? order by language_code ";

	public static final String DEFAULT_LANGUAGE_CODE = "DEFAULT_LANGUAGE_CODE";
	public static final String DEFAULT_LANGUAGE_COUNTRY_CODE = "DEFAULT_LANGUAGE_COUNTRY_CODE";
	public static final String DEFAULT_LANGUAGE_COUNTRY = "DEFAULT_LANGUAGE_COUNTRY";
	public static final String DEFAULT_LANGUAGE_STR = "DEFAULT_LANGUAGE_STR";

	private final AtomicReference<Language> createDefaultLanguageLock = new AtomicReference<>();

	private final Map<String, Date> readTimeStamps = new HashMap<>();

	/**
	 * Creates a new instance of the {@link LanguageFactory}.
	 */
	public LanguageFactoryImpl () {

	}

	@Override
	protected void deleteLanguage(Language language) {

		deleteLanguageById(language);

		CacheLocator.getLanguageCache().removeLanguage(language);
	}


	@Override
	protected Language getLanguage(final String languageCode, final String countryCode) {
		try {
			final String languageCodeLowerCase = (languageCode != null) ? languageCode.toLowerCase() : null;
			final String countryCodeLowerCase = (countryCode != null) ? countryCode.toLowerCase() : null;
			Language lang = CacheLocator.getLanguageCache().getLanguageByCode(languageCode, countryCode);

			if (lang == null) {
				lang = UtilMethods.isSet(countryCodeLowerCase)
						? fromDbList(new DotConnect()
						.setSQL(SELECT_LANGUAGE_BY_LANG_AND_COUNTRY_CODES)
						.addParam(languageCodeLowerCase)
						.addParam(countryCodeLowerCase)
						.loadObjectResults())
						.stream()
						.findFirst()
						.orElse(null)
						: getFallbackLanguage(languageCodeLowerCase);

				if (lang != null && lang.getId() > 0) {
					CacheLocator.getLanguageCache().addLanguage(lang);
				}
			}

			return lang;

		} catch (Exception e) {
			Logger.error(LanguageFactoryImpl.class, "getLanguage (" + languageCode + "-" + countryCode + ") failed:" + e);
			throw new DotRuntimeException(e);
		}

	}

	@Override
	protected Language getLanguage(final String languageId) {

		if(!UtilMethods.isSet(languageId)){
			throw new IllegalArgumentException("languageId is expected to have a value.");
		}

		// if we have a number
		if(!languageId.contains("_") && !languageId.contains("-")) {
			try {
				final long parsedLangId = Long.parseLong(languageId);
				return getLanguage(parsedLangId);
			} catch (NumberFormatException e) {
				Logger.debug(LanguageFactoryImpl.class, "getLanguage failed passed id is not numeric. Value from parameter: " + languageId, e);
				return null;
			}
		}

		try{
			final String[] codes = languageId.split("[_|-]");
			return getLanguage(codes[0], codes[1]);
		} catch (Exception e) {
			Logger.error(LanguageFactoryImpl.class, "getLanguage failed for id:" + languageId,e);
			throw new DotRuntimeException("getLanguage failed for id:" + languageId, e);

		}


	}

	@Override
	protected Language createDefaultLanguage() {

		Language language = getLanguage (Config.getStringProperty(DEFAULT_LANGUAGE_CODE, "en"), Config.getStringProperty(
				DEFAULT_LANGUAGE_COUNTRY_CODE,"US"));
		//If the default language does not exist, create it
		if(!UtilMethods.isSet(language)) {
			Logger.info(this,"Creating Default Language");
			language = new Language();
			language.setCountry(
					Config.getStringProperty(DEFAULT_LANGUAGE_COUNTRY, "United States"));
			language.setCountryCode(
					Config.getStringProperty(DEFAULT_LANGUAGE_COUNTRY_CODE, "US"));
			language.setLanguage(Config.getStringProperty(DEFAULT_LANGUAGE_STR, "English"));
			language.setLanguageCode(Config.getStringProperty(DEFAULT_LANGUAGE_CODE, "en"));

			//saves the new language
			saveLanguage(language);

			try {
				//Writes the default lang in the company table
				writeDefaultLanguage(language.getId());
				CacheLocator.getLanguageCache().setDefaultLanguage(language);
			} catch (DotDataException e) {
				throw new DotRuntimeException(String.format("Unable to persist language with id `%s` as the default in the default company.",language), e);
			}
		}


		return language;

	}

	/**
	 * Usually something like this should be located at the API level
	 * However this method is already used in a few places that require the logic to be at this level
	 * This is not a transactional method. The language will be loaded from cache on a the first atttempt
	 * if not found it will be loaded from the db and if not found there either it will be created.
	 * any of the db operations are executed separately in a transactional method
	 * @return
	 */
	@Override
	protected Language getDefaultLanguage() {

		final LanguageCache languageCache = CacheLocator.getLanguageCache();
		Language defaultLanguage = languageCache.getDefaultLanguage();

		if(!LANG_404.equals(defaultLanguage)){
			return defaultLanguage;
		}

		if (createDefaultLanguageLock.compareAndSet(null, defaultLanguage)) {
			defaultLanguage = transactionalGetDefaultLanguage();
			languageCache.setDefaultLanguage(defaultLanguage);
			//update the lock.
			createDefaultLanguageLock.set(defaultLanguage);
		}

		return createDefaultLanguageLock.get();
	}

	/**
	 * Transactional Get/Create lang method
	 * @return
	 */
	@WrapInTransaction
	private Language transactionalGetDefaultLanguage(){
		Language defaultLanguage = null;
		final long defaultLangId = readDefaultLanguage();
		if (0 != defaultLangId) {
			//Verify if it already exist. if so out put it into cache.
			defaultLanguage = getLanguage(defaultLangId);
		}
		if (null == defaultLanguage) {
			//if it doesn't exist then creates the default lang and put it in cache.
			defaultLanguage = createDefaultLanguage();
		}
		return defaultLanguage;
	}

	void writeDefaultLanguage(final long languageId) throws DotDataException {
		final String companyId = APILocator.getCompanyAPI().getDefaultCompany().getCompanyId();
		new DotConnect()
				.setSQL("UPDATE company SET default_language_id = ? WHERE companyid = ? ")
				.addParam(languageId)
				.addParam(companyId)
				.loadResult();
	}

	long readDefaultLanguage() {

		final String companyId = APILocator.getCompanyAPI().getDefaultCompany().getCompanyId();
		final String defaultLanguageId = new DotConnect()
				.setSQL("SELECT default_language_id FROM company WHERE companyid = ? ")
				.addParam(companyId).getString("default_language_id");
		return ConversionUtils.toLong(defaultLanguageId);
	}


	@Override
	protected void makeDefault(final Long languageId) throws DotDataException {
		final Language language = getLanguage(languageId);
		if(null == language ){
		   throw new DotDataException(String.format("Unable to make default language with id %s the default one.",languageId));
		}
		if (createDefaultLanguageLock.compareAndSet(getDefaultLanguage(), language)) {
			writeDefaultLanguage(languageId);
			CacheLocator.getLanguageCache().clearCache();
		}
	}

	@Override
	protected void transferAssets(final Long oldDefaultLanguage, final Long newDefaultLanguage)
			throws DotDataException {

		final DotConnect dotConnect = new DotConnect();
		if (DbConnectionFactory.isMySql()) {

			dotConnect
					.setSQL(" UPDATE contentlet SET language_id = ? WHERE identifier NOT IN ( "
							  + " SELECT identifier FROM ( "
							     + " SELECT c.identifier FROM contentlet c WHERE c.language_id = ? AND c.identifier = identifier "
							  + " ) AS T"
							+ ") AND language_id = ?  ")
					.addParam(newDefaultLanguage)
					.addParam(newDefaultLanguage)
					.addParam(oldDefaultLanguage)
					.loadResult();

			dotConnect
					.setSQL(" UPDATE contentlet_version_info SET lang = ? WHERE identifier NOT IN ( "
							  + " SELECT identifier FROM ( "
							     + " SELECT cvi.identifier FROM contentlet_version_info cvi WHERE lang = ? AND cvi.identifier = identifier "
							  + " ) AS T"
							+ ") AND lang = ? ")
					.addParam(newDefaultLanguage)
					.addParam(newDefaultLanguage)
					.addParam(oldDefaultLanguage)
					.loadResult();


			dotConnect
					.setSQL(" UPDATE workflow_task SET language_id = ? WHERE webasset NOT IN ( "
							+ " SELECT webasset FROM ( "
							   + " SELECT wft.webasset FROM workflow_task wft WHERE wft.language_id = ? AND wft.webasset = webasset "
							+ " ) AS T"
							+ ") AND language_id = ? ")
					.addParam(newDefaultLanguage)
					.addParam(newDefaultLanguage)
					.addParam(oldDefaultLanguage)
					.loadResult();

		} else {

			dotConnect
					.setSQL(" UPDATE contentlet SET language_id = ? WHERE identifier NOT IN ( "
							+ " SELECT c.identifier FROM contentlet c WHERE c.language_id = ? AND c.identifier = identifier "
							+ ") AND language_id = ?  ")
					.addParam(newDefaultLanguage)
					.addParam(newDefaultLanguage)
					.addParam(oldDefaultLanguage)
					.loadResult();

			dotConnect
					.setSQL(" UPDATE contentlet_version_info SET lang = ? WHERE identifier NOT IN ( "
							+ " SELECT cvi.identifier FROM contentlet_version_info cvi WHERE lang = ? AND cvi.identifier = identifier "
							+ ") AND lang = ? ")
					.addParam(newDefaultLanguage)
					.addParam(newDefaultLanguage)
					.addParam(oldDefaultLanguage)
					.loadResult();

			dotConnect
					.setSQL(" UPDATE workflow_task SET language_id = ? WHERE webasset NOT IN ( "
							+ " SELECT wft.webasset FROM workflow_task wft WHERE wft.language_id = ? AND wft.webasset = webasset "
							+ ") AND language_id = ? ")
					.addParam(newDefaultLanguage)
					.addParam(newDefaultLanguage)
					.addParam(oldDefaultLanguage)
					.loadResult();
		}
	}

	@Override
	protected Language getLanguage(final long id) {
		Language lang = CacheLocator.getLanguageCache().getLanguageById(id);
		if(lang != null){
			return lang;
		}
		try {
			lang =  fromDbList(new DotConnect().setSQL( SELECT_LANGUAGE_BY_ID)
					.addParam(id)
					.loadObjectResults())
					.stream()
					.findFirst()
					.orElse(null);

			//Validate we are returning a valid Language object
			if(lang != null && lang.getId() == 0){
				lang = null;//Clean up as the dh.load() returned just an empty instance
			}

			if(lang != null){
				CacheLocator.getLanguageCache().addLanguage(lang);
			}
			return lang;
		} catch (DotDataException e) {
			Logger.error(LanguageFactoryImpl.class, "getLanguage failed - id:" + id + " message: " + e);
			throw new DotRuntimeException(e);
		}

	}

	@Override
	protected List<Language> getLanguages() {
		List<Language> languages = CacheLocator.getLanguageCache().getLanguages();
		if(languages != null){
			return ImmutableList.copyOf(languages);
		}
		try {
			final Language defaultLang = getDefaultLanguage();
			languages =  fromDbList(new DotConnect().setSQL( SELECT_ALL_LANGUAGES )
					.addParam(defaultLang.getId())
					.loadObjectResults());

			languages.add(0,defaultLang);
			languages = ImmutableList.copyOf(languages);
			CacheLocator.getLanguageCache().putLanguages(languages);
			return languages;
		} catch (DotDataException e) {
			CacheLocator.getLanguageCache().putLanguages(null);
			throw new DotRuntimeException(e);
		}
	}


	@Override
	protected void saveLanguage(final Language lang) {
		try {
			if(UtilMethods.isSet(lang.getLanguageCode())) {
				lang.setLanguageCode(lang.getLanguageCode().toLowerCase());
			}
			if(UtilMethods.isSet(lang.getCountryCode())) {
				lang.setCountryCode(lang.getCountryCode().toUpperCase());
			}
			dbUpsert(lang);
			CacheLocator.getLanguageCache().clearLanguages();
			// Cleaning up the createDefaultLanguageLock and the default language cache record
			// if we are updating the default language, otherwise changes won't be reflected.
			if (createDefaultLanguageLock.get() != null
					&& createDefaultLanguageLock.get().getId() == lang.getId()) {
				CacheLocator.getLanguageCache().clearDefaultLanguage();
				createDefaultLanguageLock.set(null);
			}
		} catch (DotDataException e) {

			throw new DotRuntimeException("saveLanguage failed to save the language:" + lang, e);
		}
	}

	@Override
	protected String getLanguageCodeAndCountry(long id, String langId) {
		if (id == 0 && UtilMethods.isSet(langId)) {
			try {
				id = Long.parseLong(langId);
			} catch (Exception e) {
			}
		}

		Language language = null;

		if (id > 0) {
			language = getLanguage(id);
		} else {
			language =getDefaultLanguage ();
		}

		return language.getLanguageCode() + (UtilMethods.isSet(language.getCountryCode()) ? "_" + language.getCountryCode():"");
	}

	@Override
	protected boolean hasLanguage(String id) {
		return getLanguage(id) != null;
	}

	@Override
	protected boolean hasLanguage(long id) {
		return getLanguage(id) != null;
	}

	@Override
	protected boolean hasLanguage(String languageCode, String countryCode) {
		return getLanguage(languageCode, countryCode) != null;
	}

	/**
	 *
	 * @return
	 */
	private String getGlobalVariablesPath () {
		String ret="";
		String realPath = Config.getStringProperty("ASSET_REAL_PATH");

		String assetPath = Config.getStringProperty("ASSET_PATH");
		if(!UtilMethods.isSet(realPath)){
			ret=FileUtil.getRealPath(assetPath + "/messages");
		}else{
			ret=realPath +File.separator+"messages";
		}
		return ret+File.separator;
	}

	@Override
	protected List<LanguageKey> getLanguageKeys(String langCode) {

		return getLanguageKeys(langCode, null);
	}

	@Override
	protected List<LanguageKey> getLanguageKeys(String langCode, String countryCode) {

		String code = countryCode == null ? langCode : langCode + "_" + countryCode;
		String filePath = getGlobalVariablesPath() + "cms_language_" + code + ".properties";

		boolean forceRead = false;

		if(readTimeStamps.get(filePath) != null) {
			Date lastReadTime = readTimeStamps.get(filePath);
			int refreshInterval = Config.getIntProperty("LANGUAGES_REFRESH_INTERVAL", 1);
			if(new Date().getTime() - lastReadTime.getTime() > refreshInterval * 1000 * 60) {
				File from = new java.io.File(filePath);
				if(from.lastModified() > lastReadTime.getTime())
					forceRead = true;
			}
		}


		List<LanguageKey> list = null;

		if(!forceRead) {
			try {
				list = CacheLocator.getLanguageCache().getLanguageKeys(langCode, countryCode);
			} catch (DotCacheException e1) {
				Logger.error(this, "getLanguageKeys: " + e1.getMessage(), e1);
				throw new DotRuntimeException(e1.getMessage(), e1);
			}
		}

		if (list == null) {
			// Create empty file
			File from = new java.io.File(filePath);
			if (!from.exists()) {
				return new ArrayList<>();
			}


			list = new LinkedList<>();
			LineNumberReader lineNumberReader = null;
			InputStreamReader is = null;
			InputStream fs = null;
			try {
				fs = Files.newInputStream(from.toPath());
				is = new InputStreamReader(fs,"UTF8");
				lineNumberReader = new LineNumberReader(is);
				String line = "";
				while((line = lineNumberReader.readLine()) != null) {
					String[] splitted = line.split("=");
					if(splitted.length > 1) {
						String value = "";
						for(int i = 1; i < splitted.length; i++) {
							if(i == 1)
								value += splitted[i];
							else
								value += "=" + splitted[i];// re-adding "="s removed by line.split("=")
						}
						if(line.endsWith("="))
							value += "=";
						list.add(new LanguageKey(langCode, null, splitted[0], value));
					}
				}
			} catch (FileNotFoundException e) {
				Logger.error(this, "getLanguageKeys: " + e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			} catch (IOException e) {
				Logger.error(this, "getLanguageKeys: " + e.getMessage(), e);
				throw new DotRuntimeException(e.getMessage(), e);
			}finally{
				if(lineNumberReader != null){
					try {
						lineNumberReader.close();
					} catch (IOException e) {
						Logger.error(LanguageFactoryImpl.class,e.getMessage(),e);
					}
				}
				if(is != null){
					try {
						is.close();
					} catch (IOException e) {
						Logger.error(LanguageFactoryImpl.class,e.getMessage(),e);
					}
				}
				if(fs!=null){
					try {
						fs.close();
					} catch (IOException e) {
						Logger.error(LanguageFactoryImpl.class,e.getMessage(),e);
					}
				}
			}
			CacheLocator.getLanguageCache().setLanguageKeys(langCode, countryCode, list);
			readTimeStamps.put(filePath, new Date());
		}
		return list;
	}

	@Override
	protected void createLanguageFiles(final Language lang) {
		String langCodeAndCountryCode = lang.getLanguageCode() + "_" + lang.getCountryCode();
		String langCode = lang.getLanguageCode();

		PrintWriter pw2 = null;
		try {

			String filePath = getGlobalVariablesPath()+"cms_language_" + langCodeAndCountryCode + ".properties";
			// Create empty file
			File from = new java.io.File(filePath);

			if (!from.exists()) {
				if (!from.getParentFile().exists()) {
					from.getParentFile().mkdir();
				}
				from.createNewFile();

			}

			filePath = getGlobalVariablesPath()+"cms_language_" + langCode + ".properties";
			// Create empty file
			from = new java.io.File(filePath);
			if (!from.exists()) {
				from.createNewFile();
				pw2 = new PrintWriter(filePath, "UTF8");
				pw2.write("## BEGIN PLUGINS\n");
				pw2.write("## END PLUGINS\n");
				pw2.flush();
			}

		} catch (IOException e) {
			Logger.error(this, "_checkLanguagesFiles:Property File Copy Failed " + e, e);
			throw new DotRuntimeException(e.getMessage(), e);
		} finally {
			if(pw2 != null)
				pw2.close();
		}
	}

	/**
	 *
	 * @param fileLangName
	 * @param keys
	 * @param toDeleteKeys
	 * @throws IOException
	 */
	private void saveLanguageKeys(final String fileLangName, Map<String, String> keys, final Set<String> toDeleteKeys) throws IOException {

		if(keys == null)
			keys = new HashMap<>();

		InputStream fileReader = null;
		PrintWriter tempFileWriter = null;

		String filePath = getGlobalVariablesPath() + "cms_language_" + fileLangName + ".properties";
		File file = new java.io.File(filePath);
		String tempFilePath = getGlobalVariablesPath() + "cms_language_" + fileLangName + ".properties.temp";
		File tempFile = new java.io.File(tempFilePath);

		try {
			if (tempFile.exists())
				tempFile.delete();
			if (tempFile.createNewFile()) {
				fileReader = Files.newInputStream(file.toPath());

				tempFileWriter = new PrintWriter(tempFilePath, "UTF8");

				for (final String k : toDeleteKeys) {
					keys.remove(k);
				}

				for(Map.Entry<String,String> newKey : keys.entrySet()) {
					tempFileWriter.println(newKey.getKey() + "=" + newKey.getValue());
				}
			} else {
				Logger.warn(this, "Error creating properties temp file: '" + tempFilePath + "' already exists.");
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if(fileReader != null)
				fileReader.close();
			if(tempFileWriter != null) {
				tempFileWriter.flush();
				tempFileWriter.close();
			}

		}


		ReadableByteChannel inputChannel = null;
		WritableByteChannel outputChannel = null;
		try (final InputStream tempFileInputStream = Files.newInputStream(tempFile.toPath());
				final OutputStream fileOutputStream = Files.newOutputStream(file.toPath())) {

			if (file.exists() && tempFile.exists()) {

				inputChannel = Channels.newChannel(tempFileInputStream);
				outputChannel = Channels.newChannel(fileOutputStream);
				FileUtil.fastCopyUsingNio(inputChannel, outputChannel);

			} else {
				if (!file.exists()) {
					Logger.warn(this, "Error: properties file: '" + filePath + "' doesn't exists.");
				}
				if (!tempFile.exists()) {
					Logger.warn(this,
							"Error: properties file: '" + tempFilePath + "' doesn't exists.");
				}
			}
		} catch (IOException e) {
			throw e;
		} finally {
			CloseUtils.closeQuietly(inputChannel, outputChannel);
		}

		tempFile.delete();
	}

	@Override
	protected void saveLanguageKeys(Language lang, Map<String, String> generalKeys, Map<String, String> specificKeys, Set<String> toDeleteKeys) throws DotDataException {

		String langCodeAndCountryCode = lang.getLanguageCode() + "_" + lang.getCountryCode();
		String langCode = lang.getLanguageCode();

		createLanguageFiles(lang);
		if(generalKeys == null) {
			generalKeys = new HashMap<>();

		}
		if(specificKeys == null) {
			specificKeys = new HashMap<>();
		}
		if(toDeleteKeys == null) {
			toDeleteKeys = new HashSet<>();
		}

		try {
			for(Map.Entry<String, String> entry : generalKeys.entrySet()) {
				if(entry ==null || entry.getKey() ==null)
					continue;
				if(!entry.getKey().matches("[A-Za-z0-9-_\\.]+"))
					throw new DotDataException("Invalid key :'" +entry.getKey()  +"' submitted, only keys that match [A-Za-z0-9-_\\.]+ are allowed");
			}
			for(Map.Entry<String, String> entry : specificKeys.entrySet()) {
				if(entry ==null || entry.getKey() ==null)
					continue;
				if(!entry.getKey().matches("[A-Za-z0-9-_\\.]+"))
					throw new DotDataException("Invalid key :'" +entry.getKey()  +"' submitted, only keys that match [A-Za-z0-9-_\\.]+ are allowed");
			}
			if((toDeleteKeys.size()>0) || (specificKeys.size()>0)){
				saveLanguageKeys(langCodeAndCountryCode, specificKeys, toDeleteKeys);
			}
			if((toDeleteKeys.size()>0) || (generalKeys.size()>0)){
				saveLanguageKeys(langCode, generalKeys, toDeleteKeys);
			}

			//Cleaning cache
			CacheLocator.getLanguageCache().removeLanguageKeys( lang.getLanguageCode(), lang.getCountryCode() );
			CacheLocator.getLanguageCache().removeLanguageKeys( lang.getLanguageCode(), null );
			//Force the reading of the languages files as we add/remove/edit keys
			// doing instanceof so tests don't fail with Mockito
			if(Config.CONTEXT.getAttribute( Globals.MESSAGES_KEY ) instanceof MultiMessageResources) {
				MultiMessageResources messages = (MultiMessageResources) Config.CONTEXT.getAttribute( Globals.MESSAGES_KEY );
				messages.reload();
			}
		} catch (IOException e) {
			Logger.error(this, "A IOException as occurred while saving the properties files", e);
			throw new DotRuntimeException("A IOException as occurred while saving the properties files", e);
		}

	}

	@Override
	protected Language getFallbackLanguage(final String languageCode) {

		Language lang = CacheLocator.getLanguageCache().getLanguageByCode(languageCode, "");
		if (null != lang ) {
			return (LANG_404.equals(lang)) ? null : lang;
		}

		try {

			lang = fromDbMap(new DotConnect()
					.setSQL(SELECT_LANGUAGE_BY_LANG_CODE_ONLY)
					.addParam(languageCode.toLowerCase())
					.loadObjectResults().stream().findFirst().orElse(null));

			if(lang == null){
				CacheLocator.getLanguageCache().add404Language(languageCode, "");
			}

			return lang;

		} catch (DotDataException e) {

			Logger.error(LanguageFactoryImpl.class, "getLanguage failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

	} // getFallbackLanguage.

	@Override
	protected Optional<Language> getFindFirstLanguageByCode(final String languageCode) {

		try {

			return Optional.ofNullable(fromDbMap(new DotConnect()
					.setSQL(SELECT_FIRST_LANGUAGE_BY_LANG_CODE_ONLY)
					.addParam(languageCode.toLowerCase())
					.loadObjectResults().stream().findFirst().orElse(null)));
		} catch (DotDataException e) {

			Logger.error(LanguageFactoryImpl.class, "getFindFirstLanguageByCode failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}
	}

	@Override
	protected int deleteLanguageById(final Language language) {

		if(!UtilMethods.isSet(language)){
			throw new IllegalArgumentException("language is expected to be different from null.");
		}
		int rowsAffected;
		final long id = language.getId();
		try {
			Logger.debug(this, ()-> "Deleting the language by id: " + id);
			rowsAffected = new DotConnect().executeUpdate(DELETE_FROM_LANGUAGE_WHERE_ID, id);
		} catch (DotDataException e) {
		    final String message  =e.getMessage().toLowerCase();
			if(message.contains("fk_contentlet_version_info_lang")
					|| message.contains("fk_con_lang_ver_info_lang")
					|| message.contains("fk_contentlet_lang")) {
				final String errorMsg = Sneaky.sneak(()->LanguageUtil.get("message.language.content"));
				throw new DotStateException(errorMsg, e);
			} else {
				Logger.error(LanguageFactoryImpl.class, "deleteLanguageById failed to delete the language with id: " + id);
				throw new DotRuntimeException(e.toString(), e);
			}
		} finally {
			CacheLocator.getLanguageCache().removeLanguage(language);
		}

		return rowsAffected;
	} // deleteLanguageById.


	private void dbUpsert(final Language language) throws DotDataException {

		if (language.getId() == 0) {
			language.setId(APILocator.getDeterministicIdentifierAPI().generateDeterministicIdBestEffort(language));
		}
		Language tester = getLanguage(language.getId());
		if (tester != null) {

			new DotConnect().setSQL(UPDATE_LANGUAGE_BY_ID)
					.addParam(language.getLanguageCode().toLowerCase())
					.addParam(language.getCountryCode())
					.addParam(language.getLanguage())
					.addParam(language.getCountry())
					.addParam(language.getId())
					.loadResult();
		} else {
			DotConnect dc = new DotConnect().setSQL(INSERT_LANGUAGE_BY_ID)
					.addParam(language.getId())
					.addParam(language.getLanguageCode().toLowerCase())
					.addParam(language.getCountryCode())
					.addParam(language.getLanguage())
					.addParam(language.getCountry());
			dc.loadResult();
		}
		CacheLocator.getLanguageCache().removeLanguage(language);

	}

	private List<Language> fromDbList(final List<Map<String, Object>> resultSet) {
		return  new ArrayList<>(new LanguageTransformer(resultSet).asList());
	}


	private Language fromDbMap(final Map<String, Object> resultSet) {
		if (resultSet == null) {
			return null;
		}
		return new LanguageTransformer(List.of(resultSet)).findFirst();
	}

}

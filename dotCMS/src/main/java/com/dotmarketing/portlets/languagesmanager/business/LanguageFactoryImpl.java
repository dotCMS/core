package com.dotmarketing.portlets.languagesmanager.business;

import static com.dotcms.util.ConversionUtils.toLong;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.util.FileUtil;
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
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.struts.Globals;
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
	private static final String SELECT_LANGUAGE_BY_ID="select * from language where id = ?";
	private static final String SELECT_ALL_LANGUAGES="select * from language where id <> ? order by language_code ";

	private static Language defaultLanguage;
	private final DotConnect dotConnect;

	private final Map<String, Date> readTimeStamps = new HashMap<String, Date>();

	/**
	 * Creates a new instance of the {@link LanguageFactory}.
	 */
	public LanguageFactoryImpl () {
		this(new DotConnect());
	}

	/**
	 * Creates a new instance of the {@link LanguageFactory}.
	 */
	@VisibleForTesting
	protected LanguageFactoryImpl (final DotConnect dotConnect) {

		this.dotConnect = dotConnect;
	}

	@Override
	protected void deleteLanguage(Language language) {

		deleteLanguageById(language);

		CacheLocator.getLanguageCache().removeLanguage(language);
	}


	@Override
	protected Language getLanguage(String languageCode, String countryCode) {

		try {
			languageCode = (languageCode != null) ? languageCode.toLowerCase() : null;
			countryCode = (countryCode != null) ? countryCode.toLowerCase() : null;
			Language lang = CacheLocator.getLanguageCache().getLanguageByCode(languageCode, countryCode);

			if (lang == null) {
				lang = UtilMethods.isSet(countryCode)
						? fromDbList(new DotConnect()
						.setSQL(SELECT_LANGUAGE_BY_LANG_AND_COUNTRY_CODES)
						.addParam(languageCode)
						.addParam(countryCode)
						.loadObjectResults())
						.stream()
						.findFirst()
						.orElse(null)
						: getFallbackLanguage(languageCode);

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
	protected Language getLanguage(String id) {


		// if we have a number
		if(id!=null && !id.contains("_") || !id.contains("-")  ){
			try {
				long  x = Long.parseLong(id);
				return getLanguage(x);
			} catch (NumberFormatException e) {
				Logger.debug(LanguageFactoryImpl.class, "getLanguage failed passed id is not numeric. Value from parameter: " + id, e);
			}
		}

		try{
			String[] codes= id.split("[_|-]");
			return getLanguage(codes[0], codes[1]);
		} catch (Exception e) {
			Logger.error(LanguageFactoryImpl.class, "getLanguage failed for id:" + id,e);
			throw new DotRuntimeException("getLanguage failed for id:" + id, e);

		}


	}

	@Override
	protected Language createDefaultLanguage() {

		Language language = getLanguage (Config.getStringProperty("DEFAULT_LANGUAGE_CODE", "en"), Config.getStringProperty("DEFAULT_LANGUAGE_COUNTRY_CODE","US"));
		language.setCountry(Config.getStringProperty("DEFAULT_LANGUAGE_COUNTRY", "United States"));
		language.setCountryCode(Config.getStringProperty("DEFAULT_LANGUAGE_COUNTRY_CODE", "US"));
		language.setLanguage(Config.getStringProperty("DEFAULT_LANGUAGE_STR", "English"));
		language.setLanguageCode(Config.getStringProperty("DEFAULT_LANGUAGE_CODE", "en"));

		//saves the new language

		saveLanguage(language);


		return language;

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
		List<Language> list = CacheLocator.getLanguageCache().getLanguages();
		if(list!=null){
			return list;
		}
		try {
			Language defaultLang = getDefaultLanguage();

			list =  fromDbList(new DotConnect().setSQL( SELECT_ALL_LANGUAGES)
					.addParam(defaultLang.getId())
					.loadObjectResults());

			list.add(0,defaultLang);

			CacheLocator.getLanguageCache().putLanguages(list);
			return list;
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

		return language.getLanguageCode() + "_" + language.getCountryCode();
	}

	@Override
	protected Language getDefaultLanguage() {

		if (defaultLanguage == null) {

			synchronized (this) {

				if (defaultLanguage == null) {

					defaultLanguage = getLanguage(Config.getStringProperty("DEFAULT_LANGUAGE_CODE", "en"),
							Config.getStringProperty("DEFAULT_LANGUAGE_COUNTRY_CODE", "US"));

					if (defaultLanguage==null || defaultLanguage.getId() == 0) {

						defaultLanguage = createDefaultLanguage();
					}
				}
			}
		}

		return defaultLanguage;
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
				return new ArrayList<LanguageKey>();
			}


			list = new LinkedList<LanguageKey>();
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
	private void saveLanguageKeys(String fileLangName, Map<String, String> keys, Set<String> toDeleteKeys) throws IOException {

		if(keys == null)
			keys = new HashMap<String, String>();

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

				for (String k : toDeleteKeys) {
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

		try(InputStream tempFileInputStream = Files.newInputStream(tempFile.toPath());
				OutputStream fileOutputStream = Files.newOutputStream(file.toPath())) {

			if (file.exists() && tempFile.exists()) {
				final ReadableByteChannel inputChannel = Channels.newChannel(tempFileInputStream);
				final WritableByteChannel outputChannel = Channels.newChannel(fileOutputStream);
				FileUtil.fastCopyUsingNio(inputChannel, outputChannel);
				inputChannel.close();
				outputChannel.close();
			} else {
				if (!file.exists())
					Logger.warn(this, "Error: properties file: '" + filePath + "' doesn't exists.");
				if (!tempFile.exists())
					Logger.warn(this, "Error: properties file: '" + tempFilePath + "' doesn't exists.");
			}
		} catch (IOException e) {
			throw e;
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
			MultiMessageResources messages = (MultiMessageResources) Config.CONTEXT.getAttribute( Globals.MESSAGES_KEY );
			messages.reload();
		} catch (IOException e) {
			Logger.error(this, "A IOException as occurred while saving the properties files", e);
			throw new DotRuntimeException("A IOException as occurred while saving the properties files", e);
		}

	}

	@Override
	protected Language getFallbackLanguage(final String languageCode) {

		Language lang = null;

		try {

			return fromDbMap(new DotConnect()
					.setSQL(SELECT_LANGUAGE_BY_LANG_CODE_ONLY)
					.addParam(languageCode.toLowerCase())
					.loadObjectResults().stream().findFirst().orElse(null));


		} catch (DotDataException e) {

			Logger.error(LanguageFactoryImpl.class, "getLanguage failed:" + e, e);
			throw new DotRuntimeException(e.toString());
		}

	} // getFallbackLanguage.

	@Override
	protected int deleteLanguageById(final Language language) {

		int rowsAffected = 0;
		long id          = 0;

		try {

			id = language.getId();
			Logger.debug(this, "Deleting the language by id: " + id);
			rowsAffected = this.dotConnect.executeUpdate(DELETE_FROM_LANGUAGE_WHERE_ID, id);
		} catch (DotDataException e) {
			Logger.warn(LanguageFactoryImpl.class, "deleteLanguageById failed to delete the language with id: " + id);
			throw new DotRuntimeException(e.toString(), e);
		} finally {

			CacheLocator.getLanguageCache().removeLanguage(language);
		}

		return rowsAffected;
	} // deleteLanguageById.


	private void dbUpsert(final Language language) throws DotDataException {

		if (language.getId() == 0) {
			language.setId(nextId());
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

    private synchronized long nextId(){
       return System.currentTimeMillis();
    }

	private List<Language> fromDbList(final List<Map<String, Object>> resultSet) {
		if (resultSet == null) {
			return new ArrayList<>();
		}
		return resultSet.stream().map(this::fromDbMap).collect(Collectors.toList());
	}


	private Language fromDbMap(final Map<String, Object> resultSet){
		if(resultSet==null) {
			return null;
		}
		final long id               = toLong(resultSet.get("id"), 0L);
		final String langCode       = (resultSet.get("language_code")!=null)    ? String.valueOf(resultSet.get("language_code")): null;
		final String countryCode    = (resultSet.get("country_code")!=null)     ? String.valueOf(resultSet.get("country_code")) : null;
		final String language       = (resultSet.get("language")!=null)         ? String.valueOf(resultSet.get("language"))     : null;
		final String country        = (resultSet.get("country")!=null)          ? String.valueOf(resultSet.get("country"))      : null;
		return new Language(id, langCode, countryCode, language, country);
	}

}

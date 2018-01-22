package com.dotmarketing.portlets.languagesmanager.business;

import static com.dotcms.util.CloseUtils.closeQuietly;
import static com.dotcms.util.ConversionUtils.toLong;

import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.util.FileUtil;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public static final String DELETE_FROM_LANGUAGE_WHERE_ID = "delete from language where id = ?";
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
        try {
			HibernateUtil.delete(language);
		} catch (DotHibernateException e) {
            Logger.error(LanguageFactoryImpl.class, "deleteLanguage failed to delete the language.", e);
            throw new DotRuntimeException(e.toString(), e);
		}
        CacheLocator.getLanguageCache().removeLanguage(language);
    }

	@Override
    protected Language getLanguage(String languageCode, String countryCode) {

        try {
        	languageCode = languageCode.toLowerCase();
        	countryCode = countryCode.toLowerCase();
        	Language lang = CacheLocator.getLanguageCache().getLanguageByCode(languageCode, countryCode);

        	if(lang == null) {
	            HibernateUtil dh = new HibernateUtil(Language.class);
	            if(UtilMethods.isSet(countryCode)) {
					dh.setQuery(
							"from language in class com.dotmarketing.portlets.languagesmanager.model.Language where lower(language_code) = ? and lower(country_code) = ?");
					dh.setParam(languageCode);
					dh.setParam(countryCode);
				}else{
					dh.setQuery(
							"from language in class com.dotmarketing.portlets.languagesmanager.model.Language where lower(language_code) = ? and (country_code = '' OR country_code IS NULL)");
					dh.setParam(languageCode);
				}
	            lang = (Language) dh.load();

	            //Validate we are returning a valid Language object
				if(lang != null && lang.getId() == 0){
					lang = null;//Clean up as the dh.load() returned just an empty instance
				}

	            if(lang != null){
	            	CacheLocator.getLanguageCache().addLanguage(lang);
	            }
        	}

        	return lang;

        } catch (Exception e) {
            Logger.error(LanguageFactoryImpl.class, "getLanguage failed:" + e, e);
            throw new DotRuntimeException(e.toString());
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

        Language language = getLanguage (Config.getStringProperty("DEFAULT_LANGUAGE_CODE"), Config.getStringProperty("DEFAULT_LANGUAGE_COUNTRY_CODE"));
        language.setCountry(Config.getStringProperty("DEFAULT_LANGUAGE_COUNTRY"));
        language.setCountryCode(Config.getStringProperty("DEFAULT_LANGUAGE_COUNTRY_CODE"));
        language.setLanguage(Config.getStringProperty("DEFAULT_LANGUAGE_STR"));
        language.setLanguageCode(Config.getStringProperty("DEFAULT_LANGUAGE_CODE"));

        //saves the new language
        try {
			HibernateUtil.save(language);
		} catch (DotHibernateException e) {
            Logger.error(LanguageFactoryImpl.class, "getLanguage failed to save the language.", e);
            throw new DotRuntimeException(e.toString(), e);

		}

        //adds it to the cache
		CacheLocator.getLanguageCache().removeLanguage(language);
		CacheLocator.getLanguageCache().addLanguage(language);

        return language;

    }

	@Override
    protected Language getLanguage(long id) {
		Language lang = CacheLocator.getLanguageCache().getLanguageById(id);
		if(lang != null){
			return lang;
		}
        try {
            HibernateUtil dh = new HibernateUtil(Language.class);
            dh.setQuery("from language in class com.dotmarketing.portlets.languagesmanager.model.Language where id = ? ");
            dh.setParam(id);
            lang = (Language) dh.load();

			//Validate we are returning a valid Language object
			if(lang != null && lang.getId() == 0){
				lang = null;//Clean up as the dh.load() returned just an empty instance
			}

            if(lang != null){
            	CacheLocator.getLanguageCache().addLanguage(lang);
            }
            return lang;
        } catch (DotHibernateException e) {
            Logger.error(LanguageFactoryImpl.class, "getLanguage failed:" + e, e);
            throw new DotRuntimeException(e.toString());
        }

    }

	@Override
    @SuppressWarnings("unchecked")
	protected List<Language> getLanguages() {
        List<Language> list = CacheLocator.getLanguageCache().getLanguages();
        if(list!=null){
        	return list;
        }
        try {
        	Language defaultLang = getDefaultLanguage();

            HibernateUtil dh = new HibernateUtil(Language.class);
            dh.setQuery("from language in class com.dotmarketing.portlets.languagesmanager.model.Language order by id");

            list = dh.list();
            List<Language> copy = new ArrayList<Language>(list);
            for(Language l : copy) {
			   	if(l.getId() == defaultLang.getId()) {
			   		list.remove(l);
					list.add(0, l);
			   	}
			}
            CacheLocator.getLanguageCache().putLanguages(list);
            return list;
        } catch (DotHibernateException e) {
        	CacheLocator.getLanguageCache().putLanguages(null);
            Logger.error(LanguageFactoryImpl.class, "getLanguages failed:" + e, e);
            throw new DotRuntimeException(e.toString());
        }
    }

	@Override
    protected void saveLanguage(Language o) {
        try {
            if(UtilMethods.isSet(o.getLanguageCode())) {
                o.setLanguageCode(o.getLanguageCode().toLowerCase());
            }
            if(UtilMethods.isSet(o.getCountryCode())) {
                o.setCountryCode(o.getCountryCode().toUpperCase());
            }
			HibernateUtil.saveOrUpdate(o);
			CacheLocator.getLanguageCache().clearLanguages();
		} catch (DotHibernateException e) {
            Logger.error(LanguageFactoryImpl.class, "saveLanguage failed to save the language.", e);
            throw new DotRuntimeException(e.toString(), e);
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
			language = getLanguage(Config.getStringProperty("DEFAULT_LANGUAGE_CODE"), Config.getStringProperty("DEFAULT_LANGUAGE_COUNTRY_CODE"));
		}

		return language.getLanguageCode() + "_" + language.getCountryCode();
	}

	@Override
    protected Language getDefaultLanguage () {

        if (defaultLanguage == null) {

        	synchronized (this) {

				if (defaultLanguage == null) {

					defaultLanguage = getLanguage(
							Config.getStringProperty("DEFAULT_LANGUAGE_CODE"),
							Config.getStringProperty("DEFAULT_LANGUAGE_COUNTRY_CODE"));

					if (defaultLanguage.getId() == 0) {

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
	protected void createLanguageFiles(Language lang) {
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
			generalKeys = new HashMap<String, String>();

		}
		if(specificKeys == null) {
			specificKeys = new HashMap<String, String>();
		}
		if(toDeleteKeys == null) {
			toDeleteKeys = new HashSet<String>();
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
    		if((toDeleteKeys!= null && toDeleteKeys.size()>0) || (specificKeys!=null && specificKeys.size()>0)){
    			saveLanguageKeys(langCodeAndCountryCode, specificKeys, toDeleteKeys);
    		}
    		if((toDeleteKeys!= null && toDeleteKeys.size()>0) || (specificKeys!=null && generalKeys.size()>0)){
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

        final Connection conn = DbConnectionFactory.getConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Language lang = null;

        try {

            preparedStatement = conn.prepareStatement(
                    "SELECT * FROM language WHERE language_code = ? AND (country_code = '' OR country_code IS NULL)");
            lang              = CacheLocator.getLanguageCache().getLanguageByCode(languageCode.toLowerCase(), null);

            if (lang == null) {

                preparedStatement.setString(1, languageCode.toLowerCase());
                resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {

                    final long id          = toLong(resultSet.getObject(1), 0L);
                    final String langCode    = resultSet.getString(2);
                    final String countryCode = resultSet.getString(3);
                    final String language    = resultSet.getString(4);
                    final String country     = resultSet.getString(5);
                    lang            = new Language(id, langCode, countryCode, language, country);
                    CacheLocator.getLanguageCache().addLanguage(lang);
                }
            }
        } catch (Exception e) {

            Logger.error(LanguageFactoryImpl.class, "getLanguage failed:" + e, e);
            throw new DotRuntimeException(e.toString());
        } finally {

			closeQuietly(preparedStatement, resultSet);
        }

        return lang;
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
            Logger.error(LanguageFactoryImpl.class, "deleteLanguageById failed to delete the language with id: " + id, e);
            throw new DotRuntimeException(e.toString(), e);
        } finally {

			CacheLocator.getLanguageCache().removeLanguage(language);
		}

		return rowsAffected;
    } // deleteLanguageById.

}

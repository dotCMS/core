package com.dotmarketing.portlets.languagesmanager.business;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.struts.Globals;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.struts.MultiMessageResources;

/**
 *
 * @author  will
 * @author  david torres
 *
 */
public class LanguageFactoryImpl extends LanguageFactory {

	public LanguageFactoryImpl () {

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

        	Language lang = CacheLocator.getLanguageCache().getLanguageByCode(languageCode, countryCode);

        	if(lang == null) {
	            HibernateUtil dh = new HibernateUtil(Language.class);
	            dh.setQuery(
	                "from language in class com.dotmarketing.portlets.languagesmanager.model.Language where language_code = ? and country_code = ?");
	            dh.setParam(languageCode);
	            dh.setParam(countryCode);
	            lang = (Language) dh.load();
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
        long x = 1;

        try {
            x = Long.parseLong(id);
        } catch (Exception e) {
            Logger.error(LanguageFactoryImpl.class, "getLanguage failed passed id is not numeric.", e);
            throw new DotRuntimeException(e.toString(), e);
        }

        return getLanguage(x);
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
        try {
        	Language defaultLang = getDefaultLanguage();

            HibernateUtil dh = new HibernateUtil(Language.class);
            dh.setQuery("from language in class com.dotmarketing.portlets.languagesmanager.model.Language order by id");

            List<Language> list = dh.list();
            List<Language> copy = new ArrayList<Language>(list);
            for(Language l : copy) {
            	if(l.getId() == defaultLang.getId()) {
            		list.remove(l);
            		list.add(0, l);
            	}
            }
            return list;
        } catch (DotHibernateException e) {
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

    private static Language defaultLanguage;

	@Override
    protected Language getDefaultLanguage () {
        if (defaultLanguage == null) {
            defaultLanguage = getLanguage (Config.getStringProperty("DEFAULT_LANGUAGE_CODE"), Config.getStringProperty("DEFAULT_LANGUAGE_COUNTRY_CODE"));
            if (defaultLanguage.getId() == 0)
                defaultLanguage = createDefaultLanguage();
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

    private String getGlobalVariablesPath () {
    	String ret="";
    	String realPath = Config.getStringProperty("ASSET_REAL_PATH");

        String assetPath = Config.getStringProperty("ASSET_PATH");
    	if(!UtilMethods.isSet(realPath)){
			ret=Config.CONTEXT.getRealPath(assetPath + "/messages");
		}else{
			ret=realPath +File.separator+"messages";
		}
    	return ret+File.separator;
    }

	@Override
	protected List<LanguageKey> getLanguageKeys(String langCode) {

		return getLanguageKeys(langCode, null);
	}

	private Map<String, Date> readTimeStamps = new HashMap<String, Date>();

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
			FileInputStream fs = null;
			try {
				fs = new FileInputStream(from);
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

	private void saveLanguageKeys(String fileLangName, Map<String, String> keys, Set<String> toDeleteKeys) throws IOException {

		if(keys == null)
			keys = new HashMap<String, String>();

		FileInputStream fileReader = null;
		PrintWriter tempFileWriter = null;

		String filePath = getGlobalVariablesPath() + "cms_language_" + fileLangName + ".properties";
		File file = new java.io.File(filePath);
		String tempFilePath = getGlobalVariablesPath() + "cms_language_" + fileLangName + ".properties.temp";
		File tempFile = new java.io.File(tempFilePath);

		try {
			if (tempFile.exists())
				tempFile.delete();
			if (tempFile.createNewFile()) {
				fileReader = new FileInputStream(file);

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
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			if(fileReader != null)
				fileReader.close();
			if(fileReader != null) {
				tempFileWriter.flush();
				tempFileWriter.close();
			}

		}
		
		FileChannel fileToChannel = null;
		FileChannel fileFromChannel = null;

		try {
			if (file.exists() && tempFile.exists()) {
				fileToChannel = (new FileOutputStream(file)).getChannel();
				fileFromChannel = (new FileInputStream(tempFile)).getChannel();
				fileFromChannel.transferTo(0, fileFromChannel.size(), fileToChannel);
			} else {
				if (!file.exists())
					Logger.warn(this, "Error: properties file: '" + filePath + "' doesn't exists.");
				if (!tempFile.exists())
					Logger.warn(this, "Error: properties file: '" + tempFilePath + "' doesn't exists.");
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			if(fileFromChannel != null)
				fileFromChannel.close();
			if(fileToChannel != null) {
				fileToChannel.force(true);
				fileToChannel.close();
			}
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
}

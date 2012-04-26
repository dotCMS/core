
package com.eng.achecker.tinymce;

import java.util.HashMap;
import java.util.Map;

import com.eng.achecker.dao.GuidelinesDAO;
import com.eng.achecker.dao.LangCodesDAO;


public class DaoLocator<T>  {

	protected static DaoLocator instance;
	private static LangCodesDAO langCodesDAO;
	private static GuidelinesDAO guidelinesDAO;
	protected Map<T,Object> cache;


	private DaoLocator() {
		super();
		cache = new HashMap<T, Object>();
	}

	public synchronized static void init(){
		if(instance != null)
			return;
		instance = new DaoLocator();
	}

	public static LangCodesDAO getLangCodesDAO(){
		if( langCodesDAO == null ){
			langCodesDAO = (LangCodesDAO) getInstance(APIIndex.LANGUAGECODE_DAO );
		}
		return langCodesDAO;
	}

	public static GuidelinesDAO getGuidelinesDAO(){
		if( guidelinesDAO == null ){
			guidelinesDAO = (GuidelinesDAO) getInstance(APIIndex.GUIDELINE_DAO );
		}
		return guidelinesDAO;
	}

	private static Object getInstance(APIIndex index) {
		if(instance == null){
			init();		 
		}
		Object serviceRef = instance.getServiceInstance(index);
		return serviceRef;

	}


	protected Object createService(T enumObj) {
		return ((APIIndex) enumObj).create();
	}




	protected Object getServiceInstance(T enumObj) {

		Object serviceRef = null; 
		if ( cache.containsKey(enumObj)) {
			serviceRef = instance.cache.get(enumObj);
		}
		else {
			synchronized (enumObj.getClass()) {
				if (instance.cache.containsKey(enumObj)) {
					serviceRef = instance.cache.get(enumObj);
				} else {
					serviceRef = createService(enumObj);
					instance.cache.put(enumObj, serviceRef);
				}
			}

		}

		return serviceRef;
	}
}
enum APIIndex
{ 
	LANGUAGECODE_DAO,
	GUIDELINE_DAO ;
	Object create()    {
		switch(this) {
		case LANGUAGECODE_DAO: try {
			return new LangCodesDAO( );
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		case GUIDELINE_DAO: try {
			return new GuidelinesDAO();
		} catch ( Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		}
		throw new AssertionError("Unknown API index: " + this);
	}
}

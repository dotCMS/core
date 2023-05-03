/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/


package com.dotcms.enterprise.achecker.tinymce;

import java.util.HashMap;
import java.util.Map;

import com.dotcms.enterprise.achecker.dao.GuidelinesDAO;
import com.dotcms.enterprise.achecker.dao.LangCodesDAO;
import com.dotcms.enterprise.achecker.tinymce.APIIndex;
import com.dotcms.enterprise.achecker.tinymce.DaoLocator;


public class DaoLocator<T>  {

	protected static DaoLocator instance;
	private static LangCodesDAO langCodesDAO;
	private static GuidelinesDAO guidelinesDAO;
	protected Map<T,Object> cache;


	private DaoLocator() {
		super();
		cache = new HashMap<>();
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

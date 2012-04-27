package com.dotcms.achecker.tinymce;

import java.util.List;
import java.util.Map;

import com.dotcms.achecker.ACheckerRequest;
import com.dotcms.achecker.ACheckerResponse;
import com.dotcms.achecker.dao.GuidelinesDAO;
import com.dotcms.achecker.dao.LangCodesDAO;
import com.dotcms.achecker.impl.ACheckerImpl;
import com.dotcms.achecker.model.GuideLineBean;
import com.dotcms.achecker.tinymce.DaoLocator;

 
public class ACheckerDWR {

	private List<GuideLineBean> listaGuidelines = null;

	private List<GuideLineBean> getListaGudelines() throws Exception {
		try{
			if( listaGuidelines == null ){
				GuidelinesDAO gLines = DaoLocator.getGuidelinesDAO();
				listaGuidelines = gLines.getOpenGuidelines();		 
			}
			return listaGuidelines;
		}catch (Exception e) {
			throw e; 
		}
	}


	public List<GuideLineBean> getSupportedGudelines(){	 	
		try{
			 return  getListaGudelines();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;		
	}

 
	public ACheckerResponse validate(Map<String, String> params) {
		try {
			String lang = params.get("lang");
			String content = params.get("content");
			String guidelines = params.get("guidelines");
			String fragment = params.get("fragment");
			if( lang != null && lang.trim().length() == 2  ){
				LangCodesDAO langCodeDao = DaoLocator.getLangCodesDAO();  
				langCodeDao .getLangCodeBy3LetterCode( lang  );
			}
	 		String toValidate =  content ;
			ACheckerRequest request = new ACheckerRequest(lang, toValidate, guidelines, Boolean.parseBoolean(fragment) );	
			ACheckerImpl achecker = new ACheckerImpl();
			return achecker.validate(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}
	
	
	

}

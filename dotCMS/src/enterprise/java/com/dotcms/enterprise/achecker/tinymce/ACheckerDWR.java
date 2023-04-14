package com.dotcms.enterprise.achecker.tinymce;

import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.achecker.ACheckerRequest;
import com.dotcms.enterprise.achecker.ACheckerResponse;
import com.dotcms.enterprise.achecker.dao.GuidelinesDAO;
import com.dotcms.enterprise.achecker.dao.LangCodesDAO;
import com.dotcms.enterprise.achecker.impl.ACheckerImpl;
import com.dotcms.enterprise.achecker.model.GuideLineBean;
import com.dotcms.enterprise.achecker.tinymce.DaoLocator;
import com.dotcms.enterprise.license.LicenseLevel;


public class ACheckerDWR {

	private List<GuideLineBean> listaGuidelines = null;

	private List<GuideLineBean> getListaGudelines() throws Exception {
	    if(LicenseUtil.getLevel()< LicenseLevel.STANDARD.level)
	        throw new RuntimeException("need enterprise license");
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
	    if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            throw new RuntimeException("need enterprise license");
		try{
			 return  getListaGudelines();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;		
	}

 
	public ACheckerResponse validate(Map<String, String> params) {
	    if(LicenseUtil.getLevel()<LicenseLevel.STANDARD.level)
            throw new RuntimeException("need enterprise license");
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

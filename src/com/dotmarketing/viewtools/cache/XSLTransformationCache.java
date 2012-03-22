package com.dotmarketing.viewtools.cache;

import java.util.Date;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.util.Logger;
import com.dotmarketing.viewtools.bean.XSLTranformationDoc;

/**
 * This class manage the XSLTransformation plugin cache
 * @author Oswaldo
 */
public class XSLTransformationCache {


	private static final UserAPI userAPI = APILocator.getUserAPI();
	private static final VersionableAPI versionableAPI = APILocator.getVersionableAPI();

	/**
	 * Add into the cache the  XSL transformation Doc
	 * @param doc XSLTranformationDoc
	 */
	public static void addXSLTranformationDoc(XSLTranformationDoc doc){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		// we use the identifier uri for our mappings.
		String xmlPath = doc.getXmlPath();
		String xslPath = doc.getXslPath();
		cache.put(getPrimaryGroup() + xmlPath+"_"+xslPath, doc, getPrimaryGroup());

	}

	/**
	 * Get the XSLTranformationDoc by the xml path
	 * @param XMLPath XML path
	 * @param XSLPath XSL path
	 * @return XSLTranformationDoc
	 */
	public static XSLTranformationDoc getXSLTranformationDocByXMLPath(String XMLPath,String XSLPath) {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		XSLTranformationDoc doc = null;
		try{
			doc = (XSLTranformationDoc) cache.get(getPrimaryGroup() + XMLPath+"_"+XSLPath, getPrimaryGroup());
		}catch (DotCacheException e) {
			Logger.debug(XSLTransformationCache.class,"Cache Entry not found", e);
		}

		if (doc != null) {
			try{

				/*validate if xsl file change*/
				Identifier xslIdentifier = APILocator.getIdentifierAPI().find(doc.getIdentifier());
				File xslFile = (File) versionableAPI.findWorkingVersion(xslIdentifier, userAPI.getSystemUser(), false);

				/*validate time to live*/
				long ttl = doc.getTtl() - new Date().getTime();

				if(ttl <= 0 || doc.getInode() != xslFile.getInode()){
					removeXSLTranformationDoc(doc);
					doc =null;
				}


			}catch (Exception e) {
				Logger.debug(XSLTransformationCache.class,"Cache xsl identifier not found", e);
			}
		}
		return doc;
	}

	/**
	 * Remove from chache the specified XSLTranformationDoc 
	 * @param xmlDoc
	 */
	public static void removeXSLTranformationDoc(XSLTranformationDoc doc){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		String xmlPath = doc.getXmlPath();
		String xslPath = doc.getXslPath();
		cache.remove(getPrimaryGroup() + xmlPath+"_"+xslPath, getPrimaryGroup());
	}

	/**
	 * Flush al the cache
	 *
	 */
	public static void clearCache(){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		//clear the cache
		cache.flushGroup(getPrimaryGroup());
	}
	public static String[] getGroups() {
		String[] groups = {getPrimaryGroup()};
		return groups;
	}

	public static String getPrimaryGroup() {
		return "XSLTransformationCache";
	}
}

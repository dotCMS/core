package com.dotmarketing.viewtools.cache;

import java.util.Date;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.util.encoders.Base64;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.viewtools.bean.XmlToolDoc;



/**
 * This class manage the XmlToolDoc plugin cache
 * @author Oswaldo Gallango
 * @version 1.0
 */

public class XmlToolCache {

	/**
	 * Add into the cache the XmlTool Doc
	 * @param doc XmlToolDoc
	 */
	public static void addXmlToolDoc(XmlToolDoc doc){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		// we use the identifier uri for our mappings.
		String xmlPath = hashPath(doc.getXmlPath());
		cache.put(getPrimaryGroup() + xmlPath, doc, getPrimaryGroup());

	}

	/**
	 * Get the XmlToolDoc by the xml path
	 * @param XMLPath XML path
	 * @return XmlToolDoc
	 */
	public static XmlToolDoc getXmlToolDoc(String XMLPath) {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		XmlToolDoc doc = null;
		try{
			doc = (XmlToolDoc) cache.get(getPrimaryGroup() + hashPath(XMLPath), getPrimaryGroup());
		}catch (DotCacheException e) {
			Logger.debug(XmlToolCache.class,"Cache Entry not found", e);
		}

		if (doc != null) {
			try{

				/*validate time to live*/
				long ttl = doc.getTtl() - new Date().getTime();

				if(ttl <= 0 ){
					removeXmlToolDoc(doc);
					doc =null;
				}


			}catch (Exception e) {
				Logger.debug(XmlToolCache.class,"Cache XmlTool not found", e);
			}
		}
		return doc;
	}

	/**
	 * Remove from chache the specified XmlToolDoc 
	 * @param doc XmlToolDoc
	 */
	public static void removeXmlToolDoc(XmlToolDoc doc){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		String xmlPath = hashPath(doc.getXmlPath());
		cache.remove(getPrimaryGroup() + xmlPath, getPrimaryGroup());
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
		return "XmlToolCache";
	}
	
	private static String hashPath(String path) {
		byte[] data=path.getBytes();
		SHA1Digest digest=new SHA1Digest();
		digest.update(data, 0, data.length);
		byte[] result=new byte[digest.getDigestSize()];
		digest.doFinal(result, 0);
		String ret=null;
		ret=new String(Base64.encode(result));		
		return ret;
	}
	
}

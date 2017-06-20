package com.dotcms.util;

import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * This class provide some utility methods to interact with
 * Vanity URLs
 * @author oswaldogallango
 *
 */
public class VanityUrlUtil {

	/**
	 * Generate the sanitized cache key
	 * @param key to be used in the cache
	 * @param languageId 
	 * @return String with the sanitized key name
	 */
	public static String sanitizeKey(String key, long languageId){
		return key.replace('/', '|')+"|lang_"+languageId;
	}
	
	/**
	 * Generate the sanitized cache key
	 * @param vanityUrl The vanity Url contentlet
	 * @return String with the sanitized key name
	 * @throws DotSecurityException 
	 * @throws DotRuntimeException 
	 * @throws DotDataException 
	 */
	public static String sanitizeKey(Contentlet vanityUrl) throws DotDataException, DotRuntimeException, DotSecurityException{
		Host host = APILocator.getHostAPI().find(vanityUrl.getStringProperty(VanityUrlContentType.SITE_FIELD_VAR), APILocator.systemUser(), false);
		String key = host != null && !host.getIdentifier().equals(Host.SYSTEM_HOST)?host.getIdentifier()+"|"+fixURI(vanityUrl.getStringProperty(VanityUrlContentType.URI_FIELD_VAR)):fixURI(vanityUrl.getStringProperty(VanityUrlContentType.URI_FIELD_VAR));
		return sanitizeKey(key, vanityUrl.getLanguageId());
	}

	/**
	 * Fix the URI passed if the URI doesn't beging with a "/"
	 * @param uri The URI to fix
	 * @return The fixed uri
	 */
	public static String fixURI(String uri){
		if ( !uri.startsWith("/") ) {
			uri = "/" + uri;
		}
		return uri;
	}
}

package com.dotmarketing.filters;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.List;

public class CmsUrlUtil {
	private static CmsUrlUtil urlUtil;

	public static CmsUrlUtil getInstance() {
		if (urlUtil == null) {

			synchronized (CmsUrlUtil.class) {
				urlUtil = new CmsUrlUtil();
			}

		}
		return urlUtil;
	}

	
	public boolean isPageAsset(Versionable asset) {
		try {
			Identifier id = APILocator.getIdentifierAPI().find(asset);
			if ("contentlet".equals(id.getAssetType()) && asset instanceof Contentlet) {
				Contentlet c = (Contentlet) asset;
				return (c.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE);
			}else 	if ("htmlpage".equals(id.getAssetType())) {
				return true;
			}
			
			return false;
		} catch (Exception e) {
			throw new DotStateException("Getting id failed" + e.getMessage(), e);
		}

	}
	public boolean isPageAsset(String uri, Host host, Long languageId) {
		Identifier id;
		if(!UtilMethods.isSet(uri)){
			return false;
		}
		try {
			id = APILocator.getIdentifierAPI().find(host, uri);
		} catch (Exception e) {
			Logger.error(this.getClass(), "Unable to find" + uri);
			return false;
		}
		if (id == null || id.getId() == null)
			return false;
		if ("htmlpage".equals(id.getAssetType())) {
			return true;
		}
		if ("contentlet".equals(id.getAssetType())) {
			try {

				//Get the list of languages use by the application
				List<Language> languages = APILocator.getLanguageAPI().getLanguages();

				//First try with the given language
				ContentletVersionInfo cinfo = APILocator.getVersionableAPI().getContentletVersionInfo( id.getId(), languageId );
				if ( cinfo == null || cinfo.getWorkingInode().equals( "NOTFOUND" ) ) {

					for ( Language language : languages ) {
						/*
						If we found nothing with the given language it does not mean is not a page,
						could be just a page but it does not exist for the given language.
						Trying with the other languages use in the app.
						 */
						if ( languageId != language.getId() ) {
							cinfo = APILocator.getVersionableAPI().getContentletVersionInfo( id.getId(), language.getId() );
							if ( cinfo != null && !cinfo.getWorkingInode().equals( "NOTFOUND" ) ) {
								//Found it
								break;
							}
						}
					}

				}
				if ( cinfo == null || cinfo.getWorkingInode().equals( "NOTFOUND" ) ) {
					return false;//At this point we know is not a page
				} else {
					Contentlet c = APILocator.getContentletAPI().find( cinfo.getWorkingInode(), APILocator.getUserAPI().getSystemUser(), false );
					return (c.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE);
				}
			} catch (Exception e) {
				Logger.error(this.getClass(), "Unable to find" + uri);
				return false;
			}
		}
		return false;
	}

	public boolean isFileAsset(String uri, Host host, Long languageId) {
		
		// languageId is not used now, but will be used in future functionality. Issue #7141
		
		Identifier id;
		try {
			id = APILocator.getIdentifierAPI().find(host, uri);
		} catch (Exception e) {
			Logger.error(this.getClass(), "Unable to find" + uri);
			return false;
		}
		if (id == null || id.getId() == null)
			return false;
		if ("file_asset".equals(id.getAssetType())) {
			return true;
		}
		
        if ("contentlet".equals(id.getAssetType())) {
            try {
                ContentletVersionInfo cinfo = APILocator.getVersionableAPI().getContentletVersionInfo(id.getId(), languageId);

                if ( (cinfo == null || cinfo.getWorkingInode().equals( "NOTFOUND" )) && Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)) {
                    //Get the Default Language
                    Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
                    //If the fallback to Default Language is set to true, let's see if the requested file is stored with Default Language 
                    cinfo = APILocator.getVersionableAPI().getContentletVersionInfo( id.getId(), defaultLang.getId() );
                }

                if ( cinfo == null || cinfo.getWorkingInode().equals( "NOTFOUND" ) ) {
                    return false;//At this point we know is not a File Asset
                } else {
                    Contentlet c = APILocator.getContentletAPI().find( cinfo.getWorkingInode(), APILocator.getUserAPI().getSystemUser(), false );
                    return (c.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_FILEASSET);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Unable to find" + uri);
                return false;
            }
        }
		return false;
	}

	public boolean isFolder(String uri, Host host) {
		Identifier id;
		if("/".equals(uri)){
			return true;
		}
		while(uri.endsWith("/") && uri.length()>1){
			uri = uri.substring(0,uri.length()-1);
		}
			
		if(!uri.startsWith("/"))
			uri = "/" + uri;
		
		try {
			id = APILocator.getIdentifierAPI().find(host, uri);
			if (id == null || id.getId() == null) {
				return false;
			}
			if ("folder".equals(id.getAssetType())) {
				return true;
			}
		} catch (Exception e) {
			Logger.error(this.getClass(), "Unable to find" + uri);
		}

		return false;
	}

	public boolean isVanityUrl(String uri, Host host) {
		
		if (uri.length()>1 && uri.endsWith("/"))
            uri = uri.substring(0, uri.length() - 1);

		boolean isVanityURL = UtilMethods.isSet(VirtualLinksCache.getPathFromCache(host.getHostname() + ":" + uri));
		if (!isVanityURL) {
			isVanityURL = UtilMethods.isSet(VirtualLinksCache.getPathFromCache(uri));
		}
		// Still support legacy cmsHomePage
		if("/".equals(uri) && !isVanityURL){
			uri = "/cmsHomePage";
			isVanityURL = UtilMethods.isSet(VirtualLinksCache.getPathFromCache(host.getHostname() + ":" + uri));
			if (!isVanityURL) {
				isVanityURL = UtilMethods.isSet(VirtualLinksCache.getPathFromCache(uri));
			}
		}
		return isVanityURL;

	}

	public boolean canRead(Identifier ident, long languageId, User user) {
		if (ident == null || ident.getId() == null) {
			throw new DotStateException("Identifier cannot be null");
		}
		if (ident.getAssetType().equals("contentlet")) {
			try {
				ContentletVersionInfo cinfo = APILocator.getVersionableAPI().getContentletVersionInfo(ident.getId(), languageId);

				// If we did not find a version with for given
				// language lets try with the default language
				if (cinfo == null && languageId != APILocator.getLanguageAPI().getDefaultLanguage().getId()) {
					languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
					cinfo = APILocator.getVersionableAPI().getContentletVersionInfo(ident.getId(), languageId);
				}

				Contentlet proxy = new Contentlet();

				proxy.setInode(cinfo.getWorkingInode());
				proxy.setIdentifier(cinfo.getIdentifier());
				proxy.setLanguageId(cinfo.getLang());
				return APILocator.getPermissionAPI().doesUserHavePermission(proxy, PermissionAPI.PERMISSION_READ, user, true);
			} catch (Exception e) {
				Logger.warn(this, "Unable to find file asset contentlet with identifier " + ident.getId(), e);
			}

			if (ident.getAssetType().equals("file_asset")) {
				com.dotmarketing.portlets.files.model.File f = new com.dotmarketing.portlets.files.model.File();
				(f).setIdentifier(ident.getInode());
				try {
					return APILocator.getPermissionAPI().doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, true);
				} catch (DotDataException e) {
					Logger.warn(this, "Unable to find file asset contentlet with identifier " + ident.getId(), e);
				}

			}
			if (ident.getAssetType().equals("htmlpage")) {
				com.dotmarketing.portlets.files.model.File f = new com.dotmarketing.portlets.files.model.File();
				(f).setIdentifier(ident.getInode());
				try {
					return APILocator.getPermissionAPI().doesUserHavePermission(f, PermissionAPI.PERMISSION_READ, user, true);
				} catch (DotDataException e) {
					Logger.warn(this, "Unable to find file asset contentlet with identifier " + ident.getId(), e);
				}
			}
		}

		return false;
	}
	
	public boolean amISomething(String uri, Host host, Long languageId) {
		return (urlUtil.isFileAsset(uri, host, languageId) || urlUtil.isVanityUrl(uri, host) 
				|| urlUtil.isPageAsset(uri, host, languageId) || urlUtil.isFolder(uri, host));
	}

}

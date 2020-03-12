package com.dotmarketing.filters;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.*;
import com.dotmarketing.cms.urlmap.UrlMapContext;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.filters.CMSFilter.IAm;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.Xss;

import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.filters.Constants.CMS_FILTER_QUERY_STRING_OVERRIDE;
import static com.dotmarketing.filters.Constants.CMS_FILTER_URI_OVERRIDE;

import com.dotcms.contenttype.model.type.BaseContentType;

/**
 * Utilitary class used by the CMS Filter
 */
public class CMSUrlUtil {

	private static CMSUrlUtil urlUtil;
	private static final String CONTENTLET = "contentlet";
	private static final String HTMLPAGE = "htmlpage";
	private static final String FILE_ASSET = "file_asset";
	private static final String FOLDER = "folder";
	private static final String NOT_FOUND = "NOTFOUND";
	private static final String UNABLE_TO_FIND = "Unable to find ";

	public static final String [] BACKEND_FILTERED_LIST_ARRAY =
			new String[] {"/html","/api","/dotAdmin","/dwr","/webdav","/dA","/contentAsset","/c/","/DOTSASS","/DOTLESS"};

	/**
	 * Get the CmsUrlUtil singleton instance
	 *
	 * @return a CmsUrlUtil instance
	 */
	public static CMSUrlUtil getInstance() {
		if (urlUtil == null) {

			synchronized (CMSUrlUtil.class) {
				urlUtil = new CMSUrlUtil();
			}

		}
		return urlUtil;
	}

	private CMSUrlUtil() {
	}

	/**
	 * Indicates if the versionable object is a Page Asset
	 *
	 * @param asset Versionable assset
	 * @return true if the URI is a Page Asset, false if not
	 */
	public boolean isPageAsset(Versionable asset) {
		try {
			Identifier id = APILocator.getIdentifierAPI().find(asset);
			if (CONTENTLET.equals(id.getAssetType()) && asset instanceof Contentlet) {
				Contentlet c = (Contentlet) asset;
				return (c.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE);
			} else if (HTMLPAGE.equals(id.getAssetType())) {
				return true;
			}

			return false;
		} catch (Exception e) {
			throw new DotStateException("Getting id failed" + e.getMessage(), e);
		}

	}
	/**
	 * Returns the IAm value for a url
	 * @param iAm
	 * @param uri
	 * @param site
	 * @param languageId
	 * @return
	 */
    public IAm resolveResourceType(final IAm iAm,
            final String uri,
            final Host site,
            final long languageId) {

        final String uriWithoutQueryString = this.urlUtil.getUriWithoutQueryString (uri);
        if (isFileAsset(uriWithoutQueryString, site, languageId)) {
            return IAm.FILE;
        } else if (isPageAsset(uriWithoutQueryString, site, languageId)) {
            return IAm.PAGE;
        } else if (isFolder(uriWithoutQueryString, site)) {
            return IAm.FOLDER;
        }
        else {
            return IAm.NOTHING_IN_THE_CMS;
        }

} // resolveResourceType.
	/**
	 * Indicates if the uri belongs to a Page Asset
	 *
	 * @param uri The current uri
	 * @param host The current host
	 * @param languageId The current language Id
	 * @return true if the URI is a Page Asset, false if not
	 */
	public boolean isPageAsset(String uri, Host host, Long languageId) {
		Identifier id;
		if (!UtilMethods.isSet(uri)) {
			return false;
		}
		try {
			id = APILocator.getIdentifierAPI().find(host, uri);
		} catch (Exception e) {
			Logger.error(this.getClass(), UNABLE_TO_FIND + uri);
			return false;
		}
		if (id == null || id.getId() == null) {
			return false;
		}
		if (HTMLPAGE.equals(id.getAssetType())) {
			return true;
		}
		if (CONTENTLET.equals(id.getAssetType())) {
			try {

				//Get the list of languages use by the application
				List<Language> languages = APILocator.getLanguageAPI().getLanguages();

				//First try with the given language
				ContentletVersionInfo cinfo = APILocator.getVersionableAPI()
						.getContentletVersionInfo(id.getId(), languageId);
				if (cinfo == null || cinfo.getWorkingInode().equals(NOT_FOUND)) {

					for (Language language : languages) {
                        /*
                        If we found nothing with the given language it does not mean is not a page,
						could be just a page but it does not exist for the given language.
						Trying with the other languages use in the app.
						 */
						if (languageId != language.getId()) {
							cinfo = APILocator.getVersionableAPI()
									.getContentletVersionInfo(id.getId(), language.getId());
							if (cinfo != null && !cinfo.getWorkingInode().equals(NOT_FOUND)) {
								//Found it
								break;
							}
						}
					}

				}
				if (cinfo == null || cinfo.getWorkingInode().equals(NOT_FOUND)) {
					return false;//At this point we know is not a page
				} else {
					Contentlet c = APILocator.getContentletAPI()
							.find(cinfo.getWorkingInode(), APILocator.getUserAPI().getSystemUser(),
									false);
					return (c.getStructure().getStructureType()
							== Structure.STRUCTURE_TYPE_HTMLPAGE);
				}
			} catch (Exception e) {
				Logger.error(this.getClass(), UNABLE_TO_FIND + uri);
				return false;
			}
		}

		try {
			final UrlMapContext urlMapContext = new UrlMapContext(
					PageMode.PREVIEW_MODE,
					languageId,
					uri,
					host,
					APILocator.getUserAPI().getSystemUser());

			return APILocator.getURLMapAPI().isUrlPattern(urlMapContext);
		} catch (final DotDataException e){
			Logger.error(this.getClass(), e.getMessage());
			return false;
		}
	}

	/**
	 * Indicates if the uri belongs to a File Asset
	 *
	 * @param uri The current uri
	 * @param host The current host
	 * @param languageId The current language Id
	 * @return true if the URI is a File Asset, false if not
	 */
	public boolean isFileAsset(String uri, Host host, Long languageId) {

		// languageId is not used now, but will be used in future functionality. Issue #7141

		Identifier id;
		try {
			id = APILocator.getIdentifierAPI().find(host, uri);
		} catch (Exception e) {
			Logger.error(this.getClass(), UNABLE_TO_FIND + uri);
			return false;
		}
		if (id == null || id.getId() == null) {
			return false;
		}
		if (FILE_ASSET.equals(id.getAssetType())) {
			return true;
		}

		if (CONTENTLET.equals(id.getAssetType())) {
			try {
				ContentletVersionInfo cinfo = APILocator.getVersionableAPI()
						.getContentletVersionInfo(id.getId(), languageId);

				if ((cinfo == null || cinfo.getWorkingInode().equals(NOT_FOUND)) && Config
						.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)) {
					//Get the Default Language
					Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
					//If the fallback to Default Language is set to true, let's see if the requested file is stored with Default Language
					cinfo = APILocator.getVersionableAPI()
							.getContentletVersionInfo(id.getId(), defaultLang.getId());
				}

				if (cinfo == null || cinfo.getWorkingInode().equals(NOT_FOUND)) {
					return false;//At this point we know is not a File Asset
				} else {
					Contentlet c = APILocator.getContentletAPI()
							.find(cinfo.getWorkingInode(), APILocator.getUserAPI().getSystemUser(),
									false);
					return (c.getContentType().baseType() == BaseContentType.FILEASSET);
				}
			} catch (Exception e) {
				Logger.warnAndDebug(this.getClass(), UNABLE_TO_FIND + uri +":"+e.getMessage(),e);
				return false;
			}
		}
		return false;
	}

	/**
	 * Indicates if the uri belongs to a Folder
	 *
	 * @param uri The current uri
	 * @param host The current host
	 * @return true if the URI is a folder, false if not
	 */
	public boolean isFolder(String uri, Host host) {
		Identifier id;
		if ("/".equals(uri)) {
			return true;
		}
		while (uri.endsWith("/") && uri.length() > 1) {
			uri = uri.substring(0, uri.length() - 1);
		}

		if (!uri.startsWith("/")) {
			uri = "/" + uri;
		}

		try {
			id = APILocator.getIdentifierAPI().find(host, uri);
			if (id == null || id.getId() == null) {
				return false;
			}
			if (FOLDER.equals(id.getAssetType())) {
				return true;
			}
		} catch (Exception e) {
			Logger.debug(this.getClass(), UNABLE_TO_FIND + uri);
		}

		return false;
	}

	/**
	 * Indicates if the uri belongs to a VanityUrl
	 *
	 * @param uri The current uri
	 * @param host The current host
	 * @param languageId The current language Id
	 * @return true if the URI is a vanity URL, false if not
	 */
	public boolean isVanityUrl(final String uri,
							   final Host host,
							   final long languageId) {

		return APILocator.getVanityUrlAPI().isVanityUrl(uri, host, languageId);
	} // isVanityUrl.

	/**
	 * Determine if the url should be filtered from the vanity treatment
	 * Exists a blacklist on dotmarketing called: vanity.filtered.list=,
	 * which is separated list by commas.
	 * The algorithm basically checks if the url starts with any of the elements on the list.
	 * @param url {@link String} url to test
	 * @return boolean true if it is filtered (on the blacklist)
	 */
	public boolean isVanityUrlFiltered(final String url) {

		if (null != url) {
			for (final String vanityFiltered : BACKEND_FILTERED_LIST_ARRAY) {

				if (url.startsWith(vanityFiltered)) {

					return true;
				}
			}
		}

		return false;
	} // isVanityUrlFiltered.

	/**
	 * Indicate if the user have permision to read
	 * the contentlet associated to the identifier
	 *
	 * @param ident The object Identifier
	 * @param languageId The object language Id
	 * @param user The current user
	 */
	public boolean canRead(Identifier ident, long languageId, User user) {
		if (ident == null || ident.getId() == null) {
			throw new DotStateException("Identifier cannot be null");
		}
		if (ident.getAssetType().equals(CONTENTLET)) {
			try {
				ContentletVersionInfo cinfo = APILocator.getVersionableAPI()
						.getContentletVersionInfo(ident.getId(), languageId);

				// If we did not find a version with for given
				// language lets try with the default language
				if (cinfo == null && languageId != APILocator.getLanguageAPI().getDefaultLanguage()
						.getId()) {
					languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
					cinfo = APILocator.getVersionableAPI()
							.getContentletVersionInfo(ident.getId(), languageId);
				}

				Contentlet proxy = new Contentlet();

				proxy.setInode(cinfo.getWorkingInode());
				proxy.setIdentifier(cinfo.getIdentifier());
				proxy.setLanguageId(cinfo.getLang());
				return APILocator.getPermissionAPI()
						.doesUserHavePermission(proxy, PermissionAPI.PERMISSION_READ, user, true);
			} catch (Exception e) {
				Logger.warn(this,
						"Unable to find file asset contentlet with identifier " + ident.getId(), e);
			}
		}

		return false;
	}

	/**
	 * Indicates if the uri belongs to a File Asset or Vanity Url
	 * or Page Asset or Folder
	 *
	 * @param uri the current uri
	 * @param host The current host
	 * @param languageId The object language Id
	 * @return true if is a File Asset or Vanity Url or Page Asset or Folder, false if not
	 */
	public boolean amISomething(String uri, Host host, Long languageId) {
		return (urlUtil.isFileAsset(uri, host, languageId) || urlUtil
				.isVanityUrl(uri, host, languageId)
				|| urlUtil.isPageAsset(uri, host, languageId) || urlUtil.isFolder(uri, host));
	}

	/**
	 * Checks in the given uri and query string for possible XSS hacks
	 */
	String xssCheck(String uri, String queryString) throws ServletException {

		String rewrite = null;
		if (Xss.URIHasXSS(uri)) {
			Logger.warn(this, "XSS Found in request URI: " + uri);
			try {
				rewrite = Xss.encodeForURL(uri);
			} catch (Exception e) {
				Logger.error(this, "Encoding failure. Unable to encode URI " + uri);
				throw new ServletException(e.getMessage(), e);
			}
		} else if (queryString != null && null != UtilMethods.decodeURL(queryString)) {
			if (Xss.ParamsHaveXSS(queryString)) {
				Logger.warn(this, "XSS Found in Query String: " + queryString);
				rewrite = uri;
			}
		}

		return rewrite;
	}

	/**
	 * Search for an overridden URI by a filter and if nothing is found the URI will be read from
	 * the request.
	 */
	public String getURIFromRequest(final HttpServletRequest request) {
        return (request.getAttribute(CMS_FILTER_URI_OVERRIDE) != null) ? (String) request
				.getAttribute(CMS_FILTER_URI_OVERRIDE)
				: getRequestPath(request);
	}

    /**
     * Returns path from request URI
     * @param request - HttpServletRequest
     * @return String containing the path from the request URI
     */
	private String getRequestPath(final HttpServletRequest request){
        String requestPath = request.getRequestURI();
        try {
            final URI requestURI = new URI(requestPath);
            requestPath = requestURI.getPath();
        } catch (URISyntaxException e) {
            Logger.error(this, "Couldn't get URL from request " + requestPath, e);
        }
        return requestPath;
    }

	/**
	 * Verifies if the URI was overridden by a filter
	 */
	Boolean wasURIOverridden(HttpServletRequest request)
			throws UnsupportedEncodingException {
		return (request.getAttribute(CMS_FILTER_URI_OVERRIDE) != null);
	}

	/**
	 * Search for an overridden query string by a filter and if nothing is found the query string
	 * will be read from the request.
	 */
	String getURLQueryStringFromRequest(HttpServletRequest request)
			throws UnsupportedEncodingException {

		return (request.getAttribute(CMS_FILTER_QUERY_STRING_OVERRIDE) != null) ? (String) request
				.getAttribute(CMS_FILTER_QUERY_STRING_OVERRIDE)
				: request.getQueryString();
	}

	/**
	 * Verifies if a given user has READ permissions on a given permissionable object, if there is
	 * no logged in user and the permissionable object requires authentication a 401 response error
	 * is set in order to redirect the user to the login page and if an user is already logged in
	 * but that logged in user does not have read permissions on the given permissionable object a
	 * 403 response error is set.
	 *
	 * @param permissionable Permissionable object to validate
	 * @param requestedURIForLogging The original requested URI, this URI is required as a parameter
	 * just for logging purposes, is not used to calculate anything.
	 * @param user Current user
	 */
    public boolean isUnauthorizedAndHandleError(final Permissionable permissionable, final String requestedURIForLogging,
            final User user, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, DotDataException {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();

        PageMode mode = PageMode.get(request);
        // Check if the page is visible by a CMS Anonymous role
        if (!permissionAPI.doesUserHavePermission(permissionable, PERMISSION_READ, user, mode.respectAnonPerms)) {

            if (null == user) {// Not logged in user

                Logger.debug(this.getClass(),
                        "CHECKING PERMISSION: Page doesn't have anonymous access [" + requestedURIForLogging + "]");
                Logger.debug(this.getClass(), "401 URI = " + requestedURIForLogging);
                Logger.debug(this.getClass(), "Unauthorized URI = " + requestedURIForLogging);

                request.getSession().setAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN, requestedURIForLogging);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "The requested page/file is unauthorized");
                return true;
            } else if (!permissionAPI.getRolesWithPermission(permissionable, PERMISSION_READ)
                .contains(APILocator.getRoleAPI().loadLoggedinSiteRole())) {
                // User is logged in need to check user permissions
                if (!permissionAPI.doesUserHavePermission(permissionable, PERMISSION_READ, user, true)) {
                    // the user doesn't have permissions to see this page
                    // go to unauthorized page
                    Logger.warn(this.getClass(),
                            "CHECKING PERMISSION: Page doesn't have any access for this user [" + requestedURIForLogging + "]");
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "The requested page/file is forbidden");
                    return true;
                }
            }
        }

        return false;
    }


	/**
	 * if the uri has a query string then remove it, otherwise keep it as it is
	 * @param uri String
	 * @return String
	 */
	public String getUriWithoutQueryString(final String uri) {

		final int indexOf = uri.indexOf('?');
		return (-1 != indexOf)?
				uri.substring(0, indexOf):
				uri;

	} // getUriWithoutQueryString.

	/**
	 * if the uri has a query string then return it, otherwise null
	 * @param uri String
	 * @return String
	 */
	public String getQueryStringFromUri(final String uri) {

		final int indexOf = uri.indexOf('?');
		return (-1 != indexOf && indexOf+1 < uri.length())?
				uri.substring(indexOf+1):
				null;
	}

	public static String getCurrentURI(final HttpServletRequest request)  {
		try {
			return URLDecoder.decode((request.getAttribute(CMS_FILTER_URI_OVERRIDE) != null)
					? (String) request.getAttribute(CMS_FILTER_URI_OVERRIDE)
					: request.getRequestURI(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Logger.debug(CMSUrlUtil.class, e.getMessage(), e);
			throw new DotRuntimeException(e);
		}
	}
}
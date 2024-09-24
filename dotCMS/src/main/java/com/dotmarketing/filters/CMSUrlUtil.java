package com.dotmarketing.filters;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.cms.urlmap.UrlMapContext;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter.IAm;
import com.dotmarketing.filters.CMSFilter.IAmSubType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.Xss;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.filters.CMSFilter.CMS_INDEX_PAGE;
import static com.dotmarketing.filters.Constants.CMS_FILTER_QUERY_STRING_OVERRIDE;
import static com.dotmarketing.filters.Constants.CMS_FILTER_URI_OVERRIDE;
import static com.liferay.util.StringPool.FORWARD_SLASH;
import static com.liferay.util.StringPool.PERIOD;
import static com.liferay.util.StringPool.UNDERLINE;
import static java.util.stream.Collectors.toSet;

/**
 * Utilitary class used by the CMS Filter
 */
public class CMSUrlUtil {

	private static CMSUrlUtil urlUtil;

	public static final String NOT_FOUND = "NOTFOUND";

	private static final String CONTENTLET = "contentlet";
	private static final String HTMLPAGE = "htmlpage";
	private static final String FILE_ASSET = "file_asset";
	private static final String FOLDER = "folder";
	private static final String UNABLE_TO_FIND = "Unable to find ";

	public static final Set<String> BACKEND_FILTERED_COLLECTION =
			Stream.of("/api", "/webdav", "/dA", "/c/", "/contentAsset", "/DOTSASS", "/DOTLESS",
					"/html", "/dotAdmin", "/custom-elements","/dotcms-webcomponents","/dwr")
					.collect(Collectors.collectingAndThen(toSet(), Collections::unmodifiableSet));

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
				return c.isHTMLPage();
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
	 * @return Tuple2<IAm, IAmSubType>
	 */
	public Tuple2<IAm, IAmSubType> resolveResourceType(final IAm iAm,
			final String uri,
			final Host site,
			final long languageId) {
		Logger.debug(this.getClass(), "CMSUrlUtil_resolveResourceType");
		Logger.debug(this.getClass(), "CMSUrlUtil_resolveResourceType URI = " + uri);
		Logger.debug(this.getClass(), "CMSUrlUtil_resolveResourceType site = " + site.getIdentifier());
		Logger.debug(this.getClass(), "CMSUrlUtil_resolveResourceType lang = " + languageId);

		final String uriWithoutQueryString = this.getUriWithoutQueryString (uri);
		if (isFileAsset(uriWithoutQueryString, site, languageId)) {
			return Tuple.of(IAm.FILE, IAmSubType.NONE);
		}

		Tuple2<Boolean, IAmSubType> isPage = resolvePageAssetSubtype(uriWithoutQueryString, site, languageId);

		if (isPage._1()) {
			Logger.debug(this.getClass(), "CMSUrlUtil_resolveResourceType is a Page");
			return Tuple.of(IAm.PAGE, isPage._2());
		}

		if(isFolder(uriWithoutQueryString, site)) {
			// resolves correctly for folders with index pages
			return uriWithoutQueryString.endsWith("/") && isPageAsset(uriWithoutQueryString + CMS_INDEX_PAGE, site, languageId)
					? Tuple.of(IAm.PAGE, IAmSubType.PAGE_INDEX)
					: Tuple.of(IAm.FOLDER,IAmSubType.NONE);

		}
		Logger.debug(this.getClass(), "CMSUrlUtil_resolveResourceType is a NOTHING_IN_THE_CMS");
		return Tuple.of(IAm.NOTHING_IN_THE_CMS, IAmSubType.NONE);

	} // resolveResourceType.

	public boolean isPageAsset(final String uri, final Host host, final long languageId) {
		return resolvePageAssetSubtype(uri, host, languageId)._1();
	}

	/**
	 * Indicates if the uri belongs to a Page Asset
	 *
	 * @param uri The current uri
	 * @param host The current host
	 * @param languageId The current language Id
	 * @return a tuple where the boolean will be true if the URI is a Page Asset, false if not
	 * 		   and the IAmSubType will be the type of page asset when the boolean is true
	 */
	public Tuple2<Boolean, IAmSubType> resolvePageAssetSubtype(final String uri, final Host host, final Long languageId) {
		Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype");
		Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype URI = " + uri);
		Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtypet site = " + host.getIdentifier());
		Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype lang = " + languageId);

		Identifier id;
		if (!UtilMethods.isSet(uri)) {
			return Tuple.of(false, IAmSubType.NONE);
		}
		try {
			id = APILocator.getIdentifierAPI().find(host, uri);
		} catch (Exception e) {
			Logger.error(this.getClass(), UNABLE_TO_FIND + uri);
			return Tuple.of(false, IAmSubType.NONE);
		}
		Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype Id " + id == null? "Not Found" : id.toString());
		if (id == null || id.getId() == null) {
			return Tuple.of(false, IAmSubType.NONE);
		}
		if (HTMLPAGE.equals(id.getAssetType())) {
			Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype Id AssetType is Page");
			return Tuple.of(true, IAmSubType.NONE);
		}
		if (CONTENTLET.equals(id.getAssetType())) {
			Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype Id AssetType is Contentlet");
			try {

				//Get the list of languages use by the application
				List<Language> languages = APILocator.getLanguageAPI().getLanguages();

				//First try with the given language
				Optional<ContentletVersionInfo> cinfo = APILocator.getVersionableAPI()
						.getContentletVersionInfo(id.getId(), languageId);
				Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype contentletVersionInfo for Lang " + (cinfo.isEmpty() ? "Not Found" : cinfo.toString()));
				if (cinfo.isEmpty() || cinfo.get().getWorkingInode().equals(NOT_FOUND)) {

					for (Language language : languages) {
						Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype contentletVersionInfo for lang not found trying with all langs");
                        /*
                        If we found nothing with the given language it does not mean is not a page,
						could be just a page but it does not exist for the given language.
						Trying with the other languages use in the app.
						 */
						if (languageId != language.getId()) {
							cinfo = APILocator.getVersionableAPI()
									.getContentletVersionInfo(id.getId(), language.getId());
							if (cinfo.isPresent() && !cinfo.get().getWorkingInode().equals(NOT_FOUND)) {
								Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype contentletVersionInfo found " + cinfo.toString());
								//Found it
								break;
							}
						}
					}

				}
				if (cinfo.isEmpty() || cinfo.get().getWorkingInode().equals(NOT_FOUND)) {
					Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype is not a Page returning false");
					return Tuple.of(false, IAmSubType.NONE);//At this point we know is not a page
				}
				Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype Trying to get Contentlet");
				Contentlet c = APILocator.getContentletAPI().find(cinfo.get().getWorkingInode(), APILocator.systemUser(),false);
				Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype Contentlet found " + c.toString());
				return Tuple.of(c.isHTMLPage(), IAmSubType.NONE);

			} catch (Exception e) {
				Logger.error(this.getClass(), UNABLE_TO_FIND + uri);
				return Tuple.of(false, IAmSubType.NONE);
			}
		}

		try {
			final UrlMapContext urlMapContext = new UrlMapContext(
					PageMode.PREVIEW_MODE,
					languageId,
					uri,
					host,
					APILocator.getUserAPI().getSystemUser());

			boolean isUrlMap = APILocator.getURLMapAPI().isUrlPattern(urlMapContext);
			Logger.debug(this.getClass(), "CMSUrlUtil_resolvePageAssetSubtype Id AssetType is UrlMap " + isUrlMap);
			return Tuple.of(isUrlMap, isUrlMap ? IAmSubType.PAGE_URL_MAP : IAmSubType.NONE);
		} catch (final DotDataException | DotSecurityException e){
			Logger.error(this.getClass(), e.getMessage());
			return Tuple.of(false, IAmSubType.NONE);
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
		Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset");
		Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset URI = " + uri);
		Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset site = " + host.getIdentifier());
		Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset lang = " + languageId);

		// languageId is not used now, but will be used in future functionality. Issue #7141

		Identifier id;
		try {
			id = APILocator.getIdentifierAPI().find(host, uri);
		} catch (Exception e) {
			Logger.error(this.getClass(), UNABLE_TO_FIND + uri);
			return false;
		}
		Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset Id " + (id == null? "Not Found" : id.toString()));
		if (id == null || id.getId() == null) {
			return false;
		}
		if (FILE_ASSET.equals(id.getAssetType())) {
			Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset Id AssetType is FileAsset");
			return true;
		}

		if (CONTENTLET.equals(id.getAssetType())) {
			Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset Id AssetType is Contentlet");
			try {
				Optional<ContentletVersionInfo> cinfo = APILocator.getVersionableAPI()
						.getContentletVersionInfo(id.getId(), languageId);
				Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset contentletVersionInfo for Lang " + (cinfo.isEmpty() ? "Not Found" : cinfo.toString()));
				if ((cinfo.isEmpty() || cinfo.get().getWorkingInode().equals(NOT_FOUND)) && Config
						.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)) {
					Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset contentletVersionInfo for lang not found trying defaultLang");
					//Get the Default Language
					Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
					//If the fallback to Default Language is set to true, let's see if the requested file is stored with Default Language
					cinfo = APILocator.getVersionableAPI()
							.getContentletVersionInfo(id.getId(), defaultLang.getId());
					Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset contentletVersionInfo for defaultLang " + (cinfo.isEmpty() ? "Not Found" : cinfo.toString()));
				}

				if (cinfo.isEmpty() || cinfo.get().getWorkingInode().equals(NOT_FOUND)) {
					Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset is not a FileAsset returning false");
					return false;//At this point we know is not a File Asset
				} else {
					Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset Trying to get Contentlet");
					Contentlet c = APILocator.getContentletAPI()
							.find(cinfo.get().getWorkingInode(), APILocator.getUserAPI().getSystemUser(),
									false);
					Logger.debug(this.getClass(), "CMSUrlUtil_isFileAsset Contentlet found " + c.toString());
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
			Logger.debug(this.getClass(), "CMSUrlUtil_isFolder Id " + (id == null? "Not Found" : id.toString()));
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
	
  public boolean isVanityUrl(final String uri,
      final Host host,
      final long languageId) {
    
    return isVanityUrl(uri, host, APILocator.getLanguageAPI().getLanguage(languageId));
  }
	/**
	 * Indicates if the uri belongs to a VanityUrl
	 *
	 * @param uri The current uri
	 * @param host The current host
	 * @param language The current language Id
	 * @return true if the URI is a vanity URL, false if not
	 */
	public boolean isVanityUrl(final String uri,
							   final Host host,
							   final Language language) {

		return APILocator.getVanityUrlAPI().resolveVanityUrl(uri, host, language).isPresent();
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
			return BACKEND_FILTERED_COLLECTION.stream().anyMatch(url::startsWith);
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
				Optional<ContentletVersionInfo> cinfo = APILocator.getVersionableAPI()
						.getContentletVersionInfo(ident.getId(), languageId);

				// If we did not find a version with for given
				// language lets try with the default language
				if (cinfo.isEmpty() && languageId != APILocator.getLanguageAPI().getDefaultLanguage()
						.getId()) {
					languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
					cinfo = APILocator.getVersionableAPI()
							.getContentletVersionInfo(ident.getId(), languageId);
				}

				if(cinfo.isEmpty()) {
					throw new DotDataException("Unable to find file asset contentlet with identifier: "+ ident.getId() + ". Lang:" + languageId);
				}

				Contentlet proxy = new Contentlet();
				proxy.setInode(cinfo.get().getWorkingInode());
				proxy.setIdentifier(cinfo.get().getIdentifier());
				proxy.setLanguageId(cinfo.get().getLang());
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
        return (request.getAttribute(CMS_FILTER_URI_OVERRIDE) != null) 
                ? (String) request.getAttribute(CMS_FILTER_URI_OVERRIDE)
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
	Boolean wasURIOverridden(HttpServletRequest request) {
		return (request.getAttribute(CMS_FILTER_URI_OVERRIDE) != null);
	}

	/**
	 * Search for an overridden query string by a filter and if nothing is found the query string
	 * will be read from the request.
	 */
	String getURLQueryStringFromRequest(HttpServletRequest request) {

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

	/**
	 * Tries to recover the Inode from the URL path. The URL could be a page, such as:
	 * {@code /LIVE/27e8f845c3bd21ad1c601b8fe005caa6/dotParser_1695072095296.container} , or a call
	 * to a resource, such as: {@code Content/27e8f845c3bd21ad1c601b8fe005caa6_1695072095296}
	 *
	 * @param urlPath The URL path from a Contentlet.
	 *
	 * @return The Inode of the Contentlet.
	 */
	public String getInodeFromUrlPath(final String urlPath) {

		// tries the edit mode first
		final PageMode[] modes = PageMode.values();
		for (final PageMode mode : modes) {
			if (urlPath.startsWith(FORWARD_SLASH + mode.name() + FORWARD_SLASH)) {
				final String urlPathWithoutMode = urlPath.substring(mode.name().length() + 2);
				return urlPathWithoutMode.substring(0, urlPathWithoutMode.indexOf(FORWARD_SLASH));
			}
			if (urlPath.startsWith(mode.name() + FORWARD_SLASH)) {
				final String urlPathWithoutMode = urlPath.substring(mode.name().length() + 1);
				int indexOf = urlPathWithoutMode.indexOf(FORWARD_SLASH);
				if (indexOf == -1) {
					indexOf = urlPathWithoutMode.indexOf(UNDERLINE);
				}
				if (indexOf == -1) {
					indexOf = urlPathWithoutMode.indexOf(PERIOD);
				}
				return urlPathWithoutMode.substring(0, indexOf);
			}
		}

		// tries the fe mode: /data/shared/assets/c/e/ce837ff5-dc6f-427a-8f60-d18afc395be9/fileAsset/openai-summarize.vtl
		final Optional<String> inodeOPt = UUIDUtil.findInode(urlPath);
		if (inodeOPt.isPresent()) {
			return inodeOPt.get();
		}

		// tries the content mode: CONTENT/27e8f845c3bd21ad1c601b8fe005caa6_1695072095296.content
		return urlPath.substring(urlPath.indexOf(FORWARD_SLASH) + 1, urlPath.indexOf(UNDERLINE));
	}

}

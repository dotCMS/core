package com.dotmarketing.filters;


import static com.dotmarketing.filters.CMSUrlUtil.isDotAdminRequest;

import com.dotcms.rest.api.v1.page.PageResource;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.WebKeys;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This filter takes all incoming requests related to displaying the contents of
 * a specific site in a specific language in a specific point in time inside the
 * Time Machine portlet.
 * <p>
 * Time Machine allows a user to browse snapshots of how a site has looked
 * historically, or to view how a site will look in the future based on
 * scheduled content publishing and expiration dates. It also allows a user to
 * take a snapshot, or static copy, of selected hosts and save it as a "bundle"
 * in dotCMS.
 *
 * @author michele.mastrogiovanni@gmail.com
 * @version 1.0
 * @since May 31, 2012
 *
 */
@Deprecated(since = "250221", forRemoval = true)
public class TimeMachineFilter implements Filter {

    ServletContext ctx;

    public static final String TM_DATE_VAR="tm_date";
    public static final String TM_LANG_VAR="tm_lang";
    public static final String TM_HOST_VAR="tm_host";

    private static final String ERROR_404 = "/html/portlet/ext/timemachine/timemachine_404.jsp";

    CMSUrlUtil urlUtil = CMSUrlUtil.getInstance();

    @Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		String uri=req.getRequestURI();

		if(req.getSession(false)==null){
		    chain.doFilter(request, response);
		    return;
		}

		// If there's a session attribute that indicates that the request is a page resource, then skip the filter
		// This is a temporary fix to avoid the filter to be executed when the request is a page resource until we retire the old
		if(null != req.getSession().getAttribute(PageResource.IS_PAGE_RESOURCE)){
			chain.doFilter(request, response);
			return;
		}

		if(!uri.startsWith("/")) {
		    uri="/"+uri;
		}
		if(uri.startsWith("/admin") && req.getSession().getAttribute("tm_date") != null){
			req.getSession().removeAttribute(TM_DATE_VAR);
			req.getSession().removeAttribute(TM_LANG_VAR);
			req.getSession().removeAttribute(TM_HOST_VAR);
			
		}
		// If a Time Machine request is present...
		if(req.getSession().getAttribute("tm_date")!=null && urlUtil.amISomething(uri,(Host)req.getSession().getAttribute("tm_host")
				,Long.parseLong((String)req.getSession().getAttribute("tm_lang")))) {
			com.liferay.portal.model.User user = null;
			PageMode.setPageMode(req, PageMode.PREVIEW_MODE,false);
			try {
				user = com.liferay.portal.util.PortalUtil.getUser((HttpServletRequest) request);
				if(!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("time-machine", user)){
					throw new DotSecurityException("user does not have access to the timemachine portlet");
				}
			} catch (Exception e) {
				Logger.error(TimeMachineFilter.class,e.getMessage(),e);
				return;
			}
		    String datestr=(String)req.getSession().getAttribute("tm_date");
		    Date date;
		    try {
		        date=new Date(Long.parseLong(datestr));
		    }
		    catch(Exception ex) {
		        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
		        return;
		    }

		    String langid=(String) req.getSession().getAttribute("tm_lang");
		    // future date. Lets handle in other places
		    if(date.after(new Date())) {
		        request.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, langid);
		        chain.doFilter(request, response);
		        return;
		    }

		    Host host=(Host) req.getSession().getAttribute("tm_host");
			Tuple2<File,String> file = getFileFromTimeMachine(host, uri, date, langid);
			if (file._1().exists()) {
				sendFile(file, request, response);
			} else {
				// File not found for the selected language
				boolean useDefaultLanguage = APILocator.getLanguageAPI().canDefaultPageToDefaultLanguage();
				if (!useDefaultLanguage) {
					Logger.error(TimeMachineFilter.class, "Time Machine file NOT found:" + file._1.getAbsolutePath());
					// Send page in default language is false, so send an error
					sendError(request, response, ERROR_404, host.getHostname() + uri, HttpServletResponse.SC_BAD_REQUEST);
				} else {
					// Check if there's a version of the requested file in the
					// default language
					final String defaultLangId = Long.toString(APILocator.getLanguageAPI().getDefaultLanguage().getId());
					file = getFileFromTimeMachine(host, uri, date, defaultLangId);
					if (file._1().exists()) {
						// It exists, so send file in the default language
						sendFile(file, request, response);
					} else {
						Logger.error(TimeMachineFilter.class, "Time Machine file NOT found (Default Language):" + file._1.getAbsolutePath());
						sendError(request, response, ERROR_404, host.getHostname() + uri, HttpServletResponse.SC_BAD_REQUEST);
					}
				}
			}
		} else {
			chain.doFilter(request, response);
		}
	}

    @Override
    public void destroy() {}

    @Override
    public void init(FilterConfig fc) throws ServletException {
        ctx=fc.getServletContext();
    }

	/**
	 * Reads the a file from a specific Site in a given language. Viewing a Site
	 * from the Time Machine portlet will load all the required files from the
	 * bundled site.
	 *
	 * @param host
	 *            - The Site that users want to inspect.
	 * @param uri
	 *            - The URI to the file required to render a page.
	 * @param selectedDate
	 *            - The specified date of the bundle generated by the Time
	 *            Machine.
	 * @param selectedLangId
	 *            - The user-selected language used to display the site content.
	 * @return The {@link File} object pointing to the resource that belongs to
	 *         the page.
	 */
	private Tuple2<File,String> getFileFromTimeMachine(final Host host, String uri, final Date selectedDate, final String selectedLangId) {
		final StringBuilder basePath = new StringBuilder();
		final String sep = java.io.File.separator;
		
		
		// Base path (e.g., "/usr/opt/dotcms/assets/timemachine/tm_111222333/")
		basePath.append(ConfigUtils.getTimeMachinePath()).append(sep).append("tm_").append(selectedDate.getTime())
				.append(sep);
		// Site and language path (e.g., "live/demo.dotcms.com/1")
		basePath.append(PageMode.LIVE.name().toLowerCase()).append(sep).append(host.getHostname()).append(sep).append(selectedLangId);
		// URI (e.g., "/folder/your-page")
		uri = (java.io.File.separator.equals("\\") ? uri.replaceAll("/", "\\\\") : uri);
		String completePath = basePath.toString() + uri;
		File file = new File(completePath);
		if (file.isDirectory()) {
			if (!uri.endsWith("/")) {
				uri += "/";
			}
			// It's a folder, so append the default index page to the URI
			uri += CMSFilter.CMS_INDEX_PAGE;
			completePath = basePath.toString() + uri;
			file = new File(completePath);
		}
		
        String mimeType = APILocator.getFileAssetAPI().getMimeType(file.getName());
        if (mimeType == null || "unknown".equals(mimeType)) {
            mimeType = (CMSUrlUtil.getInstance().isPageAsset(uri, host, Long.parseLong(selectedLangId))) ? MediaType.TEXT_HTML : MediaType.APPLICATION_OCTET_STREAM;
        }
        
        
		
		
		return Tuple.of(file,mimeType);
	}

	/**
	 * Returns an error to the user in order to indicate that something went
	 * wrong when displaying the page.
	 *
	 * @param request
	 *            - The HTTP Request object.
	 * @param response
	 *            - The HTTP Response object.
	 * @param errorPage
	 *            - The path inside the dotCMS folder structure to the JSP that
	 *            contains the error description.
	 * @param uri
	 *            - The URI that cannot be served back to the portlet.
	 * @param errorCode
	 *            - The HTTP error code.
	 * @throws IOException
	 *             An error occurred when generating the error response.
	 */
	private void sendError(final ServletRequest request, final ServletResponse response, final String errorPage, String uri,
			final int errorCode) throws IOException {
		try {
			request.setAttribute("uri", uri);
			request.getRequestDispatcher(errorPage).forward(request, response);
		} catch (ServletException | IOException e) {
			Logger.error(TimeMachineFilter.class, e.getMessage(), e);
			HttpServletResponse resp = (HttpServletResponse) response;
			resp.sendError(errorCode);
		}
	}

	/**
	 * Takes the contents of the file in the bundle and serves them back to the
	 * user in the Time Machine portlet. Keep in mind that different browsers
	 * require specific MIME types to render a page instead of sending it for
	 * download. This might also change in time.
	 *
	 * @param file
	 *            - The {@link File} with the contents to display.
	 * @param request
	 *            - The {@link ServletRequest} object.
	 * @param response
	 *            - The HTTP Response object.
	 */
	private void sendFile(final Tuple2<File,String> file, ServletRequest request, final ServletResponse response) {
		final HttpServletResponse resp = (HttpServletResponse) response;


		resp.setContentType(file._2());
		resp.setContentLength((int) file._1().length());
		try (InputStream fis = Files.newInputStream(file._1().toPath())) {
			IOUtils.copy(fis, resp.getOutputStream());
		} catch (FileNotFoundException e) {
			Logger.error(this, "Time Machine : File [" + file._1().getAbsolutePath() + "] cannot be found.", e);
		} catch (IOException e) {
			Logger.error(this, "Time Machine : File [" + file._1().getAbsolutePath() + "] cannot be read.", e);
		}
	}

}

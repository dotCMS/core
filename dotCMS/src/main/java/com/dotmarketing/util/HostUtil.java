package com.dotmarketing.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This utility class provides common-use methods to handle specific information of Sites in
 * dotCMS. The main purpose of this class is to expose routines that don't belong to the Site API
 * per se, but use the data provided by it.
 *
 * @author Christian Barigelli
 * @since Apr 5th, 2012
 */
public class HostUtil {

	public static final String HOST_INDICATOR     = "//";

	public static String hostNameUtil(ActionRequest req, User user) throws DotDataException, DotSecurityException {

		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		HttpSession session = httpReq.getSession();

		String hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);

		Host h = null;
		
		h = APILocator.getHostAPI().find(hostId, user, false);
		

		return h.getTitle()!=null?h.getTitle():"default";
	}

	/**
	 * Try to find the host on the current thread, empty optional if not able to do it.
	 * @param user {@link User}
	 * @return Optional Host
	 */
	public static Optional<Host> tryToFindCurrentHost (final User user) {

		final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();

		return null != request?
			Optional.ofNullable(Try.of(()->WebAPILocator.getHostWebAPI().getCurrentHost(request, user)).getOrNull()):
			Optional.empty();
	}

	/**
	 * Tries to find the current host
	 * @return  Host
	 */
	public static Host findCurrentHost () {

		final Optional<Host> hostOpt = tryToFindCurrentHost(APILocator.systemUser());
		return hostOpt.isPresent()? hostOpt.get():null;
	}

	/**
	 * Tries to recover the host from the path, or the current host (if not possible returns the default one)
	 * @param path {@link String} path (could be relative or full)
	 * @param postHostToken {@link String} is the token that become immediate to the host, for instance if getting the host from a container postHostToken could be "/application/containers"
	 * @return Optional Host
	 */
	public static Optional<Host> getHostFromPathOrCurrentHost (final String path, final String postHostToken) {

		if (UtilMethods.isSet(path)) {
			try {
				final Tuple2<String, Host> pathHostTuple = splitPathHost(path, APILocator.systemUser(), postHostToken, HostUtil::findCurrentHost);
				return Optional.ofNullable(pathHostTuple._2);
			} catch (DotSecurityException | DotDataException e) {
				// quiet
			}
		}

		return Optional.empty();
	}

	/**
	 * Based on a path tries to split the host and the relative path. If the path is already relative, gets the host from the resourceHost or gets the default one
	 * @param inputPath {@link String} relative or absolute path
	 * @param user {@link User} user
	 * @param resourceHost {@link Supplier}
	 * @param posHostToken {@link String} is the token that become immediate to the host, for instance if getting the host from a container postHostToken could be "/application/containers"
	 * @return Tuple2 path and host
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public static Tuple2<String, Host> splitPathHost(final String inputPath, final User user, final String posHostToken,
													  final Supplier<Host> resourceHost) throws DotSecurityException, DotDataException {

		final HostAPI hostAPI        = APILocator.getHostAPI();
		final int hostIndicatorIndex = inputPath.indexOf(HOST_INDICATOR);
		final boolean hasHost        = hostIndicatorIndex != -1;
		final int     hostIndicatorLength = HOST_INDICATOR.length();
		final int applicationContainerFolderStartsIndex = hasHost?
				inputPath.substring(hostIndicatorLength).indexOf(posHostToken)+hostIndicatorLength:inputPath.indexOf(posHostToken);
		final boolean hasPos         = applicationContainerFolderStartsIndex != -1;
		final String hostName        = hasHost && hasPos?
				inputPath.substring(hostIndicatorIndex+2, applicationContainerFolderStartsIndex):null;
		final String path            = hasHost && hasPos?inputPath.substring(applicationContainerFolderStartsIndex):inputPath;
		final Host host 	         = hasHost?hostAPI.findByName(hostName, user, false):resourceHost.get();

		return Tuple.of(path, null == host? hostAPI.findDefaultHost(user, false): host);
	}

	/**
	 * Based on a path tries to split the host and the relative path. If the path is already relative, gets the host from the resourceHost or gets the default one
	 * @param inputPath {@link String} relative or absolute path
	 * @param user {@link User} user
	 * @param posHostToken {@link String} is the token that become immediate to the host, for instance if getting the host from a container postHostToken could be "/application/containers"
	 * @return Tuple2 path and host
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	public static Tuple2<String, Host> splitPathHost(final String inputPath, final User user, final String posHostToken) throws DotSecurityException, DotDataException {

		final HostAPI hostAPI        = APILocator.getHostAPI();
		final int hostIndicatorIndex = inputPath.indexOf(HOST_INDICATOR);
		final boolean hasHost        = hostIndicatorIndex != -1;
		final int     hostIndicatorLength = HOST_INDICATOR.length();
		final int applicationContainerFolderStartsIndex = hasHost?
				inputPath.substring(hostIndicatorLength).indexOf(posHostToken)+hostIndicatorLength:inputPath.indexOf(posHostToken);
		final boolean hasPos         = applicationContainerFolderStartsIndex != -1;
		final String hostName        = hasHost && hasPos?
				inputPath.substring(hostIndicatorIndex+2, applicationContainerFolderStartsIndex):null;
		final String path            = hasHost && hasPos?inputPath.substring(applicationContainerFolderStartsIndex):inputPath;
		final Host host 	         = hasHost?hostAPI.findByName(hostName, user, false):null;

		return Tuple.of(path, host);
	}

	/**
	 * Switch a site based on the id on session
	 * @param req {@link HttpServletRequest}
	 * @param hostId {@link String}
	 */
	public static void switchSite(final HttpServletRequest req, final String hostId) {
		final HttpSession session = req.getSession();

		session.removeAttribute(WebKeys.CMS_SELECTED_HOST_ID); // we do this in order to get a properly behaviour of the SwichSiteListener
		session.setAttribute(WebKeys.CMS_SELECTED_HOST_ID, hostId);
		session.removeAttribute(WebKeys.CONTENTLET_LAST_SEARCH);
	}

	/**
	 * Switch a site based on the host on session
	 * @param req {@link HttpServletRequest}
	 * @param host {@link HttpServletRequest}
	 */
	public static void switchSite(final HttpServletRequest req, final Host host) {

		switchSite(req, host.getIdentifier());

		final HttpSession session = req.getSession();
		session.removeAttribute(WebKeys.CURRENT_HOST);
		session.setAttribute(WebKeys.CURRENT_HOST, host);
	}

	/**
	 * Takes a list of Site IDs and/or Site Keys, and resolves them to a list of Site IDs only.
	 * This allows developers/users to pass down a Site Key when they don't have access to its ID,
	 * or simply don't want to use it.
	 *
	 * @param sites The list of Site IDs and/or Site Keys to resolve.
	 *
	 * @return A list of Site IDs only.
	 */
	public static List<String> resolveSiteIds(final List<String> sites, final User user,
											  final boolean respectFrontendRoles) {
		if (UtilMethods.isNotSet(sites)) {
			return List.of();
		}
		final Set<String> uniqueIds = new HashSet<>(sites);
		return uniqueIds.stream().map(siteIdOrKey -> {

			final Optional<Host> siteOpt =
					Try.of(() -> APILocator.getHostAPI().findByIdOrKey(siteIdOrKey, user,
							respectFrontendRoles)).getOrElse(Optional.empty());
			return siteOpt.map(Host::getIdentifier).orElse(siteIdOrKey);

		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

}

package com.dotmarketing.portlets.folders.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import java.util.function.Supplier;

public class PathUtil {

    private static class SingletonHolder {
        private static final PathUtil INSTANCE = new PathUtil();
    }
    /**
     * Get the instance.
     * @return PathUtil
     */
    public static PathUtil getInstance() {

        return PathUtil.SingletonHolder.INSTANCE;
    } // getInstance.

    private static final String HOST_INDICATOR  = "//";
    private static final String PATH_INDICATOR  = "/";
    private final HostAPI hostAPI        = APILocator.getHostAPI();

    /**
     * Get the Host and Path from the path parameter, if the path does not have host (if it is relative)
     * calls the resourceHost otherwise gets the default host
     * @param pathParameter {@link String}
     * @param user          {@link User}
     * @param resourceHost  {@link Supplier}
     * @return Tuple2 (Path and Host)
     * @throws DotSecurityException
     * @throws DotDataException
     */
    public Tuple2<String, Host> getHostAndPath(final String pathParameter, final User user,
                                                      final Supplier<Host> resourceHost) throws DotSecurityException, DotDataException {

        final int hostIndicatorIndex = pathParameter.indexOf(HOST_INDICATOR);
        Host   host = null;
        String path = pathParameter;
        if (-1 != hostIndicatorIndex) {

            final int finalHostIndicatorIndex =
                    pathParameter.indexOf(PATH_INDICATOR, hostIndicatorIndex+2);
            final String hostname = pathParameter.substring(hostIndicatorIndex+2, finalHostIndicatorIndex);
            path = pathParameter.substring(finalHostIndicatorIndex);
            host = hostAPI.findByName(hostname, user, false);
        } else {

            host = resourceHost.get();
        }

        return Tuple.of(path, null == host? hostAPI.findDefaultHost(user, false): host);
    }
}

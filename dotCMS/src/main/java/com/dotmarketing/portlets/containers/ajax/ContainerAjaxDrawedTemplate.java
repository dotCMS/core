package com.dotmarketing.portlets.containers.ajax;

import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.ajax.util.ContainerAjaxUtil;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Class used by new dwr interface created for Design Template.
 *
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *         <p/>
 *         May 3, 2012 - 11:26:32 AM
 */
public class ContainerAjaxDrawedTemplate extends ContainerAjax {

    /**
     * This method don't put into the container's map the containers with some tag like: <html>, </html>, <body>, </body>, <head>, </head>
     *
     * @param query
     * @param queryOptions
     * @param start
     * @param count
     * @param sort
     * @return
     * @throws PortalException
     * @throws SystemException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public Map<String, Object> fetchContainersDesignTemplate ( final Map<String, String> query,
                                                               final Map<String, String> queryOptions,
                                                               int start, int count,
                                                               final List<String> sort,
                                                               final List<String> exclude ) throws PortalException, SystemException, DotDataException, DotSecurityException {

        final HttpServletRequest request   = WebContextFactory.get().getHttpServletRequest();
        final User user                    = userWebAPI.getLoggedInUser(request);
        final boolean respectFrontendRoles = userWebAPI.isLoggedToFrontend( request );
        final String baseHostId            = (String) request.getSession().getAttribute( com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID );

        List<Container> fullListContainers;
        try {
            if ( UtilMethods.isSet( query.get( "hostId" ) ) ) {
                final Host host    = hostAPI.find( query.get( "hostId" ), user, respectFrontendRoles );
                fullListContainers = containerAPI.findContainersUnder( host );
            } else {
                final Host host    = hostAPI.find(baseHostId, user, respectFrontendRoles);
                fullListContainers = containerAPI.findAllContainers( host, user, respectFrontendRoles );
            }
        } catch ( DotDataException e ) {
            Logger.error( this, e.getMessage(), e );
            throw new DotDataException( e.getMessage(), e );
        }


        Collections.sort( fullListContainers, new ContainerComparator( baseHostId ) );
        final Map<String, Object> results    = new HashMap<>();
        final List<Map<String, Object>> list = new LinkedList<>();

        for (final Container container : fullListContainers ) {
          if(container != null && !container.isArchived()) {

            final Map<String, Object> contMap = container.getMap();
            if ( passFilter( contMap, query ) ) {

                final Host parentHost = containerAPI.getParentHost( container, user, respectFrontendRoles );
                if ( parentHost != null ) {
                    contMap.put( "hostName", parentHost.getHostname() );
                    contMap.put( "hostId", parentHost.getIdentifier() );
                    contMap.put( "fullTitle", parentHost.getHostname() + ": " + contMap.get( "title" ) );
                } else {
                    contMap.put( "fullTitle", contMap.get( "title" ) );
                }

                contMap.put("source", container.getSource().toString());
                if (container instanceof FileAssetContainer) {

                    contMap.put("path",
                            FileAssetContainerUtil.getInstance()
                                    .getFullPath(FileAssetContainer.class.cast(container))
                    );
                }

//                StringBuffer containerCode = new StringBuffer( cont.getCode() );
                final List<ContainerStructure> containerStructureList = APILocator.getContainerAPI().getContainerStructures(container);
                boolean checkContainerCode = false;

                for (final ContainerStructure containerStructure : containerStructureList) {
                	if(ContainerAjaxUtil.checkContainerCode( containerStructure.getCode())) {
                		checkContainerCode = true;
                	} else {
                		checkContainerCode = false;
                		break;
                	}
                }

                /**
                 * adding the max_contentlets information to the result map because we must filter for the containers that accept
                 * contents and doesn't stay into the template for more than one time.
                 */
                contMap.put( "maxContentlets", container.getMaxContentlets() );
                if ( !checkContainerCode  ) {

                    //Now we need to verify if an exclude list was sent from the client
                    Boolean excludeContainer = false;
                    if ( exclude != null && !exclude.isEmpty() ) {

                        for ( String identifierToExclude : exclude ) {
                            if ( identifierToExclude.equals( contMap.get( "identifier" ) ) ) {
                                excludeContainer = true;
                                break;
                            }
                        }
                    }

                    if ( !excludeContainer ) {
                        list.add( contMap );
                    }

                }
            }
          }
        }

        if ( start >= list.size() ) start = list.size() - 1;
        if ( start < 0 ) start = 0;
        if ( start + count >= list.size() ) count = list.size() - start;
        List<Map<String, Object>> containers = list.subList( start, start + count );

        results.put( "totalResults", list.size() );
        results.put( "list", containers );

        return results;
    }

}
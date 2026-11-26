package com.dotmarketing.portlets.workflows.ajax;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.InvalidLicenseException;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.commons.beanutils.BeanUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Deprecated
public class WfRoleStoreAjax extends WfBaseAction {

    private final WorkflowAjaxReadAccess readAccess;

    /**
     * Creates the action as the ajax director does, with the default access check.
     */
    public WfRoleStoreAjax() {
        this(new WorkflowAjaxReadAccess());
    }

    /**
     * Creates the action with the given access check.
     *
     * @param readAccess decides who may read the role and assignee lists
     */
    @VisibleForTesting
    WfRoleStoreAjax(final WorkflowAjaxReadAccess readAccess) {
        this.readAccess = readAccess;
    }

    /**
     * Writes the roles matching {@code searchName}, or the single role named by {@code roleId}, as
     * the JSON store the legacy role pickers read. Only back-end users who can open one of the
     * portlets {@code workflow-schemes}, {@code workflow}, {@code configuration} or
     * {@code publishing-queue} may read it; anyone else gets a 403.
     *
     * @param request  the request with the optional {@code searchName}, {@code roleId},
     *                 {@code start}, {@code count} and include flags
     * @param response the response the JSON is written to
     * @throws ServletException if the request cannot be served
     * @throws IOException      if the response cannot be written
     */
    public void action ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        if (!readAccess.canReadRoles(getUser())) {
            sendForbidden(response, "role list");
            return;
        }

        String searchName = request.getParameter( "searchName" );

        if ( searchName == null ) searchName = "";
        String roleId = request.getParameter( "roleId" );
        RoleAPI rapi = APILocator.getRoleAPI();

        int start = 0;
        int count = 20;
        try {
            start = Integer.parseInt( request.getParameter( "start" ) );
        } catch ( Exception e ) {

        }
        try {
            count = Integer.parseInt( request.getParameter( "count" ) );
        } catch ( Exception e ) {

        }

        final boolean includeFake = UtilMethods.isSet(request.getParameter( "includeFake" ))&&request.getParameter( "includeFake" ).equals("true");
        final boolean includeWorkflowRoles = UtilMethods.isSet(request.getParameter( "includeWfRoles" ))&&request.getParameter( "includeWfRoles" ).equals("true");
        final boolean includeLabelAll = UtilMethods.isSet(request.getParameter( "includeLabelAll" ))&&request.getParameter( "includeLabelAll" ).equals("true");

        try {
            final Role cmsAnonOrig = APILocator.getRoleAPI().loadCMSAnonymousRole();
            
            Role cmsAnon  = new Role();
            BeanUtils.copyProperties(cmsAnon, cmsAnonOrig);
            
            String cmsAnonName = LanguageUtil.get( getUser(), "current-user" );
            cmsAnon.setName( cmsAnonName );
            boolean addSystemUser = false;
            if ( searchName.length() > 0 && cmsAnonName.startsWith( searchName ) ) {
                addSystemUser = true;
            }

            List<Role> roleList = new ArrayList<>();
            if ( UtilMethods.isSet( roleId ) ) {
                try {
                    Role r = rapi.loadRoleById( roleId );
                    if ( r != null ) {
                        if ( r.getId().equals( cmsAnon.getId() ) )
                            roleList.add( cmsAnon );
                        else
                            roleList.add( r );
                        response.getWriter().write( rolesToJson( roleList, includeFake, includeLabelAll ) );
                        return;
                    }
                } catch ( Exception e ) {

                }

            }

            while ( roleList.size() < count ) {
                List<Role> roles = rapi.findRolesByFilterLeftWildcard( searchName, start, count );
                if ( roles.size() == 0 ) {
                    break;
                }
                for ( Role role : roles ) {
                    if ( role.isUser() ) {
                        try {
                            APILocator.getUserAPI().loadUserById( role.getRoleKey(), APILocator.getUserAPI().getSystemUser(), false );
                        } catch ( Exception e ) {
                            //Logger.error(WfRoleStoreAjax.class,e.getMessage(),e);
                            continue;
                        }
                    }
                    if ( role.getId().equals( cmsAnon.getId() ) ) {
                        role = cmsAnon;
                        addSystemUser = false;
                    }
                    if ( role.isSystem() && !role.isUser() && !role.getId().equals( cmsAnon.getId() ) && !role.getId().equals( APILocator.getRoleAPI().loadCMSAdminRole().getId() ) ) {
                        continue;
                    }
                    if ( role.getName().equals( searchName ) ) {
                        roleList.add( 0, role );
                    } else {
                        roleList.add( role );
                    }
                }
                start = start + count;
            }
            if ( addSystemUser ) {
                cmsAnon.setName( cmsAnonName );
                roleList.add( 0, cmsAnon );
            }

            if(includeWorkflowRoles){
                roleList.addAll( APILocator.getRoleAPI().findWorkflowSpecialRoles());
            }
            //x = x.replaceAll("identifier", "x");
            response.setContentType("application/json");
            response.getWriter().write( rolesToJson( roleList, includeFake, includeLabelAll ) );

        } catch ( Exception e ) {
            Logger.error( WfRoleStoreAjax.class, e.getMessage(), e );
        }

    }

    /**
     * Writes the roles and users the workflow action in {@code actionId} can be assigned to, as the
     * JSON store the assignee picker reads. An action the user may not list, and an id that matches
     * no action, both get the same 403.
     *
     * @param request  the request with {@code actionId} and the optional {@code name} filter
     * @param response the response the JSON is written to
     * @throws ServletException if the request cannot be served
     * @throws IOException      if the response cannot be written
     */
    public void assignable ( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

        final WorkflowAction action = findAction( request.getParameter( "actionId" ) );
        if (!readAccess.canListAssignees(getUser(), action)) {
            sendForbidden(response, "assignees of a workflow action");
            return;
        }

        String name = request.getParameter( "name" );

        boolean includeFake = UtilMethods.isSet(request.getParameter( "includeFake" ))&&request.getParameter( "includeFake" ).equals("true");

        try {
            Role role = APILocator.getRoleAPI().loadRoleById( action.getNextAssign() );
            List<Role> roleList = new ArrayList<>();
            List<User> userList = new ArrayList<>();
            if ( !role.isUser() ) {
                if ( action.isRoleHierarchyForAssign() ) {
                    userList = APILocator.getRoleAPI().findUsersForRole( role, true );
                    roleList.addAll( APILocator.getRoleAPI().findRoleHierarchy( role ) );
                } else {
                    userList = APILocator.getRoleAPI().findUsersForRole( role, false );
                    roleList.add( role );
                }
            } else {
                userList.add( APILocator.getUserAPI().loadUserById( role.getRoleKey(), APILocator.getUserAPI().getSystemUser(), false ) );
            }

            for ( User user : userList ) {
                Role r = APILocator.getRoleAPI().getUserRole( user );
                if ( r != null && UtilMethods.isSet( r.getId() ) ) {
                    roleList.add( r );
                }
            }
            if ( name != null ) {

                name = name.toLowerCase().replaceAll( "\\*", "" );
                if ( UtilMethods.isSet( name ) ) {
                    List<Role> newRoleList = new ArrayList<>();
                    for ( Role r : roleList ) {
                        if ( r.getName().toLowerCase().startsWith( name ) ) {
                            newRoleList.add( r );
                        }
                    }
                    roleList = newRoleList;
                }

            }

            response.setContentType("application/json");
            response.getWriter().write( rolesToJson( roleList, includeFake, false ) );
        } catch ( Exception e ) {
            Logger.error( WfRoleStoreAjax.class, e.getMessage(), e );
        }


    }

    /**
     * Loads the workflow action with the given id.
     *
     * @param actionId the action id from the request
     * @return the {@link WorkflowAction}, or {@code null} when no id is given, the id matches no
     *         action, the lookup fails or the action's scheme needs a license this instance lacks
     */
    private WorkflowAction findAction(final String actionId) {
        if (!UtilMethods.isSet(actionId)) {
            return null;
        }
        try {
            return APILocator.getWorkflowAPI().findAction(actionId, getUser());
        } catch (final DotDataException | DotSecurityException | InvalidLicenseException e) {
            Logger.warn(WfRoleStoreAjax.class, String.format(
                    "Unable to load a workflow action for the assignee list of user ID '%s'",
                    null != getUser() ? getUser().getUserId() : null));
            Logger.debug(WfRoleStoreAjax.class, "Workflow action lookup failed", e);
            return null;
        }
    }

    private String rolesToJson ( List<Role> roles, boolean includeFake, boolean includeLabelAll ) throws IOException, DotDataException, LanguageException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        Map<String, Object> m = new LinkedHashMap<>();

        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> map = null;

        if(includeFake) {
        	map = new HashMap<>();
	        map.put( "name", StringPool.BLANK );
	        map.put( "id", 0 );
	        list.add( map );
        }

        if(includeLabelAll) {
            map = new HashMap<>();
            map.put( "name", "All" );
            map.put( "id", StringPool.BLANK);
            list.add( map );
        }

        User defaultUser = APILocator.getUserAPI().getDefaultUser();
        Role defaultUserRole = null;
        if ( defaultUser != null ) {
            defaultUserRole = APILocator.getRoleAPI().getUserRole( defaultUser );
        }

        for ( Role role : roles ) {

            map = new HashMap<>();

            //Exclude default user
            if ( defaultUserRole != null && role.getId().equals( defaultUserRole.getId() ) ) {
                continue;
            }

            //We just want to add roles that can have permissions assigned to them
            if ( !role.isEditPermissions() ) {
                continue;
            }

            //We need to exclude also the system anonymous user
            if ( role.getName().contains( "anonymous user" ) ) {
                continue;
            }

            map.put( "name",  role.getName() + ((role.isUser()) ? " (" + LanguageUtil.get( PublicCompanyFactory.getDefaultCompany(), "User" ) + ")" : ""));
            map.put( "id", role.getId() );

            list.add( map );
        }

        m.put( "identifier", "id" );
        m.put( "label", "name" );
        m.put( "items", list );

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString( m );
    }

    /**
     * Security check demanded by Sonar
     * We register all the allowed methods down here
     *
     * @return allowed method names
     */
    @Override
    protected Set<String> getAllowedCommands() {
        return Set.of( "rolesToJson", "assignable", "action" );
    }

}
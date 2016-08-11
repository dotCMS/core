package com.dotcms.rest.api.v1.user;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.UserFirstNameException;
import com.dotmarketing.exception.UserLastNameException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.ejb.UserLocalManagerFactory;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;
@Path("/v1/users")
public class UserResource {

    private final WebResource webResource;
    private final UserWebAPI userWebAPI;
    private final UserAPI userAPI;
    private final PermissionAPI permissionAPI;
    private final UserProxyAPI userProxyAPI;
    private final UserHelper userHelper;
    private final ErrorResponseHelper errorHelper;
    private final UserLocalManager userLocalManager;

    @SuppressWarnings("unused")
    public UserResource() {
        this(new WebResource(new ApiProvider()),
                WebAPILocator.getUserWebAPI(),
                APILocator.getUserAPI(),
                APILocator.getPermissionAPI(),
                APILocator.getUserProxyAPI(),
                UserHelper.INSTANCE,
                ErrorResponseHelper.INSTANCE,
                UserLocalManagerFactory.getManager()
                );
    }

    @VisibleForTesting
    protected UserResource(final WebResource webResource,
                           final UserWebAPI userWebAPI,
                           final UserAPI userAPI,
                           final PermissionAPI permissionAPI,
                           final UserProxyAPI userProxyAPI,
                           final UserHelper userHelper,
                           final ErrorResponseHelper errorHelper,
                           final UserLocalManager userLocalManager) {

        this.webResource      = webResource;
        this.userWebAPI       = userWebAPI;
        this.userAPI          = userAPI;
        this.permissionAPI    = permissionAPI;
        this.userProxyAPI     = userProxyAPI;
        this.userHelper       = userHelper;
        this.errorHelper      = errorHelper;
        this.userLocalManager = userLocalManager;
    }

    @GET
    @JSONP
    @Path("/current")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public RestUser self(@Context HttpServletRequest request) {

        final User user = webResource.init(true, request, true).getUser();
        final RestUser.Builder currentUser = new RestUser.Builder();

        if(user != null) {
            try {

                final Role role = APILocator.getRoleAPI().getUserRole(user);
                currentUser.userId(user.getUserId())
                    .givenName(user.getFirstName())
                    .email(user.getEmailAddress())
                    .surname(user.getLastName())
                    .roleId(role.getId());
            } catch (DotDataException e) {

                Logger.error(this, e.getMessage(), e);
                throw new BadRequestException("Could not provide current user.");
            }
        }

        return currentUser.build();
    }

    @PUT
    @JSONP
    @Path("/current")
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response update(@Context final HttpServletRequest request,
                                 final UpdateUserForm updateUserForm) throws Exception {

        final User modUser = webResource.init(true, request, true).getUser();
        final HttpSession session = request.getSession();
        Response response = null;
        final String date = DateUtil.getCurrentDate();
        final User userToSave;
        boolean reAuthenticationRequired = false;
        boolean validatePassword = false;
        Locale locale = LocaleUtil.getLocale(request);
        Map<String, Object> userMap = Collections.EMPTY_MAP;

        this.userHelper.log("Updating User", "Date: " + date + "; "
                + "User:" + modUser.getUserId());

        try {

            if (null == locale) {

                locale = modUser.getLocale();
            }

            userToSave = (User)this.userAPI.loadUserById
                    (updateUserForm.getUserId(), this.userAPI.getSystemUser(), false).clone();
            userToSave.setModified(false);
            userToSave.setFirstName(updateUserForm.getGivenName());
            userToSave.setLastName(updateUserForm.getSurname());

            if (null != updateUserForm.getEmail()) {

                userToSave.setEmailAddress(updateUserForm.getEmail());
            }

            if (null != updateUserForm.getPassword()) {
                // Password has changed, so it has to be validated
                userToSave.setPassword(updateUserForm.getPassword());
                // And re-authentication might be required
                validatePassword = reAuthenticationRequired = true;
            }

            if (userToSave.getUserId().equalsIgnoreCase(modUser.getUserId())) {

                this.userAPI.save(userToSave, this.userAPI.getSystemUser(), validatePassword, false);
                // if the user logged is the same of the user to save, we need to set the new user changes to the session.
                session.setAttribute(com.dotmarketing.util.WebKeys.CMS_USER, userToSave);
            } else if (this.permissionAPI.doesUserHavePermission
                    (this.userProxyAPI.getUserProxy(userToSave, modUser, false),
                        PermissionAPI.PERMISSION_EDIT, modUser, false)) {

                this.userAPI.save(userToSave, modUser, validatePassword, !userWebAPI.isLoggedToBackend(request));
            } else {

                throw new DotSecurityException(LanguageUtil.get(locale, "User-Doesnot-Have-Permission"));
            }

            this.userHelper.log("User Updated", "Date: " + date + "; "+ "User:" + modUser.getUserId());

            if (!reAuthenticationRequired) { // if re authentication is not required, sent the current changed user

                userMap = userToSave.toMap();
            }

            response = Response.ok(new ResponseEntityView(map("userID", userToSave.getUserId(),
                    "reauthenticate", reAuthenticationRequired, "user", userMap))).build(); // 200
        } catch (UserFirstNameException e) {

            this.userHelper.log("Error Updating User. Invalid First Name", "Date: " + date + ";  "+ "User:" + modUser.getUserId());
            response = this.errorHelper.getErrorResponse(Response.Status.BAD_REQUEST, locale, "User-Info-Save-First-Name-Failed");
        } catch (UserLastNameException e) {

            this.userHelper.log("Error Updating User. Invalid Last Name", "Date: " + date + ";  "+ "User:" + modUser.getUserId());
            response = this.errorHelper.getErrorResponse(Response.Status.BAD_REQUEST, locale, "User-Info-Save-Last-Name-Failed");
        } catch (DotSecurityException  e) {

            this.userHelper.log("Error Updating User", "Date: " + date + ";  "+ "User:" + modUser.getUserId());
            response = this.errorHelper.getErrorResponse(Response.Status.UNAUTHORIZED, locale, "User-Doesnot-Have-Permission");
        } catch (NoSuchUserException  e) {

            this.userHelper.log("Error Updating User", "Date: " + date + ";  "+ "User:" + modUser.getUserId());
            response = this.errorHelper.getErrorResponse(Response.Status.NOT_FOUND, locale, "User-Not-Found");
        } catch (Exception  e) {

            this.userHelper.log("Error Updating User", "Date: " + date + ";  "+ "User:" + modUser.getUserId());
            e.printStackTrace();
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // update.


} // E:O:F:UserResource

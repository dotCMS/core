package com.dotcms.filters.interceptor.jwt;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.beans.DotCMSSubjectBean;
import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotcms.util.security.Encryptor;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyLocalManager;
import com.liferay.portal.ejb.CompanyLocalManagerFactory;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.util.CookieKeys;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * This Interceptor is useful to active the remember me using JWT It is going to
 * look for a cookie and try to get the access token from it.
 *
 * Usually the cookie should runs under HTTPS, but you can avoid HTTPS by
 * setting the {@code JSON_WEB_TOKEN_ALLOW_HTTP} property to true.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 14, 2016
 */
public class JsonWebTokenInterceptor implements WebInterceptor {

    public static final String JSON_WEB_TOKEN_ALLOW_HTTP = "json.web.token.allowhttp";

    private JsonWebTokenService jsonWebTokenService;

    private MarshalUtils marshalUtils;

    private CompanyLocalManager companyLocalManager;

    private Encryptor encryptor;

    private LoginServiceAPI loginService;


	private UserAPI userAPI;

	public JsonWebTokenInterceptor() {

		this(JsonWebTokenFactory.getInstance().getJsonWebTokenService(),
				MarshalFactory.getInstance().getMarshalUtils(),
				CompanyLocalManagerFactory.getManager(),
				EncryptorFactory.getInstance().getEncryptor(),
				APILocator.getLoginServiceAPI(),
				APILocator.getUserAPI()
				);
	}

	@VisibleForTesting
	protected JsonWebTokenInterceptor(final JsonWebTokenService jsonWebTokenService,
								   final MarshalUtils marshalUtils,
								   final CompanyLocalManager companyLocalManager,
								   final Encryptor encryptor,
								   final LoginServiceAPI loginService,
								   final UserAPI userAPI) {

		this.jsonWebTokenService = jsonWebTokenService;
		this.marshalUtils = marshalUtils;
		this.companyLocalManager = companyLocalManager;
		this.encryptor = encryptor;
		this.loginService = loginService;
		this.userAPI = userAPI;
	}

	/**
	 * In case you need a diff implementation of the APILocator.
	 * @param userAPI {@link UserAPI}
     */
	public void setUserAPI(final UserAPI userAPI) {

		this.userAPI = userAPI;
	}

	/**
	 * Sets a specific {@link JsonWebTokenService} implementation by dependency
	 * injection.
	 * 
	 * @param jsonWebTokenService
	 *            - The @link JsonWebTokenService} implementation.
	 */
    public void setJsonWebTokenService(final JsonWebTokenService jsonWebTokenService) {
        this.jsonWebTokenService = jsonWebTokenService;
    }

	/**
	 * Sets a specific {@link MarshalUtils} implementation by dependency
	 * injection.
	 * 
	 * @param marshalUtils
	 *            - The {@link MarshalUtils} implementation.
	 */
    public void setMarshalUtils(final MarshalUtils marshalUtils) {
        this.marshalUtils = marshalUtils;
    }

	/**
	 * Sets the {@link CompanyLocalManager} by dependency injection.
	 * 
	 * @param companyLocalManager
	 *            - The {@link CompanyLocalManager}.
	 */
    public void setCompanyLocalManager(final CompanyLocalManager companyLocalManager) {
        this.companyLocalManager = companyLocalManager;
    }

    /**
     * Sets the {@link Encryptor} implementation by dependency injection.
     * 
     * @param encryptor - The {@link Encryptor} implementation.
     */
    public void setEncryptor(final Encryptor encryptor) {
        this.encryptor = encryptor;
    }

	/**
	 * Sets the {@link LoginServiceAPI} by dependency injection.
	 * 
	 * @param loginService
	 *            - The {@link LoginServiceAPI}.
	 */
    public void setLoginService(final LoginServiceAPI loginService) {
        this.loginService = loginService;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void init() {

    }

    @Override
    public Result intercept(final HttpServletRequest req, final HttpServletResponse res) throws IOException {

		Result result = Result.NEXT;

        if (!this.isLoggedIn(req)) {

            if (Config.getBooleanProperty(JSON_WEB_TOKEN_ALLOW_HTTP, false) || this.isHttpSecure(req)) {

                try {

					result = this.processJwtCookie(res, req);
                } catch (Exception e) {

                    if (Logger.isErrorEnabled(JsonWebTokenInterceptor.class)) {

                        Logger.error(JsonWebTokenInterceptor.class,
                                e.getMessage(), e);
                    }
                }
            }
        }

        return result;
    } // intercept.

	/**
	 * Checks whether the current request belongs to a user that has already
	 * been authenticated.
	 * 
	 * @param req
	 *            - The {@link HttpServletRequest} object.
	 * @return If the request belongs to an authenticated user, returns
	 *         {@code true}. Otherwise, returns {@code false}.
	 */
    protected boolean isLoggedIn (final HttpServletRequest req) {

        return this.loginService.isLoggedIn(req);
    }

	/**
	 * Takes the JWT from the HTTP Request and parses its contents looking for
	 * authentication data.
	 * 
	 * @param response
	 *            - The {@link HttpServletResponse} object.
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 */
    protected Result processJwtCookie(final HttpServletResponse response,
                                    final HttpServletRequest request) {

        final String jwtAccessToken =
                UtilMethods.getCookieValue(
                        HttpServletRequest.class.cast(request).getCookies(),
                        CookieKeys.JWT_ACCESS_TOKEN);
		Result result = Result.NEXT;

        if (null != jwtAccessToken) {

			result =
				this.parseJwtToken(jwtAccessToken, response, request);
        }

		return result;
    } // processJwtCookie.

	/**
	 * Parses the String contents of the JWT into a Java object. If the
	 * expiration date is still valid, then the token's subject is processed to
	 * carry on with the authentication process.
	 * 
	 * @param jwtAccessToken
	 *            - The JWT as a String.
	 * @param response
	 *            - The {@link HttpServletResponse} object.
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 */
    protected Result parseJwtToken(final String jwtAccessToken,
                                 final HttpServletResponse response,
                                 final HttpServletRequest request) {

        final JWTBean jwtBean =
                this.jsonWebTokenService.parseToken(jwtAccessToken);
		Result result = Result.NEXT;

        if (null != jwtBean) {

            if (JsonWebTokenUtils.isJsonWebTokenValid (jwtBean)) {
            	// todo: handle exceptions here
                result =
					this.processSubject(jwtBean, response, request);
            }
        }

		return result;
    } // parseJwtToken.


	/**
	 * Takes the subject from the JWT to carry on with the authentication
	 * process.
	 * 
	 * @param jwtBean
	 *            - The JWT data.
	 * @param response
	 *            - The {@link HttpServletResponse} object.
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 */
    protected Result processSubject(final JWTBean jwtBean,
                                  final HttpServletResponse response,
                                  final HttpServletRequest request) {


        final DotCMSSubjectBean dotCMSSubjectBean =
                this.marshalUtils.unmarshal(jwtBean.getSubject(), DotCMSSubjectBean.class);
		Result result = Result.NEXT;

        if (null != dotCMSSubjectBean) {

            result =
				this.performAuthentication (dotCMSSubjectBean, response, request);
        }

		return result;
    }

	/**
	 * Performs the user authentication based on the data from the JWT.
	 * 
	 * @param subjectBean
	 *            - The JWT subject data.
	 * @param response
	 *            - The {@link HttpServletResponse} object.
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 */
    protected Result performAuthentication(final DotCMSSubjectBean subjectBean,
                                         final HttpServletResponse response,
                                         final HttpServletRequest request) {

        final Company company;
        final String userId;
		Result result = Result.NEXT;

        try {

            company = this.getCompany(subjectBean.getCompanyId());

            userId = this.encryptor.decrypt(company.getKeyObj(),
                    subjectBean.getUserId()); // encrypt the user id.

            // todo: if there is a custom implementation to handle the authentication use it
			result = this.performDefaultAuthentication
                    (company, userId, subjectBean.getLastModified(),
                            response, request);
        } catch (Exception e) {

            if (Logger.isErrorEnabled(JsonWebTokenInterceptor.class)) {

                Logger.error(JsonWebTokenInterceptor.class,
                        e.getMessage(), e);
            }
        }

		return result;
    }

	/**
	 * Returns the {@link Company} object based on its ID.
	 * 
	 * @param companyId
	 *            - The ID of the company
	 * @return The {@link Company} object.
	 * @throws SystemException
	 *             A system error occurred.
	 * @throws PortalException
	 *             The specified company does not exist.
	 */
    protected Company getCompany (final String companyId) throws SystemException, PortalException {

        return this.companyLocalManager.getCompany(companyId);
    }

	/**
	 * Performs the default system authentication based on the information
	 * retrieved from the JWT.
	 * 
	 * @param company
	 *            - The company that the user belongs to.
	 * @param userId
	 *            - The user ID.
	 * @param lastModified
	 *            - The last modification date of the user.
	 * @param response
	 *            - The {@link HttpServletResponse} object.
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @throws DotSecurityException
	 * @throws DotDataException
	 *             An error occurred when retrieving the user's data.
	 */
    protected Result performDefaultAuthentication(final Company company,
                                                final String userId,
                                                final Date lastModified,
                                                final HttpServletResponse response,
                                                final HttpServletRequest request) throws DotSecurityException, DotDataException {

        final User user = this.userAPI.loadUserById(userId);
		Result result = Result.NEXT;

        if (null != user) {

            // The user hasn't change since the creation of the JWT
            if (0 == user.getModificationDate().compareTo(lastModified)) {

                if (this.loginService.
                        doCookieLogin(this.encryptor.encryptString(userId), request, response)) {

					// if this login was successfully, do not need to do any other.
					result = Result.SKIP;
				}
            }
        }

		return result;
    }

	/**
	 * Determines if the request is running under HTTPS protocol.
	 * 
	 * @param req
	 *            - The {@link ServletRequest} object.
	 * @return boolean If dotCMS is running on HTTPS, returns {@code true}.
	 *         Otherwise, returns {@code false}.
	 */
    protected boolean isHttpSecure(final ServletRequest req) {

        return req.isSecure();
    } // isHttpSecure.

} // E:O:F:JsonWebTokenInterceptor.

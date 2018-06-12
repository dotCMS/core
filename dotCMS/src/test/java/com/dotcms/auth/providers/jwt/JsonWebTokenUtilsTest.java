package com.dotcms.auth.providers.jwt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.factories.KeyFactoryUtils;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.liferay.portal.model.User;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Jonathan Gamba 6/5/18
 */
@PowerMockIgnore({"javax.management.*", "javax.crypto.*"})
@PrepareForTest({ClusterFactory.class, JsonWebTokenFactory.class})
@RunWith(PowerMockRunner.class)
public class JsonWebTokenUtilsTest extends UnitTestBase {

    /**
     * Testing the generateToken JsonWebTokenUtils.getUser
     */
    @Test
    public void get_user_in_token()
            throws DotSecurityException, DotDataException, ParseException {

        final String jwtId = "jwt1";
        final String userId = "jsanca";
        final String clusterId = "CLUSTER-123";
        final String tempPath = "/tmp";
        final String assetsPath = "/tmp/assets";

        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);
        final Date date = dateFormat.parse("04/10/1981");

        //Mocking data
        PowerMockito.mockStatic(ClusterFactory.class);
        PowerMockito.when(ClusterFactory.getClusterId()).thenReturn(clusterId);
        Config.CONTEXT = mock(ServletContext.class);
        Config.CONTEXT_PATH = tempPath;
        final FileAssetAPI fileAssetAPI = mock(FileAssetAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        KeyFactoryUtils.getInstance(fileAssetAPI);
        when(fileAssetAPI.getRealAssetsRootPath()).thenReturn(assetsPath);

        User user = new User();
        user.setUserId(userId);
        user.setModificationDate(date);
        when(userAPI.loadUserById(userId)).thenReturn(user);

        //Generate the token service
        final JsonWebTokenService jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();
        assertNotNull(jsonWebTokenService);

        //Generate a new token
        String jsonWebToken = jsonWebTokenService.generateToken(new JWTBean(jwtId,
                userId, date, DateUtil.daysToMillis(2)
        ));
        System.out.println(jsonWebToken);
        assertNotNull(jsonWebToken);
        assertTrue(jsonWebToken.startsWith("eyJhbGciOiJIUzI1NiJ9"));

        //Parse the generated token
        final JWTBean jwtBean = jsonWebTokenService.parseToken(jsonWebToken);
        assertNotNull(jwtBean);
        assertEquals(jwtBean.getId(), jwtId);
        assertEquals(jwtBean.getIssuer(), clusterId);
        final String subject = jwtBean.getSubject();
        assertNotNull(subject);
        assertEquals(subject, userId);

        //Get the user
        JsonWebTokenUtils jsonWebTokenUtils = new JsonWebTokenUtils(jsonWebTokenService, userAPI);
        User userInToken = jsonWebTokenUtils.getUser(jsonWebToken);
        assertNotNull(userInToken);
        assertEquals(user, userInToken);
    }

    /**
     * Testing the generateToken JsonWebTokenUtils.getUser with a modification after the creation of
     * the token, no user should return as the system notice a modification on the user was made
     * after the creation of the token.
     */
    @Test
    public void get_user_in_token_modified()
            throws DotSecurityException, DotDataException, ParseException {

        final String jwtId = "jwt1";
        final String userId = "jsanca";
        final String clusterId = "CLUSTER-123";
        final String tempPath = "/tmp";
        final String assetsPath = "/tmp/assets";

        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);
        final Date date = dateFormat.parse("04/10/1981");

        //Mocking data
        PowerMockito.mockStatic(ClusterFactory.class);
        PowerMockito.when(ClusterFactory.getClusterId()).thenReturn(clusterId);
        Config.CONTEXT = mock(ServletContext.class);
        Config.CONTEXT_PATH = tempPath;
        final FileAssetAPI fileAssetAPI = mock(FileAssetAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);
        KeyFactoryUtils.getInstance(fileAssetAPI);
        when(fileAssetAPI.getRealAssetsRootPath()).thenReturn(assetsPath);

        User user = new User();
        user.setUserId(userId);
        user.setModificationDate(new Date());
        when(userAPI.loadUserById(userId)).thenReturn(user);

        //Generate the token service
        final JsonWebTokenService jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();
        assertNotNull(jsonWebTokenService);

        //Generate a new token
        String jsonWebToken = jsonWebTokenService.generateToken(new JWTBean(jwtId,
                userId, date, DateUtil.daysToMillis(2)
        ));
        System.out.println(jsonWebToken);
        assertNotNull(jsonWebToken);
        assertTrue(jsonWebToken.startsWith("eyJhbGciOiJIUzI1NiJ9"));

        //Parse the generated token
        final JWTBean jwtBean = jsonWebTokenService.parseToken(jsonWebToken);
        assertNotNull(jwtBean);
        assertEquals(jwtBean.getId(), jwtId);
        assertEquals(jwtBean.getIssuer(), clusterId);
        final String subject = jwtBean.getSubject();
        assertNotNull(subject);
        assertEquals(subject, userId);

        //Get the user
        JsonWebTokenUtils jsonWebTokenUtils = new JsonWebTokenUtils(jsonWebTokenService, userAPI);
        User userInToken = jsonWebTokenUtils.getUser(jsonWebToken);
        assertNull(userInToken);
    }

}
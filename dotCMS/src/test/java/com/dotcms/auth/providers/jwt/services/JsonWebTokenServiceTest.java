package com.dotcms.auth.providers.jwt.services;

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
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import io.jsonwebtoken.IncorrectClaimException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * JsonWebTokenService Test
 *
 * @author jsanca
 */
@PowerMockIgnore({"javax.management.*", "javax.crypto.*"})
@PrepareForTest({ClusterFactory.class, JsonWebTokenFactory.class})
@RunWith(PowerMockRunner.class)
public class JsonWebTokenServiceTest extends UnitTestBase {

    private String clusterId;
    private JsonWebTokenService jsonWebTokenService;

    /**
     * Testing the generateToken JsonWebTokenServiceTest
     */
    @Test
    public void generateTokenTest() {

        final String jwtId = "jwt1";
        final String userId = "jsanca";
        clusterId = "CLUSTER-123";
        final String tempPath = "/tmp";
        final String assetsPath = "/tmp/assets";

        //Mocking data
        PowerMockito.mockStatic(ClusterFactory.class);
        PowerMockito.when(ClusterFactory.getClusterId()).thenReturn(clusterId);
        Config.CONTEXT = mock(ServletContext.class);
        Config.CONTEXT_PATH = tempPath;
        final FileAssetAPI fileAssetAPI = mock(FileAssetAPI.class);
        KeyFactoryUtils.getInstance(fileAssetAPI);
        when(fileAssetAPI.getRealAssetsRootPath()).thenReturn(assetsPath);

        //Generate the token service
        jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();
        assertNotNull(jsonWebTokenService);

        //Generate a new token
        String jsonWebToken = jsonWebTokenService.generateToken(new JWTBean(jwtId,
                userId, new Date(), DateUtil.daysToMillis(2)
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
    }

    /**
     * Testing the generateToken JsonWebTokenServiceTest
     */
    @Test
    public void generateToken_expired_token_Test() throws ParseException {

        final String jwtId = "jwt1";
        final String userId = "jsanca";
        clusterId = "CLUSTER-123";
        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);
        final Date date = dateFormat.parse("04/10/1981");
        final String tempPath = "/tmp";
        final String assetsPath = "/tmp/assets";

        //Mocking data
        PowerMockito.mockStatic(ClusterFactory.class);
        PowerMockito.when(ClusterFactory.getClusterId()).thenReturn(clusterId);
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.currentTimeMillis())
                .thenReturn(date.getTime());//Current time to 1981

        Config.CONTEXT = mock(ServletContext.class);
        Config.CONTEXT_PATH = tempPath;
        final FileAssetAPI fileAssetAPI = mock(FileAssetAPI.class);
        KeyFactoryUtils.getInstance(fileAssetAPI);
        when(fileAssetAPI.getRealAssetsRootPath()).thenReturn(assetsPath);

        //Generate the token service
        jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();
        assertNotNull(jsonWebTokenService);

        //Generate a new token
        String jsonWebToken = jsonWebTokenService.generateToken(new JWTBean(jwtId,
                userId, new Date(), DateUtil.daysToMillis(2)
        ));
        System.out.println(jsonWebToken);
        assertNotNull(jsonWebToken);
        assertTrue(jsonWebToken.startsWith("eyJhbGciOiJIUzI1NiJ9"));

        //Setting back the right value for the currentTimeMillis
        PowerMockito.when(System.currentTimeMillis()).thenReturn(new Date().getTime());

        //Parse the expired token
        final JWTBean jwtBean = jsonWebTokenService.parseToken(jsonWebToken);
        assertNull(jwtBean);
    }

    /**
     * Testing the generateToken and parseToken but trying to simulate the use of a token in a
     * different server.
     */
    @Test(expected = IncorrectClaimException.class)
    public void generateToken_incorrect_issuer() {

        final String jwtId = "jwt1";
        final String userId = "jsanca";
        clusterId = "CLUSTER-123";
        final String tempPath = "/tmp";
        final String assetsPath = "/tmp/assets";

        //Mocking data
        PowerMockito.mockStatic(ClusterFactory.class);
        PowerMockito.when(ClusterFactory.getClusterId()).thenReturn(clusterId);
        Config.CONTEXT = mock(ServletContext.class);
        Config.CONTEXT_PATH = tempPath;
        final FileAssetAPI fileAssetAPI = mock(FileAssetAPI.class);
        KeyFactoryUtils.getInstance(fileAssetAPI);
        when(fileAssetAPI.getRealAssetsRootPath()).thenReturn(assetsPath);

        //Generate the token service
        jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();
        assertNotNull(jsonWebTokenService);

        //Generate a new token
        String jsonWebToken = jsonWebTokenService.generateToken(new JWTBean(jwtId,
                userId, new Date(), DateUtil.daysToMillis(2)
        ));
        System.out.println(jsonWebToken);
        assertNotNull(jsonWebToken);
        assertTrue(jsonWebToken.startsWith("eyJhbGciOiJIUzI1NiJ9"));

        /*
        Change the existing cluster id in order to simulate we are using the token in a
        different server.
         */
        set(jsonWebTokenService, "issuerId", "ANOTHER-CLUSTER-456");

        //Parse the generated token
        jsonWebTokenService.parseToken(jsonWebToken);
    }

    private void set(Object object, String fieldName, Object fieldValue) {
        Class<?> clazz = object.getClass();
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, fieldValue);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @After
    public void cleanUp() {
        set(jsonWebTokenService, "issuerId", clusterId);
    }

}
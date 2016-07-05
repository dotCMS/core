package com.dotcms.auth.providers.jwt.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

import com.dotcms.auth.providers.jwt.beans.DotCMSSubjectBean;
import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

/**
 * JsonWebTokenService
 * Test
 * @author jsanca
 */

public class JsonWebTokenServiceTest {



    /**
     * Testing the generateToken JsonWebTokenServiceTest
     */
    @Test
    public void generateTokenTest() throws ParseException, JSONException {

        final String jwtId  = "jwt1";
        final String userId = "jsanca";
        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);

        final JsonWebTokenService jsonWebTokenService =
                JsonWebTokenFactory.getInstance().getJsonWebTokenService();

        assertNotNull(jsonWebTokenService);

        final MarshalFactory marshalFactory =
                MarshalFactory.getInstance();

        assertNotNull(marshalFactory);

        final MarshalUtils marshalUtils =
                marshalFactory.getMarshalUtils();

        assertNotNull(marshalUtils);

        final Date date = dateFormat.parse("04/10/1981");
        final DotCMSSubjectBean subjectBean = new DotCMSSubjectBean(date, userId, "myCompany");
        final String jsonWebTokenSubject = marshalUtils.marshal(
                subjectBean
        );

        System.out.println(jsonWebTokenSubject);

        assertNotNull(jsonWebTokenSubject);
        assertTrue(
                new JSONObject("{\"userId\":\"jsanca\",\"lastModified\":371030400000, \"companyId\":\"myCompany\"}").toString().equals
                        (new JSONObject(jsonWebTokenSubject).toString())
        );

        String jsonWebToken = jsonWebTokenService.generateToken(new JWTBean(jwtId,
                jsonWebTokenSubject, userId, date.getTime()
                ));

        System.out.println(jsonWebToken);

        assertNotNull(jsonWebToken);
        assertTrue(jsonWebToken.startsWith("eyJhbGciOiJIUzI1NiJ9"));

        final JWTBean jwtBean = jsonWebTokenService.parseToken(jsonWebToken);

        assertNotNull(jwtBean);
        assertEquals(jwtBean.getId(), jwtId);
        assertEquals(jwtBean.getIssuer(), userId);

        final String subject = jwtBean.getSubject();

        assertNotNull(subject);
        assertTrue(
                new JSONObject(subject).toString().equals
                        (new JSONObject(jsonWebTokenSubject).toString())
        );

        final DotCMSSubjectBean dotCMSSubjectBean =
                marshalUtils.unmarshal(subject, DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean);

        assertTrue(dotCMSSubjectBean.equals(subjectBean));
    }

}

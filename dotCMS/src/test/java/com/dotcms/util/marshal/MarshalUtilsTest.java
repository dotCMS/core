package com.dotcms.util.marshal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.providers.jwt.beans.DotCMSSubjectBean;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

/**
 * MarshalUtils
 * Test
 * @author jsanca
 * @version 3.7
 */

public class MarshalUtilsTest extends UnitTestBase {
	
    /**
     * Testing the marshall
     */
    @Test
    public void marshalTest() throws ParseException, JSONException {


        final MarshalFactory marshalFactory =
                MarshalFactory.getInstance();

        assertNotNull(marshalFactory);

        final MarshalUtils marshalUtils =
                marshalFactory.getMarshalUtils();

        assertNotNull(marshalUtils);

        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);

        final DotCMSSubjectBean subjectBean =
                new DotCMSSubjectBean(dateFormat.parse("04/10/1981"), "jsanca", "myCompany");

        final String json = marshalUtils.marshal(subjectBean);

        assertNotNull(json);
        System.out.println(json);

        assertTrue(
                new JSONObject("{\"userId\":\"jsanca\",\"lastModified\":371030400000, \"companyId\":\"myCompany\"}").toString().equals
                        (new JSONObject(json).toString())
        );


        final DotCMSSubjectBean dotCMSSubjectBean2 =
                marshalUtils.unmarshal(json, DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean2);
        assertEquals(subjectBean, dotCMSSubjectBean2);

        final DotCMSSubjectBean dotCMSSubjectBean3 =
                marshalUtils.unmarshal(new StringReader(json), DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean3);
        assertEquals(subjectBean, dotCMSSubjectBean3);

        final DotCMSSubjectBean dotCMSSubjectBean4 =
                marshalUtils.unmarshal(new ByteArrayInputStream(json.getBytes()), DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean4);
        assertEquals(subjectBean, dotCMSSubjectBean4);
    }

    /**
     * Testing the marshall
     */
    @Test
    public void metaDataMarshalTest() throws ParseException, JSONException {
        final MarshalFactory marshalFactory =
            MarshalFactory.getInstance();

        assertNotNull(marshalFactory);

        final MarshalUtils marshalUtils =
            marshalFactory.getMarshalUtils();

        assertNotNull(marshalUtils);

        String correctJson = "{\"xmptpgNpages\":\"2\",\"keywords\":\"\",\"lastModified\":\"2012-07-05T20:53:14Z\",\"subject\":\"\",\"aaplKeywords\":\"\",\"contentType\":\"application/pdf\",\"creator\":\"Pages\",\"fileSize\":\"46510\",\"content\":\"Malware Attack Threatens to Kick Users Offline\\nBy Cadie Thompson | CNBC\\nA malware attack that has infected some computers for more than a year may cause tens \\nof thousands of Americans to lose their Internet connection on Monday.\\nThe only way for users to prevent losing their Internet connection is to do a quick check \\nfor malware on their computer. One way to do this is to visit a website the FBI set up for \\nusers to determine if their computers are infected. The site also walks users through \\nwhat steps to take if their computer is infected.\\nHackers are to blame for the malware attack, which was part of an international online \\nadvertising scam that resulted in more than 570,000 infected computers around the \\nworld. The malicious software disabled users' antivirus software, which could also make \\ntheir computer prone to other issues.\\nThe FBI caught the hackers last year, but the agents soon discovered they could not \\nsimply turn off the servers being used to control the computers, because victims of the \\nattack would lose their ability to go online. In an unprecedented move, the FBI brought \\nin a private company to set up clean servers to support the victims of the attack. \\nHowever, it is a temporary fix and the safety net system will be shut down at 12:01 EDT \\nMonday, July 9.\\nThere have been repeated warnings issued to the public about the attack from Internet \\nservice providers and internet companies, including Facebook (FB) and Google (GOOG), \\nwhich both issued warning messages on their sites if the companies detected that a \\nuser's computer might be infected.\\nDespite these warnings, the FBI estimates that there are still more than 277,000 \\ncomputers infected worldwide and about 64,000 in the United States.\\n\\n\\\"I think they'll watch, wait and see what the U.S. economy really does in the summer \\nmonths,\\\" he predicts, starting with tomorrow's payroll report, which will either extend a \\nweak trend or break it.\\n\\n\",\"creationDate\":\"2012-07-05T20:53:14Z\",\"author\":\"Jason Smith\",\"title\":\"Untitled 2\",\"created\":\"Thu Jul 05 16:53:14 EDT 2012\",\"producer\":\"Mac OS X 10.7.4 Quartz PDFContext\"}";
        Map<String, Object> data = marshalUtils.unmarshal(correctJson, new DotTypeToken<LinkedHashMap<String, String>>().getType());

        assertNotNull(data);
    }

    @Test
    public void marshalSQLDateTest() throws ParseException, JSONException {


        final MarshalFactory marshalFactory =
                MarshalFactory.getInstance();

        assertNotNull(marshalFactory);

        final MarshalUtils marshalUtils =
                marshalFactory.getMarshalUtils();

        assertNotNull(marshalUtils);

        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);

        final DotCMSSubjectBean subjectBean =
                new DotCMSSubjectBean(new java.sql.Date(dateFormat.parse("04/10/1981").getTime()), "jsanca", "myCompany");

        final String json = marshalUtils.marshal(subjectBean);

        assertNotNull(json);
        System.out.println(json);

        assertTrue(
                new JSONObject("{\"userId\":\"jsanca\",\"lastModified\":371030400000, \"companyId\":\"myCompany\"}").toString().equals
                        (new JSONObject(json).toString())
        );


        final DotCMSSubjectBean dotCMSSubjectBean2 =
                marshalUtils.unmarshal(json, DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean2);
        assertEquals(subjectBean, dotCMSSubjectBean2);

        final DotCMSSubjectBean dotCMSSubjectBean3 =
                marshalUtils.unmarshal(new StringReader(json), DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean3);
        assertEquals(subjectBean, dotCMSSubjectBean3);

        final DotCMSSubjectBean dotCMSSubjectBean4 =
                marshalUtils.unmarshal(new ByteArrayInputStream(json.getBytes()), DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean4);
        assertEquals(subjectBean, dotCMSSubjectBean4);
    }

    @Test
    public void marshalSqlTimeTest() throws ParseException, JSONException {


        final MarshalFactory marshalFactory =
                MarshalFactory.getInstance();

        assertNotNull(marshalFactory);

        final MarshalUtils marshalUtils =
                marshalFactory.getMarshalUtils();

        assertNotNull(marshalUtils);

        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);

        final DotCMSSubjectBean subjectBean =
                new DotCMSSubjectBean(new java.sql.Time(dateFormat.parse("04/10/1981").getTime()), "jsanca", "myCompany");

        final String json = marshalUtils.marshal(subjectBean);

        assertNotNull(json);
        System.out.println(json);

        assertTrue(
                new JSONObject("{\"userId\":\"jsanca\",\"lastModified\":371030400000, \"companyId\":\"myCompany\"}").toString().equals
                        (new JSONObject(json).toString())
        );


        final DotCMSSubjectBean dotCMSSubjectBean2 =
                marshalUtils.unmarshal(json, DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean2);
        assertEquals(subjectBean, dotCMSSubjectBean2);

        final DotCMSSubjectBean dotCMSSubjectBean3 =
                marshalUtils.unmarshal(new StringReader(json), DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean3);
        assertEquals(subjectBean, dotCMSSubjectBean3);

        final DotCMSSubjectBean dotCMSSubjectBean4 =
                marshalUtils.unmarshal(new ByteArrayInputStream(json.getBytes()), DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean4);
        assertEquals(subjectBean, dotCMSSubjectBean4);
    }


    @Test
    public void marshalTimeStampTest() throws ParseException, JSONException {


        final MarshalFactory marshalFactory =
                MarshalFactory.getInstance();

        assertNotNull(marshalFactory);

        final MarshalUtils marshalUtils =
                marshalFactory.getMarshalUtils();

        assertNotNull(marshalUtils);

        final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-8:00"));
        dateFormat.setLenient(true);

        final DotCMSSubjectBean subjectBean =
                new DotCMSSubjectBean(new java.sql.Timestamp(dateFormat.parse("04/10/1981").getTime()), "jsanca", "myCompany");

        final String json = marshalUtils.marshal(subjectBean);

        assertNotNull(json);
        System.out.println(json);

        assertTrue(
                new JSONObject("{\"userId\":\"jsanca\",\"lastModified\":371030400000, \"companyId\":\"myCompany\"}").toString().equals
                        (new JSONObject(json).toString())
        );


        final DotCMSSubjectBean dotCMSSubjectBean2 =
                marshalUtils.unmarshal(json, DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean2);
        assertEquals(subjectBean, dotCMSSubjectBean2);

        final DotCMSSubjectBean dotCMSSubjectBean3 =
                marshalUtils.unmarshal(new StringReader(json), DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean3);
        assertEquals(subjectBean, dotCMSSubjectBean3);

        final DotCMSSubjectBean dotCMSSubjectBean4 =
                marshalUtils.unmarshal(new ByteArrayInputStream(json.getBytes()), DotCMSSubjectBean.class);

        assertNotNull(dotCMSSubjectBean4);
        assertEquals(subjectBean, dotCMSSubjectBean4);
    }
}

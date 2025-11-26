package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.auth.PrincipalThreadLocal;
import com.liferay.portal.ejb.CompanyManagerUtil;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.junit.BeforeClass;
import org.junit.Test;

public class UtilMethodsITest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Test method {@link UtilMethods#dateToHTMLDate
     * Given scenario: changing system timezone
     * Expected result: returned formatted date with system timezone taken into consideration
     */
    @Test
    public void test_dateToHTMLDate_DefaultTimeZone()
            throws ParseException, SystemException, PortalException {
        final Date OctSix22at2am = new SimpleDateFormat("yyyy-MM-dd H:mm:ss aa")
                .parse("2022-10-06 2:00:00 AM");
        final String parsedDate = UtilMethods.dateToHTMLDate(OctSix22at2am);
        assertEquals("10/6/2022", parsedDate);

    }
    /**
     * Test method {@link UtilMethods#dateToHTMLDate
     * Given scenario: changing system timezone
     * Expected result: returned formatted date with system timezone taken into consideration
     */
    @Test
    public void test_dateToHTMLDate_USPacificTimeZone()
            throws ParseException, SystemException, PortalException {
        final TimeZone previousTimeZone = APILocator.systemTimeZone();

        try {
            final Date OctSix22at2am = new SimpleDateFormat("yyyy-MM-dd H:mm:ss aa")
                    .parse("2022-10-06 1:00:00 AM");
            PrincipalThreadLocal.setName(APILocator.systemUser().getUserId());
            CompanyManagerUtil.updateUsers("en_US", "US/Pacific",
                    null, false, false, null);

            final String parsedDate = UtilMethods.dateToHTMLDate(OctSix22at2am);
            assertEquals("10/5/2022", parsedDate);
        } finally {
            CompanyManagerUtil.updateUsers("en_US", previousTimeZone.getID(),
                    null, false, false, null);
        }

    }


    @Test
    public void test_isSet_with_null_object() {
        String obj=null;
        assertFalse(UtilMethods.isSet(() -> obj.toString()));
    }


    @Test
    public void test_isEmpty_with_null_object() {
        String obj=null;
        assertTrue(UtilMethods.isEmpty(() -> obj.toString()));
    }


}

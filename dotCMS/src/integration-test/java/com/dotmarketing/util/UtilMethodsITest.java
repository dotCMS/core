package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;

import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyManagerUtil;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Test;

public class UtilMethodsITest {

    /**
     * Test method {@link UtilMethods#dateToHTMLDate
     * Given scenario: changing system timezone
     * Expected result: returned formatted date with system timezone taken into consideration
     */
    @Test
    public void test_dateToHTMLDate_USPacificTimeZone()
            throws ParseException, SystemException, PortalException {
        final Date OctSix22at2am = new SimpleDateFormat("yyyy-MM-dd H:mm:ss")
                .parse("2022-10-06 2:00:00");
        CompanyManagerUtil.updateUsers( "en_US", "US/Pacific",
                null, false, false, null );

        final String parsedDate = UtilMethods.dateToHTMLDate(OctSix22at2am);
        assertEquals("10/6/2022", parsedDate);

    }
}

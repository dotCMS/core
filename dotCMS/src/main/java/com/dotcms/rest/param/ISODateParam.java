package com.dotcms.rest.param;

import com.dotmarketing.util.DateUtil;

import java.text.ParseException;
import java.util.Date;

/**
 * Encapsulates the logic to parse a ISO String date into a {@link Date}
 * @author jsanca
 */
public class ISODateParam extends Date {

    public ISODateParam(final String stringDate) throws ParseException {

        super(DateUtil.parseISO (stringDate).getTime());
    }
}

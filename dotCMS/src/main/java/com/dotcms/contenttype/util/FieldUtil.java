package com.dotcms.contenttype.util;

import com.dotmarketing.util.UtilMethods;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class FieldUtil {

    public boolean validDate(String value) {
        if (UtilMethods.isSet(value) && !"now".equals(value)) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

            try {
                df.parse(value);
            } catch (Exception e) {
                return false;
            }
        }
        return true;

    }

    public boolean validDateTime(String value) {
        if (UtilMethods.isSet(value) && !"now".equals(value)) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            try {
                df.parse(value);
            } catch (Exception e) {
                return false;
            }
        }
        return true;

    }

    public boolean validTime(String value) {
        if (UtilMethods.isSet(value) && !"now".equals(value)) {
            DateFormat df = new SimpleDateFormat("HH:mm:ss");

            try {
                df.parse(value);
            } catch (Exception e) {
                return false;
            }
        }
        return true;

    }
}

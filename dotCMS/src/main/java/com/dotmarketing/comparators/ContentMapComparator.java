package com.dotmarketing.comparators;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;
import java.util.Comparator;
import java.util.Date;

/**
 * Comparator class used to sort by a field value in a content map
 * @author nollymar
 */
public class ContentMapComparator implements Comparator<Contentlet> {

    private String direction = "asc";

    private String field = StringPool.BLANK;

    public ContentMapComparator(String orderFieldAndDirection) {
        super();
        if (orderFieldAndDirection != null) {
            String[] values = orderFieldAndDirection.split(" ");
            if (values.length > 0) {
                if (values[0] != null && values[0].contains(".")) {
                    this.field = values[0].split("\\.")[values[0].split("\\.").length - 1];
                } else {
                    this.field = values[0];
                }
            }

            if (values.length > 1)
                this.direction = values[1];
        }
    }

    public int compare(final Contentlet contentlet1, final Contentlet contentlet2) {

        try {
            Object value1 = contentlet1.get(field);
            Object value2 = contentlet2.get(field);

            int ret = 0;

            if (value1 == null || value2 == null) {
                return ret;
            } else if (value1 instanceof Integer && value2 instanceof Integer) {
                ret = ((Integer)value1).compareTo((Integer)value2);
            } else if (value1 instanceof Long && value2 instanceof Long) {
                ret = ((Long)value1).compareTo((Long)value2);
            } else if (value1 instanceof Date && value2 instanceof Date) {
                ret = ((Date)value1).compareTo((Date)value2);
            } else if (value1 instanceof Float && value2 instanceof Float) {
                ret = ((Float)value1).compareTo((Float)value2);
            } else if (value1 instanceof Boolean && value2 instanceof Boolean) {
                ret = ((Boolean)value1).compareTo((Boolean)value2);
            } else {
                ret = (String.valueOf(value1)).compareTo(String.valueOf(value2));
            }

            if (direction.equals("asc")) {
                return ret;
            }

            return ret * -1;

        } catch (Exception e) {
            Logger.warnAndDebug(ContentMapComparator.class,
                    "Error sorting contents using criteria: " + field + " " + direction, e);
        }
        return 0;
    }

}

package com.dotmarketing.util;

import java.io.Serializable;
import java.util.List;

/**
 * Util class to convertion operations
 * @author jsanca
 */
public class ConversionUtils implements Serializable {

    public static ConversionUtils INSTANCE =
            new ConversionUtils();

    private ConversionUtils() {}

    /**
     * Converts from the Original to Destiny bean using a converter.
     * @param origin O
     * @param converter {@link Converter}
     * @param <O>
     * @param <D>
     * @return D
     */
    public <O, D> D convert (final O origin,
                             final Converter<O, D> converter) {

        D d = null;

        if (null != origin && null != converter) {

            d = converter.convert(origin);
        }

        return d;
    } // convert

    public <O, D> List<D> convert (final List<O> originList,
                                   final Converter<O, D> converter) {

        List<D> destinyList = null;

        if (null != originList && null != converter) {

            destinyList = CollectionsUtils.getNewList();

            for (O origin : originList) {

                destinyList.add(converter.convert(origin));
            }
        }

        return destinyList;
    } // convert


} // E:O:F:ConversionUtils.

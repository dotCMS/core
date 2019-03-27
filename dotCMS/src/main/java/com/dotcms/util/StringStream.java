package com.dotcms.util;

import java.util.stream.Stream;

/**
 * The interface provides kinda wrapper to use extra functions in your pipeline for string streams
 * @param <T>
 * @author jsanca
 */
public interface StringStream {

    Stream<String> stream();

    static StringStream of(final Stream<String> stream) {

        return () -> stream;
    }

    default  StringStream lowerCase() {

        return of(stream().map(t -> String.class.cast(t).toLowerCase()));
    }

    default  StringStream upperCase() {

        return of(stream().map(t -> String.class.cast(t).toUpperCase()));
    }
}

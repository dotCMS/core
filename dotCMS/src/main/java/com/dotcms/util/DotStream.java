package com.dotcms.util;

import java.util.stream.Stream;

/**
 * The interface provides kinda wrapper to use extra functions in your pipeline
 * @param <T>
 * @author jsanca
 */
public interface DotStream <T>  {

    Stream<T> stream();

    static <T> DotStream<T> of(final Stream<T> stream) {

        return () -> stream;
    }


    // todo: add here extra functions
}

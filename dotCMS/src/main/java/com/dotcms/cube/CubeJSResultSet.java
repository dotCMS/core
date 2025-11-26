package com.dotcms.cube;

import com.dotcms.analytics.model.ResultSetItem;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public interface CubeJSResultSet extends Iterable<ResultSetItem>  {

    long size();

    @Override
    Iterator<ResultSetItem> iterator();

    @Override
    default void forEach(Consumer<? super ResultSetItem> action) {
        Iterable.super.forEach(action);
    }

    @Override
    default Spliterator<ResultSetItem> spliterator() {
        return Iterable.super.spliterator();
    }

}

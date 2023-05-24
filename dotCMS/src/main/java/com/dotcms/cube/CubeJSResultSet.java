package com.dotcms.cube;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;

public interface CubeJSResultSet extends Iterable<CubeJSResultSet.ResultSetItem>  {

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

    class ResultSetItem {

        private Map<String, Object> item;

        public ResultSetItem(final Map<String, Object> item) {
            this.item = item;
        }

        /**
         * Return the value of the specific attribute
         * @param attributeName
         * @return
         */
        public Optional<Object> get(final String attributeName){
            return Optional.ofNullable(item.get(attributeName));
        }

        /**
         * Return all the attributes and values.
         *
         * @return
         */
        public Map<String, Object> getAll(){
            return new HashMap<>(item);
        }

    }
}

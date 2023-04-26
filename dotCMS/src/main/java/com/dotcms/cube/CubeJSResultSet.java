package com.dotcms.cube;

import com.dotcms.cube.CubeJSResultSet.ResultSetItem;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Represent a Result from running a CubeJS Query in a CubeJS Server.
 */
public class CubeJSResultSet implements Iterable<ResultSetItem> {

    private final List<ResultSetItem> data;

    public CubeJSResultSet(final List<Map<String, Object>> data){
        this.data = data.stream().map(ResultSetItem::new).collect(Collectors.toList());
    }

    public int size() {
        return data.size();
    }

    @NotNull
    @Override
    public Iterator<ResultSetItem> iterator() {
        return data.iterator();
    }

    @Override
    public void forEach(Consumer<? super ResultSetItem> action) {
        Iterable.super.forEach(action);
    }

    @Override
    public Spliterator<ResultSetItem> spliterator() {
        return Iterable.super.spliterator();
    }

    public static class ResultSetItem {

        private Map<String, Object> item;

        private ResultSetItem(final Map<String, Object> item) {
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

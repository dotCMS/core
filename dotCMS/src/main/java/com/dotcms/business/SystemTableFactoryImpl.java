package com.dotcms.business;

import com.dotmarketing.business.CachableSupport;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.runonce.Task230707CreateSystemTable;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link SystemTableFactory}
 */
public class SystemTableFactoryImpl  implements SystemTableFactory, CacheableEagerFactory<String, Object> {

    private final SystemCache systemCache;
    private final static String DOT_SYSTEM_CACHE_LOADED = "DOT_SYSTEM_CACHE_LOADED";

    public SystemTableFactoryImpl () {

        this.systemCache = CacheLocator.getSystemCache();
    }

    @Override
    public String getLoadedKey() {

        return DOT_SYSTEM_CACHE_LOADED;
    }

    @Override
    public Object getLoadedValue() {

        return true;
    }

    @Override
    public CachableSupport<String, Object> getCache() {
        return this.systemCache;
    }

    @Override
    public  String getSelectAllSQL() {
        return " SELECT * FROM system_table";
    }

    @Override
    public void saveOrUpdateInternal(final String key, final Object value) throws DotDataException {

        new DotConnect()
                .setSQL( //upsert
                        "INSERT INTO system_table (value, key) values(?,?)" +
                                "ON CONFLICT(key) DO UPDATE SET value = ?")
                .addParam(value)
                .addParam(key)
                .addParam(value)
                .loadResult();
    }

    @Override
    public void deleteInternal(final String key) throws DotDataException {

        new DotConnect()
            .setSQL("DELETE FROM system_table WHERE key=?")
            .addParam(key)
            .loadResult();
    }

    @Override
    public String getKey(final Map<String, Object> recordMap) {

            return toString(recordMap.get("key"));
    }

    @Override
    public Object getValue(final Map<String, Object> recordMap) {

            return recordMap.get("value");
    }

    @Override
    public Object wrap(final Object value) {

        return toString(value);
    }


    private String toString(final Object value) {

        return Objects.nonNull(value)  ? value.toString() : null;
    }

    @Override
    public Optional<Object> find(final String key) throws DotDataException {
        return CacheableEagerFactory.super.find(key);
    }

    @Override
    public Map<String, Object> findAll() throws DotDataException {
        return CacheableEagerFactory.super.findAll();
    }

    @Override
    public void saveOrUpdate(final String key, final Object Object) throws DotDataException {
        CacheableEagerFactory.super.saveOrUpdate(key, Object);
    }

    @Override
    public void delete(final String key) throws DotDataException {
        CacheableEagerFactory.super.delete(key);
    }

    @Override
    public void clearCache() {
        CacheableEagerFactory.super.clearCache();
    }

    @Override
    public void initIfNeeded() {

        Try.run(()->new DotConnect().executeStatement("CREATE TABLE if not exists system_table ("
                + "key varchar(511) primary key,"
                + "value text not null"
                + ")")).onFailure(e -> new DotRuntimeException(e));
    }
} // E:O:F:SystemTableFactoryImpl

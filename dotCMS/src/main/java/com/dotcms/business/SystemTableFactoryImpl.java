package com.dotcms.business;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Default implementation of {@link SystemTableFactory}
 */
public class SystemTableFactoryImpl extends SystemTableFactory {

    private final SystemCache systemCache;
    private final static String DOT_SYSTEM_CACHE_LOADED = "DOT_SYSTEM_CACHE_LOADED";

    public SystemTableFactoryImpl () {

        this.systemCache = CacheLocator.getSystemCache();
    }
    @Override
    protected Optional<String> find(final String key) throws DotDataException {

        Object value = null;
        if (UtilMethods.isSet(key)) {

            value = this.systemCache.get(key);
            if(Objects.isNull(value) && Objects.isNull((systemCache.get(DOT_SYSTEM_CACHE_LOADED)))){

                this.findAll();
                return find(key);
            }
        }

        return  Optional.ofNullable(toString(value));
    }

    @Override
    protected Map<String, String> findAll() throws DotDataException {

        final Map<String, String> records = new LinkedHashMap<>();

        final List<Map<String, Object>> result = new DotConnect()
                .setSQL(" SELECT * FROM system_table")
                .loadObjectResults();

        for (final Map<String, Object> recordMap : result) {

            final Object key = recordMap.get("key");
            final Object value = recordMap.get("value");
            if (Objects.nonNull(key) && Objects.nonNull(value)) {
                records.put(key.toString(), toString(value.toString()));
                this.systemCache.put(key.toString(), value);
            }
        }

        this.systemCache.put(DOT_SYSTEM_CACHE_LOADED, true);
        return records;
    }

    @Override
    protected void saveOrUpdate(final String key, final String value) throws DotDataException {

        if (Objects.nonNull(key) && Objects.nonNull(value)) {

            new DotConnect()
                    .setSQL( //upsert
                            "INSERT INTO system_table (value, key) values(?,?)" +
                                    "ON CONFLICT(key) DO UPDATE SET value = ?")
                    .addParam(value)
                    .addParam(key)
                    .addParam(value)
                    .loadResult();

            this.clearCache();
        } else {

            throw new DotDataException("The key and value should not be null");
        }
    }

    @Override
    protected void delete(final String key) throws DotDataException {

        if (Objects.nonNull(key)) {

            new DotConnect()
                .setSQL("DELETE FROM system_table WHERE key=?")
                .addParam(key)
                .loadResult();
            this.clearCache();
        } else {

            throw new DotDataException("The key should not be null");
        }
    }

    @Override
    protected void clearCache() {

        this.systemCache.clearCache();
    }

    private String toString(final Object value) {

        return Objects.nonNull(value)  ? value.toString() : null;
    }
} // E:O:F:SystemTableFactoryImpl

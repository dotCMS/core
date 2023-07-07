package com.dotcms.business;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDuplicateDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link SystemTableFactory}
 */
public class SystemTableFactoryImpl extends SystemTableFactory {

    private final SystemCache systemCache;

    public SystemTableFactoryImpl () {

        this.systemCache = CacheLocator.getSystemCache();
    }
    @Override
    protected Optional<String> find(final String key) throws DotDataException {

        if(UtilMethods.isSet(key)) {

            Object value = this.systemCache.get(key);
            if(value == null) {

                final List<Map<String, Object>> result = new DotConnect()
                        .setSQL(" SELECT * FROM system_table WHERE key = ? ")
                        .addParam(key)
                        .loadObjectResults();

                value = result.isEmpty() ? null : result.get(0).get("value");

                if(value != null) {
                    this.systemCache.put(key, value);
                }

                return Optional.ofNullable(value.toString());
            }
        }

        return Optional.empty();
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
                records.put(key.toString(), value.toString());
                this.systemCache.put(key.toString(), value);
            }
        }

        return records;
    }

    @Override
    protected void save(final String key, final String value) throws DotDataException {

        if (Objects.nonNull(key) && Objects.nonNull(value)) {

            final Optional<String> valueOpt = find(key);
            valueOpt.ifPresent(s -> {
                throw new DotDuplicateDataException("The key " + key + " already exists");
            });

            new DotConnect()
                    .setSQL("INSERT INTO system_table (key, value) VALUES (?,?")
                    .addParam(key)
                    .addParam(value)
                    .loadResult();

            this.systemCache.remove(key);
        } else {

            throw new DotDataException("The key and value should not be null");
        }
    }

    @Override
    protected void update(final String key, final String value) throws DotDataException {

        if (Objects.nonNull(key) && Objects.nonNull(value)) {

            final Optional<String> valueOpt = find(key);
            valueOpt.ifPresentOrElse(
                    s -> {
                        Try.run(()->new DotConnect()
                            .setSQL("UPDATE system_table value=? WHERE key=?")
                            .addParam(value)
                            .addParam(key)
                            .loadResult()).getOrElseThrow(e-> new DotRuntimeException(e)); },
                    ()-> {throw new DotDuplicateDataException("The key " + key + " does not exist");});

            this.systemCache.remove(key);
        } else {

            throw new DotDataException("The key and value should not be null");
        }
    }

    @Override
    protected void delete(final String key) throws DotDataException {

        if (Objects.nonNull(key)) {

            final Optional<String> valueOpt = find(key);
            valueOpt.ifPresentOrElse(
                    s -> {
                        Try.run(()->new DotConnect()
                                .setSQL("DELETE FROM system_table WHERE key=?")
                                .addParam(key)
                                .loadResult()).getOrElseThrow(e-> new DotRuntimeException(e)); },
                    ()-> {throw new DotDuplicateDataException("The key " + key + " does not exist");});

            this.systemCache.remove(key);
        } else {

            throw new DotDataException("The key should not be null");
        }
    }

    @Override
    protected void clearCache() {

        this.systemCache.clearCache();
    }
} // E:O:F:SystemTableFactoryImpl

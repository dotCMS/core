package com.dotmarketing.portlets.variant.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.rest.validation.Preconditions;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.variant.model.Variant;
import com.dotmarketing.util.UUIDGenerator;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class VariantFactoryImpl implements VariantFactory{

    private String VARIANT_INSERT_QUERY = "INSERT INTO variant (id, name, deleted) VALUES (?, ?, ?)";
    private String VARIANT_UPDATE_QUERY = "UPDATE variant SET name = ?, deleted = ? WHERE id =?";
    private String VARIANT_DELETE_QUERY = "DELETE from variant WHERE id =?";
    private String VARIANT_SELECT_QUERY = "SELECT * from variant WHERE id =?";

    /**
     * Implementation for {@link VariantFactory#save(Variant)}
     * @param variant
     *
     * @return
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public Variant save(final Variant variant) throws DotDataException {
        final String identifier = UUIDGenerator.generateUuid();

        new DotConnect().setSQL(VARIANT_INSERT_QUERY)
                .addParam(identifier)
                .addParam(variant.getName())
                .addParam(variant.isDeleted())
                .loadResult();

        return new Variant(identifier, variant.getName(), false);
    }

    /**
     * Implementation for {@link VariantFactory#update(Variant)}
     * @param variant
     *
     * @throws DotDataException
     */
    @Override
    @WrapInTransaction
    public void update(Variant variant) throws DotDataException {
        new DotConnect().setSQL(VARIANT_UPDATE_QUERY)
                .addParam(variant.getName())
                .addParam(variant.isDeleted())
                .addParam(variant.getIdentifier())
                .loadResult();
    }

    @Override
    @WrapInTransaction
    public void delete(final String id) throws DotDataException {
        new DotConnect().setSQL(VARIANT_DELETE_QUERY)
                .addParam(id)
                .loadResult();
    }

    @Override
    public Optional<Variant> get(final String identifier) throws DotDataException {
        final ArrayList loadResults = new DotConnect().setSQL(VARIANT_SELECT_QUERY)
                .addParam(identifier)
                .loadResults();

        if (!loadResults.isEmpty()) {
            final Map resultMap = (Map) loadResults.get(0);
            return Optional.of(
                    new Variant(
                        resultMap.get("id").toString(),
                        resultMap.get("name").toString(),
                        ConversionUtils.toBooleanFromDb(resultMap.get("deleted"))
                )
            );
        } else {
            return Optional.empty();
        }
    }
}

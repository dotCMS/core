package com.dotmarketing.portlets.variant.business;

import com.dotcms.util.ConversionUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.variant.model.Variant;
import com.dotmarketing.util.UUIDGenerator;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;

public class VariantFactoryImpl implements VariantFactory{

    private String VARIANT_INSERT_QUERY = "INSERT INTO variant (id, name, archived) VALUES (?, ?, ?)";
    private String VARIANT_UPDATE_QUERY = "UPDATE variant SET name = ?, archived = ? WHERE id =?";
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
    public Variant save(final Variant variant) throws DotDataException {
        final String identifier = getId(variant);

        new DotConnect().setSQL(VARIANT_INSERT_QUERY)
                .addParam(identifier)
                .addParam(variant.getName())
                .addParam(variant.isArchived())
                .loadResult();

        return new Variant(identifier, variant.getName(), false);
    }

    private String getId(final Variant variant) {

        final String deterministicID = DigestUtils.sha256Hex(variant.getName());

        final Optional<Variant> variantFromDataBase;
        try {
            variantFromDataBase = get(deterministicID);

            if (variantFromDataBase.isPresent()) {
                return UUIDGenerator.generateUuid();
            } else {
                return deterministicID;
            }
        } catch (DotDataException e) {
            return UUIDGenerator.generateUuid();
        }
    }

    /**
     * Implementation for {@link VariantFactory#update(Variant)}
     * @param variant
     *
     * @throws DotDataException
     */
    @Override
    public void update(Variant variant) throws DotDataException {
        new DotConnect().setSQL(VARIANT_UPDATE_QUERY)
                .addParam(variant.getName())
                .addParam(variant.isArchived())
                .addParam(variant.getIdentifier())
                .loadResult();
    }

    @Override
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
                        ConversionUtils.toBooleanFromDb(resultMap.get("archived"))
                )
            );
        } else {
            return Optional.empty();
        }
    }
}

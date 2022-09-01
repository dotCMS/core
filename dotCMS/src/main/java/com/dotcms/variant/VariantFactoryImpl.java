package com.dotcms.variant;

import com.dotcms.util.DotPreconditions;
import com.dotcms.util.transform.TransformerLocator;
import com.dotcms.variant.model.transform.VariantTransformer;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
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
    private String VARIANT_SELECT_BY_NAME_QUERY = "SELECT * from variant WHERE name =?";

    /**
     * Implementation for {@link VariantFactory#save(Variant)}
     * @param variant
     *
     * @return
     * @throws DotDataException
     */
    @Override
    public Variant save(final Variant variant) throws DotDataException {

        DotPreconditions.checkNotNull(variant.name(), IllegalArgumentException.class,
                "Name must not be null");
        final String identifier = getId(variant);

        new DotConnect().setSQL(VARIANT_INSERT_QUERY)
                .addParam(identifier)
                .addParam(variant.name())
                .addParam(variant.archived())
                .loadResult();

        return Variant.builder()
                .identifier(identifier)
                .name(variant.name())
                .archived(false).build();
    }

    private String getId(final Variant variant) {

        final String deterministicID = DigestUtils.sha256Hex(variant.name());

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
        DotPreconditions.checkNotNull(variant.identifier(), IllegalArgumentException.class,
                "The ID should not bee null");

        new DotConnect().setSQL(VARIANT_UPDATE_QUERY)
                .addParam(variant.name())
                .addParam(variant.archived())
                .addParam(variant.identifier())
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
            return Optional.of(TransformerLocator.createVariantTransformer(loadResults).from());
        } else {
            return Optional.empty();
        }
    }

    public Optional<Variant> getByName(final String name) throws DotDataException {
        final ArrayList loadResults = new DotConnect().setSQL(VARIANT_SELECT_BY_NAME_QUERY)
                .addParam(name)
                .loadResults();

        if (!loadResults.isEmpty()) {
            return Optional.of(TransformerLocator.createVariantTransformer(loadResults).from());
        } else {
            return Optional.empty();
        }
    }
}

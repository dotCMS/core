package com.dotcms.variant;

import com.dotcms.util.DotPreconditions;
import com.dotcms.util.transform.TransformerLocator;
import com.dotcms.variant.model.transform.VariantTransformer;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
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

        final Variant variantWithID = Variant.builder()
                .identifier(identifier)
                .name(variant.name())
                .archived(variant.archived()).build();

        CacheLocator.getVariantCache().put(variantWithID);
        return variantWithID;
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

    public void update(final Variant variant) throws DotDataException {
        DotPreconditions.checkNotNull(variant.identifier(), IllegalArgumentException.class,
                "The ID should not bee null");

        new DotConnect().setSQL(VARIANT_UPDATE_QUERY)
                .addParam(variant.name())
                .addParam(variant.archived())
                .addParam(variant.identifier())
                .loadResult();

        CacheLocator.getVariantCache().put(variant);
    }

    @Override
    public void delete(final String id) throws DotDataException {
        final Variant variant = get(id).orElseThrow(() ->
                new DoesNotExistException(String.format("Variant with id %s does not exists", id)));

        new DotConnect().setSQL(VARIANT_DELETE_QUERY)
                .addParam(id)
                .loadResult();

        CacheLocator.getVariantCache().remove(variant);
    }

    @Override
    public Optional<Variant> get(final String identifier) throws DotDataException {
        Variant variant = CacheLocator.getVariantCache().getById(identifier);

        if (!UtilMethods.isSet(variant)) {
            final ArrayList loadResults = new DotConnect().setSQL(VARIANT_SELECT_QUERY)
                    .addParam(identifier)
                    .loadResults();
            variant = !loadResults.isEmpty() ? TransformerLocator.createVariantTransformer(loadResults).from() : null;
        }

        return Optional.ofNullable(variant);
    }

    public Optional<Variant> getByName(final String name) throws DotDataException {
        Variant variant = CacheLocator.getVariantCache().getByName(name);

        if (!UtilMethods.isSet(variant)) {
            final ArrayList loadResults = new DotConnect().setSQL(VARIANT_SELECT_BY_NAME_QUERY)
                    .addParam(name)
                    .loadResults();

            variant = !loadResults.isEmpty() ?
                    TransformerLocator.createVariantTransformer(loadResults).from() : null;
        }

        return Optional.ofNullable(variant);
    }
}

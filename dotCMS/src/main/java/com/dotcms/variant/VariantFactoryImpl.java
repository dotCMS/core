package com.dotcms.variant;

import com.dotcms.util.DotPreconditions;
import com.dotcms.util.transform.TransformerLocator;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;

public class VariantFactoryImpl implements VariantFactory{

    private String VARIANT_INSERT_QUERY = "INSERT INTO variant (name, description, archived) VALUES (?, ?, ?)";
    private String VARIANT_UPDATE_QUERY = "UPDATE variant SET description = ?, archived = ? WHERE name =?";
    private String VARIANT_DELETE_QUERY = "DELETE from variant WHERE name =?";
    private String VARIANT_SELECT_QUERY = "SELECT * from variant WHERE name =?";
    private String VARIANT_SELECT_ALL_QUERY = "SELECT * from variant WHERE archived=?";

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
        DotPreconditions.checkArgument(validateName(variant.name()), IllegalArgumentException.class,
                "Name must have just alphanumeric characters and '_' or '/'");

        new DotConnect().setSQL(VARIANT_INSERT_QUERY)
                .addParam(variant.name())
                .addParam(variant.description().orElse(null))
                .addParam(variant.archived())
                .loadResult();

        CacheLocator.getVariantCache().remove(variant);

        return getFromDataBaseByName(variant.name()).orElseThrow(
                () -> new DotRuntimeException("Error Saving variant " + variant));
    }

    private boolean validateName(final String name) {
        return name.matches("^[a-zA-Z]([-_a-zA-Z0-9]+/?)*$");
    }

    /**
     * Implementation for {@link VariantFactory#update(Variant)}
     * @param variant
     *
     * @throws DotDataException
     */
    @Override

    public void update(final Variant variant) throws DotDataException {
        DotPreconditions.checkNotNull(variant.name(), IllegalArgumentException.class,
                "The ID should not bee null");

        new DotConnect().setSQL(VARIANT_UPDATE_QUERY)
                .addParam(variant.description().orElse(null))
                .addParam(variant.archived())
                .addParam(variant.name())
                .loadResult();

        CacheLocator.getVariantCache().remove(variant);
    }

    @Override
    public void delete(final String name) throws DotDataException {
        final Variant variant = get(name).orElseThrow(() ->
                new DoesNotExistException(String.format("Variant with id %s does not exists", name)));

        new DotConnect().setSQL(VARIANT_DELETE_QUERY)
                .addParam(name)
                .loadResult();

        CacheLocator.getVariantCache().remove(variant);
    }

    private Optional<Variant> getFromDataBaseByName(final String name) throws DotDataException {
        return getFromDataBase(VARIANT_SELECT_QUERY, name);
    }

    private Optional<Variant> getFromDataBase(final String query, final String parameter) throws DotDataException {
        final ArrayList loadResults = new DotConnect().setSQL(query)
                .addParam(parameter)
                .loadResults();

        return !loadResults.isEmpty() ?
                Optional.of(TransformerLocator.createVariantTransformer(loadResults).from()) :
                Optional.empty();
    }

    @Override
    public Optional<Variant> get(final String name) throws DotDataException {

        Variant variant = CacheLocator.getVariantCache().get(name);

        if (UtilMethods.isSet(variant)) {
            return variant.equals(VARIANT_404) ? Optional.empty() : Optional.of(variant);
        } else {
            final Optional<Variant> variantFromDataBase = getFromDataBaseByName(name);

            if (variantFromDataBase.isPresent()) {
                CacheLocator.getVariantCache().put(variantFromDataBase.get());
            } else {
                CacheLocator.getVariantCache().put(name, VariantFactory.VARIANT_404);
            }

            return variantFromDataBase;
        }
    }

    @Override
    public List<Variant> getVariants() throws DotDataException {
        return TransformerLocator.createVariantTransformer(new DotConnect()
                .setSQL(VARIANT_SELECT_ALL_QUERY)
                .addParam(false)
                .loadResults()).asList();
    }
}

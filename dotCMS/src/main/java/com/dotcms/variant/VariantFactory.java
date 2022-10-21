package com.dotcms.variant;

import com.dotcms.variant.model.Variant;
import com.dotmarketing.exception.DotDataException;
import java.util.List;
import java.util.Optional;

/**
 * Factory for {@link Variant}
 */
public interface VariantFactory {

    Variant VARIANT_404 = Variant.builder()
            .description(Optional.of("Not found variant"))
            .name("VARIANT_404")
            .archived(false)
            .build();

    /**
     * Save a new {@link Variant}.
     * if the the {@link Variant}'s identifier value is not null then it will be ignored.
     * Also, if {@link Variant}'s deleted value is true then it will be ignored.
     *
     * @param variant
     *
     * @throws NullPointerException if the {@link Variant}'s name is null
     */

    Variant save(final Variant variant) throws DotDataException;

    /**
     * Update a {@link Variant}, the {@link Variant}'s identifier should not be null or a
     * {@link NullPointerException} will throw.
     *
     * @param variant
     *
     * @throws NullPointerException if the {@link Variant}'s identifier is null or
     *                              if {@link Variant}'s name is null
     */
    void update(final Variant variant) throws DotDataException;

    /**
     * Delete a {@link Variant}
     *
     * @param name Variant's id to be deleted
     */
    void delete(final String name) throws DotDataException;

    /**
     * Return a {@link Variant} by Name
     * @param name {@link Variant}'s name
     * @return {@link Variant}
     */
    Optional<Variant> get(final String name) throws DotDataException;

    /**
     * Gets all persisted {@link Variant}
     * @return the variants
     */
    List<Variant> getVariants() throws DotDataException;
}

package com.dotmarketing.portlets.variant.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.variant.model.Variant;
import java.util.Optional;

public interface VariantAPI {
    /**
     * Save a new {@link Variant}.
     * if the the {@link Variant}'s identifier value is not null then it will be ignored.
     * Also, if {@link Variant}'s deleted value is true then it will be ignored.
     *
     * @param variant
     *
     * @throws NullPointerException if the {@link Variant}'s name is null
     * @throws IllegalArgumentException if the {@link Variant#isArchived()} is true
     */
    Variant save(final Variant variant);

    /**
     * Update a {@link Variant}, the {@link Variant}'s identifier should not be null or a
     * {@link NullPointerException} will throw.
     *
     * @param variant
     *
     * @throws NullPointerException if the {@link Variant}'s identifier is null or
     *                              if {@link Variant}'s name is null
     */
    void update(final Variant variant);

    /**
     * Delete a {@link Variant}
     *
     * @param id Variant's id to be deleted
     */
    void delete(final String id);

    /**
     * Archive a {@link Variant}
     *
     * @param id Variant's id to be archive
     */
    void archive(final String id);

    /**
     * Return a {@link Variant} by Identifier
     * @param identifier {@link Variant}'s identifier
     * @return {@link Variant}
     */
    Optional<Variant> get(final String identifier);
}

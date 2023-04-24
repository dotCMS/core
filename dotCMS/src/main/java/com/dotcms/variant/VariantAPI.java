package com.dotcms.variant;

import com.dotcms.variant.model.Variant;
import com.dotmarketing.exception.DotDataException;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Optional;

public interface VariantAPI {

    Variant DEFAULT_VARIANT = Variant.builder()
            .name("DEFAULT")
            .description(Optional.of("Variant use by DEFAULT when a Contentlet is created"))
            .archived(false)
            .build();

    String VARIANT_KEY = "variantName";

    /**
     * Save a new {@link Variant}.
     * if the the {@link Variant}'s identifier value is not null then it will be ignored.
     * Also, if {@link Variant}'s deleted value is true then it will be ignored.
     *
     * @param variant
     *
     * @throws NullPointerException if the {@link Variant}'s name is null
     * @throws IllegalArgumentException if the {@link Variant#archived()} is true
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
     * Archive a {@link Variant}
     *
     * @param name Variant's id to be archive
     */
    void archive(final String name) throws DotDataException;

    /**
     * Return a {@link Variant} by Name
     * @param name {@link Variant}'s name
     * @return {@link Variant}
     */
    Optional<Variant> get(final String name) throws DotDataException;

    /**
     * Gets all persisted {@link Variant} that are not archived
     * @return the variants
     */
    List<Variant> getVariants() throws DotDataException;

    /**
     * This method will set the {@link Variant} as the DEFAULT Variant.
     *
     * It means that all the contentlets's version in the specific {@link Variant} are going
     * to be copied and set as the DEFAULT version.
     *
     * Also this new copies are going to be the LIVE version of the DEFAULT {@link Variant}.
     *
     * @param variant
     * @param user to check permissions
     * @throws DotDataException
     */
    void promote(final Variant variant, final User user) throws DotDataException;
}

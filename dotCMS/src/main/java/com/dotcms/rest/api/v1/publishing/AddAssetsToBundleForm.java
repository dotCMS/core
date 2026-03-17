package com.dotcms.rest.api.v1.publishing;

import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Form for adding assets to a publishing bundle.
 * Supports referencing an existing bundle by ID or name, or auto-creating a new one.
 *
 * @author hassandotcms
 * @since Mar 2026
 */
@Schema(description = "Form for adding assets to a bundle")
public class AddAssetsToBundleForm extends Validated {

    @Schema(
            description = "Optional bundle ID. If provided and found, assets are added to this bundle. " +
                    "If not found, falls through to bundleName lookup, then auto-creation — does NOT return 404",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private String bundleId;

    @Schema(
            description = "Optional bundle name. Used as fallback when bundleId is not provided or not found. " +
                    "Searches unsent bundles by name (case-insensitive). If no match, a new bundle is created with this name",
            example = "My Content Bundle"
    )
    private String bundleName;

    @Schema(
            description = "List of asset identifiers to add to the bundle",
            example = "[\"asset-123\", \"asset-456\"]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<String> assetIds;

    /**
     * Default constructor for Jackson deserialization.
     */
    public AddAssetsToBundleForm() {
    }

    /**
     * Constructor with all fields for testing.
     *
     * @param bundleId   Optional existing bundle ID
     * @param bundleName Optional bundle name for lookup or creation
     * @param assetIds   Asset identifiers to add
     */
    @JsonCreator
    public AddAssetsToBundleForm(
            @JsonProperty("bundleId") final String bundleId,
            @JsonProperty("bundleName") final String bundleName,
            @JsonProperty("assetIds") final List<String> assetIds) {
        this.bundleId = bundleId;
        this.bundleName = bundleName;
        this.assetIds = assetIds;
    }

    @Override
    public void checkValid() {
        super.checkValid();

        if (!UtilMethods.isSet(assetIds) || assetIds.isEmpty()) {
            throw new BadRequestException("assetIds must not be null or empty");
        }
    }

    public String getBundleId() {
        return bundleId;
    }

    public void setBundleId(final String bundleId) {
        this.bundleId = bundleId;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(final String bundleName) {
        this.bundleName = bundleName;
    }

    public List<String> getAssetIds() {
        return assetIds;
    }

    public void setAssetIds(final List<String> assetIds) {
        this.assetIds = assetIds;
    }

}

package com.dotcms.experiments.model;

import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;

import com.dotcms.experiments.business.ConfigExperimentUtil;
import com.dotcms.experiments.model.RunningIds.RunningId;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.business.Ruleable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vavr.control.Try;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;


/**
 * Immutable implementation of an Experiment.
 * <p>
 * Experiments are a way to test changes to HTML Pages by creating new versions of a Page
 * and then get a report on which page performed better according to the decided goals.
 * <p>
 * The Experiment can be started now or scheduled to start and finish according to given dates
 * <p>
 *
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@JsonDeserialize(as = Experiment.class)
@JsonSerialize(as = Experiment.class)
@Value.Immutable
public interface AbstractExperiment extends Serializable, ManifestItem, Ruleable {
    @JsonProperty("name")
    String name();

    @JsonProperty("description")
    Optional<String> description();

    @JsonProperty("id")
    Optional<String> id();

    @JsonProperty("status")
    @Value.Default
    default Status status() {
        return Status.DRAFT;
    }

    @JsonProperty("trafficProportion")
    @Value.Default
    default TrafficProportion trafficProportion() {
        return TrafficProportion.builder().build();
    }

    @JsonProperty("scheduling")
    Optional<Scheduling> scheduling();

    @JsonProperty("trafficAllocation")
    @Value.Default
    default float trafficAllocation() {
        return 100;
    }

    @JsonProperty("creationDate")
    @Value.Default
    default Instant creationDate() {
        return Instant.now();
    }

    @JsonProperty("modDate")
    @Value.Default
    default Instant modDate() {
        return Instant.now();
    }

    @JsonProperty("pageId")
    String pageId();

    @JsonProperty("createdBy")
    String createdBy();

    /**
     * The display name of the user behind {@link #createdBy()}, resolved when the Experiment is
     * serialized and never stored, so a creator who renames themselves is reported under the new
     * name. Reports {@code "System"} for the system user and {@code "unknown"} when the creator
     * cannot be resolved or has no name set, matching the labels the Content Drive folder view
     * already uses — the value is never null, absent or empty. See
     * {@link ExperimentCreatorNameResolver}.
     *
     * <p>Deliberately a plain {@code default} method rather than a {@code @Value.Derived} or
     * {@code @Value.Lazy} attribute: those are computed at construction or memoized per instance,
     * which would resolve a user on every Experiment built from the database — including on the
     * page-render and push-publish paths, which never serialize the object — and would let a
     * memoized name outlive a rename inside the running-experiments cache. As an ordinary default
     * method it costs nothing until something serializes the Experiment.
     *
     * <p>{@code READ_ONLY} is load-bearing: the generated {@code Experiment.Json} delegate binds
     * settable attributes only, so without it a payload carrying this field would fail to
     * deserialize as an unknown property.
     */
    @JsonProperty(value = "createdByUserName", access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Display name of the user who created the experiment. Reports \"System\" "
            + "for the system user and \"unknown\" when the user cannot be resolved or has no name "
            + "set, so the value is never empty.",
            example = "Admin User")
    default String createdByUserName() {
        return ExperimentCreatorNameResolver.INSTANCE.resolve(createdBy());
    }

    @JsonProperty("lastModifiedBy")
    String lastModifiedBy();

    @JsonProperty("goals")
    Optional<Goals> goals();

    @JsonProperty("targetingConditions")
    Optional<List<TargetingCondition>> targetingConditions();

    @JsonProperty("lookBackWindowExpireTime")
    @Value.Default
    default long lookBackWindowExpireTime() {
        return ConfigExperimentUtil.INSTANCE.lookBackWindowDefaultExpireTime();
    }

    // Beginning Permissionable methods

    @Value.Derived
    @JsonIgnore
    default String getIdentifier() {
        return id().orElse("");
    }

    @Value.Derived
    @JsonIgnore
    default String getPermissionId() {
        return id().orElse("");
    }

    @Value.Derived
    @JsonIgnore
    default String getOwner() {
        return createdBy();
    }

    @Value.Derived
    default void setOwner(String owner){

    }
    @Value.Derived
    @JsonIgnore
    default List<PermissionSummary> acceptedPermissions () {
        return Collections.emptyList();
    }

    @Value.Derived
    @JsonIgnore
    default List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
        return Collections.emptyList();
    }

    @Value.Derived
    @Nullable
    @JsonIgnore
    default Permissionable getParentPermissionable() {
        return Try.of(()->APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(pageId(),
                        DEFAULT_VARIANT.name(), true))
                .getOrElse(() -> null);
    }

    @Value.Derived
    @JsonIgnore
    default String getPermissionType() {
        return this.getClass().getCanonicalName();
    }

    @Value.Derived
    @JsonIgnore
    default boolean isParentPermissionable() {
        return false;
    }

    @Value.Derived
    @Override
    @JsonIgnore
    default ManifestInfo getManifestInfo() {
        return new ManifestInfoBuilder()
                .objectType(PusheableAsset.EXPERIMENT.getType())
                .id(this.id().orElse(null))
                .title(this.name())
                .build();
    }


    @Value.Default
    default RunningIds runningIds() {
        return new RunningIds();
    }

    enum Status {
        RUNNING,
        SCHEDULED,
        ENDED,
        DRAFT,
        ARCHIVED
    }
}

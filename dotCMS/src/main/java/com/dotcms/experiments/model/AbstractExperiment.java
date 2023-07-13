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
import io.vavr.control.Try;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
    default String getIdentifier() {
        return id().orElse("");
    }

    @Value.Derived
    default String getPermissionId() {
        return id().orElse("");
    }

    @Value.Derived
    default String getOwner() {
        return createdBy();
    }

    @Value.Derived
    default void setOwner(String owner){

    }
    @Value.Derived
    default List<PermissionSummary> acceptedPermissions () {
        return Collections.emptyList();
    }

    @Value.Derived
    default List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
        return Collections.emptyList();
    }

    @Value.Derived
    @JsonIgnore
    default Permissionable getParentPermissionable() {
        return Try.of(()->APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(pageId(),
                        DEFAULT_VARIANT.name()))
                .getOrElseThrow((e)->{
                    throw new DotStateException(e.getMessage() + ". Page ID:" + pageId(), e);
                });
    }

    @Value.Derived
    default String getPermissionType() {
        return this.getClass().getCanonicalName();
    }

    @Value.Derived
    default boolean isParentPermissionable() {
        return false;
    }

    @Value.Derived
    @Override
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

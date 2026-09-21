package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.model.Experiment;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST view of an {@link Experiment}: the experiment exactly as it is persisted, plus the display
 * name of the user who created it.
 *
 * <p>The derivation lives here rather than on the model on purpose. {@link Experiment} is a data
 * object, and anything computed on it is computed everywhere it is serialized — which is not only
 * REST. Push-publish bundling and starter export serialize the whole Experiment too, and their
 * receiver reads it back through a mapper that rejects unknown properties and rolls back the entire
 * bundle when it finds one. Keeping the name in the view means only the endpoint response carries
 * it, the model stays a plain POJO, and no other serialization path pays a user lookup.
 *
 * <p>The experiment is {@link JsonUnwrapped}, so the payload is the Experiment's own fields with
 * {@code createdByUserName} beside them rather than nested — the wire contract consumers see is a
 * flat object, unchanged except for the added field.
 */
public class ExperimentView {

    private final Experiment experiment;

    /**
     * Wraps an Experiment for a REST response.
     *
     * @param experiment the experiment to publish
     * @return a view that serializes the experiment plus its creator's display name
     */
    public static ExperimentView of(final Experiment experiment) {
        return new ExperimentView(experiment);
    }

    private ExperimentView(final Experiment experiment) {
        this.experiment = experiment;
    }

    @JsonUnwrapped
    public Experiment getExperiment() {
        return this.experiment;
    }

    /**
     * The display name of the user behind the experiment's {@code createdBy}, resolved while the
     * response is written and never stored, so a creator who renames themselves is reported under
     * the new name. Reports {@code "System"} for the system user and {@code "unknown"} when the
     * creator cannot be resolved or has no name set, matching the labels the Content Drive folder
     * view already uses. Never null, never blank.
     *
     * @return the creator's display name
     */
    @JsonProperty("createdByUserName")
    @Schema(description = "Display name of the user who created the experiment. Reports \"System\" "
            + "for the system user and \"unknown\" when the user cannot be resolved or has no name "
            + "set, so the value is never empty.",
            example = "Admin User")
    public String getCreatedByUserName() {
        return ExperimentCreatorNameResolver.INSTANCE.resolve(this.experiment.createdBy());
    }
}

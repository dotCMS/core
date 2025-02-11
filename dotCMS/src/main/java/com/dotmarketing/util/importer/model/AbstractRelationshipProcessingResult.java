package com.dotmarketing.util.importer.model;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

/**
 * Immutable data structure representing relationship processing results
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = RelationshipProcessingResult.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractRelationshipProcessingResult extends Serializable {

    /**
     * @return Parent-only relationship records
     */
    Map<Relationship, List<Contentlet>> parentOnlyRelationships();

    /**
     * @return Child-only relationship records
     */
    Map<Relationship, List<Contentlet>> childOnlyRelationships();

    /**
     * @return General relationship records
     */
    Map<Relationship, List<Contentlet>> relationships();

    /**
     * @return Any validation messages generated during processing
     */
    List<ValidationMessage> messages();

}

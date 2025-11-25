package com.dotcms.contenttype.model.field;

import com.dotcms.model.views.CommonViews;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

/**
 * The RelationshipCardinalityViewBasedSerializer class is a JsonSerializer implementation that
 * serializes a RelationshipCardinality enum value based on the active view of the
 * SerializerProvider. It delegates the serialization to different serializers based on the active
 * view. The serializers used are RelationshipCardinalityNameSerializer and
 * RelationshipCardinalityOrdinalSerializer. If no view is active, it uses the default serializer,
 * RelationshipCardinalityNameSerializer.
 *
 * @see JsonSerializer
 * @see RelationshipCardinalityNameSerializer
 * @see RelationshipCardinalityOrdinalSerializer
 */
public class RelationshipCardinalityViewBasedSerializer extends
        JsonSerializer<RelationshipCardinality> {

    private static final JsonSerializer<RelationshipCardinality> nameSerializer =
            new RelationshipCardinalityNameSerializer();
    private static final JsonSerializer<RelationshipCardinality> ordinalSerializer =
            new RelationshipCardinalityOrdinalSerializer();
    private static final JsonSerializer<RelationshipCardinality> defaultSerializer = nameSerializer;

    private static final Class<?> INTERNAL_VIEW = CommonViews.ContentTypeInternalView.class;
    private static final Class<?> EXTERNAL_VIEW = CommonViews.ContentTypeExternalView.class;

    @Override
    public void serialize(RelationshipCardinality value, JsonGenerator gen,
            SerializerProvider serializer) throws IOException {

        Class<?> activeView = serializer.getActiveView();

        // Check the active view and delegate to the appropriate serializer
        if (activeView != null) {

            if (activeView.equals(INTERNAL_VIEW)) {
                nameSerializer.serialize(value, gen, serializer);
            } else if (activeView.equals(EXTERNAL_VIEW)) {
                ordinalSerializer.serialize(value, gen, serializer);
            } else {
                throw new IllegalStateException("Unexpected value: " + activeView.getName());
            }
        } else {
            // Fallback behavior when no View is active, use the ordinal serializer
            defaultSerializer.serialize(value, gen, serializer);
        }
    }
}
package com.dotcms.graphql.business;

import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.RowField;
import com.dotcms.util.DotPreconditions;

/**
 * This class represents a Factory of {@link GraphQLFieldGenerator}
 */
public class GraphQLFieldGeneratorFactory {

    /**
     * Returns a {@link GraphQLFieldGenerator} based on the type of the provided {@link Field}
     * @param field field whose proper generator is requested
     * @return the field generator
     */
    public GraphQLFieldGenerator getGenerator(final Field field) {
        DotPreconditions.checkArgument(!(field instanceof RowField)
                && !(field instanceof ColumnField), "Can't process this type of Field",
                IllegalArgumentException.class);

        if(field instanceof RelationshipField) {
            return new RelationshipFieldGenerator();
        } else {
            return new RegularContentFieldGenerator();
        }
    }
}

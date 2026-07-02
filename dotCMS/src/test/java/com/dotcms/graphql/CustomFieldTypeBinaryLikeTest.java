package com.dotcms.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graphql.Scalars;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for issue #34540: unify GraphQL queries across Binary, File, and Image fields.
 *
 * <p>Today, a {@code BinaryField} maps to the {@code DotBinary} GraphQL type and a
 * {@code FileField}/{@code ImageField} maps to the {@code DotFileasset} GraphQL type. The two
 * types expose different sub-fields, so a client cannot query them uniformly. This test
 * asserts the new contract: both concrete types implement a shared {@code DotBinaryLike}
 * interface and expose the same common set of binary properties at the top level.
 */
class CustomFieldTypeBinaryLikeTest {

    private static final String INTERFACE_NAME = "DotBinaryLike";

    /** The 12 properties the shared interface must expose on every binary-bearing type. */
    private static final List<String> COMMON_BINARY_FIELDS = Arrays.asList(
            "name", "title", "size", "mime", "versionPath", "idPath", "path",
            "sha256", "isImage", "width", "height", "modDate");

    @Test
    void dotBinary_implementsDotBinaryLikeInterface() {
        final GraphQLObjectType dotBinary = CustomFieldType.BINARY.getType();
        assertTrue(
                dotBinary.getInterfaces().stream()
                        .anyMatch(i -> INTERFACE_NAME.equals(((GraphQLInterfaceType) i).getName())),
                "DotBinary must implement the " + INTERFACE_NAME + " interface");
    }

    @Test
    void dotFileasset_implementsDotBinaryLikeInterface() {
        final GraphQLObjectType dotFileasset = CustomFieldType.FILEASSET.getType();
        assertTrue(
                dotFileasset.getInterfaces().stream()
                        .anyMatch(i -> INTERFACE_NAME.equals(((GraphQLInterfaceType) i).getName())),
                "DotFileasset must implement the " + INTERFACE_NAME + " interface");
    }

    @Test
    void dotBinary_exposesAllCommonBinaryFields() {
        final GraphQLObjectType dotBinary = CustomFieldType.BINARY.getType();
        for (final String field : COMMON_BINARY_FIELDS) {
            assertNotNull(dotBinary.getFieldDefinition(field),
                    "DotBinary must expose common field: " + field);
        }
    }

    @Test
    void dotFileasset_exposesAllCommonBinaryFields() {
        final GraphQLObjectType dotFileasset = CustomFieldType.FILEASSET.getType();
        for (final String field : COMMON_BINARY_FIELDS) {
            assertNotNull(dotFileasset.getFieldDefinition(field),
                    "DotFileasset must expose common field: " + field);
        }
    }

    @Test
    void commonBinaryFields_useConsistentScalarTypes() {
        final GraphQLObjectType dotBinary = CustomFieldType.BINARY.getType();
        final GraphQLObjectType dotFileasset = CustomFieldType.FILEASSET.getType();
        for (final String field : COMMON_BINARY_FIELDS) {
            final GraphQLFieldDefinition binaryDef = dotBinary.getFieldDefinition(field);
            final GraphQLFieldDefinition fileAssetDef = dotFileasset.getFieldDefinition(field);
            assertEquals(binaryDef.getType(), fileAssetDef.getType(),
                    "Scalar type for common field '" + field
                            + "' must be consistent across DotBinary and DotFileasset");
        }
    }

    @Test
    void commonBinaryFields_useExpectedScalarTypes() {
        final GraphQLObjectType dotBinary = CustomFieldType.BINARY.getType();
        assertScalar(dotBinary, "name", Scalars.GraphQLString);
        assertScalar(dotBinary, "title", Scalars.GraphQLString);
        assertScalar(dotBinary, "size", ExtendedScalars.GraphQLLong);
        assertScalar(dotBinary, "mime", Scalars.GraphQLString);
        assertScalar(dotBinary, "versionPath", Scalars.GraphQLString);
        assertScalar(dotBinary, "idPath", Scalars.GraphQLString);
        assertScalar(dotBinary, "path", Scalars.GraphQLString);
        assertScalar(dotBinary, "sha256", Scalars.GraphQLString);
        assertScalar(dotBinary, "isImage", Scalars.GraphQLBoolean);
        assertScalar(dotBinary, "width", ExtendedScalars.GraphQLLong);
        assertScalar(dotBinary, "height", ExtendedScalars.GraphQLLong);
        assertScalar(dotBinary, "modDate", ExtendedScalars.GraphQLLong);
    }

    @Test
    void dotBinary_keepsTypeSpecificField_focalPoint() {
        final GraphQLObjectType dotBinary = CustomFieldType.BINARY.getType();
        assertNotNull(dotBinary.getFieldDefinition("focalPoint"),
                "focalPoint remains a DotBinary-specific field");
    }

    @Test
    void dotFileasset_keepsBackwardCompatibleFields() {
        final GraphQLObjectType dotFileasset = CustomFieldType.FILEASSET.getType();
        // These were the existing fields before #34540; they must still be present so
        // existing queries keep working.
        assertNotNull(dotFileasset.getFieldDefinition("fileName"));
        assertNotNull(dotFileasset.getFieldDefinition("description"));
        assertNotNull(dotFileasset.getFieldDefinition("fileAsset"));
        assertNotNull(dotFileasset.getFieldDefinition("metaData"));
        assertNotNull(dotFileasset.getFieldDefinition("showOnMenu"));
        assertNotNull(dotFileasset.getFieldDefinition("sortOrder"));
    }

    private static void assertScalar(final GraphQLObjectType type, final String field,
            final GraphQLOutputType expected) {
        final GraphQLFieldDefinition def = type.getFieldDefinition(field);
        assertNotNull(def, "Missing field: " + field);
        assertEquals(expected, def.getType(),
                "Field '" + field + "' on " + type.getName() + " has unexpected scalar type");
    }
}

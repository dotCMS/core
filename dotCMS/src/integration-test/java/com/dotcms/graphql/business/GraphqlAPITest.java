package com.dotcms.graphql.business;

import static com.dotcms.graphql.InterfaceType.CONTENT_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.FILE_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.FORM_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.KEY_VALUE_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.PAGE_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.PERSONA_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.VANITY_URL_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.WIDGET_INTERFACE_NAME;
import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY;
import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_ONE;
import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableCategoryField;
import com.dotcms.contenttype.model.field.ImmutableCheckboxField;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.ImmutableCustomField;
import com.dotcms.contenttype.model.field.ImmutableDateField;
import com.dotcms.contenttype.model.field.ImmutableFileField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableImageField;
import com.dotcms.contenttype.model.field.ImmutableKeyValueField;
import com.dotcms.contenttype.model.field.ImmutableMultiSelectField;
import com.dotcms.contenttype.model.field.ImmutableRadioField;
import com.dotcms.contenttype.model.field.ImmutableSelectField;
import com.dotcms.contenttype.model.field.ImmutableTagField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.ImmutableTimeField;
import com.dotcms.contenttype.model.field.ImmutableWysiwygField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.function.BiFunction;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class GraphqlAPITest {

    private static CustomRandom random = new CustomRandom();

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    @DataProvider
    public static Object[] typeTestCases() {

        return new TypeTestCase[]{

                // TODO commented cases pending for researching. Do not remove them

                // CREATE TYPE CASES
                new TypeTestCase.Builder()
                        .operations(Collections.singletonList(GraphqlAPITest::createType))
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
//                        .expectedGraphQLInterfaceToInherit(CONTENT_INTERFACE_NAME)
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
//                                        GraphqlAPITest::assertTypeInheritFromInterface
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .operations(Collections.singletonList(GraphqlAPITest::createType))
                        .baseType(BaseContentType.WIDGET)
                        .contentTypeName("newWidgetContentType" + random.nextPositive())
//                        .expectedGraphQLInterfaceToInherit(WIDGET_INTERFACE_NAME)
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
//                                        GraphqlAPITest::assertTypeInheritFromInterface
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .operations(Collections.singletonList(GraphqlAPITest::createType))
                        .baseType(BaseContentType.FORM)
//                        .expectedGraphQLInterfaceToInherit(FORM_INTERFACE_NAME)
                        .contentTypeName("newFormContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
//                                        GraphqlAPITest::assertTypeInheritFromInterface
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .operations(Collections.singletonList(GraphqlAPITest::createType))
                        .baseType(BaseContentType.FILEASSET)
//                        .expectedGraphQLInterfaceToInherit(FILE_INTERFACE_NAME)
                        .contentTypeName("newFileContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
//                                        GraphqlAPITest::assertTypeCreated,
                                        GraphqlAPITest::assertTypeInheritFromInterface
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .operations(Collections.singletonList(GraphqlAPITest::createType))
                        .baseType(BaseContentType.HTMLPAGE)
//                        .expectedGraphQLInterfaceToInherit(PAGE_INTERFACE_NAME)
                        .contentTypeName("newPageContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
//                                        GraphqlAPITest::assertTypeInheritFromInterface
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .operations(Collections.singletonList(GraphqlAPITest::createType))
                        .baseType(BaseContentType.PERSONA)
//                        .expectedGraphQLInterfaceToInherit(PERSONA_INTERFACE_NAME)
                        .contentTypeName("newPersonaContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
//                                        GraphqlAPITest::assertTypeCreated
                                        GraphqlAPITest::assertTypeInheritFromInterface
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .operations(Collections.singletonList(GraphqlAPITest::createType))
                        .baseType(BaseContentType.VANITY_URL)
//                        .expectedGraphQLInterfaceToInherit(VANITY_URL_INTERFACE_NAME)
                        .contentTypeName("newVanityURLContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
//                                        GraphqlAPITest::assertTypeInheritFromInterface
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .operations(Collections.singletonList(GraphqlAPITest::createType))
                        .baseType(BaseContentType.KEY_VALUE)
//                        .expectedGraphQLInterfaceToInherit(KEY_VALUE_INTERFACE_NAME)
                        .contentTypeName("newKeyValueContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
//                                        GraphqlAPITest::assertTypeInheritFromInterface
                                )
                        )
                        .build(),

                // DELETE TYPE CASES

                new TypeTestCase.Builder()
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .expectedGraphQLInterfaceToInherit(CONTENT_INTERFACE_NAME)
                        .assertions(Collections.singletonList(GraphqlAPITest::assertTypeDeleted))
                        .build(),

                new TypeTestCase.Builder()
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.WIDGET)
                        .contentTypeName("newWidgetContentType" + random.nextPositive())
                        .expectedGraphQLInterfaceToInherit(WIDGET_INTERFACE_NAME)
                        .assertions(Collections.singletonList(GraphqlAPITest::assertTypeDeleted))
                        .build(),
                new TypeTestCase.Builder()
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.FORM)
                        .expectedGraphQLInterfaceToInherit(FORM_INTERFACE_NAME)
                        .contentTypeName("newFormContentType" + random.nextPositive())
                        .assertions(Collections.singletonList(GraphqlAPITest::assertTypeDeleted))
                        .build(),
                new TypeTestCase.Builder()
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.FILEASSET)
                        .expectedGraphQLInterfaceToInherit(FILE_INTERFACE_NAME)
                        .contentTypeName("newFileContentType" + random.nextPositive())
                        .assertions(Collections.singletonList(GraphqlAPITest::assertTypeDeleted))
                        .build(),
                new TypeTestCase.Builder()
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.HTMLPAGE)
                        .expectedGraphQLInterfaceToInherit(PAGE_INTERFACE_NAME)
                        .contentTypeName("newPageContentType" + random.nextPositive())
                        .assertions(Collections.singletonList(GraphqlAPITest::assertTypeDeleted))
                        .build(),
                new TypeTestCase.Builder()
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.PERSONA)
                        .expectedGraphQLInterfaceToInherit(PERSONA_INTERFACE_NAME)
                        .contentTypeName("newPersonaContentType" + random.nextPositive())
                        .assertions(Collections.singletonList(GraphqlAPITest::assertTypeDeleted))
                        .build(),
                new TypeTestCase.Builder()
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.VANITY_URL)
                        .expectedGraphQLInterfaceToInherit(VANITY_URL_INTERFACE_NAME)
                        .contentTypeName("newVanityURLContentType" + random.nextPositive())
                        .assertions(Collections.singletonList(GraphqlAPITest::assertTypeDeleted))
                        .build(),
                new TypeTestCase.Builder()
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.KEY_VALUE)
                        .expectedGraphQLInterfaceToInherit(KEY_VALUE_INTERFACE_NAME)
                        .contentTypeName("newKeyValueContentType" + random.nextPositive())
                        .assertions(Collections.singletonList(GraphqlAPITest::assertTypeDeleted))
                        .build(),


        };
    }


    @DataProvider
    public static Object[] fieldTestCases() {

        return new TypeTestCase[]{

                // test each field type with required FALSE
                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableBinaryField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableCategoryField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableImageField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableFileField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableKeyValueField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableHostFolderField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableCheckboxField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableConstantField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableCustomField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableDateField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableMultiSelectField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableRadioField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableSelectField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableTagField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableTextAreaField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableTextField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableTimeField.class)
                        .setFieldRequired(false)
                        .build(),

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableWysiwygField.class)
                        .setFieldRequired(false)
                        .build(),

                // test field with required TRUE

                new TypeTestCase.Builder()
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .setFieldVarName("testFieldVar" + random.nextPositive())
                        .setFieldType(ImmutableBinaryField.class)
                        .setFieldRequired(true)
                        .build(),


        };
    }


    @UseDataProvider("typeTestCases")
    @Test
    public void testGetSchema_ContentTypeOperations(final TypeTestCase testCase)
            throws DotDataException, DotSecurityException {

        ContentType contentType = null;

        // contentType gets assigned to the return of the last operation
        for (BiFunction<String, BaseContentType, ContentType> operation : testCase
                .getOperations()) {
            contentType = operation.apply(testCase.getContentTypeName(), testCase.getBaseType());
        }

        final String contentTypeVar = contentType != null ? contentType.variable() : null;

        final GraphqlAPI api = APILocator.getGraphqlAPI();
        final GraphQLSchema schema = api.getSchema();

        final TypeTestCase.AssertionParams assertionParams =
                new TypeTestCase.AssertionParams(schema, contentTypeVar, testCase.expectedGraphQLInterfaceToInherit);

        testCase.assertions.forEach((assertion) -> assertion.accept(assertionParams));

    }


    @UseDataProvider("fieldTestCases")
    @Test
    public void testGetSchema_FieldOperations(final TypeTestCase testCase) throws DotDataException,
            DotSecurityException {

        // create type
        ContentType contentType = createType(testCase.contentTypeName, testCase.baseType);
        final Field field = createField(contentType, testCase.fieldVarName,
                testCase.fieldType, testCase.fieldRequired);

        final GraphqlAPI api = APILocator.getGraphqlAPI();
        final GraphQLSchema schema = api.getSchema();

        final GraphQLFieldDefinition fieldDefinition =
                schema.getObjectType(testCase.contentTypeName).getFieldDefinition(testCase.fieldVarName);

        GraphQLOutputType expectedType = api.getGraphqlTypeForFieldClass(
                (Class<? extends Field>) testCase.fieldType.getSuperclass(), field);

        final GraphQLOutputType graphQLFieldType = fieldDefinition.getType();

        assertNotNull(expectedType);
        assertNotNull(graphQLFieldType);

        if (testCase.fieldRequired) {
            Assert.assertEquals("Type of GraphQL Field should match type expected",
                    new GraphQLNonNull(expectedType)
                    , graphQLFieldType);
        } else {
            Assert.assertEquals("Type of GraphQL Field should match type expected", expectedType
                    , graphQLFieldType);
        }

    }

    @Test
    public void testGetSchema_WhenFieldDeleted_ShouldNotBeAvailableInSchema() throws DotDataException,
            DotSecurityException {

        // create type
        final String contentTypeName = "testType" + random.nextPositive();
        final String fieldVarName = "testFieldVar" + random.nextPositive();
        ContentType contentType = createType(contentTypeName, BaseContentType.CONTENT);
        final Field field = createField(contentType, fieldVarName,
                ImmutableTextField.class, false);

        final GraphqlAPI api = APILocator.getGraphqlAPI();
        final GraphQLSchema schema = api.getSchema();

        final GraphQLFieldDefinition fieldDefinition =
                schema.getObjectType(contentTypeName).getFieldDefinition(fieldVarName);

        final GraphQLOutputType graphQLFieldType = fieldDefinition.getType();
        assertNotNull(graphQLFieldType);

        // let's now delete the field
        APILocator.getContentTypeFieldAPI().delete(field);

        // after deletion the field should not be available in the schema
        final GraphQLSchema schemaReloaded = api.getSchema();

        final GraphQLFieldDefinition reloadedFieldDefinition =
                schemaReloaded.getObjectType(contentTypeName).getFieldDefinition(fieldVarName);

        assertNull(reloadedFieldDefinition);

    }


    @DataProvider
    public static Object[] relationshipFieldTestCases() {
        return RELATIONSHIP_CARDINALITY.values();
    }

    @UseDataProvider("relationshipFieldTestCases")
    @Test
    public void testGetSchema_BothSidedRelationshipFields(final RELATIONSHIP_CARDINALITY cardinality) throws DotDataException,
            DotSecurityException {

        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            parentContentType = createAndSaveSimpleContentType("parentContentTypeGraphQL");
            childContentType = createAndSaveSimpleContentType("childContentTypeGraphQL");

            final Field relFieldFromParentToChild = createAndSaveRelationshipField(
                    "newRelGraphQL",
                    parentContentType.id(), childContentType.variable(),
                    String.valueOf(cardinality.ordinal()));

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + relFieldFromParentToChild.variable();

            //Adding a RelationshipField to the child

            final Field relFieldFromChildToParent = createAndSaveRelationshipField("otherSideNewRelGraphQL",
                    childContentType.id(), fullFieldVar, String.valueOf(cardinality.ordinal()));

            final GraphQLSchema schema = APILocator.getGraphqlAPI().getSchema();

            final GraphQLFieldDefinition fieldDefinitionFromParentToChild =
                    schema.getObjectType(parentContentType.variable())
                            .getFieldDefinition(relFieldFromParentToChild.variable());

            final GraphQLOutputType outputTypeFromParentToChild = fieldDefinitionFromParentToChild.getType();

            if(isOneEndingCardinality(cardinality)) {
                assertFalse(outputTypeFromParentToChild instanceof GraphQLList);
                assertEquals(childContentType.variable(), outputTypeFromParentToChild.getName());
            } else {
                assertTrue(outputTypeFromParentToChild instanceof GraphQLList);
                assertEquals(childContentType.variable(), ((GraphQLList)outputTypeFromParentToChild).getWrappedType().getName());
            }

            final GraphQLFieldDefinition fieldDefinitionFromChildToParent =
                    schema.getObjectType(childContentType.variable())
                            .getFieldDefinition(relFieldFromChildToParent.variable());

            final GraphQLOutputType outputTypeFromChildToParent = fieldDefinitionFromChildToParent.getType();

            if(isManyStartingCardinality(cardinality)) {
                assertTrue(outputTypeFromChildToParent instanceof GraphQLList);
                assertEquals(parentContentType.variable(), ((GraphQLList)outputTypeFromChildToParent).getWrappedType().getName());
            } else {
                assertFalse(outputTypeFromChildToParent instanceof GraphQLList);
                assertEquals(parentContentType.variable(), outputTypeFromChildToParent.getName());
            }


        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(childContentType);
            }
        }

    }

    private ContentType createAndSaveSimpleContentType(final String name) throws DotSecurityException, DotDataException {
        return APILocator.getContentTypeAPI(APILocator.systemUser())
                .save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                        FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                        .owner(APILocator.systemUser().getUserId()).build());
    }

    private Field createAndSaveRelationshipField(final String relationshipName, final String parentTypeId,
            final String childTypeVar, final String cardinality)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(RelationshipField.class).name(relationshipName)
                .contentTypeId(parentTypeId).values(cardinality)
                .relationType(childTypeVar).build();

        //One side of the relationship is set parentContentType --> childContentType
        return APILocator.getContentTypeFieldAPI().save(field, APILocator.systemUser());
    }

    private static ContentType deleteType(final String typeVariable, final BaseContentType baseType) {
        try {
            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
            final ContentType typeToDelete = contentTypeAPI.find(typeVariable);

            contentTypeAPI.delete(typeToDelete);

            return null;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static ContentType createType(final String typeName, final BaseContentType baseType) {
        try {
            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
            final ContentTypeBuilder contentTypeBuilder = getContentTypeBuilder(baseType);

            final ContentType contentType = contentTypeBuilder.name(typeName).
                    variable(typeName).build();
            return contentTypeAPI.save(contentType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field createField(final ContentType contentType, final String fieldVarName, final Class<? extends Field> fieldType,
            final boolean fieldRequired) {
        try {
            final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();
            final FieldBuilder fieldBuilder = getFieldBuilder(fieldType);
            final Field field =  fieldBuilder.contentTypeId(contentType.id())
                    .name(fieldVarName).variable(fieldVarName)
                    .required(fieldRequired).build();
            return fieldAPI.save(field, APILocator.systemUser());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ContentTypeBuilder getContentTypeBuilder(BaseContentType baseType)
            throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final String packageName = baseType.immutableClass().getPackage().getName();
        final Class
                immutableClass =
                Class.forName(
                        packageName + ".Immutable" + baseType.immutableClass().getSimpleName());

        return (ContentTypeBuilder) immutableClass.getMethod("builder").invoke(null);
    }

    private static FieldBuilder getFieldBuilder(final Class<? extends Field> fieldType)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (FieldBuilder) fieldType.getMethod("builder").invoke(null);
    }

    private static void assertTypeCreated(final TypeTestCase.AssertionParams assertionParams) {
        assertNotNull("GraphQL Type: "+assertionParams.typeName+" exists",
                assertionParams.schema.getType(assertionParams.typeName));
    }

    private static void assertTypeDeleted(final TypeTestCase.AssertionParams assertionParams) {
        Assert.assertNull("GraphQL Type: "+assertionParams.typeName+" does NOT exist",
                assertionParams.schema.getType(assertionParams.typeName));
    }

    private static void assertTypeInheritFromInterface(final TypeTestCase.AssertionParams assertionParams) {
        final GraphQLObjectType graphQLObjectType = assertionParams.schema.getObjectType(assertionParams.typeName);

        if(assertionParams.expectedInterfaceName!=null) {
            assertTrue("GraphQL Type:" + assertionParams.typeName + " inherits from:"
                            + assertionParams.expectedInterfaceName,
                    graphQLObjectType.getInterfaces().stream().anyMatch(
                            (interfaceType) -> interfaceType.getName().toLowerCase()
                                    .equals(assertionParams.expectedInterfaceName
                                            .toLowerCase())
                    ));
        }
    }

    private boolean isOneEndingCardinality(final RELATIONSHIP_CARDINALITY cardinality) {
        return cardinality.equals(ONE_TO_ONE) || cardinality.equals(MANY_TO_ONE);
    }

    private boolean isManyStartingCardinality(final RELATIONSHIP_CARDINALITY cardinality) {
        return cardinality.equals(MANY_TO_ONE) || cardinality.equals(MANY_TO_MANY);
    }

    static class CustomRandom extends Random {

        int nextPositive() {
            return next(Integer.SIZE - 1);
        }
    }

}
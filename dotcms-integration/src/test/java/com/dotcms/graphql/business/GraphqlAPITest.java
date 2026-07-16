package com.dotcms.graphql.business;

import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_DESCRIPTION_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_FILEASSET_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_FILE_NAME_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_METADATA_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_SHOW_ON_MENU_FIELD_VAR;
import static com.dotcms.contenttype.model.type.FileAssetContentType.FILEASSET_SORT_ORDER_FIELD_VAR;
import static com.dotcms.graphql.InterfaceType.CONTENT_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.FILE_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.FORM_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.KEY_VALUE_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.PAGE_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.PERSONA_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.VANITY_URL_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.WIDGET_INTERFACE_NAME;
import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY;
import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_ONE;
import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FileField;
import com.dotcms.contenttype.model.field.ImageField;
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
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.EnterpriseType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.graphql.CustomFieldType;
import com.dotcms.graphql.util.TypeUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
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
import io.vavr.Tuple2;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(DataProviderRunner.class)
public class GraphqlAPITest extends IntegrationTestBase {

    private static CustomRandom random = new CustomRandom();

    private static List<User> users;

    List<User> loadUsers() {
        if(null == users) {
            try {
                users = ImmutableList.of(
                        TestUserUtils.getChrisPublisherUser(),
                        TestUserUtils.getBillIntranetUser(),
                        TestUserUtils.getJaneReviewerUser(),
                        TestUserUtils.getAdminUser(),
                        APILocator.systemUser(),
                        APILocator.getUserAPI().getAnonymousUserNoThrow()
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return users;
    }

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        
        Config.setProperty("GRAPHQL_SCHEMA_DEBOUNCE_DELAY_MILLIS", 0);
        
    }


    @DataProvider(format = "%m: %p[0]")
    public static Object[] typeTestCases() {

        return new TypeTestCase[]{

                // CREATE TYPE CASES
                new TypeTestCase.Builder()
                        .description("Given Content BaseType, Should Create Type")
                        .operations(List.of(GraphqlAPITest::createType))
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given Widget BaseType, Should Create Type")
                        .operations(List.of(GraphqlAPITest::createType))
                        .baseType(BaseContentType.WIDGET)
                        .contentTypeName("newWidgetContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given FORM BaseType, Should Create Type")
                        .operations(List.of(GraphqlAPITest::createType))
                        .baseType(BaseContentType.FORM)
                        .contentTypeName("newFormContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given FILEASSET BaseType, Should Create Type")
                        .operations(List.of(GraphqlAPITest::createType))
                        .baseType(BaseContentType.FILEASSET)
                        .contentTypeName("newFileContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given HTMLPAGE BaseType, Should Create Type")
                        .operations(List.of(GraphqlAPITest::createType))
                        .baseType(BaseContentType.HTMLPAGE)
                        .contentTypeName("newPageContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given PERSONA BaseType, Should Create Type")
                        .operations(List.of(GraphqlAPITest::createType))
                        .baseType(BaseContentType.PERSONA)
                        .contentTypeName("newPersonaContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given VANITY_URL BaseType, Should Create Type")
                        .operations(List.of(GraphqlAPITest::createType))
                        .baseType(BaseContentType.VANITY_URL)
                        .contentTypeName("newVanityURLContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
                                )
                        )
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given KEY_VALUE BaseType, Should Create Type")
                        .operations(List.of(GraphqlAPITest::createType))
                        .baseType(BaseContentType.KEY_VALUE)
                        .contentTypeName("newKeyValueContentType" + random.nextPositive())
                        .assertions(
                                Arrays.asList(
                                        GraphqlAPITest::assertTypeCreated
                                )
                        )
                        .build(),

                // DELETE TYPE CASES

                new TypeTestCase.Builder()
                        .description("Given existing CONTENT BaseType, Should Delete Type")
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.CONTENT)
                        .contentTypeName("newContentContentType" + random.nextPositive())
                        .expectedGraphQLInterfaceToInherit(CONTENT_INTERFACE_NAME)
                        .assertions(List.of(GraphqlAPITest::assertTypeDeleted))
                        .build(),

                new TypeTestCase.Builder()
                        .description("Given existing WIDGET BaseType, Should Delete Type")
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.WIDGET)
                        .contentTypeName("newWidgetContentType" + random.nextPositive())
                        .expectedGraphQLInterfaceToInherit(WIDGET_INTERFACE_NAME)
                        .assertions(List.of(GraphqlAPITest::assertTypeDeleted))
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given existing FORM BaseType, Should Delete Type")
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.FORM)
                        .expectedGraphQLInterfaceToInherit(FORM_INTERFACE_NAME)
                        .contentTypeName("newFormContentType" + random.nextPositive())
                        .assertions(List.of(GraphqlAPITest::assertTypeDeleted))
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given existing FILEASSET BaseType, Should Delete Type")
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.FILEASSET)
                        .expectedGraphQLInterfaceToInherit(FILE_INTERFACE_NAME)
                        .contentTypeName("newFileContentType" + random.nextPositive())
                        .assertions(List.of(GraphqlAPITest::assertTypeDeleted))
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given existing HTMLPAGE BaseType, Should Delete Type")
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.HTMLPAGE)
                        .expectedGraphQLInterfaceToInherit(PAGE_INTERFACE_NAME)
                        .contentTypeName("newPageContentType" + random.nextPositive())
                        .assertions(List.of(GraphqlAPITest::assertTypeDeleted))
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given existing PERSONA BaseType, Should Delete Type")
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.PERSONA)
                        .expectedGraphQLInterfaceToInherit(PERSONA_INTERFACE_NAME)
                        .contentTypeName("newPersonaContentType" + random.nextPositive())
                        .assertions(List.of(GraphqlAPITest::assertTypeDeleted))
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given existing VANITY_URL BaseType, Should Delete Type")
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.VANITY_URL)
                        .expectedGraphQLInterfaceToInherit(VANITY_URL_INTERFACE_NAME)
                        .contentTypeName("newVanityURLContentType" + random.nextPositive())
                        .assertions(List.of(GraphqlAPITest::assertTypeDeleted))
                        .build(),
                new TypeTestCase.Builder()
                        .description("Given existing KEY_VALUE BaseType, Should Delete Type")
                        .operations(
                                Arrays.asList(
                                        GraphqlAPITest::createType,
                                        GraphqlAPITest::deleteType
                                )
                        )
                        .baseType(BaseContentType.KEY_VALUE)
                        .expectedGraphQLInterfaceToInherit(KEY_VALUE_INTERFACE_NAME)
                        .contentTypeName("newKeyValueContentType" + random.nextPositive())
                        .assertions(List.of(GraphqlAPITest::assertTypeDeleted))
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

        // contentType gets assigned to the return of the last operation
        for (BiFunction<String, BaseContentType, ContentType> operation : testCase
                .getOperations()) {
            operation.apply(testCase.getContentTypeName(), testCase.getBaseType());
        }

        final GraphqlAPI api = APILocator.getGraphqlAPI();

        for(User user:loadUsers()) {

            final GraphQLSchema schema = api.getSchema(user);

            final TypeTestCase.AssertionParams assertionParams =
                    new TypeTestCase.AssertionParams(schema, testCase.getContentTypeName(),
                            testCase.expectedGraphQLInterfaceToInherit);

            testCase.assertions.forEach((assertion) -> assertion.accept(assertionParams));
        }
    }

    /**
     * Given Scenario: Generate a graphQL Schema and validate the field definitions.
     * <p>
     * ExpectedResult: Field definitions are not duplicated.
     */
    @Test
    public void testGetSchema_Validate_No_Duplicated_Fields() throws DotDataException {

        final GraphqlAPI api = APILocator.getGraphqlAPI();

        final GraphQLSchema schema = api.getSchema();
        final var fieldDefinitions = schema.getQueryType().getFieldDefinitions();

        var fieldNames = new HashSet<String>();
        for (GraphQLFieldDefinition fieldDefinition : fieldDefinitions) {
            assertFalse(
                    String.format("Found duplicated field in graphQL Scheme [%s]",
                            fieldDefinition.getName()),
                    fieldNames.contains(fieldDefinition.getName().toLowerCase())
            );
            fieldNames.add(fieldDefinition.getName().toLowerCase());
        }
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

        for(User user:loadUsers()) {
            final GraphQLSchema schema = api.getSchema(user);

            final GraphQLFieldDefinition fieldDefinition =
                    schema.getObjectType(testCase.contentTypeName)
                            .getFieldDefinition(testCase.fieldVarName);

            GraphQLOutputType expectedType = ContentAPIGraphQLTypesProvider.INSTANCE
                    .getGraphqlTypeForFieldClass(
                            (Class<? extends Field>) testCase.fieldType.getSuperclass(), field);

            final GraphQLOutputType graphQLFieldType = fieldDefinition.getType();

            assertNotNull(expectedType);
            assertNotNull(graphQLFieldType);

            if (testCase.fieldRequired) {
                Assert.assertEquals("Type of GraphQL Field should match type expected",
                        new GraphQLNonNull(expectedType).toString()
                        , graphQLFieldType.toString());
            } else {
                Assert.assertEquals("Type of GraphQL Field should match type expected", expectedType
                        , graphQLFieldType);
            }
        }
    }

    @Test
    public void testGetSchema_WhenFieldDeleted_ShouldNotBeAvailableInSchema() throws DotDataException {

        for(final User user:loadUsers()) {
            // create type
            final String contentTypeName = "testType" + random.nextPositive();
            final String fieldVarName = "testFieldVar" + random.nextPositive();
            ContentType contentType = createType(contentTypeName, BaseContentType.CONTENT);
            final Field field = createField(contentType, fieldVarName,
                    ImmutableTextField.class, false);

            final GraphqlAPI api = APILocator.getGraphqlAPI();
            final GraphQLSchema schema = api.getSchema(user);

            final GraphQLFieldDefinition fieldDefinition =
                    schema.getObjectType(contentTypeName).getFieldDefinition(fieldVarName);

            final GraphQLOutputType graphQLFieldType = fieldDefinition.getType();
            assertNotNull(graphQLFieldType);

            // let's now delete the field
            APILocator.getContentTypeFieldAPI().delete(field);

            // after deletion the field should not be available in the schema
            final GraphQLSchema schemaReloaded = api.getSchema(user);

            final GraphQLFieldDefinition reloadedFieldDefinition =
                    schemaReloaded.getObjectType(contentTypeName).getFieldDefinition(fieldVarName);

            assertNull(reloadedFieldDefinition);
        }
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

            for (final User user : loadUsers()) {

                final GraphQLSchema schema = APILocator.getGraphqlAPI().getSchema(user);

                final GraphQLFieldDefinition fieldDefinitionFromParentToChild =
                        schema.getObjectType(parentContentType.variable())
                                .getFieldDefinition(relFieldFromParentToChild.variable());

                final GraphQLOutputType outputTypeFromParentToChild = fieldDefinitionFromParentToChild.getType();

                if (isOneEndingCardinality(cardinality)) {
                    assertFalse(outputTypeFromParentToChild instanceof GraphQLList);
                    assertEquals(childContentType.variable(),
                            TypeUtil.getName(outputTypeFromParentToChild));
                } else {
                    assertTrue(outputTypeFromParentToChild instanceof GraphQLList);
                    assertEquals(childContentType.variable(),
                            TypeUtil.getName(((GraphQLList) outputTypeFromParentToChild).getWrappedType()));
                }

                final GraphQLFieldDefinition fieldDefinitionFromChildToParent =
                        schema.getObjectType(childContentType.variable())
                                .getFieldDefinition(relFieldFromChildToParent.variable());

                final GraphQLOutputType outputTypeFromChildToParent = fieldDefinitionFromChildToParent.getType();

                if (isManyStartingCardinality(cardinality)) {
                    assertTrue(outputTypeFromChildToParent instanceof GraphQLList);
                    assertEquals(parentContentType.variable(),
                            TypeUtil.getName(((GraphQLList) outputTypeFromChildToParent).getWrappedType()));
                } else {
                    assertFalse(outputTypeFromChildToParent instanceof GraphQLList);
                    assertEquals(parentContentType.variable(),
                            TypeUtil.getName(outputTypeFromChildToParent));
                }
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

    @Test
    public void testGetSchema_GivenNoEELicense_EnterpriseTypesShouldNotBeAvailableInSchema() throws Exception{

        // filter only Enterprise content types
        List<ContentType> eeTypes = APILocator
                .getContentTypeAPI(APILocator.systemUser()).findAll().stream()
                .filter((type)->type instanceof EnterpriseType).collect(Collectors.toList());

        List<Tuple2<String, BaseContentType>> eeTypesList = eeTypes.stream().map((type)->
                        new Tuple2<>("my"+type.variable(), type.baseType())).collect(Collectors.toList());

        for (Tuple2<String, BaseContentType> testCase : eeTypesList) {
            ContentType customType = null;

            try {
                // create custom persona type. 1=typeName, 2=BaseType
                customType = createType(testCase._1,
                        testCase._2);

            } finally {
                if(customType!=null) {
                    APILocator.getContentTypeAPI(APILocator.systemUser()).delete(customType);
                }
            }
        }
    }

    @DataProvider
    public static List<Object> dataProviderEEBaseTypes() throws Exception {
        // data provider needs stuff to get initialized because of API access
        IntegrationTestInitService.getInstance().init();
        // data provider needs to return List<Object>
        return BaseContentType.getEnterpriseBaseTypes().stream().map(baseType->(Object)baseType)
                .collect(Collectors.toList());
    }

    @Test
    @UseDataProvider("dataProviderEEBaseTypes")
    public void testGetSchema_GivenNoEELicense_EnterpriseBaseTypeCollectionsShouldNOTBeAvailableInSchema(
            final BaseContentType baseType)
            throws Exception{

        APILocator.getGraphqlAPI().invalidateSchema();

    }

    @Test
    @UseDataProvider("dataProviderEEBaseTypes")
    public void testGetSchema_GivenEELicense_EnterpriseBaseTypeCollectionsShouldBeAvailableInSchema(
        final BaseContentType baseType)
            throws Exception{
        APILocator.getGraphqlAPI().invalidateSchema();
        for (final User user : loadUsers()) {
            final GraphQLSchema schema = APILocator.getGraphqlAPI().getSchema(user);
            assertNotNull("BaseType Collection exists: " + baseType.getAlternateName()
                            + "BaseTypeCollection",
                    schema.getQueryType().getFieldDefinition(baseType.getAlternateName()
                            + "BaseTypeCollection"));
        }
    }

    /**
     * This method tests that given a {@link Field} of type {@link com.dotcms.contenttype.model.field.FileField}
     * or {@link com.dotcms.contenttype.model.field.ImageField}, the following GraphQL fields are
     * available to query:
     *
     * {@link FileAssetContentType#FILEASSET_FILE_NAME_FIELD_VAR}
     * {@link FileAssetContentType#FILEASSET_DESCRIPTION_FIELD_VAR}
     * {@link FileAssetContentType#FILEASSET_FILEASSET_FIELD_VAR}
     * {@link FileAssetContentType#FILEASSET_METADATA_FIELD_VAR}
     * {@link FileAssetContentType#FILEASSET_SHOW_ON_MENU_FIELD_VAR}
     * {@link FileAssetContentType#FILEASSET_SORT_ORDER_FIELD_VAR}
     */

    @Test
    public void testAvailableGraphQLFieldsOnImageAndFileFields()
            throws DotDataException, DotSecurityException {
        ContentType contentType = null;
        for (final User user : loadUsers()) {
            try {
                contentType = new ContentTypeDataGen().nextPersisted();
                final Field fileField = new FieldDataGen().contentTypeId(contentType.id())
                        .type(FileField.class).nextPersisted();
                final Field imageField = new FieldDataGen().contentTypeId(contentType.id())
                        .type(ImageField.class).nextPersisted();

                APILocator.getGraphqlAPI().invalidateSchema();

                final GraphQLSchema schema = APILocator.getGraphqlAPI().getSchema(user);

                final GraphQLFieldDefinition fileFieldDefinition = schema
                        .getObjectType(contentType.variable())
                        .getFieldDefinition(fileField.variable());

                final GraphQLFieldDefinition imageFieldDefinition = schema
                        .getObjectType(contentType.variable())
                        .getFieldDefinition(imageField.variable());

                assertEquals(CustomFieldType.FILEASSET.getType(), fileFieldDefinition.getType());

                assertTrue(areFileassetFieldsPresent((GraphQLObjectType) fileFieldDefinition.getType()));
                assertTrue(areFileassetFieldsPresent((GraphQLObjectType) imageFieldDefinition.getType()));

                assertEquals(CustomFieldType.FILEASSET.getType(), imageFieldDefinition.getType());
            } finally {
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType);
            }
        }
    }

    /**
     * Method to rest: {@link GraphqlAPI#getSchema()}
     * Given scenario: A bad relationship field which returns null when trying to get the
     * {@link com.dotmarketing.portlets.structure.model.Relationship} out of it.
     * Expected result: The schema should be able to generate but without that field present
     */
    @Test
    public void testGetSchema_GivenFailuresInRelationshipField_SchemaShouldStillGenerate()
            throws DotDataException, DotSecurityException {
        ContentType contentType = null;
        for (final User user : loadUsers()) {
            try {
                contentType = new ContentTypeDataGen().nextPersisted();

                Field relationshipField = FieldBuilder.builder(RelationshipField.class)
                        .name("relationshipField")
                        .contentTypeId(contentType.id())
                        .values(String.valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                        .relationType(contentType.variable()).build();

                final Field titleField = new FieldDataGen().contentTypeId(contentType.id())
                        .type(TextField.class).nextPersisted();

                APILocator.getGraphqlAPI().invalidateSchema();

                // this mock relationship api will produce errors when generating the rel field
                setMockRelationshipAPI(relationshipField);

                GraphQLSchema schema = APILocator.getGraphqlAPI().getSchema(user);

                final GraphQLFieldDefinition relationshipFieldDefinition = schema
                        .getObjectType(contentType.variable())
                        .getFieldDefinition(relationshipField.variable());

                final GraphQLFieldDefinition titleFieldDefinition = schema
                        .getObjectType(contentType.variable())
                        .getFieldDefinition(titleField.variable());

                assertNull(relationshipFieldDefinition);
                assertNotNull(titleFieldDefinition);
            } finally {
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType);
                // restore normal RelationshipAPI for ContentAPIGraphQLTypesProvider
                ContentAPIGraphQLTypesProvider.INSTANCE.setFieldGeneratorFactory(
                        new GraphQLFieldGeneratorFactory());
            }
        }
    }

    /**
     * Method to rest: {@link GraphqlAPI#getSchema()}
     * Given scenario: empty schema cache
     * Expected result: cache miss, generation of the schema takes place
     */
    @Test
    public void testGetSchema_GivenEmptyCache_SchemaShouldGenerate()
            throws DotDataException {
        GraphqlAPIImpl api =
                Mockito.spy(new GraphqlAPIImpl());

        api.invalidateSchema();
        api.getSchema();
        verify(api, times(1)).generateSchema(APILocator.systemUser());
    }

    /**
     * Method to test: {@link GraphqlAPI#getSchema()}
     * Given scenario: schema in cache
     * Expected result: cache hit, generation of the schema should not happen
     */
    @Test
    public void testGetSchema_GivenFilledCache_SchemaShouldNotGenerate()
            throws DotDataException {
        GraphqlAPIImpl api =
                Mockito.spy(new GraphqlAPIImpl());

        api.invalidateSchema();
        final GraphQLSchema nonCachedSchema = api.getSchema(); // generate schema is called
        final GraphQLSchema cachedSchema = api.getSchema(); // got from cache - generate schema is NOT called
        verify(api, times(1)).generateSchema(APILocator.systemUser());
        assertEquals(nonCachedSchema, cachedSchema);
    }

    /**
     * This test makes sure that all our GraphQLTypesProviders include the Dot prefix to avoid conflicts
     * with content type names. Exceptions: {@link QueryMetadataTypeProvider} and {@link PaginationTypeProvider}
     * We are excluding these two providers for backward compatibility
     * @throws Exception
     */
    @Test
    public void testAllGraphQLTypesProviderTypesStartWithDotPrefix() throws Exception {
        // List of allowed legacy/existing type names that do not start with 'Dot'
        final Set<String> allowedNonDotTypes = Set.of(
                "Pagination",
                "QueryMetadata"
        );

        // All providers registered in GraphqlAPIImpl
        Set<GraphQLTypesProvider> providers = new GraphqlAPIImpl().getRegisteredTypesProviders();

        Set<String> offendingTypes = new java.util.HashSet<>();
        for (GraphQLTypesProvider provider : providers) {
            //Ignoring content type providers
            if (!(provider instanceof ContentAPIGraphQLTypesProvider)){
                for (graphql.schema.GraphQLType type : provider.getTypes()) {
                    String typeName = com.dotcms.graphql.util.TypeUtil.getName(type);
                    if (!typeName.startsWith("Dot") && !allowedNonDotTypes.contains(typeName)) {
                        offendingTypes.add(typeName + " (from " + provider.getClass().getSimpleName() + ")");
                    }
                }
            }
        }
        assertTrue(
                "The following GraphQL types do not start with 'Dot':\n" +
                        String.join("\n", offendingTypes),
                offendingTypes.isEmpty()
        );
    }

    @NotNull
    private void setMockRelationshipAPI(Field relationshipField)
            throws DotDataException, DotSecurityException {
        RelationshipAPI relationshipAPI = Mockito.mock(RelationshipAPI.class);
        Mockito.when(relationshipAPI.
                getRelationshipFromField(relationshipField, APILocator.systemUser()))
                .thenReturn(null);

        RelationshipFieldGenerator relationshipFieldGenerator =
                new RelationshipFieldGenerator(relationshipAPI);

        // lets create a mocked FieldGeneratorFactory
        GraphQLFieldGeneratorFactory fieldGeneratorFactory =
                Mockito.spy(new GraphQLFieldGeneratorFactory());

        Mockito.doReturn(relationshipFieldGenerator).when(fieldGeneratorFactory)
                .getGenerator(relationshipField);

        ContentAPIGraphQLTypesProvider.INSTANCE.setFieldGeneratorFactory(fieldGeneratorFactory);
    }

    private boolean areFileassetFieldsPresent(final GraphQLObjectType objectType) {
        final List<String> fileAssetFields = list(FILEASSET_FILE_NAME_FIELD_VAR,
                FILEASSET_DESCRIPTION_FIELD_VAR, FILEASSET_FILEASSET_FIELD_VAR,
                FILEASSET_METADATA_FIELD_VAR, FILEASSET_SHOW_ON_MENU_FIELD_VAR,
                FILEASSET_SORT_ORDER_FIELD_VAR);
        return objectType.getFieldDefinitions().stream().allMatch(fieldDefinition ->
                fileAssetFields.contains(fieldDefinition.getName()));
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

    public static Field createField(final ContentType contentType, final String fieldVarName, final Class<? extends Field> fieldType,
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

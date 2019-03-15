package com.dotcms.graphql.business;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableCategoryField;
import com.dotcms.contenttype.model.field.ImmutableFileField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableImageField;
import com.dotcms.contenttype.model.field.ImmutableKeyValueField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.BiFunction;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;

import static com.dotcms.graphql.InterfaceType.CONTENT_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.FILE_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.FORM_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.KEY_VALUE_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.PAGE_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.PERSONA_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.VANITY_URL_INTERFACE_NAME;
import static com.dotcms.graphql.InterfaceType.WIDGET_INTERFACE_NAME;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(DataProviderRunner.class)
public class GraphqlAPITest {

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    @DataProvider
    public static Object[] typeTestCases() {
        return new TypeTestCase[]{

            // CREATE TYPE CASES
            new TypeTestCase.Builder()
                .operations(Collections.singletonList(GraphqlAPITest::createType))
                .baseType(BaseContentType.CONTENT)
                .contentTypeName("newContentContentType")
                .expectedGraphQLInterfaceToInherit(CONTENT_INTERFACE_NAME)
                .assertions(
                    Arrays.asList(
                        GraphqlAPITest::assertTypeCreated,
                        GraphqlAPITest::assertTypeInheritFromInterface
                    )
                )
                .build(),
            new TypeTestCase.Builder()
                .operations(Collections.singletonList(GraphqlAPITest::createType))
                .baseType(BaseContentType.WIDGET)
                .contentTypeName("newWidgetContentType")
                .expectedGraphQLInterfaceToInherit(WIDGET_INTERFACE_NAME)
                .assertions(
                    Arrays.asList(
                        GraphqlAPITest::assertTypeCreated,
                        GraphqlAPITest::assertTypeInheritFromInterface
                    )
                )
                .build(),
            new TypeTestCase.Builder()
                .operations(Collections.singletonList(GraphqlAPITest::createType))
                .baseType(BaseContentType.FORM)
                .expectedGraphQLInterfaceToInherit(FORM_INTERFACE_NAME)
                .contentTypeName("newFormContentType")
                .assertions(
                    Arrays.asList(
                        GraphqlAPITest::assertTypeCreated,
                        GraphqlAPITest::assertTypeInheritFromInterface
                    )
                )
                .build(),
            new TypeTestCase.Builder()
                .operations(Collections.singletonList(GraphqlAPITest::createType))
                .baseType(BaseContentType.FILEASSET)
                .expectedGraphQLInterfaceToInherit(FILE_INTERFACE_NAME)
                .contentTypeName("newFileContentType")
                .assertions(
                    Arrays.asList(
                        GraphqlAPITest::assertTypeCreated,
                        GraphqlAPITest::assertTypeInheritFromInterface
                    )
                )
                .build(),
            new TypeTestCase.Builder()
                .operations(Collections.singletonList(GraphqlAPITest::createType))
                .baseType(BaseContentType.HTMLPAGE)
                .expectedGraphQLInterfaceToInherit(PAGE_INTERFACE_NAME)
                .contentTypeName("newPageContentType")
                .assertions(
                    Arrays.asList(
                        GraphqlAPITest::assertTypeCreated,
                        GraphqlAPITest::assertTypeInheritFromInterface
                    )
                )
                .build(),
            new TypeTestCase.Builder()
                .operations(Collections.singletonList(GraphqlAPITest::createType))
                .baseType(BaseContentType.PERSONA)
                .expectedGraphQLInterfaceToInherit(PERSONA_INTERFACE_NAME)
                .contentTypeName("newPersonaContentType")
                .assertions(
                    Arrays.asList(
                        GraphqlAPITest::assertTypeCreated,
                        GraphqlAPITest::assertTypeInheritFromInterface
                    )
                )
                .build(),
            new TypeTestCase.Builder()
                .operations(Collections.singletonList(GraphqlAPITest::createType))
                .baseType(BaseContentType.VANITY_URL)
                .expectedGraphQLInterfaceToInherit(VANITY_URL_INTERFACE_NAME)
                .contentTypeName("newVanityURLContentType")
                .assertions(
                    Arrays.asList(
                        GraphqlAPITest::assertTypeCreated,
                        GraphqlAPITest::assertTypeInheritFromInterface
                    )
                )
                .build(),
            new TypeTestCase.Builder()
                .operations(Collections.singletonList(GraphqlAPITest::createType))
                .baseType(BaseContentType.KEY_VALUE)
                .expectedGraphQLInterfaceToInherit(KEY_VALUE_INTERFACE_NAME)
                .contentTypeName("newKeyValueContentType")
                .assertions(
                    Arrays.asList(
                        GraphqlAPITest::assertTypeCreated,
                        GraphqlAPITest::assertTypeInheritFromInterface
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
                .contentTypeName("newContentContentType")
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
                .contentTypeName("newWidgetContentType")
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
                .contentTypeName("newFormContentType")
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
                .contentTypeName("newFileContentType")
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
                .contentTypeName("newPageContentType")
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
                .contentTypeName("newPersonaContentType")
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
                .contentTypeName("newVanityURLContentType")
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
                .contentTypeName("newKeyValueContentType")
                .assertions(Collections.singletonList(GraphqlAPITest::assertTypeDeleted))
                .build(),


        };
    }


    @DataProvider
    public static Object[] fieldTestCases() {
        return new TypeTestCase[]{

            // CREATE TYPE CASES
            new TypeTestCase.Builder()
                .baseType(BaseContentType.CONTENT)
                .contentTypeName("newContentContentType")
                .setFieldVarName("testFieldVar")
                .setFieldType(ImmutableBinaryField.class)
                .setFieldRequired(false)
                .build(),

            new TypeTestCase.Builder()
                .baseType(BaseContentType.CONTENT)
                .contentTypeName("newContentContentType")
                .setFieldVarName("testFieldVar")
                .setFieldType(ImmutableCategoryField.class)
                .setFieldRequired(false)
                .build(),

            new TypeTestCase.Builder()
                .baseType(BaseContentType.CONTENT)
                .contentTypeName("newContentContentType")
                .setFieldVarName("testFieldVar")
                .setFieldType(ImmutableImageField.class)
                .setFieldRequired(false)
                .build(),

            new TypeTestCase.Builder()
                .baseType(BaseContentType.CONTENT)
                .contentTypeName("newContentContentType")
                .setFieldVarName("testFieldVar")
                .setFieldType(ImmutableFileField.class)
                .setFieldRequired(false)
                .build(),

            new TypeTestCase.Builder()
                .baseType(BaseContentType.CONTENT)
                .contentTypeName("newContentContentType")
                .setFieldVarName("testFieldVar")
                .setFieldType(ImmutableKeyValueField.class)
                .setFieldRequired(false)
                .build(),

            new TypeTestCase.Builder()
                .baseType(BaseContentType.CONTENT)
                .contentTypeName("newContentContentType")
                .setFieldVarName("testFieldVar")
                .setFieldType(ImmutableHostFolderField.class)
                .setFieldRequired(false)
                .build(),
        };
    }


    @UseDataProvider("typeTestCases")
    @Test
    public void testGetSchema_ContentTypeOperations(final TypeTestCase testCase)
        throws DotDataException, DotSecurityException {

        ContentType contentType = null;

        try {

            // contentType gets assigned to the return of the last operation
            for(BiFunction<String, BaseContentType, ContentType> operation: testCase.getOperations()) {
                contentType = operation.apply(testCase.getContentTypeName(), testCase.getBaseType());
            }

            final String contentTypeVar = contentType!=null?contentType.variable():null;

            final GraphqlAPI api = APILocator.getGraphqlAPI();
            final GraphQLSchema schema = api.getSchema();

            final TypeTestCase.AssertionParams assertionParams =
                new TypeTestCase.AssertionParams(schema, contentTypeVar, testCase.expectedGraphQLInterfaceToInherit);

            testCase.assertions.forEach((assertion)->assertion.accept(assertionParams));



        } finally {
            if(contentType!=null) {
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType);
            }
        }

    }


    @UseDataProvider("fieldTestCases")
    @Test
    public void testGetSchema_FieldOperations(final TypeTestCase testCase) throws DotDataException,
        DotSecurityException {

        // create type
        ContentType contentType = null;

        try {
            contentType = createType(testCase.contentTypeName, testCase.baseType);
            createField(contentType, testCase.fieldVarName,
                testCase.fieldType, testCase.fieldRequired);

            final GraphqlAPI api = APILocator.getGraphqlAPI();
            final GraphQLSchema schema = api.getSchema();

            final GraphQLFieldDefinition fieldDefinition =
                schema.getObjectType(testCase.contentTypeName).getFieldDefinition(testCase.fieldVarName);

            final GraphQLOutputType expectedType = api.getFieldClassGraphqlTypeMap().get(testCase.fieldType.getSuperclass());
            final GraphQLOutputType graphQLFieldType = fieldDefinition.getType();

            assertNotNull(expectedType);
            assertNotNull(graphQLFieldType);

            Assert.assertEquals("Type of GraphQL Field should match type expected", expectedType
                ,graphQLFieldType);

        } finally {
            if(contentType!=null) {
                APILocator.getContentTypeAPI(APILocator.systemUser()).delete(contentType);
            }
        }

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

            final ContentType contentType = contentTypeBuilder.name(typeName).variable(typeName).build();
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
            final Field field =  fieldBuilder.contentTypeId(contentType.id()).name(fieldVarName).variable(fieldVarName)
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
            Class.forName(packageName + ".Immutable" + baseType.immutableClass().getSimpleName());

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

        assertTrue("GraphQL Type:"+assertionParams.typeName+" inherits from:"+assertionParams.expectedInterfaceName,
            graphQLObjectType.getInterfaces().stream().anyMatch(
            (interfaceType)-> interfaceType.getName().toLowerCase().equals(assertionParams.expectedInterfaceName.toLowerCase())
        ));
    }

}

package com.dotcms.graphql.business;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;

import java.util.function.BiFunction;
import java.util.List;
import java.util.function.Consumer;

import graphql.schema.GraphQLSchema;

public class TypeTestCase {

    final List<BiFunction<String, BaseContentType, ContentType>> operations;
    final String contentTypeName;
    final String updatedContentTypeName;
    final BaseContentType baseType;
    final String expectedGraphQLTypeName;
    final String expectedGraphQLUpdatedTypeName;
    final String expectedCollectionTypeName;
    final String expectedGraphQLInterfaceToInherit;
    final List<Consumer<AssertionParams>> assertions;


    private TypeTestCase(
        List<BiFunction<String, BaseContentType, ContentType>> operations, String contentTypeName, String updatedContentTypeName,
        BaseContentType baseType, String expectedGraphQLTypeName, String expectedGraphQLUpdatedTypeName,
        String expectedCollectionTypeName, String expectedGraphQLInterfaceToInherit,
        List<Consumer<AssertionParams>> assertions) {
        this.operations = operations;
        this.contentTypeName = contentTypeName;
        this.updatedContentTypeName = updatedContentTypeName;
        this.baseType = baseType;
        this.expectedGraphQLTypeName = expectedGraphQLTypeName;
        this.expectedGraphQLUpdatedTypeName = expectedGraphQLUpdatedTypeName;
        this.expectedCollectionTypeName = expectedCollectionTypeName;
        this.expectedGraphQLInterfaceToInherit = expectedGraphQLInterfaceToInherit;
        this.assertions = assertions;
    }

    public List<BiFunction<String, BaseContentType, ContentType>> getOperations() {
        return operations;
    }

    public String getContentTypeName() {
        return contentTypeName;
    }

    public String getUpdatedContentTypeName() {
        return updatedContentTypeName;
    }

    public BaseContentType getBaseType() {
        return baseType;
    }

    public String getExpectedGraphQLTypeName() {
        return expectedGraphQLTypeName;
    }

    public String getExpectedGraphQLUpdatedTypeName() {
        return expectedGraphQLUpdatedTypeName;
    }

    public String getExpectedCollectionTypeName() {
        return expectedCollectionTypeName;
    }

    public String getExpectedGraphQLInterfaceToInherit() {
        return expectedGraphQLInterfaceToInherit;
    }

    public static class Builder {
        private List<BiFunction<String, BaseContentType, ContentType>> operations;
        private String contentTypeName;
        private String updatedContentTypeName;
        private BaseContentType baseType;
        private String expectedGraphQLTypeName;
        private String expectedGraphQLUpdatedTypeName;
        private String expectedCollectionTypeName;
        private String expectedGraphQLInterfaceToInherit;
        private List<Consumer<AssertionParams>> assertions;

        public Builder operations(List<BiFunction<String, BaseContentType, ContentType>> operations) {
            this.operations = operations;
            return this;
        }

        public Builder contentTypeName(String contentTypeName) {
            this.contentTypeName = contentTypeName;
            return this;
        }

        public Builder updatedContentTypeName(String updatedContentTypeName) {
            this.updatedContentTypeName = updatedContentTypeName;
            return this;
        }

        public Builder baseType(BaseContentType baseType) {
            this.baseType = baseType;
            return this;
        }

        public Builder expectedGraphQLTypeName(String expectedGraphQLTypeName) {
            this.expectedGraphQLTypeName = expectedGraphQLTypeName;
            return this;
        }

        public Builder expectedGraphQLUpdatedTypeName(String expectedGraphQLUpdatedTypeName) {
            this.expectedGraphQLUpdatedTypeName = expectedGraphQLUpdatedTypeName;
            return this;
        }

        public Builder expectedCollectionTypeName(String expectedCollectionTypeName) {
            this.expectedCollectionTypeName = expectedCollectionTypeName;
            return this;
        }

        public Builder expectedGraphQLInterfaceToInherit(String expectedGraphQLInterfaceToInherit) {
            this.expectedGraphQLInterfaceToInherit = expectedGraphQLInterfaceToInherit;
            return this;
        }

        public Builder assertions(List<Consumer<AssertionParams>> assertions) {
            this.assertions = assertions;
            return this;
        }

        public TypeTestCase build() {
            return new TypeTestCase(operations, contentTypeName, updatedContentTypeName, baseType, expectedGraphQLTypeName,
                expectedGraphQLUpdatedTypeName, expectedCollectionTypeName, expectedGraphQLInterfaceToInherit, assertions);
        }
    }

    static class AssertionParams {
        GraphQLSchema schema;
        String typeName;
        String expectedInterfaceName;

        AssertionParams(GraphQLSchema schema, String typeName, String baseTypeName) {
            this.schema = schema;
            this.typeName = typeName;
            this.expectedInterfaceName = baseTypeName;
        }
    }
}

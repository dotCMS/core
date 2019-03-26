package com.dotcms.graphql.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class TypeTestCase {

  final List<BiFunction<String, BaseContentType, ContentType>> operations;
  final String contentTypeName;
  final BaseContentType baseType;
  final String expectedGraphQLInterfaceToInherit;
  final List<Consumer<AssertionParams>> assertions;
  final String fieldVarName;
  final Class<? extends Field> fieldType;
  final boolean fieldRequired;

  private TypeTestCase(
      final List<BiFunction<String, BaseContentType, ContentType>> operations,
      final String contentTypeName,
      final BaseContentType baseType,
      final String expectedGraphQLInterfaceToInherit,
      final List<Consumer<AssertionParams>> assertions,
      final String fieldVarName,
      final Class<? extends Field> fieldType,
      final boolean fieldRequired) {
    this.operations = operations;
    this.contentTypeName = contentTypeName;
    this.baseType = baseType;
    this.expectedGraphQLInterfaceToInherit = expectedGraphQLInterfaceToInherit;
    this.assertions = assertions;
    this.fieldVarName = fieldVarName;
    this.fieldType = fieldType;
    this.fieldRequired = fieldRequired;
  }

  List<BiFunction<String, BaseContentType, ContentType>> getOperations() {
    return operations;
  }

  public String getContentTypeName() {
    return contentTypeName;
  }

  public BaseContentType getBaseType() {
    return baseType;
  }

  public static class Builder {
    private List<BiFunction<String, BaseContentType, ContentType>> operations;
    private String contentTypeName;
    private BaseContentType baseType;
    private String expectedGraphQLInterfaceToInherit;
    private List<Consumer<AssertionParams>> assertions;
    private String fieldVarName;
    private Class<? extends Field> fieldType;
    private boolean fieldRequired;

    public Builder setFieldVarName(String fieldVarName) {
      this.fieldVarName = fieldVarName;
      return this;
    }

    public Builder setFieldType(Class<? extends Field> fieldType) {
      this.fieldType = fieldType;
      return this;
    }

    public Builder setFieldRequired(boolean fieldRequired) {
      this.fieldRequired = fieldRequired;
      return this;
    }

    public Builder operations(List<BiFunction<String, BaseContentType, ContentType>> operations) {
      this.operations = operations;
      return this;
    }

    public Builder contentTypeName(String contentTypeName) {
      this.contentTypeName = contentTypeName;
      return this;
    }

    public Builder baseType(BaseContentType baseType) {
      this.baseType = baseType;
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
      return new TypeTestCase(
          operations,
          contentTypeName,
          baseType,
          expectedGraphQLInterfaceToInherit,
          assertions,
          fieldVarName,
          fieldType,
          fieldRequired);
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

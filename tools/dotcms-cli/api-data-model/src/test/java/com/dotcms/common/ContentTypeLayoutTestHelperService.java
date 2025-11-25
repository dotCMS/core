package com.dotcms.common;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.workflow.ImmutableWorkflow;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class ContentTypeLayoutTestHelperService {

    public static final String SYSTEM_WORKFLOW_ID = "d61a59e1-a49c-46f2-a929-db2b4bfa88b2";
    public static final String SYSTEM_WORKFLOW_VARIABLE_NAME = "SystemWorkflow";
    /**
     * Builds a content type with the specified identifier, variable, and number of columns.
     *
     * @param identifier      the identifier for the content type.
     * @param variable        the variable name for the content type.
     * @param numberOfColumns the number of columns to include in the content type.
     * @return the built content type with the specified columns.
     */
    public ImmutableSimpleContentType buildContentTypeWithColumns(final String identifier, final String variable,
                                                                  final int numberOfColumns) {
        final long millis = System.currentTimeMillis();
        final String contentTypeVariable = Objects.requireNonNullElseGet(variable,
                () -> "var_" + millis);

        var rowField = buildRowField(0);

        var columnFieldList = buildColumnFields(numberOfColumns);
        var layoutColumnFieldList = buildLayoutColumns(columnFieldList, null);

        ImmutableFieldLayoutRow fieldLayoutRow = buildFieldLayoutRow(rowField, layoutColumnFieldList);

        var builder = buildContentTypeBuilder(contentTypeVariable, rowField, columnFieldList, fieldLayoutRow);

        if (identifier != null) {
            builder.id(identifier);
        }

        return builder.build();
    }

    /**
     * Builds a content type with the specified identifier, variable, and number of columns, without a layout.
     *
     * @param identifier      the identifier for the content type.
     * @param variable        the variable name for the content type.
     * @param numberOfColumns the number of columns to include in the content type.
     * @return the built content type with the specified columns but without a layout.
     */
    public ImmutableSimpleContentType buildContentTypeWithColumnsWithoutLayout(final String identifier, final String variable,
                                                                               final int numberOfColumns) {
        final long millis = System.currentTimeMillis();
        final String contentTypeVariable = Objects.requireNonNullElseGet(variable,
                () -> "var_" + millis);

        var rowField = buildRowField(0);

        var columnFieldList = buildColumnFields(numberOfColumns);

        var builder = buildContentTypeBuilderWithoutLayout(contentTypeVariable, rowField, columnFieldList);

        if (identifier != null) {
            builder.id(identifier);
        }

        return builder.build();
    }

    /**
     * Builds a column field with the specified index.
     *
     * @param index the index of the column field.
     * @return the built column field.
     */
    public ImmutableColumnField buildColumnField(int index) {
        return ImmutableColumnField.builder()
                .searchable(false)
                .unique(false)
                .indexed(false)
                .listed(false)
                .readOnly(false)
                .forceIncludeInApi(false)
                .name("fields-" + index)
                .required(false)
                .sortOrder(index)
                .fixed(false)
                .fieldType("Column")
                .fieldTypeLabel("Column")
                .variable("fields" + index)
                .dataType(DataTypes.SYSTEM)
                .build();
    }

    /**
     * Builds a row field with the specified index.
     *
     * @param index the index of the row field.
     * @return the built row field.
     */
    public RowField buildRowField(int index) {
        return ImmutableRowField.builder()
                .searchable(false)
                .unique(false)
                .indexed(false)
                .listed(false)
                .readOnly(false)
                .forceIncludeInApi(false)
                .name("fields-" + index)
                .required(false)
                .sortOrder(index)
                .fixed(false)
                .fieldType("Row")
                .fieldTypeLabel("Row")
                .variable("fields" + index)
                .dataType(DataTypes.SYSTEM)
                .build();
    }

    /**
     * Builds a list of layout columns for the specified column fields.
     *
     * @param columnFieldList the list of column fields to include in the layout.
     * @return the list of built layout columns.
     */
    public List<ImmutableFieldLayoutColumn> buildLayoutColumns(List<ImmutableColumnField> columnFieldList, List<ImmutableTextField> fieldList) {
        var layoutColumnFieldList = new ArrayList<ImmutableFieldLayoutColumn>();
        for (var columnField : columnFieldList) {
            var layoutColumn = ImmutableFieldLayoutColumn.builder()
                    .columnDivider(columnField)
                    .fields(fieldList)
                    .build();
            layoutColumnFieldList.add(layoutColumn);
        }
        return layoutColumnFieldList;
    }

    /**
     * Builds a field layout row with the specified row field and layout columns.
     *
     * @param rowField             the row field for the layout row.
     * @param layoutColumnFieldList the list of layout columns for the layout row.
     * @return the built field layout row.
     */
    public ImmutableFieldLayoutRow buildFieldLayoutRow(RowField rowField, List<ImmutableFieldLayoutColumn> layoutColumnFieldList) {
        return ImmutableFieldLayoutRow.builder()
                .divider(rowField)
                .addAllColumns(layoutColumnFieldList)
                .build();
    }
    /**
     * Builds a list of column fields for the specified number of columns.
     *
     * @param numberOfColumns the number of columns to build.
     * @return the list of built column fields.
     */
    public List<ImmutableColumnField> buildColumnFields(int numberOfColumns) {
        var columnFieldList = new ArrayList<ImmutableColumnField>();
        for (int i = 1; i <= numberOfColumns; i++) {
            var columnField = buildColumnField(i);
            columnFieldList.add(columnField);
        }
        return columnFieldList;
    }


    /**
     * Builds a list of text fields for the specified number.
     *
     * @param prefix the prefix of the name and variable of the field.
     * @param numberOfFields the number of fields to build.
     * @return the list of built column fields.
     */
    public List<ImmutableTextField> buildTextFields(String prefix, int numberOfFields) {
        var textFieldList = new ArrayList<ImmutableTextField>();
        for (int i = 1; i <= numberOfFields; i++) {
            var textField = buildTextField(prefix, i);
            textFieldList.add(textField);
        }
        return textFieldList;
    }
    /**
     * Builds a text field with the specified prefix and index.
     *
     * @param prefix the prefix of the name and variable of the field.
     * @param index the index of the column field.
     * @return the built column field.
     */
    public ImmutableTextField buildTextField(String prefix, int index) {
        return ImmutableTextField.builder()
                .name(prefix + "__text_" + index)
                .variable(prefix + "__text_" + index + "_" + System.nanoTime())
                .build();
    }


    /**
     * Builds the content type builder with the specified parameters.
     *
     * @param contentTypeVariable the variable name for the content type.
     * @param rowField            the row field for the content type.
     * @param columnFieldList     the list of column fields for the content type.
     * @param fieldLayoutRow      the layout row for the content type.
     * @return the content type builder.
     */
    public ImmutableSimpleContentType.Builder buildContentTypeBuilder(String contentTypeVariable,
                                                                       RowField rowField,
                                                                       List<ImmutableColumnField> columnFieldList,
                                                                       FieldLayoutRow fieldLayoutRow) {
        return ImmutableSimpleContentType.builder()
                .baseType(BaseContentType.CONTENT)
                .description("ct for testing.")
                .name("name-" + contentTypeVariable)
                .variable(contentTypeVariable)
                .modDate(new Date())
                .fixed(false)
                .iDate(new Date())
                .host("SYSTEM_HOST")
                .folder("SYSTEM_FOLDER")
                .folderPath("/")
                .icon("360")
                .description("test")
                .defaultType(false)
                .system(false)
                .addFields(rowField)
                .addAllFields(columnFieldList)
                .addLayout(fieldLayoutRow)
                .workflows(
                        List.of(
                                ImmutableWorkflow.builder()
                                        .id(SYSTEM_WORKFLOW_ID)
                                        .variableName(SYSTEM_WORKFLOW_VARIABLE_NAME)
                                        .build()
                        )
                );
    }

    /**
     * Builds the content type builder without a layout with the specified parameters.
     *
     * @param contentTypeVariable the variable name for the content type.
     * @param rowField            the row field for the content type.
     * @param columnFieldList     the list of column fields for the content type.
     * @return the content type builder without a layout.
     */
    public ImmutableSimpleContentType.Builder buildContentTypeBuilderWithoutLayout(String contentTypeVariable,
                                                                                    RowField rowField,
                                                                                    List<ImmutableColumnField> columnFieldList) {
        return ImmutableSimpleContentType.builder()
                .baseType(BaseContentType.CONTENT)
                .description("ct for testing.")
                .name("name-" + contentTypeVariable)
                .variable(contentTypeVariable)
                .modDate(new Date())
                .fixed(false)
                .iDate(new Date())
                .host("SYSTEM_HOST")
                .folder("SYSTEM_FOLDER")
                .folderPath("/")
                .icon("360")
                .description("test")
                .defaultType(false)
                .system(false)
                .addFields(rowField)
                .addAllFields(columnFieldList)
                .workflows(
                        List.of(
                                ImmutableWorkflow.builder()
                                        .id(SYSTEM_WORKFLOW_ID)
                                        .variableName(SYSTEM_WORKFLOW_VARIABLE_NAME)
                                        .build()
                        )
                );
    }
}

package com.dotcms.cli.common;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.workflow.ImmutableWorkflow;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.contenttype.AbstractSaveContentTypeRequest;
import com.dotcms.model.contenttype.SaveContentTypeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

@ApplicationScoped
public class ContentTypesTestHelperService {

    private static final Duration MAX_WAIT_TIME = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    public static final String SYSTEM_WORKFLOW_ID = "d61a59e1-a49c-46f2-a929-db2b4bfa88b2";
    public static final String SYSTEM_WORKFLOW_VARIABLE_NAME = "SystemWorkflow";

    @Inject
    RestClientFactory clientFactory;

    /**
     * This method creates a content type on the server
     *
     * @return The variable name of the created content type
     */
    public String createContentTypeOnServer() {
        return createContentTypeOnServer(null, null);
    }

    /**
     * This method creates a content type on the server
     *
     * @param detailPage    The detail page of the content type
     * @param urlMapPattern The URL map pattern of the content type
     * @return The variable name of the created content type
     */
    public String createContentTypeOnServer(final String detailPage, final String urlMapPattern) {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        final long identifier = System.currentTimeMillis();
        final String varName = "var_" + identifier;

        final ImmutableSimpleContentType contentType = buildContentType(
                null, null, detailPage, urlMapPattern
        );

        final SaveContentTypeRequest saveRequest = AbstractSaveContentTypeRequest.builder()
                .of(contentType).build();
        contentTypeAPI.createContentTypes(List.of(saveRequest));

        return varName;
    }

    /**
     * Creates a content type descriptor in the given workspace.
     *
     * @param workspace The workspace in which the content type descriptor should be created.
     * @return The result of the content type descriptor creation.
     * @throws IOException If an error occurs while writing the content type descriptor to disk.
     */
    public ContentTypeDescriptorCreationResult createContentTypeDescriptor(Workspace workspace)
            throws IOException {
        return createContentTypeDescriptor(workspace, null, null, null, null);
    }

    /**
     * Creates a content type descriptor in the given workspace with a given detail page and URL map
     * pattern.
     *
     * @param workspace     The workspace in which the content type descriptor should be created.
     * @param detailPage    The detail page of the content type.
     * @param urlMapPattern The URL map pattern of the content type.
     * @return The result of the content type descriptor creation.
     * @throws IOException If an error occurs while writing the content type descriptor to disk.
     */
    public ContentTypeDescriptorCreationResult createContentTypeDescriptorWithDetailData(
            Workspace workspace, final String detailPage, final String urlMapPattern)
            throws IOException {

        return createContentTypeDescriptor(
                workspace, null, null, detailPage, urlMapPattern
        );
    }

    /**
     * Creates a content type descriptor in the given workspace with a given identifier and
     * variable.
     *
     * @param workspace  The workspace in which the content type descriptor should be created.
     * @param identifier The identifier of the content type.
     * @param variable   The variable of the content type.
     * @return The result of the content type descriptor creation.
     * @throws IOException If an error occurs while writing the content type descriptor to disk.
     */
    public ContentTypeDescriptorCreationResult createContentTypeDescriptorWithIdData(
            Workspace workspace, final String identifier, final String variable)
            throws IOException {

        return createContentTypeDescriptor(
                workspace, identifier, variable, null, null
        );
    }

    /**
     * Creates a content type descriptor in the given workspace.
     *
     * @param workspace     The workspace in which the content type descriptor should be created.
     * @param identifier    The identifier of the content type.
     * @param variable      The variable of the content type.
     * @param detailPage    The detail page of the content type.
     * @param urlMapPattern The URL map pattern of the content type.
     * @return The result of the content type descriptor creation.
     * @throws IOException If an error occurs while writing the content type descriptor to disk.
     */
    public ContentTypeDescriptorCreationResult createContentTypeDescriptor(
            Workspace workspace, final String identifier, final String variable,
            final String detailPage, final String urlMapPattern) throws IOException {

        final ImmutableSimpleContentType contentType = buildContentType(
                identifier, variable, detailPage, urlMapPattern
        );

        final ObjectMapper objectMapper = new ClientObjectMapper().getContext(null);
        final String asString = objectMapper.writeValueAsString(contentType);

        final Path path = Path.of(workspace.contentTypes().toString(),
                String.format("%s.json", contentType.variable()));
        Files.writeString(path, asString);

        return new ContentTypeDescriptorCreationResult(contentType.variable(), path);
    }

    /**
     * Builds a content type object.
     *
     * @param identifier    The identifier of the content type.
     * @param variable      The variable of the content type.
     * @param detailPage    The detail page of the content type.
     * @param urlMapPattern The URL map pattern of the content type.
     * @return The content type object.
     */
    private ImmutableSimpleContentType buildContentType(final String identifier,
            final String variable, final String detailPage, final String urlMapPattern) {

        final long millis = System.currentTimeMillis();
        final String contentTypeVariable = Objects.requireNonNullElseGet(variable,
                () -> "var_" + millis);

        var builder = ImmutableSimpleContentType.builder()
                .baseType(BaseContentType.CONTENT)
                .description("ct for testing.")
                .name("name-" + contentTypeVariable)
                .variable(contentTypeVariable)
                .modDate(new Date())
                .fixed(true)
                .iDate(new Date())
                .host("SYSTEM_HOST")
                .folder("SYSTEM_FOLDER")
                .detailPage(detailPage)
                .urlMapPattern(urlMapPattern)
                .addFields(
                        ImmutableBinaryField.builder()
                                .name("__bin_var__" + millis)
                                .fixed(false)
                                .listed(true)
                                .searchable(true)
                                .unique(false)
                                .indexed(true)
                                .readOnly(false)
                                .forceIncludeInApi(false)
                                .modDate(new Date())
                                .required(false)
                                .variable("lol")
                                .sortOrder(1)
                                .dataType(DataTypes.SYSTEM).build(),
                        ImmutableTextField.builder()
                                .indexed(true)
                                .dataType(DataTypes.TEXT)
                                .fieldType("text")
                                .readOnly(false)
                                .required(true)
                                .searchable(true)
                                .listed(true)
                                .sortOrder(2)
                                .searchable(true)
                                .name("Name")
                                .variable("name")
                                .fixed(false)
                                .build()
                )
                .workflows(
                        List.of(
                                ImmutableWorkflow.builder()
                                        .id(SYSTEM_WORKFLOW_ID)
                                        .variableName(SYSTEM_WORKFLOW_VARIABLE_NAME)
                                        .build()
                        )
                );

        if (identifier != null) {
            builder.id(identifier);
        }

        return builder.build();
    }

    /**
     * Searches for a content type by its variable.
     *
     * @param variable The variable of the content type.
     * @return The content type if found, otherwise an empty optional.
     */
    public Optional<ContentType> findContentType(final String variable) {

        try {

            final AtomicReference<ContentType> contentTypeRef = new AtomicReference<>();

            await()
                    .atMost(MAX_WAIT_TIME)
                    .pollInterval(POLL_INTERVAL)
                    .until(() -> {
                        try {
                            var response = findContentTypeByVariable(variable);
                            if (response != null && response.entity() != null) {
                                contentTypeRef.set(response.entity());
                                return true;
                            }

                            return false;
                        } catch (NotFoundException e) {
                            return false;
                        }
                    });

            ContentType contentType = contentTypeRef.get();
            if (contentType != null) {
                return Optional.of(contentType);
            } else {
                return Optional.empty();
            }
        } catch (ConditionTimeoutException ex) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves a content type by its variable.
     *
     * @param variable The variable of the content type.
     * @return The ResponseEntityView containing the content type.
     */
    @ActivateRequestContext
    public ResponseEntityView<ContentType> findContentTypeByVariable(final String variable) {

        final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        // Execute the REST call to retrieve folder contents
        return contentTypeAPI.getContentType(
                variable, null, null
        );
    }

    /**
     * Represents the result of a Content Type descriptor creation.
     */
    public static class ContentTypeDescriptorCreationResult {

        private final String variable;
        private final Path path;

        public ContentTypeDescriptorCreationResult(final String variable, final Path path) {
            this.variable = variable;
            this.path = path;
        }

        public String variable() {
            return variable;
        }

        public Path path() {
            return path;
        }
    }


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
        var layoutColumnFieldList = buildLayoutColumns(columnFieldList);

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
    private ImmutableColumnField buildColumnField(int index) {
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
    private RowField buildRowField(int index) {
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
     * Builds a list of column fields for the specified number of columns.
     *
     * @param numberOfColumns the number of columns to build.
     * @return the list of built column fields.
     */
    private List<ImmutableColumnField> buildColumnFields(int numberOfColumns) {
        var columnFieldList = new ArrayList<ImmutableColumnField>();
        for (int i = 1; i <= numberOfColumns; i++) {
            var columnField = buildColumnField(i);
            columnFieldList.add(columnField);
        }
        return columnFieldList;
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
    private ImmutableSimpleContentType.Builder buildContentTypeBuilder(String contentTypeVariable,
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
    private ImmutableSimpleContentType.Builder buildContentTypeBuilderWithoutLayout(String contentTypeVariable,
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

    /**
     * Builds a list of layout columns for the specified column fields.
     *
     * @param columnFieldList the list of column fields to include in the layout.
     * @return the list of built layout columns.
     */
    private List<ImmutableFieldLayoutColumn> buildLayoutColumns(List<ImmutableColumnField> columnFieldList) {
        var layoutColumnFieldList = new ArrayList<ImmutableFieldLayoutColumn>();
        for (var columnField : columnFieldList) {
            var layoutColumn = ImmutableFieldLayoutColumn.builder()
                    .columnDivider(columnField)
                    .fields(List.of())
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
    private ImmutableFieldLayoutRow buildFieldLayoutRow(RowField rowField, List<ImmutableFieldLayoutColumn> layoutColumnFieldList) {
        return ImmutableFieldLayoutRow.builder()
                .divider(rowField)
                .addAllColumns(layoutColumnFieldList)
                .build();
    }

}

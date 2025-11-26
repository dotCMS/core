package com.dotcms.cli.common;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.workflow.ImmutableWorkflow;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.contenttype.AbstractSaveContentTypeRequest;
import com.dotcms.model.contenttype.SaveContentTypeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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

        final ImmutableSimpleContentType contentType = buildContentType(
                null, null, detailPage, urlMapPattern
        );

        final SaveContentTypeRequest saveRequest = AbstractSaveContentTypeRequest.builder()
                .of(contentType).build();
        contentTypeAPI.createContentTypes(List.of(saveRequest));

        // Make sure the content type is created, and we are giving the server some time to process
        findContentType(contentType.variable()).orElseThrow(() -> new RuntimeException(
                "Content type not found after creation: " + contentType.variable()
        ));

        return contentType.variable();
    }

    /**
     * Creates a HTML page content type descriptor in the given workspace.
     *
     * @param workspace The workspace in which the content type descriptor should be created.
     * @return The result of the content type descriptor creation.
     * @throws IOException If an error occurs while writing the content type descriptor to disk.
     */
    public ContentTypeDescriptorCreationResult createPageContentTypeDescriptor(Workspace workspace)
            throws IOException {
        return createPageContentTypeDescriptor(workspace, null, null);
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
     * Creates a HTML page content type descriptor in the given workspace.
     *
     * @param workspace  The workspace in which the content type descriptor should be created.
     * @param identifier The identifier of the content type.
     * @param variable   The variable of the content type.
     * @return The result of the content type descriptor creation.
     * @throws IOException If an error occurs while writing the content type descriptor to disk.
     */
    public ContentTypeDescriptorCreationResult createPageContentTypeDescriptor(
            Workspace workspace, final String identifier, final String variable)
            throws IOException {

        final ImmutablePageContentType contentType = buildPageContentType(
                identifier, variable
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
     * Builds a HTML page content type object.
     *
     * @param identifier The identifier of the content type.
     * @param variable   The variable of the content type.
     * @return The content type object.
     */
    private ImmutablePageContentType buildPageContentType(
            final String identifier, final String variable) {

        final long millis = System.currentTimeMillis();
        final String contentTypeVariable = Objects.requireNonNullElseGet(variable,
                () -> "var_" + millis);

        var builder = ImmutablePageContentType.builder()
                .baseType(BaseContentType.HTMLPAGE)
                .description("ct for testing.")
                .name("name-" + contentTypeVariable)
                .variable(contentTypeVariable)
                .modDate(new Date())
                .fixed(true)
                .iDate(new Date())
                .host("SYSTEM_HOST")
                .folder("SYSTEM_FOLDER")
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

}
